package com.avexvertex.smarttext.llm;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Qwen 大模型客户端
 * <p>
 * 负责通过阿里云百炼 OpenAI-compatible API 调用 Qwen 模型。
 * <p>
 * 当前只负责：
 * 1. 发送 Prompt
 * 2. 获取模型响应
 * 3. 返回模型生成的文本
 * <p>
 * 不负责具体的文本分析业务。
 */
@Component
public class QwenLlmClient implements LlmClient {

    /**
     * JDK 自带的 HTTP 客户端
     */
    private final HttpClient httpClient;

    /**
     * 阿里云百炼 API Key
     */
    private final String apiKey;

    /**
     * Qwen API 请求地址
     */
    private final String baseUrl;

    /**
     * 当前使用的模型名称
     */
    private final String model;

    /**
     * 构造 Qwen 客户端
     *
     * @param apiKey  阿里云百炼 API Key
     * @param baseUrl API 请求地址
     * @param model   模型名称
     */
    public QwenLlmClient(
            @Value("${ai.qwen.api-key}") String apiKey,
            @Value("${ai.qwen.base-url}") String baseUrl,
            @Value("${ai.qwen.model}") String model) {

        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;

        // 创建 HTTP 客户端，并设置建立连接的最长等待时间
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 向 Qwen 发送 Prompt，并返回模型生成的文本。
     *
     * @param prompt 发送给模型的提示词
     * @return Qwen 返回的文本内容
     */
    @Override
    public String chat(String prompt) {
        try {
            // 构造 user 消息
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            // messages 是一个数组，因为 Chat Completions 支持多轮消息
            JSONArray messages = new JSONArray();
            messages.add(userMessage);

            // 构造最终 JSON
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            // 要求模型返回合法的JSON字符串
            /*
                "response_format": {
                    "type": "json_object"
                }
             */
            JSONObject responseFormat = new JSONObject();
            responseFormat.put("type", "json_object");
            requestBody.put("response_format", responseFormat);

            // 转换成真正发送给服务器的 JSON 字符串
            String requestJson = requestBody.toJSONString();

            // 构造 HTTP POST 请求
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            // 向阿里云百炼发送请求，并等待响应
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            // 非 2xx 状态码统一视为调用失败
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Qwen API 调用失败，HTTP 状态码："
                                + response.statusCode()
                                + "，响应内容："
                                + response.body()
                );
            }

            // 将响应 JSON 字符串解析成 JSONObject
            JSONObject responseJson = JSONObject.parseObject(response.body());

            /*
             * 返回结构大致如下：
             *
             * {
             *   "choices": [
             *     {
             *       "message": {
             *         "role": "assistant",
             *         "content": "模型回答"
             *       }
             *     }
             *   ]
             * }
             */
            JSONArray choices = responseJson.getJSONArray("choices");
            // 判断响应结构是否符合预期。
            if (choices == null || choices.isEmpty()) {
                throw new IllegalStateException(
                        "Qwen API 响应中没有 choices：" + response.body()
                );
            }

            JSONObject firstChoice = choices.getJSONObject(0);
            JSONObject message = firstChoice.getJSONObject("message");

            if (message == null) {
                throw new IllegalStateException(
                        "Qwen API 响应中没有 message：" + response.body()
                );
            }

            String content = message.getString("content");

            if (content == null || content.isBlank()) {
                throw new IllegalStateException(
                        "Qwen API 响应中没有模型返回内容：" + response.body()
                );
            }

            // 最终只把模型生成的文本返回给业务层。
            return content;

        } catch (InterruptedException e) {

            // 恢复线程中断状态
            Thread.currentThread().interrupt();

            throw new IllegalStateException("Qwen API 调用被中断", e);

        } catch (IOException e) {

            // HTTP 网络通信相关异常
            throw new IllegalStateException("Qwen API 调用发生 I/O 异常", e);
        }

    }
}
