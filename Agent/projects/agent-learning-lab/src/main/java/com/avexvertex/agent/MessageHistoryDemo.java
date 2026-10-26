package com.avexvertex.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class MessageHistoryDemo {
    public static void main(String[] args) {
        if (args.length > 1 || (args.length == 1
                && !"--compare".equals(args[0]) && !"--history".equals(args[0]))) {
            System.err.println("程序参数只能是 --compare 或 --history；不填默认执行 A/B 对照。");
            System.exit(1);
            return;
        }
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        String baseUrl = System.getenv("DASHSCOPE_BASE_URL");
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("缺少 DASHSCOPE_API_KEY，请在 IDEA 运行配置的环境变量中填写。");
            System.exit(1);
            return;
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            System.err.println("缺少 DASHSCOPE_BASE_URL，请在 IDEA 运行配置的环境变量中填写。");
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
            System.err.println("DASHSCOPE_BASE_URL 格式错误，请填写完整 HTTPS Base URL，不带查询参数或片段。");
            System.exit(1);
            return;
        }

        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build()) {
            if (args.length == 1 && "--history".equals(args[0])) {
                runHistory(client, endpoint, apiKey);
            } else {
                runCompare(client, endpoint, apiKey);
            }
        } catch (JSONException e) {
            System.err.println("响应 JSON 解析失败，实验已停止。");
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("请求构造失败，请检查环境变量格式，API Key 中不能包含换行等非法字符。");
            System.exit(1);
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("HTTP 请求失败（" + e.getClass().getSimpleName()
                    + "），请检查网络和 Base URL，或稍后重试。");
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("HTTP 请求已被中断。");
            System.exit(1);
        }
    }

    private static void runCompare(HttpClient client, URI endpoint, String apiKey)
            throws IOException, InterruptedException {
        String firstQuestion = "本次对话的实验代号是青柠47。请只回复“收到”。";
        String secondQuestion = "刚才我告诉你的实验代号是什么？如果当前对话信息不足，请回答“无法确定”。";

        /*
         * 第 1 次请求（seed）：告诉模型实验代号。发送的 JSON：
         * {
         *   "model": "qwen3.8-max",
         *   "messages": [
         *     {"role": "user", "content": "本次对话的实验代号是青柠47。请只回复“收到”。"}
         *   ],
         *   "reasoning_effort": "none"
         * }
         */
        JSONArray seedMessages = new JSONArray();
        seedMessages.add(message("user", firstQuestion));
        String firstAnswer = callModel(client, endpoint, apiKey, "seed", seedMessages);

        /*
         * 第 2 次请求（A，无历史）：只发送当前问题，JSON 中没有实验代号。
         * {
         *   "model": "qwen3.8-max",
         *   "messages": [
         *     {"role": "user", "content": "刚才我告诉你的实验代号是什么？如果当前对话信息不足，请回答“无法确定”。"}
         *   ],
         *   "reasoning_effort": "none"
         * }
         */
        JSONArray withoutHistory = new JSONArray();
        withoutHistory.add(message("user", secondQuestion));
        callModel(client, endpoint, apiKey, "A（无历史）", withoutHistory);

        /*
         * 第 3 次请求（B，有历史）：第一轮 User + 第一轮实际 Assistant + 当前问题。
         * {
         *   "model": "qwen3.8-max",
         *   "messages": [
         *     {"role": "user", "content": "本次对话的实验代号是青柠47。请只回复“收到”。"},
         *     {"role": "assistant", "content": "<seed 实际返回的 firstAnswer>"},
         *     {"role": "user", "content": "刚才我告诉你的实验代号是什么？如果当前对话信息不足，请回答“无法确定”。"}
         *   ],
         *   "reasoning_effort": "none"
         * }
         * 上面的 <...> 仅为注释占位；实际发送的是 firstAnswer 的内容，不是这段占位文字。
         * A 的回答没有放进 B，A 和 B 是两个独立的对照分支。
         */
        JSONArray withHistory = new JSONArray();
        withHistory.add(message("user", firstQuestion));
        withHistory.add(message("assistant", firstAnswer));
        withHistory.add(message("user", secondQuestion));
        callModel(client, endpoint, apiKey, "B（有历史）", withHistory);
    }

    private static void runHistory(HttpClient client, URI endpoint, String apiKey)
            throws IOException, InterruptedException {
        String[] questions = {
                "本次实验的变量名是 alpha-21。请回复“收到”。",
                "刚才我告诉你的变量名是什么？",
                "你上一轮回答了什么？请复述原话。"
        };
        JSONArray history = new JSONArray();

        /*
         * 循环中的三次请求如下。<第 N 轮实际回答> 只是注释占位，运行时取自模型响应。
         * 第 1 轮：没有历史，只发送当前问题。
         * {
         *   "model": "qwen3.8-max",
         *   "messages": [
         *     {"role": "user", "content": "本次实验的变量名是 alpha-21。请回复“收到”。"}
         *   ],
         *   "reasoning_effort": "none"
         * }
         * 第 2 轮：带上第 1 轮实际问答，再加当前问题。
         * {
         *   "model": "qwen3.8-max",
         *   "messages": [
         *     {"role": "user", "content": "本次实验的变量名是 alpha-21。请回复“收到”。"},
         *     {"role": "assistant", "content": "<第 1 轮实际回答>"},
         *     {"role": "user", "content": "刚才我告诉你的变量名是什么？"}
         *   ],
         *   "reasoning_effort": "none"
         * }
         * 第 3 轮：带上前两轮实际问答，再加当前问题。
         * {
         *   "model": "qwen3.8-max",
         *   "messages": [
         *     {"role": "user", "content": "本次实验的变量名是 alpha-21。请回复“收到”。"},
         *     {"role": "assistant", "content": "<第 1 轮实际回答>"},
         *     {"role": "user", "content": "刚才我告诉你的变量名是什么？"},
         *     {"role": "assistant", "content": "<第 2 轮实际回答>"},
         *     {"role": "user", "content": "你上一轮回答了什么？请复述原话。"}
         *   ],
         *   "reasoning_effort": "none"
         * }
         */
        try {
            for (int round = 0; round < questions.length; round++) {
                // 先复制有效历史，失败时不会把本轮 User 留在主历史里。
                JSONArray messages = new JSONArray();
                messages.addAll(history);
                messages.add(message("user", questions[round]));

                String answer = callModel(client, endpoint, apiKey, "history 第 " + (round + 1) + " 轮", messages);

                // HTTP 和响应校验都成功后，才保存本轮完整的 User + Assistant。
                messages.add(message("assistant", answer));
                history = messages;
                System.out.println("本轮成功后历史条数: " + history.size());
            }
        } finally {
            System.out.println("已保存历史条数: " + history.size());
            System.out.println("已保存历史 JSON: " + JSON.toJSONString(history));
        }
    }

    private static JSONObject message(String role, String content) {
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private static String callModel(HttpClient client, URI endpoint, String apiKey,
                                    String label, JSONArray messages)
            throws IOException, InterruptedException {
        JSONObject body = new JSONObject();
        body.put("model", "qwen3.8-max");
        body.put("messages", messages);
        body.put("reasoning_effort", "none");

        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body)))
                .build();

        System.out.println("\n=== " + label + " ===");
        System.out.println("Messages: " + JSON.toJSONString(messages));
        JSONArray roles = new JSONArray();
        for (Object item : messages) {
            roles.add(((JSONObject) item).getString("role"));
        }
        System.out.println("Roles: " + JSON.toJSONString(roles));
        System.out.println("发送前消息条数: " + messages.size());
        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        System.out.println("HTTP Status: " + response.statusCode());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            System.out.println("Error Response: " + response.body());
            throw new IllegalStateException("HTTP 请求未成功，实验已停止。");
        }

        JSONObject root = JSON.parseObject(response.body());
        if (root == null || !(root.get("model") instanceof String model) || model.isBlank()) {
            throw new IllegalStateException("响应缺少有效的 model 字段。");
        }
        if (!(root.get("choices") instanceof JSONArray choices) || choices.isEmpty()) {
            throw new IllegalStateException("响应中的 choices 必须是非空数组。");
        }
        if (!(choices.get(0) instanceof JSONObject choice)
                || !(choice.get("message") instanceof JSONObject message)
                || !(message.get("content") instanceof String content) || content.isBlank()) {
            throw new IllegalStateException("响应缺少有效的 choices[0].message.content 文本字段。");
        }
        if (!(choice.get("finish_reason") instanceof String finishReason) || finishReason.isBlank()) {
            throw new IllegalStateException("响应缺少有效的 finish_reason 字段。");
        }
        if (!(root.get("usage") instanceof JSONObject usage)) {
            throw new IllegalStateException("响应缺少有效的 usage 对象。");
        }
        for (String field : new String[]{"prompt_tokens", "completion_tokens", "total_tokens"}) {
            Object value = usage.get(field);
            if (!(value instanceof Integer || value instanceof Long) || usage.getLongValue(field) < 0) {
                throw new IllegalStateException("响应中的 usage." + field + " 必须是非负整数。");
            }
        }

        System.out.println("Model: " + model);
        System.out.println("Content: " + content);
        System.out.println("Finish Reason: " + finishReason);
        System.out.println("Prompt Tokens: " + usage.getLongValue("prompt_tokens"));
        System.out.println("Completion Tokens: " + usage.getLongValue("completion_tokens"));
        System.out.println("Total Tokens: " + usage.getLongValue("total_tokens"));
        return content;
    }
}
