package com.avexvertex.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class ContextEngineeringDemo {
    public static void main(String[] args) {
        String mode = args.length == 0 ? "--minimal" : args[0];
        String label = switch (mode) {
            case "--minimal" -> "A（最少必要资料）";
            case "--noise" -> "B（加入无关资料）";
            case "--position-start" -> "C1（目标资料在开头）";
            case "--position-middle" -> "C2（目标资料在中间）";
            case "--position-end" -> "C3（目标资料在结尾）";
            default -> null;
        };
        if (args.length > 1 || label == null) {
            System.err.println("程序实参请留空，或选择 --minimal、--noise、--position-start、--position-middle、--position-end。");
            System.exit(1);
            return;
        }

        // A 组的资料只保留回答两个问题所需的事实。
        String target = """
                [CURRENT TARGET PROJECT]
                项目代号：ORANGE-47
                数据库：PostgreSQL
                """.stripTrailing();

        // B 和 C 组复用相同的资料块，避免位置实验混入内容差异。
        String buildNotes = """
                [内部资料：构建与发布]
                - 文档站点由独立流水线构建，预览页每晚重新部署。
                - 桌面工具的安装包按平台归档，归档前需要核对签名。
                - 测试团队每周整理一次失败截图，并按浏览器版本分类。
                - 界面语言包由运营同事维护，调整后需要更新截图。
                """.stripTrailing();

        String officeNotes = """
                [内部资料：值班与办公]
                - 办公网络周六上午维护，访客无线网络会短时中断。
                - 每月第一个工作日检查会议室投影和视频设备。
                - 值班同事在交接时登记告警、影响范围和待跟进事项。
                - 公共演示素材放在共享盘，过期版本移入归档目录。
                """.stripTrailing();

        String serviceNotes = """
                [内部资料：其他服务]
                - Atlas 负责图片缩略图，发布前用固定尺寸样本检查裁剪效果。
                - Nimbus 负责生成静态周报，生成后会核对标题、日期和页码。
                - 文件转换服务定期清理临时文件，清理量记录在周报里。
                - 通知服务的邮件模板按语言分目录，更新模板要同步预览页面。
                """.stripTrailing();

        String separator = "\n\n";
        String context = switch (mode) {
            case "--minimal" -> target;
            case "--position-middle" -> String.join(separator, buildNotes, target, officeNotes, serviceNotes);
            case "--position-end" -> String.join(separator, buildNotes, officeNotes, serviceNotes, target);
            // B 与 C1 的请求内容相同，目标资料都放在开头。
            default -> String.join(separator, target, buildNotes, officeNotes, serviceNotes);
        };

        // %s 是资料的插入位置。后续对照组替换这里的资料，问题保持一样。
        String prompt = """
                请严格根据下面提供的资料回答，不要猜测。某项缺少依据时，该项填写 UNKNOWN。
                请只输出两行：
                ProjectCode: <项目代号>
                Database: <数据库>

                资料：
                %s

                问题：
                1. 当前目标项目的项目代号是什么？
                2. 当前目标项目使用什么数据库？
                """.formatted(context).stripTrailing();

        /*
         * A 组请求 JSON（content 里的 \n 表示换行）：
         * {
         *   "model": "qwen3.8-max",
         *   "messages": [{
         *     "role": "user",
         *     "content": "请严格根据下面提供的资料回答，不要猜测。某项缺少依据时，该项填写 UNKNOWN。\n请只输出两行：\nProjectCode: <项目代号>\nDatabase: <数据库>\n\n资料：\n[CURRENT TARGET PROJECT]\n项目代号：ORANGE-47\n数据库：PostgreSQL\n\n问题：\n1. 当前目标项目的项目代号是什么？\n2. 当前目标项目使用什么数据库？"
         *   }],
         *   "reasoning_effort": "none"
         * }
         * B/C 组沿用同样的 JSON 字段，按上面的顺序拼接 content 中的资料块。
         */
        // 资料和问题都在同一条 user 消息里，messages 数组此时只有一条消息。
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        JSONArray messages = new JSONArray();
        messages.add(message);

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "qwen3.8-max");
        requestBody.put("messages", messages);
        requestBody.put("reasoning_effort", "none");

        System.out.println("=== " + label + " ===");
        System.out.println("Request Body:");
        String requestJson = JSON.toJSONString(requestBody, JSONWriter.Feature.PrettyFormat);
        System.out.println(requestJson);

        try {
            sendRequest(requestJson);
        } catch (JSONException e) {
            System.err.println("响应 JSON 解析失败，请查看上面的原始响应。");
            System.exit(1);
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("HTTP 请求失败（" + e.getClass().getSimpleName() + "）。");
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("HTTP 请求已中断。");
            System.exit(1);
        }
    }

    private static void sendRequest(String requestJson) throws IOException, InterruptedException {
        // 从 IDEA 运行配置读取地址和密钥。密钥只用于请求头。
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        String baseUrl = System.getenv("DASHSCOPE_BASE_URL");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("请在 IDEA 运行配置中填写 DASHSCOPE_API_KEY。");
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("请在 IDEA 运行配置中填写 DASHSCOPE_BASE_URL。");
        }

        HttpRequest request;
        try {
            URI endpoint = URI.create(baseUrl.strip().replaceAll("/+$", "") + "/chat/completions");
            if (!"https".equalsIgnoreCase(endpoint.getScheme()) || endpoint.getHost() == null
                    || endpoint.getUserInfo() != null || endpoint.getQuery() != null
                    || endpoint.getFragment() != null) {
                throw new IllegalArgumentException();
            }
            // 用 POST 发送刚刚打印的 JSON，按 UTF-8 编码中文内容。
            request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(120))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                    .build();
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("请求构造失败，请检查 Base URL 和 API Key 的格式。");
        }

        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15)).build()) {
            System.out.println("正在发送 HTTP 请求，方法：POST");
            // 非流式请求：从开始发送计时，收到完整响应正文后停止。
            long start = System.nanoTime();
            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            double elapsedMillis = (System.nanoTime() - start) / 1_000_000.0;
            System.out.println("HTTP Status: " + response.statusCode());
            System.out.println("Total Time (ms): " + elapsedMillis);
            System.out.println("Response Body:");
            System.out.println(response.body());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("请求失败，请查看上面的 HTTP 状态码和响应。");
            }
            printResult(response.body());
        }
    }

    private static void printResult(String responseBody) {
        /*
         * A 组响应节选：
         * {
         *   "choices": [{"message": {"content": "ProjectCode: ORANGE-47\nDatabase: PostgreSQL"}}],
         *   "usage": {"prompt_tokens": 102, "completion_tokens": 12, "total_tokens": 114}
         * }
         */
        JSONObject result = JSON.parseObject(responseBody);
        if (result == null || !(result.get("choices") instanceof JSONArray choices)
                || choices.isEmpty() || !(choices.get(0) instanceof JSONObject choice)
                || !(choice.get("message") instanceof JSONObject message)
                || !(message.get("content") instanceof String answer) || answer.isBlank()) {
            throw new IllegalStateException("响应缺少可读的答案，请检查 choices[0].message.content。");
        }
        if (!(result.get("usage") instanceof JSONObject usage)) {
            throw new IllegalStateException("响应缺少 usage，无法记录 Token 基线。");
        }

        // 读取接口报告的 Token 用量。输入用量是后续加噪声时的比较基线。
        long promptTokens = readTokenCount(usage, "prompt_tokens");
        long completionTokens = readTokenCount(usage, "completion_tokens");
        long totalTokens = readTokenCount(usage, "total_tokens");
        // shortcut: 只检查两个预期值是否出现，需要严格评估时再逐字段匹配。
        boolean correct = answer.contains("ORANGE-47") && answer.contains("PostgreSQL");
        System.out.println("Answer:");
        System.out.println(answer);
        System.out.println("Prompt Tokens: " + promptTokens);
        System.out.println("Completion Tokens: " + completionTokens);
        System.out.println("Total Tokens: " + totalTokens);
        System.out.println("Correct: " + correct);
    }

    private static long readTokenCount(JSONObject usage, String field) {
        Object value = usage.get(field);
        if (!(value instanceof Integer || value instanceof Long) || ((Number) value).longValue() < 0) {
            throw new IllegalStateException("响应中的 usage." + field + " 缺失或不是非负整数。");
        }
        return ((Number) value).longValue();
    }
}
