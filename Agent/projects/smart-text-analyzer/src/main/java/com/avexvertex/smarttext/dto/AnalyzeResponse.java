package com.avexvertex.smarttext.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文本分析响应对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文本分析结果")
public class AnalyzeResponse {

    /**
     * 文本摘要
     */
    @Schema(description = "文本摘要")
    private String summary;

    /**
     * 文本所属分类
     */
    @Schema(description = "文本分类", example = "科技")
    private String category;

    /**
     * 文本情感倾向
     */
    @Schema(description = "情感倾向", example = "positive")
    private String sentiment;

    /**
     * 从文本中提取出的关键词
     */
    @Schema(
            description = "关键词列表",
            example = "[\"ChatGPT\", \"程序员\", \"AI\"]"
    )
    private List<String> keywords;
}