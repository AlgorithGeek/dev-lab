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

public class FirstModelCall {
    public static void main(String[] args) {
        // System 是 Java 提供的系统类。
        // getenv() 是它的一个静态方法，用来读取环境变量
        System.out.println();
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        String baseUrl = System.getenv("DASHSCOPE_BASE_URL");

        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("缺少 DASHSCOPE_API_KEY，请在 IDEA 运行配置的环境变量中填写。");
            System.exit(1);
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            System.err.println("缺少 DASHSCOPE_BASE_URL，请在 IDEA 运行配置的环境变量中填写。");
            System.exit(1);
        }

        System.out.println("配置已读取，下一步接入模型 HTTP 请求。");

        URI endpoint;
        try {
            String normalizedBaseUrl = baseUrl.strip().replaceAll("/+$", "");
            endpoint = URI.create(normalizedBaseUrl + "/chat/completions");
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

        String requestBody = """
                {
                  "model": "qwen3.8-max",
                  "messages": [
                    {"role": "user", "content": "请用一句话解释 Java 是什么。"}
                  ],
                  "reasoning_effort": "none"
                }
                """;

        // 这是 Java 11 引入的 HTTP Client API 中用于表示 HTTP 请求的类
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(endpoint)
                    .timeout(Duration.ofSeconds(120))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
        } catch (IllegalArgumentException e) {
            System.err.println("请求构造失败，请检查环境变量的格式，API Key 中不能包含换行等非法字符。");
            System.exit(1);
            return;
        }

        System.out.println("正在发送 HTTP 请求，方法：" + request.method());
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build()) {
            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            System.out.println("HTTP Status: " + response.statusCode());
            System.out.println("Response Body:");
            System.out.println(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                System.exit(1);
            }

            JSONObject root = JSON.parseObject(response.body());
            if (root == null || !(root.get("model") instanceof String)
                    || root.getString("model").isBlank()) {
                System.err.println("响应缺少有效的 model 字段。");
                System.exit(1);
                return;
            }
            String model = root.getString("model");
            System.out.println("Model: " + model);

            if (!(root.get("choices") instanceof JSONArray choices) || choices.isEmpty()) {
                System.err.println("响应中的 choices 必须是非空数组。");
                System.exit(1);
                return;
            }
            if (!(choices.get(0) instanceof JSONObject choice)
                    || !(choice.get("message") instanceof JSONObject message)
                    || !(message.get("content") instanceof String)) {
                System.err.println("响应缺少有效的 choices[0].message.content 文本字段。");
                System.exit(1);
                return;
            }
            String content = message.getString("content");
            System.out.println("Content: " + content);

            if (!(choice.get("finish_reason") instanceof String)
                    || choice.getString("finish_reason").isBlank()) {
                System.err.println("响应缺少有效的 finish_reason 字段。");
                System.exit(1);
                return;
            }
            String finishReason = choice.getString("finish_reason");
            System.out.println("Finish Reason: " + finishReason);

            if (!(root.get("usage") instanceof JSONObject usage)) {
                System.err.println("响应缺少有效的 usage 对象。");
                System.exit(1);
                return;
            }
            for (String field : new String[]{"prompt_tokens", "completion_tokens", "total_tokens"}) {
                Object value = usage.get(field);
                if (!(value instanceof Integer || value instanceof Long) || usage.getLongValue(field) < 0) {
                    System.err.println("响应中的 usage." + field + " 必须是非负整数。");
                    System.exit(1);
                    return;
                }
            }
            System.out.println("Prompt Tokens: " + usage.getLongValue("prompt_tokens"));
            System.out.println("Completion Tokens: " + usage.getLongValue("completion_tokens"));
            System.out.println("Total Tokens: " + usage.getLongValue("total_tokens"));
        } catch (JSONException e) {
            System.err.println("响应 JSON 解析失败，请检查上面的原始响应。");
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
}
