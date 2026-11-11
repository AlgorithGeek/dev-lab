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
        请分析下面这段文本。

        分析要求：
        1. summary：简洁概括文本主要内容
        2. category：给出一个最合适的文本分类
        3. sentiment：判断文本整体情感倾向
        4. keywords：提取最重要的关键词

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