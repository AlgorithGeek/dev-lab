package com.avexvertex.smarttext.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文本分析请求对象
 */
@Data
@Schema(description = "文本分析请求")
public class AnalyzeRequest {

    /**
     * 需要分析的原始文本
     */
    @NotBlank(message = "待分析文本不能为空")
    @Schema(
            description = "需要进行分析的原始文本",
            example = "ChatGPT 正在改变程序员的开发方式"
    )
    private String text;
}