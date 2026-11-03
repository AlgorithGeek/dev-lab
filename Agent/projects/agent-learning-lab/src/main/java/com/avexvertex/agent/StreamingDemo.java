package com.avexvertex.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

public class StreamingDemo {
    public static void main(String[] args) {
        // 留空看实时正文；--debug 额外打印实际收到的 SSE payload，方便对照协议。
        if (args.length > 1 || (args.length == 1 && !"--debug".equals(args[0]))) {
            System.err.println("请清空程序实参，或填写 --debug 查看原始 SSE 数据。");
            System.exit(1);
            return;
        }
        boolean debug = args.length == 1;
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        String baseUrl = System.getenv("DASHSCOPE_BASE_URL");
        if (apiKey == null || apiKey.isBlank() || baseUrl == null || baseUrl.isBlank()) {
            System.err.println("缺少 DASHSCOPE_API_KEY 或 DASHSCOPE_BASE_URL，请在 IDEA 当前运行配置中填写。");
            System.exit(1);
            return;
        }

        URI endpoint;
        try {
            endpoint = URI.create(baseUrl.strip().replaceAll("/+$", "") + "/chat/completions");
            if (!"https".equalsIgnoreCase(endpoint.getScheme()) || endpoint.getHost() == null
                    || endpoint.getUserInfo() != null || endpoint.getQuery() != null
                    || endpoint.getFragment() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            System.err.println("DASHSCOPE_BASE_URL 必须是完整 HTTPS 基础地址，不带查询参数或片段。");
            System.exit(1);
            return;
        }

        /*
         * 第 1 步：普通文本请求，开启 Streaming；本次不携带 Schema 或 DTO。
         * {
         *   "model": "qwen3.8-max",
         *   "messages": [
         *     {"role":"user","content":"请用 3 到 5 句话解释 Java HashMap 为什么不是线程安全的。"}
         *   ],
         *   "stream": true,
         *   "stream_options": {"include_usage": true},
         *   "reasoning_effort": "none"
         * }
         */
        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", "请用 3 到 5 句话解释 Java HashMap 为什么不是线程安全的。");
        JSONArray messages = new JSONArray();
        messages.add(user);
        JSONObject streamOptions = new JSONObject();
        streamOptions.put("include_usage", true); // 请求最后返回 Token Usage，不能把缺失值当成 0。
        JSONObject body = new JSONObject();
        body.put("model", "qwen3.8-max");
        body.put("messages", messages);
        body.put("stream", true);
        body.put("stream_options", streamOptions);
        body.put("reasoning_effort", "none");
        System.out.println("Request Body: " + JSON.toJSONString(body));

        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()) {
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(120))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body), StandardCharsets.UTF_8))
                    .build();

            // 第 2 步：从发出请求前开始计时。ofInputStream() 让我们在 Body 未收完时开始读。
            long startNanos = System.nanoTime();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            System.out.println("HTTP Status: " + response.statusCode());
            try (InputStream stream = response.body()) {
                // 错误响应可能是普通 JSON 或文本，必须先检查状态，不能直接按 SSE 解析。
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    System.out.println("Error Response: " + new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                    throw new IllegalStateException("HTTP 请求未成功，已停止。");
                }
                String contentType = response.headers().firstValue("Content-Type").orElse("");
                if (!contentType.toLowerCase(Locale.ROOT).startsWith("text/event-stream")) {
                    throw new IllegalStateException("成功响应不是 text/event-stream，请检查服务端流式支持。");
                }
                // UTF-8 解码器处理中文跨网络片段的情况；readLine() 得到文本行，不是一个 Token。
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                readStream(reader, debug, startNanos);
            }
        } catch (JSONException e) {
            System.err.println("\nSSE 数据不是有效 JSON，已停止；此前实时输出不能视为完整回答。");
            System.exit(1);
        } catch (IllegalStateException e) {
            System.err.println("\n" + e.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("请求构造失败，请检查环境变量格式，密钥不能包含换行等非法字符。");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("\n请求或读流失败（" + e.getClass().getSimpleName()
                    + "），此前输出可能只是片段，请检查网络或稍后重试。");
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("\n请求被中断，已停止。");
            System.exit(1);
        }
    }

    private static void readStream(BufferedReader reader, boolean debug, long startNanos) throws IOException {
        StringBuilder eventData = new StringBuilder(); // 暂存一个 SSE 事件的 data 行。
        StringBuilder fullContent = new StringBuilder(); // 暂存同一条回答的所有正文增量。
        String model = null;
        String finishReason = null;
        JSONObject usage = null;
        String firstContent = null;
        long firstContentNanos = 0;
        boolean done = false;
        System.out.println("实时正文:");

        /*
         * SSE 示例（示意数据，不是实际模型回复）：
         * data: {"model":"qwen3.8-max","choices":[{"delta":{"role":"assistant","content":""},"finish_reason":null}]}
         * <空行>
         * data: {"choices":[{"delta":{"content":"HashMap"},"finish_reason":null}]}
         * <空行>
         * data: {"choices":[{"delta":{},"finish_reason":"stop"}]}
         * <空行>
         * data: {"choices":[],"usage":{"prompt_tokens":30,"completion_tokens":20,"total_tokens":50}}
         * <空行>
         * data: [DONE]
         * <空行>
         * data: 和空行是协议边界；只有普通 payload 才交给 FastJSON2。
         */
        // shortcut: 本例未单独实现流读取的空闲超时，卡住时可在 IDEA 停止；生产接入时补充超时与取消。
        // HttpRequest.timeout 不能被当成整个 InputStream 读取阶段的总时限。
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("data:")) {
                String data = line.substring(5); // 去掉 data:；协议允许冒号后有一个空格。
                if (data.startsWith(" ")) {
                    data = data.substring(1);
                }
                if (!eventData.isEmpty()) {
                    eventData.append('\n'); // 一个事件有多条 data 行时，要先合并，不能逐行当 JSON。
                }
                eventData.append(data);
                continue;
            }
            // 空行才结束一个事件；忽略注释、event/id 等非 data 行，不把它们交给 JSON 解析。
            if (!line.isEmpty() || eventData.isEmpty()) {
                continue;
            }
            String payload = eventData.toString().strip();
            eventData.setLength(0);
            if (debug) {
                System.out.println("\n[SSE] " + payload);
            }
            // 第 3 步：先识别结束标记，它不是 JSON；不能看到 finish_reason 就提前结束读流。
            if ("[DONE]".equals(payload)) {
                done = true;
                break;
            }
            JSONObject chunk = JSON.parseObject(payload);
            if (chunk == null || chunk.containsKey("error")) {
                throw new IllegalStateException("收到空 chunk 或 SSE 错误事件，流式回答未成功完成。");
            }
            if (chunk.get("model") != null) {
                if (!(chunk.get("model") instanceof String returnedModel) || returnedModel.isBlank()) {
                    throw new IllegalStateException("SSE 的 model 字段无效。");
                }
                model = returnedModel;
            }

            // 第 4 步：先保存 Usage。它可能位于 choices=[] 的独立事件，不可以直接取第 0 项。
            if (chunk.get("usage") != null) {
                if (!(chunk.get("usage") instanceof JSONObject returnedUsage)) {
                    throw new IllegalStateException("SSE 的 usage 必须是对象。");
                }
                for (String field : new String[]{"prompt_tokens", "completion_tokens", "total_tokens"}) {
                    Object value = returnedUsage.get(field);
                    if (!(value instanceof Integer || value instanceof Long) || returnedUsage.getLongValue(field) < 0) {
                        throw new IllegalStateException("SSE 的 usage." + field + " 必须是非负整数。");
                    }
                }
                usage = returnedUsage;
            }
            if (chunk.get("choices") == null && chunk.get("usage") != null) {
                continue;
            }
            if (!(chunk.get("choices") instanceof JSONArray choices)) {
                throw new IllegalStateException("SSE 的 choices 必须是数组。");
            }
            if (choices.isEmpty()) {
                continue;
            }
            if (!(choices.get(0) instanceof JSONObject choice)) {
                throw new IllegalStateException("SSE 的 choices[0] 必须是对象。");
            }
            if (choice.get("finish_reason") != null) {
                if (!(choice.get("finish_reason") instanceof String reason) || reason.isBlank()) {
                    throw new IllegalStateException("SSE 的 finish_reason 无效。");
                }
                finishReason = reason; // 只记录原因，继续等待 Usage 与 [DONE]。
            }
            if (choice.get("delta") == null && choice.get("finish_reason") != null) {
                continue;
            }
            if (!(choice.get("delta") instanceof JSONObject delta)) {
                throw new IllegalStateException("SSE 的 delta 必须是对象。");
            }
            // 第 5 步：角色块的 content 可以缺失或为空；正文中的空格则不能用 isBlank() 丢掉。
            if (delta.get("content") == null) {
                continue;
            }
            if (!(delta.get("content") instanceof String content)) {
                throw new IllegalStateException("SSE 的 delta.content 必须是文本。");
            }
            if (!content.isEmpty()) {
                if (firstContentNanos == 0) {
                    firstContentNanos = System.nanoTime(); // 只记首个非空正文，不把空角色块记作 TTFT。
                    firstContent = content;
                }
                System.out.print(content);
                System.out.flush(); // 及时刷新控制台，不等拼接完才展示。
                fullContent.append(content);
            }
        }
        long endNanos = System.nanoTime();
        System.out.println();
        // 第 6 步：EOF 不等于成功。没有结束标记时，前面的实时文字只算部分结果。
        if (!done) {
            throw new IllegalStateException("流提前结束，未收到 [DONE]；上面输出仅为片段。");
        }
        if (firstContentNanos == 0 || model == null || finishReason == null || usage == null) {
            throw new IllegalStateException("流已结束，但缺少正文、Model、Finish Reason 或 Usage，不能记为实验完成。");
        }
        System.out.println("Full Content（已拼接正文）:");
        System.out.println(fullContent);
        System.out.println("Model: " + model);
        System.out.println("First Content: " + JSON.toJSONString(firstContent));
        System.out.println("TTFT（首个非空正文，ms）: " + (firstContentNanos - startNanos) / 1_000_000.0);
        System.out.println("Total Time（收到 DONE，ms）: " + (endNanos - startNanos) / 1_000_000.0);
        System.out.println("Finish Reason: " + finishReason);
        System.out.println("Prompt Tokens: " + usage.getLongValue("prompt_tokens"));
        System.out.println("Completion Tokens: " + usage.getLongValue("completion_tokens"));
        System.out.println("Total Tokens: " + usage.getLongValue("total_tokens"));
        System.out.println("[DONE] received: true");
    }
}
