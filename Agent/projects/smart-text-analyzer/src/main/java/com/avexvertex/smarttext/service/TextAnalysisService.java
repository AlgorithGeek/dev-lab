package com.avexvertex.smarttext.service;

import com.avexvertex.smarttext.dto.AnalyzeRequest;
import com.avexvertex.smarttext.dto.AnalyzeResponse;
import com.avexvertex.smarttext.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文本分析业务服务
 *
 * 当前阶段暂时返回模拟数据，
 * 后续会将这里替换为真实的 LLM 调用。
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

        // 将用户输入直接发送给大模型
        String content = llmClient.chat(request.getText());

        return new AnalyzeResponse(
                content,
                "待实现",
                "待实现",
                List.of()
        );
    }
}