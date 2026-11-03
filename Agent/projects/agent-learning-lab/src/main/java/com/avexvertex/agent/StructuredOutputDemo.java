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

public class StructuredOutputDemo {
    public static void main(String[] args) {
        // --errors 只运行下面两个本地 JSON 样本，直接返回，不会读取密钥或走 HTTP。
        if (args.length == 1 && "--errors".equals(args[0])) {
            try {
                runLocalErrors();
            } catch (JSONException | IllegalStateException e) {
                System.err.println("本地演示出现非预期结果：" + e.getMessage());
                System.exit(1);
            }
            return;
        }
        if (args.length != 0) {
            System.err.println("不填参数发送真实请求；填写 --errors 只运行本地错误样本。");
            System.exit(1);
            return;
        }
        // 本步发送一次请求，先读取 content，再把里面的业务 JSON 转成 DTO 并打印字段。
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        String baseUrl = System.getenv("DASHSCOPE_BASE_URL");
        // 环境变量来自 IDEA 的当前运行配置；缺少时先停止，避免发送无效请求。
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
            // Base URL 不含具体接口路径：去掉末尾斜杠，再追加 /chat/completions。
            endpoint = URI.create(baseUrl.strip().replaceAll("/+$", "") + "/chat/completions");
            // 只接受完整 HTTPS 地址，不允许用户名、查询参数或片段。
            if (!"https".equalsIgnoreCase(endpoint.getScheme()) || endpoint.getHost() == null
                    || endpoint.getUserInfo() != null || endpoint.getQuery() != null
                    || endpoint.getFragment() != null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            // 不打印异常原文，避免把环境变量中的私密地址带进日志。
            System.err.println("DASHSCOPE_BASE_URL 格式错误，请填写完整 HTTPS Base URL，不带查询参数或片段。");
            System.exit(1);
            return;
        }

        // 第 1 步：告诉模型要分析什么。这些是虚构的学习数据。
        String prompt = "请分析下面广告活动的风险。Campaign ID：10001；日预算：5000 元；近 7 天转化率：0.3%。";
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        JSONArray messages = new JSONArray();
        messages.add(userMessage);

        // 第 2 步：描述输出字段的规则，注意这里放的是规则，不是模型的实际分析结果。
        // campaignId 对应 DTO 的 Long；Schema 中仍写 JSON 类型 integer，不写 Java 类型名。
        JSONObject campaignIdProperty = new JSONObject();
        campaignIdProperty.put("type", "integer");
        campaignIdProperty.put("description", "广告活动 ID");

        // riskLevel 是字符串，而且只允许这三个取值；enum 在 JSON 中是一个数组。
        JSONArray riskLevels = new JSONArray();
        riskLevels.add("HIGH");
        riskLevels.add("MEDIUM");
        riskLevels.add("LOW");
        JSONObject riskLevelProperty = new JSONObject();
        riskLevelProperty.put("type", "string");
        riskLevelProperty.put("enum", riskLevels);
        riskLevelProperty.put("description", "广告活动当前风险等级");

        JSONObject reasonProperty = new JSONObject();
        reasonProperty.put("type", "string");
        reasonProperty.put("description", "风险判断的主要原因");

        JSONObject suggestionProperty = new JSONObject();
        suggestionProperty.put("type", "string");
        suggestionProperty.put("description", "给出的简短优化建议");

        // properties 把四个字段各自的规则放在一起，字段名与 CampaignAnalysis 一致。
        JSONObject properties = new JSONObject();
        properties.put("campaignId", campaignIdProperty);
        properties.put("riskLevel", riskLevelProperty);
        properties.put("reason", reasonProperty);
        properties.put("suggestion", suggestionProperty);

        // properties 定义字段规则；required 另外规定这四个字段都不能缺少。
        JSONArray required = new JSONArray();
        required.add("campaignId");
        required.add("riskLevel");
        required.add("reason");
        required.add("suggestion");

        // Schema 的 type=object 指“模型结果最外层是对象”。required 与 properties 同级。
        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false); // 不允许模型额外增加未定义的字段。

        // 第 3 步：包装 Schema 的名字和严格模式。这些不是模型需要返回的业务字段。
        JSONObject jsonSchema = new JSONObject();
        jsonSchema.put("name", "campaign_analysis");
        jsonSchema.put("strict", true); // boolean 值，不是字符串 "true"。
        jsonSchema.put("schema", schema); // 放入整个 Schema 对象，不要先转成 JSON 字符串。

        // 这里的 type=json_schema 指“输出格式模式”，与 Schema 的 type=object 含义不同。
        JSONObject responseFormat = new JSONObject();
        responseFormat.put("type", "json_schema");
        responseFormat.put("json_schema", jsonSchema);

        /*
         * 第 4 步：把 messages 和 response_format 放进同一个顶层对象。
         * 完整请求 JSON 如下（只规定输出结构，没有提前填入模型答案）：
         * {
         *   "model": "qwen3.8-max",
         *   "messages": [
         *     {
         *       "role": "user",
         *       "content": "请分析下面广告活动的风险。Campaign ID：10001；日预算：5000 元；近 7 天转化率：0.3%。"
         *     }
         *   ],
         *   "response_format": {
         *     "type": "json_schema",
         *     "json_schema": {
         *       "name": "campaign_analysis",
         *       "strict": true,
         *       "schema": {
         *         "type": "object",
         *         "properties": {
         *           "campaignId": {"type": "integer", "description": "广告活动 ID"},
         *           "riskLevel": {
         *             "type": "string",
         *             "enum": ["HIGH", "MEDIUM", "LOW"],
         *             "description": "广告活动当前风险等级"
         *           },
         *           "reason": {"type": "string", "description": "风险判断的主要原因"},
         *           "suggestion": {"type": "string", "description": "给出的简短优化建议"}
         *         },
         *         "required": ["campaignId", "riskLevel", "reason", "suggestion"],
         *         "additionalProperties": false
         *       }
         *     }
         *   },
         *   "reasoning_effort": "none"
         * }
         */
        JSONObject body = new JSONObject();
        body.put("model", "qwen3.8-max");
        body.put("messages", messages);
        body.put("response_format", responseFormat);
        body.put("reasoning_effort", "none");

        // 将 Java 对象转成 JSON 文本；PrettyFormat 只增加缩进换行，方便肉眼阅读。
        String requestBody = JSON.toJSONString(body, JSONWriter.Feature.PrettyFormat);
        System.out.println("Request Body:");
        System.out.println(requestBody);

        // 第 5 步：构造 HTTP 请求。JSON 放请求体，密钥只放 Authorization 请求头。
        // headers 不会写进上面打印的 Request Body，也不是 messages 的一部分。
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(120)) // 请求超时为 120 秒。
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();
        } catch (IllegalArgumentException e) {
            System.err.println("请求构造失败，请检查环境变量格式，API Key 中不能包含换行等非法字符。");
            System.exit(1);
            return;
        }

        // 第 6 步：创建客户端并发送。建立连接最多等 15 秒；try 结束后自动关闭客户端。
        System.out.println("正在发送 HTTP 请求，方法：" + request.method());
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build()) {
            // send() 等待完整响应；响应正文按 UTF-8 读取为 String。
            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            System.out.println("HTTP Status: " + response.statusCode());
            System.out.println("Response Body:");
            System.out.println(response.body());

            // 非 2xx 视为失败。先保留服务端错误信息，不把它当成业务分析结果。
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                System.err.println("HTTP 请求未成功，已停止；请查看上面的 Response Body。");
                System.exit(1);
                return;
            }

            /*
             * 第 7 步：先解析接口响应的外层 JSON，再沿路径找到模型回答。
             * 外层结构示意（示例内容，不是本次实际回复）：
             * {
             *   "choices": [
             *     {"message": {"content": "{\"campaignId\":10001,\"riskLevel\":\"HIGH\",\"reason\":\"示例原因\",\"suggestion\":\"示例建议\"}"}}
             *   ]
             * }
             * 路径是 choices[0].message.content；content 此时仍是 String，不是 DTO。
             */
            JSONObject root = JSON.parseObject(response.body());
            // choices 必须存在且非空，否则不能取数组中的第 0 项。
            if (root == null || !(root.get("choices") instanceof JSONArray choices) || choices.isEmpty()) {
                System.err.println("响应中的 choices 必须是非空数组。");
                System.exit(1);
                return;
            }
            // 逐层确认对象和文本类型，避免字段缺失时继续读取造成空指针。
            if (!(choices.get(0) instanceof JSONObject choice)
                    || !(choice.get("message") instanceof JSONObject message)
                    || !(message.get("content") instanceof String content) || content.isBlank()) {
                System.err.println("响应缺少有效的 choices[0].message.content 文本字段。");
                System.exit(1);
                return;
            }
            System.out.println("Structured Content:");
            System.out.println(content);
            /*
             * 第 8 步：第二次处理 JSON，把业务 JSON 字符串转换成 Java 对象。
             * 示例：{"campaignId":10001,"riskLevel":"HIGH","reason":"示例原因","suggestion":"示例建议"}
             * 对应：CampaignAnalysis 的 Long campaignId 和三个 String 字段。
             * CampaignAnalysis.class 告诉 FastJSON2：这次目标类型是这个类，而非 JSONObject。
             */
            CampaignAnalysis analysis;
            try {
                analysis = JSON.parseObject(content, CampaignAnalysis.class);
            } catch (JSONException e) {
                // 这里失败的是内层业务 JSON 的语法或类型转换，与外层 HTTP 响应解析分开提示。
                System.err.println("Structured Content 无法反序列化为 CampaignAnalysis，请检查 JSON 语法和字段类型。");
                System.exit(1);
                return;
            }

            // 第 9 步：真实请求和本地演示共用同一份字段检查，避免两边规则不一致。
            validateCampaignAnalysis(analysis);

            // 第 10 步：先打印对象，再通过 getter 读字段，证明此时已经可以按 Java 类型访问。
            // 这些是最小字段检查，不是完整 Schema 校验，也没有核实真实广告业务数据。
            System.out.println("CampaignAnalysis: " + analysis);
            System.out.println("Campaign ID: " + analysis.getCampaignId());
            System.out.println("Risk Level: " + analysis.getRiskLevel());
            System.out.println("Reason: " + analysis.getReason());
            System.out.println("Suggestion: " + analysis.getSuggestion());
        } catch (JSONException e) {
            System.err.println("响应 JSON 解析失败，请检查上面的原始 Response Body。");
            System.exit(1);
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            // 输出异常类型便于区分超时等情况，不输出可能包含私密地址的异常详情。
            System.err.println("HTTP 请求失败（" + e.getClass().getSimpleName()
                    + "），请检查网络和 Base URL，或稍后重试。");
            System.exit(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 保留请求被打断的标记，再退出。
            System.err.println("HTTP 请求已被中断。");
            System.exit(1);
        }
    }

    // 这里只检查 DTO 是否可继续使用，不是完整的 JSON Schema 或广告业务事实校验。
    private static void validateCampaignAnalysis(CampaignAnalysis analysis) {
        if (analysis == null || analysis.getCampaignId() == null) {
            throw new IllegalStateException("DTO 或 campaignId 为空，不能继续使用。");
        }
        // String 可以保存任意文字，允许的风险等级需要程序自己限制。
        if (!("HIGH".equals(analysis.getRiskLevel()) || "MEDIUM".equals(analysis.getRiskLevel())
                || "LOW".equals(analysis.getRiskLevel()))) {
            throw new IllegalStateException("DTO 的 riskLevel 必须是 HIGH、MEDIUM 或 LOW。");
        }
        if (analysis.getReason() == null || analysis.getReason().isBlank()
                || analysis.getSuggestion() == null || analysis.getSuggestion().isBlank()) {
            throw new IllegalStateException("DTO 的 reason 和 suggestion 必须有非空白文本。");
        }
    }

    private static void runLocalErrors() {
        System.out.println("本地错误样本演示：不调用模型，不产生 API 用量。");

        // 缺少 suggestion：JSON 语法合法，预期仍能创建 DTO，但这个字段是 null。
        showInvalidSample("缺少 suggestion", """
                {
                  "campaignId": 10001,
                  "riskLevel": "HIGH",
                  "reason": "本地模拟原因"
                }
                """, "DTO 的 reason 和 suggestion 必须有非空白文本。");

        // CRITICAL 是合法字符串，预期 String 字段照样接收，再由字段检查拒绝。
        showInvalidSample("非法风险等级 CRITICAL", """
                {
                  "campaignId": 10001,
                  "riskLevel": "CRITICAL",
                  "reason": "本地模拟原因",
                  "suggestion": "本地模拟建议"
                }
                """, "DTO 的 riskLevel 必须是 HIGH、MEDIUM 或 LOW。");

        System.out.println("本地演示完成：两个样本都成功转成 DTO，但均被字段检查拒绝。");
    }

    private static void showInvalidSample(String label, String content, String expectedError) {
        System.out.println("\n=== " + label + " ===");
        System.out.println("模拟 JSON:");
        System.out.println(content);
        CampaignAnalysis analysis = JSON.parseObject(content, CampaignAnalysis.class);
        System.out.println("反序列化结果: " + analysis);
        if (analysis == null) {
            throw new IllegalStateException("本例预期可以创建 DTO，实际却得到了 null。");
        }
        // 先展示字段，再检查；这样能分清“转换时接受了什么”和“后面为什么拒绝”。
        System.out.println("suggestion == null: " + (analysis.getSuggestion() == null));
        System.out.println("riskLevel: " + analysis.getRiskLevel());
        try {
            validateCampaignAnalysis(analysis);
        } catch (IllegalStateException e) {
            // 同时核对拒绝原因，不能把任何异常都当成演示成功。
            if (!expectedError.equals(e.getMessage())) {
                throw e;
            }
            System.out.println("字段检查拒绝: " + e.getMessage());
            return;
        }
        throw new IllegalStateException("本例预期字段检查拒绝，但实际通过了。");
    }
}
