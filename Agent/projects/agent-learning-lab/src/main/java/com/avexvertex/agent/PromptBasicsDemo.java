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

public class PromptBasicsDemo {
    // 阅读顺序：main 读取配置 → runCompare 组织 A/B Prompt → callModel 发送请求。
    public static void main(String[] args) {
        // 不填参数运行 A/B；填 --context 只运行 B/C，不会把三组都再请求一遍。
        if (args.length > 1 || (args.length == 1 && !"--context".equals(args[0]))) {
            System.err.println("请清空程序实参运行 A/B，或填写 --context 运行 B/C 对照。");
            System.exit(1);
            return;
        }
        // 读取 IDEA 运行配置里的环境变量：apiKey 用于认证，baseUrl 是服务基础地址。
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        String baseUrl = System.getenv("DASHSCOPE_BASE_URL");
        // null 表示没有配置；isBlank() 表示空字符串或全是空白。缺配置就停止。
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
            // 去掉首尾空白和末尾斜杠，再拼上接口路径；URI 是 Java 表示地址的对象。
            endpoint = URI.create(baseUrl.strip().replaceAll("/+$", "") + "/chat/completions");
            // 只接受完整 HTTPS 地址，不允许夹带用户名、查询参数或片段。
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

        // 创建发送请求的客户端；建立连接最多等 15 秒，try 结束后自动关闭客户端。
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build()) {
            runCompare(client, endpoint, apiKey, args.length == 1);
        // 各种失败在这里统一提示；退出码 1 表示程序没有正常完成。
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
            // 请求被打断时，保留线程的中断标记，然后结束实验。
            Thread.currentThread().interrupt();
            System.err.println("HTTP 请求已被中断。");
            System.exit(1);
        }
    }

    // A、B 是两次独立请求；A 失败就停止，不把 A 的回答传给 B。
    private static void runCompare(HttpClient client, URI endpoint, String apiKey, boolean contextMode)
            throws IOException, InterruptedException {
        // Input：要交给模型审查的代码，只定义一次，保证两组拿到的代码逐字相同。
        // 这是字符串里的学习材料，不是本程序实际运行的 Service。
        String input = """
                public class CounterService {
                    private int count = 0;

                    public int increase() {
                        return ++count;
                    }
                }
                """.stripTrailing();

        // A：宽泛的任务 + 相同 Input，没有补充运行背景或回答格式。
        String vaguePrompt = "帮我看看下面这段代码有什么问题：\n\n" + input;

        // B：Role=视角，Task=做什么，Context=运行背景，Input=代码，
        // Constraints=限制范围，Output Format=怎样回答。它们都是 User content 里的文字。
        String structuredPrompt = """
                Role:
                你是一名 Java 后端代码审查助手。

                Task:
                检查下面代码中最值得优先修复的问题，重点判断是否存在并发安全风险。

                Context:
                这段代码位于 Spring Boot 单例 Service 中，同一个 Service 实例可能被多个 HTTP 请求线程并发访问。

                Input:
                %s

                Constraints:
                1. 最多指出 3 个问题。
                2. 只基于给出的代码和 Context 分析。
                3. 不要虚构不存在的数据库、缓存、接口或调用链。

                Output Format:
                先给一句总体结论。
                然后按“问题 / 原因 / 建议”输出。
                """.formatted(input).stripTrailing();
        // formatted(input) 用上面的代码替换 %s；stripTrailing() 去掉文本末尾换行。

        // contextMode 为 true 时复用同一份 B 和 Input，改跑 B/C；随后 return，不执行 A/B。
        if (contextMode) {
            runContextCompare(client, endpoint, apiKey, input, structuredPrompt);
            return;
        }

        /*
         * A 请求 JSON：消息只有一条，角色是 user。
         * {
         *   "model": "qwen3.8-max",
         *   "messages": [
         *     {"role":"user","content":"帮我看看下面这段代码有什么问题：\n\npublic class CounterService {\n    private int count = 0;\n\n    public int increase() {\n        return ++count;\n    }\n}"}
         *   ],
         *   "reasoning_effort": "none"
         * }
         */
        JSONArray vagueMessages = new JSONArray();
        vagueMessages.add(message("user", vaguePrompt));
        callModel(client, endpoint, apiKey, "A（模糊 Prompt）", vagueMessages);

        /*
         * B 请求 JSON：同样只有一条 user，不增加 System，也不带 A 的回答。
         * {
         *   "model": "qwen3.8-max",
         *   "messages": [
         *     {"role":"user","content":"Role:\n你是一名 Java 后端代码审查助手。\n\nTask:\n检查下面代码中最值得优先修复的问题，重点判断是否存在并发安全风险。\n\nContext:\n这段代码位于 Spring Boot 单例 Service 中，同一个 Service 实例可能被多个 HTTP 请求线程并发访问。\n\nInput:\npublic class CounterService {\n    private int count = 0;\n\n    public int increase() {\n        return ++count;\n    }\n}\n\nConstraints:\n1. 最多指出 3 个问题。\n2. 只基于给出的代码和 Context 分析。\n3. 不要虚构不存在的数据库、缓存、接口或调用链。\n\nOutput Format:\n先给一句总体结论。\n然后按“问题 / 原因 / 建议”输出。"}
         *   ],
         *   "reasoning_effort": "none"
         * }
         * 这些标题不变成 JSON 字段；它们都在 content 这个字符串里。
         * 控制台 Messages 会展示本次完整 JSON，包括代码和换行转义。
         */
        JSONArray structuredMessages = new JSONArray();
        structuredMessages.add(message("user", structuredPrompt));
        callModel(client, endpoint, apiKey, "B（结构化 Prompt）", structuredMessages);
    }

    private static void runContextCompare(HttpClient client, URI endpoint, String apiKey,
                                          String input, String structuredPrompt)
            throws IOException, InterruptedException {
        // C 保留 Role、Task、代码和输出格式；删除运行背景，按笔记调整“不自行假设”的约束。
        // 所以这是背景与约束的联合观察，不能把所有差异只归因于删除 Context。
        String withoutContextPrompt = """
                Role:
                你是一名 Java 后端代码审查助手。

                Task:
                检查下面代码中最值得优先修复的问题，重点判断是否存在并发安全风险。

                Input:
                %s

                Constraints:
                1. 最多指出 3 个问题。
                2. 只基于给出的代码分析。
                3. 信息不足时明确说明，不要自行假设对象生命周期或并发场景。

                Output Format:
                先给一句总体结论。
                然后按“问题 / 原因 / 建议”输出。
                """.formatted(input).stripTrailing();

        // B 仍用第一阶段原文，完整请求 JSON 见上方 B 注释；没有携带上次运行的回答。
        JSONArray withContextMessages = new JSONArray();
        withContextMessages.add(message("user", structuredPrompt));
        callModel(client, endpoint, apiKey, "B（有 Context）", withContextMessages);

        /*
         * C 仍只发送一条 user，不增加 System，也不带 B 的回答。
         * {
         *   "model": "qwen3.8-max",
         *   "messages": [
         *     {"role":"user","content":"Role:\n你是一名 Java 后端代码审查助手。\n\nTask:\n检查下面代码中最值得优先修复的问题，重点判断是否存在并发安全风险。\n\nInput:\npublic class CounterService {\n    private int count = 0;\n\n    public int increase() {\n        return ++count;\n    }\n}\n\nConstraints:\n1. 最多指出 3 个问题。\n2. 只基于给出的代码分析。\n3. 信息不足时明确说明，不要自行假设对象生命周期或并发场景。\n\nOutput Format:\n先给一句总体结论。\n然后按“问题 / 原因 / 建议”输出。"}
         *   ],
         *   "reasoning_effort": "none"
         * }
         * 不提供 Spring 单例 / HTTP 并发背景，观察回答会不会主动保留“如果被共享”的条件。
         */
        JSONArray withoutContextMessages = new JSONArray();
        withoutContextMessages.add(message("user", withoutContextPrompt));
        callModel(client, endpoint, apiKey, "C（无 Context）", withoutContextMessages);
    }

    private static JSONObject message(String role, String content) {
        // 把“谁说的、说了什么”装成一条消息，例如 {"role":"user","content":"你好"}。
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", content);
        // return 把这一条消息交给调用处，再由 add() 放进消息数组。
        return message;
    }

    private static String callModel(HttpClient client, URI endpoint, String apiKey,
                                    String label, JSONArray messages)
            throws IOException, InterruptedException {
        // body 是最外层的 {}；messages 是其中的消息数组。label 只用于控制台区分 A/B。
        JSONObject body = new JSONObject();
        body.put("model", "qwen3.8-max");
        body.put("messages", messages);
        body.put("reasoning_effort", "none");

        // 先构造请求，还没发送。Key 放在请求头中，不放进模型可见的 messages。
        // JSON.toJSONString(body) 把 Java 对象变成实际发送的 JSON 文本，并处理换行等转义。
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJSONString(body)))
                .build();

        System.out.println("\n=== " + label + " ===");
        System.out.println("Messages: " + JSON.toJSONString(messages));
        JSONArray roles = new JSONArray();
        // 只取出 role，方便核对两组都只有 [user]，没有新增 System。
        for (Object item : messages) {
            roles.add(((JSONObject) item).getString("role"));
        }
        System.out.println("Roles: " + JSON.toJSONString(roles));
        System.out.println("发送前消息条数: " + messages.size());
        System.out.println("User Prompt 字符数: " + messages.getJSONObject(0).getString("content").length());
        // send() 才真正发送请求，并等待响应；响应正文按 UTF-8 读取为字符串。
        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        System.out.println("HTTP Status: " + response.statusCode());
        // 先检查状态：200～299 是成功。错误响应不能按正常回答的结构读取。
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            System.out.println("Error Response: " + response.body());
            throw new IllegalStateException("HTTP 请求未成功，实验已停止。");
        }

        // 把响应 JSON 文本解析回 Java 对象，root 表示响应最外层的 {}。
        JSONObject root = JSON.parseObject(response.body());
        // 先检查类型再读取。instanceof String model 通过时，model 就是取出的字符串。
        if (root == null || !(root.get("model") instanceof String model) || model.isBlank()) {
            throw new IllegalStateException("响应缺少有效的 model 字段。");
        }
        // choices 是回答数组，必须存在且非空，后面才能安全取第一个回答。
        if (!(root.get("choices") instanceof JSONArray choices) || choices.isEmpty()) {
            throw new IllegalStateException("响应中的 choices 必须是非空数组。");
        }
        // 逐层读取 choices[0] → message → content，取得模型回答正文。
        if (!(choices.get(0) instanceof JSONObject choice)
                || !(choice.get("message") instanceof JSONObject message)
                || !(message.get("content") instanceof String content) || content.isBlank()) {
            throw new IllegalStateException("响应缺少有效的 choices[0].message.content 文本字段。");
        }
        // finish_reason 说明回答为何结束；usage 记录本次请求的 Token 用量。
        if (!(choice.get("finish_reason") instanceof String finishReason) || finishReason.isBlank()) {
            throw new IllegalStateException("响应缺少有效的 finish_reason 字段。");
        }
        if (!(root.get("usage") instanceof JSONObject usage)) {
            throw new IllegalStateException("响应缺少有效的 usage 对象。");
        }
        // 三项 Token 都必须是非负整数，避免字段缺失时被错误地当成 0。
        for (String field : new String[]{"prompt_tokens", "completion_tokens", "total_tokens"}) {
            Object value = usage.get(field);
            if (!(value instanceof Integer || value instanceof Long) || usage.getLongValue(field) < 0) {
                throw new IllegalStateException("响应中的 usage." + field + " 必须是非负整数。");
            }
        }

        // 全部校验通过后，输出本次真实返回的字段，不修改模型原话。
        System.out.println("Model: " + model);
        System.out.println("Content: " + content);
        System.out.println("Finish Reason: " + finishReason);
        System.out.println("Prompt Tokens: " + usage.getLongValue("prompt_tokens"));
        System.out.println("Completion Tokens: " + usage.getLongValue("completion_tokens"));
        System.out.println("Total Tokens: " + usage.getLongValue("total_tokens"));
        return content;
    }
}
