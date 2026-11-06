package com.avexvertex.smarttext.controller;

import com.avexvertex.smarttext.dto.AnalyzeRequest;
import com.avexvertex.smarttext.dto.AnalyzeResponse;
import com.avexvertex.smarttext.service.TextAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文本分析接口
 * <p>
 * 负责接收客户端请求，并调用文本分析业务服务。
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Tag(name = "文本分析", description = "基于 AI 的文本分析接口")
public class TextAnalysisController {
    /**
     * 文本分析业务服务
     */
    private final TextAnalysisService textAnalysisService;

    /**
     * 分析用户提交的文本
     *
     * @param request 文本分析请求
     * @return 文本分析结果
     */
    @PostMapping("/analyze")
    @Operation(
            summary = "分析文本",
            description = "对输入文本进行摘要、分类、情感分析以及关键词提取"
    )
    public AnalyzeResponse analyze(@RequestBody AnalyzeRequest request){

        return textAnalysisService.analyze(request);

    }
}
