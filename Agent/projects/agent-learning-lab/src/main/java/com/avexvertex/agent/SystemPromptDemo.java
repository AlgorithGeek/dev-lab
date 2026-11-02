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

public class SystemPromptDemo {
    // 阅读顺序：main 选择模式 → runCompare 或 runHistory 组织消息 → callModel 发送和解析。
    public static void main(String[] args) {
        // 不填参数运行 A/B；填 --history 运行带 System 的两轮对话。
        if (args.length > 1 || (args.length == 1 && !"--history".equals(args[0]))) {
            System.err.println("请清空程序实参运行 A/B，或填写 --history 运行两轮 System 对话。");
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
            if (args.length == 1) {
                runHistory(client, endpoint, apiKey);
            } else {
                runCompare(client, endpoint, apiKey);
            }
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

    // throws 表示请求失败时交给 main 的 catch 处理；A 失败就不会继续执行 B。
    private static void runCompare(HttpClient client, URI endpoint, String apiKey)
            throws IOException, InterruptedException {
        // System：应用规定模型怎样工作。三引号可以直接写多行文本。
        String systemPrompt = """
                你是一名 Java Code Review Assistant。

                你的任务是审查用户提供的 Java 代码。

                规则：
                1. 先给一句总体结论。
                2. 最多指出 3 个最重要的问题。
                3. 优先关注正确性、空指针风险、并发安全和资源管理。
                4. 只分析用户实际提供的代码；如果信息不足，明确说明，不要虚构不存在的上下文。
                5. 使用中文回答。
                """.stripTrailing();
        // stripTrailing() 去掉文本末尾的空白和换行，保留中间的换行和代码缩进。
        // User：本轮具体任务。这里的 Java 代码只是发给模型看的文字，不在本程序中执行。
        String userPrompt = """
                请帮我 Review 下面这段代码：

                public String getUserName(User user) {
                    return user.getProfile().getName();
                }
                """.stripTrailing();

        /*
         * A：只有当前 User，不包含 System。对应的完整请求 JSON：
         * {
         *   "model": "qwen3.8-max",
         *   "messages": [
         *     {"role": "user", "content": "请帮我 Review 下面这段代码：\n\npublic String getUserName(User user) {\n    return user.getProfile().getName();\n}"}
         *   ],
         *   "reasoning_effort": "none"
         * }
         * JSON 字符串中的 \n 表示换行，由 FastJSON2 自动转义。
         */
        JSONArray withoutSystem = new JSONArray();
        // JSONArray 对应 JSON 的 []；add() 把一条消息放到数组末尾。
        withoutSystem.add(message("user", userPrompt));
        callModel(client, endpoint, apiKey, "A（无 System）", withoutSystem);

        /*
         * B：System 在第一位，User 与 A 完全相同。对应的完整请求 JSON：
         * {
         *   "model": "qwen3.8-max",
         *   "messages": [
         *     {"role": "system", "content": "你是一名 Java Code Review Assistant。\n\n你的任务是审查用户提供的 Java 代码。\n\n规则：\n1. 先给一句总体结论。\n2. 最多指出 3 个最重要的问题。\n3. 优先关注正确性、空指针风险、并发安全和资源管理。\n4. 只分析用户实际提供的代码；如果信息不足，明确说明，不要虚构不存在的上下文。\n5. 使用中文回答。"},
         *     {"role": "user", "content": "请帮我 Review 下面这段代码：\n\npublic String getUserName(User user) {\n    return user.getProfile().getName();\n}"}
         *   ],
         *   "reasoning_effort": "none"
         * }
         * A 的回答不进入 B；这是两个独立请求，没有传递对话历史。
         */
        JSONArray withSystem = new JSONArray();
        // 按加入顺序发送：先 System，再 User；复用 userPrompt 保证两组问题逐字相同。
        withSystem.add(message("system", systemPrompt));
        withSystem.add(message("user", userPrompt));
        callModel(client, endpoint, apiKey, "B（有 System）", withSystem);
    }

    private static void runHistory(HttpClient client, URI endpoint, String apiKey)
            throws IOException, InterruptedException {
        // 规则单独保存；history 只存成功的 User / Assistant，不包含 System。
        String systemPrompt = """
                你是一名 Java Code Review Assistant。
                回答每个问题时，第一行必须以“JAVA-REVIEW:”开头。
                只讨论 Java 代码或 Java 后端工程问题。
                使用中文回答。
                """.stripTrailing();
        JSONArray history = new JSONArray();
        String[] questions = {
                "ThreadLocal 最常见的风险是什么？请用一句话回答。",
                "那在线程池里为什么更需要小心？仍然用一句话回答。"
        };

        /*
         * 第 1 轮请求：System + 当前 User，共 2 条。
         * {
         *   "model": "qwen3.8-max",
         *   "messages": [
         *     {"role":"system","content":"你是一名 Java Code Review Assistant。\n回答每个问题时，第一行必须以“JAVA-REVIEW:”开头。\n只讨论 Java 代码或 Java 后端工程问题。\n使用中文回答。"},
         *     {"role":"user","content":"ThreadLocal 最常见的风险是什么？请用一句话回答。"}
         *   ],
         *   "reasoning_effort": "none"
         * }
         * 第 2 轮请求：重新加入 System，再放历史和当前问题，共 4 条。
         * {
         *   "model": "qwen3.8-max",
         *   "messages": [
         *     {"role":"system","content":"你是一名 Java Code Review Assistant。\n回答每个问题时，第一行必须以“JAVA-REVIEW:”开头。\n只讨论 Java 代码或 Java 后端工程问题。\n使用中文回答。"},
         *     {"role":"user","content":"ThreadLocal 最常见的风险是什么？请用一句话回答。"},
         *     {"role":"assistant","content":"<第 1 轮实际回答>"},
         *     {"role":"user","content":"那在线程池里为什么更需要小心？仍然用一句话回答。"}
         *   ],
         *   "reasoning_effort": "none"
         * }
         * <第 1 轮实际回答> 只是注释占位，实际内容来自响应，不能写死或补前缀。
         */
        try {
            for (int round = 0; round < questions.length; round++) {
                JSONObject currentUser = message("user", questions[round]);
                // messages 是本轮临时请求：规则 + 已成功的历史 + 新问题。
                JSONArray messages = new JSONArray();
                messages.add(message("system", systemPrompt));
                messages.addAll(history);
                messages.add(currentUser);

                System.out.println("Chat History Count before: " + history.size());
                String answer = callModel(client, endpoint, apiKey, "history 第 " + (round + 1) + " 轮", messages);

                // 只有请求和响应校验都成功，才保存本轮问答；System 不写入 history。
                history.add(currentUser);
                history.add(message("assistant", answer));
                System.out.println("Chat History Count after: " + history.size());
            }
        } finally {
            // 失败时也展示最后有效的历史：第一轮失败为 0 条，第二轮失败保留 2 条。
            System.out.println("已保存历史条数: " + history.size());
            System.out.println("已保存历史 JSON: " + JSON.toJSONString(history));
        }
    }

    private static JSONObject message(String role, String content) {
        // 把“谁说的、说了什么”装成一条消息，例如 {"role":"user","content":"你好"}。
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", content);
        // return 把构造好的对象交给调用它的地方，例如 withoutSystem.add(...)。
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
        // 遍历消息，只取出 role，方便核对顺序；发送前消息条数包含 System。
        for (Object item : messages) {
            roles.add(((JSONObject) item).getString("role"));
        }
        System.out.println("Roles: " + JSON.toJSONString(roles));
        System.out.println("发送前消息条数: " + messages.size());
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
