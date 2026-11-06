package com.avexvertex.smarttext.service;

import com.avexvertex.smarttext.dto.AnalyzeRequest;
import com.avexvertex.smarttext.dto.AnalyzeResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 文本分析业务服务
 *
 * 当前阶段暂时返回模拟数据，
 * 后续会将这里替换为真实的 LLM 调用。
 */
@Service
public class TextAnalysisService {

    /**
     * 分析用户提交的文本
     *
     * @param request 文本分析请求
     * @return 文本分析结果
     */
    public AnalyzeResponse analyze(AnalyzeRequest request) {

        // 当前暂不调用 AI，先返回固定数据，
        // 用于验证 Controller → Service → Response 整条链路。
        return new AnalyzeResponse(
                "这是一段模拟的文本摘要",
                "科技",
                "positive",
                List.of("AI", "ChatGPT", "程序员")
        );
    }
}