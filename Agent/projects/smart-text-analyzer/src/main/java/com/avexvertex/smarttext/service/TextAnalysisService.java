package com.avexvertex.smarttext.service;

import com.alibaba.fastjson2.JSONObject;
import com.avexvertex.smarttext.dto.AnalyzeRequest;
import com.avexvertex.smarttext.dto.AnalyzeResponse;
import com.avexvertex.smarttext.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文本分析业务服务
 */
@Service
@RequiredArgsConstructor
public class TextAnalysisService {

    private final LlmClient llmClient;

    /**
     * 分析用户提交的文本
     *
     * @param request 文本分析请求
     * @return 文本分析结果
     */
    public AnalyzeResponse analyze(AnalyzeRequest request) {

        String prompt = """
        请分析下面这段文本，并严格按照 JSON 格式返回结果。

        JSON 必须包含以下字段：

        {
          "summary": "文本摘要",
          "category": "文本分类",
          "sentiment": "positive、neutral 或 negative",
          "keywords": ["关键词1", "关键词2"]
        }

        要求：
        1. summary 简洁概括文本主要内容
        2. category 给出一个最合适的分类
        3. sentiment 只能是 positive、neutral、negative 之一
        4. keywords 提取 3～5 个关键词
        5. 只返回 JSON，不要输出其他解释

        待分析文本：
        %s
        """.formatted(request.getText());
        String content = llmClient.chat(prompt);

        return JSONObject.parseObject(
                content,
                AnalyzeResponse.class
        );
    }
}