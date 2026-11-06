package com.avexvertex.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class LlmApiErrorDemo {
    public static void main(String[] args) {
        String mode = args.length == 1 ? args[0] : "";
        if ("--rate-limit".equals(mode) || "--server-error".equals(mode) || "--timeout".equals(mode)
                || "--context-too-long".equals(mode)) {
            try {
                runLocalDemo(mode);
            } catch (IOException e) {
                System.err.println("本地模拟失败（" + e.getClass().getSimpleName() + "）。");
                System.exit(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("本地模拟已中断。");
                System.exit(1);
            }
            return;
        }
        if (args.length != 1 || !"--invalid-model".equals(args[0])) {
            System.err.println("请在程序实参中填写 --invalid-model、--rate-limit、--server-error、--timeout 或 --context-too-long。");
            System.exit(1);
            return;
        }

        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        String baseUrl = System.getenv("DASHSCOPE_BASE_URL");
        if (apiKey == null || apiKey.isBlank() || baseUrl == null || baseUrl.isBlank()) {
            System.err.println("请在 IDEA 运行配置中填写 DASHSCOPE_API_KEY 和 DASHSCOPE_BASE_URL。");
            System.exit(1);
            return;
        }

        // 直接用文本块展示请求 JSON。实验只故意填错模型名。
        String requestBody = """
                {
                  "model": "qwen3.8-max-invalid-demo",
                  "messages": [{"role": "user", "content": "hello"}]
                }
                """;
        HttpRequest request;
        try {
            URI endpoint = URI.create(baseUrl.strip().replaceAll("/+$", "") + "/chat/completions");
            if (!"https".equalsIgnoreCase(endpoint.getScheme()) || endpoint.getHost() == null
                    || endpoint.getUserInfo() != null || endpoint.getQuery() != null
                    || endpoint.getFragment() != null) {
                throw new IllegalArgumentException();
            }
            request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(120))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();
        } catch (IllegalArgumentException e) {
            System.err.println("请求构造失败，请检查 Base URL 和 API Key 的格式。");
            System.exit(1);
            return;
        }

        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()) {
            System.out.println("=== A（真实服务：错误模型） ===");
            System.out.println("Request Body:");
            System.out.print(requestBody);
            System.out.println("Attempt: 1");
            // 404 仍然是一个 HTTP 响应。拿到响应后读取状态码和正文。
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();
            System.out.println("HTTP Status: " + status);
            if (status >= 200 && status < 300) {
                System.err.println("请求成功，未触发预期的错误模型场景。");
                System.exit(1);
                return;
            }
            String body = response.body().replace(apiKey, "[已隐藏]");
            System.out.println("Raw Error Body:");
            System.out.println(body);
            printErrorDecision(body, false);
        } catch (IOException e) {
            System.err.println("未能读取完整的 HTTP 响应（" + e.getClass().getSimpleName() + "）。");
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("HTTP 请求已中断。");
            System.exit(1);
        }
    }

    private static void runLocalDemo(String mode) throws IOException, InterruptedException {
        boolean timeout = "--timeout".equals(mode);
        int firstStatus = switch (mode) {
            case "--server-error" -> 503;
            case "--timeout" -> 200;
            case "--context-too-long" -> 400;
            default -> 429;
        };
        String firstBody = switch (firstStatus) {
            case 503 -> """
                    {"error":{"code":"service_unavailable","message":"Model service is temporarily unavailable"},"request_id":"local-503-demo"}
                    """;
            // 短请求也能验证客户端决策，服务端直接返回模拟的上下文超限 JSON。
            case 400 -> """
                    {"error":{"code":"context_length_exceeded","message":"The request exceeds the model context window"},"request_id":"local-context-too-long"}
                    """;
            case 429 -> """
                    {"error":{"code":"rate_limit_exceeded","message":"Too many requests"},"request_id":"local-429-demo"}
                    """;
            default -> "";
        };
        // 端口 0 让系统分配空闲端口，服务只接受本机请求。
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger received = new AtomicInteger();
        server.createContext("/chat/completions", exchange -> {
            try (exchange) {
                exchange.getRequestBody().readAllBytes();
                boolean firstRequest = received.incrementAndGet() == 1;
                if (timeout) {
                    // 收到请求后故意晚发响应头，超过客户端的等待时间。
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                boolean returnError = firstStatus == 400 || (firstRequest && !timeout);
                String body = returnError
                        ? firstBody
                        : """
                          {"choices":[{"message":{"role":"assistant","content":"hello"}}]}
                          """;
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(returnError ? firstStatus : 200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
        server.start();
        try (HttpClient client = HttpClient.newHttpClient()) {
            System.out.println(switch (mode) {
                case "--timeout" -> "=== D（本地模拟：客户端超时） ===";
                case "--context-too-long" -> "=== E（本地模拟：上下文过长） ===";
                case "--server-error" -> "=== C（本地模拟：503 服务暂不可用） ===";
                default -> "=== B（本地模拟：429 限流） ===";
            });
            URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/chat/completions");
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofMillis(timeout ? 300 : 5000))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("""
                            {"model":"local-demo","messages":[{"role":"user","content":"hello"}]}
                            """))
                    .build();

            // 首次收到模拟 429 或 503 才等待并重发，同一次运行最多发送两次。
            for (int attempt = 1; attempt <= 2; attempt++) {
                System.out.println("Attempt: " + attempt);
                HttpResponse<String> response;
                try {
                    response = client.send(request, HttpResponse.BodyHandlers.ofString());
                } catch (HttpTimeoutException e) {
                    if (!timeout) {
                        throw e;
                    }
                    // send 没有返回 HttpResponse，客户端也没有收到 HTTP 状态码。
                    System.out.println("HTTP Status: NONE");
                    System.out.println("Layer: TRANSPORT");
                    System.out.println("Java Exception: " + e.getClass().getSimpleName());
                    System.out.println("Retry Policy: CONDITIONAL");
                    System.out.println("Action: TIMEOUT_RETRY_OPTIONAL");
                    break;
                }
                int status = response.statusCode();
                System.out.println("HTTP Status: " + status);
                if (status == 200) {
                    System.out.println("Result: SUCCESS");
                    break;
                }
                boolean retry = (status == 429 || status == 503) && attempt == 1;
                System.out.println("Raw Error Body:");
                System.out.println(response.body());
                printErrorDecision(response.body(), retry);
                if (!retry) {
                    System.out.println("Result: FAILED");
                    break;
                }
                System.out.println("Backoff: 300 ms");
                Thread.sleep(300);
            }
            System.out.println("Local Requests Received: " + received.get());
        } finally {
            server.stop(0);
        }
    }

    private static void printErrorDecision(String body, boolean retry) {
        // 参考前一次实测：{"error":{"code":"model_not_found","message":"Model not exist.","id":"示例 ID"}}
        JSONObject root = null;
        JSONObject error = null;
        try {
            root = JSON.parseObject(body);
            if (root != null && root.get("error") instanceof JSONObject details) {
                error = details;
            }
        } catch (JSONException e) {
            // 有些网关返回 HTML 或纯文本，保留原始正文供排查。
            System.out.println("错误正文无法解析为 JSON。");
        }
        String code = error == null ? null : error.getString("code");
        System.out.println("Provider Code: " + code);
        System.out.println("Provider Message: " + (error == null ? null : error.getString("message")));
        System.out.println("Request ID (error.id): " + (error == null ? null : error.getString("id")));
        if (root != null && root.get("request_id") instanceof String requestId) {
            System.out.println("Request ID (request_id): " + requestId);
        }
        System.out.println("Retry Policy: " + (retry ? "RETRY" : "DO_NOT_RETRY"));
        // 根据服务端错误码判断原因，单凭 404 无法区分模型不存在和接口路径写错。
        String action = "model_not_found".equals(code) ? "FIX_MODEL" : "CHECK_ERROR_BODY";
        if ("context_length_exceeded".equals(code)) {
            action = "REDUCE_CONTEXT";
        }
        System.out.println("Action: " + (retry ? "BACKOFF_RETRY" : action));
    }
}
