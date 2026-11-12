package com.avexvertex.smarttext.exception;

import lombok.Data;

@Data
public class LlmApiException extends RuntimeException {

    /**
     * Provider 返回的 HTTP 状态码。
     */
    private final int statusCode;

    /**
     * Provider 返回的原始响应内容。
     */
    private final String responseBody;

    public LlmApiException(int statusCode, String responseBody) {
        super("LLM API 调用失败，HTTP 状态码：" + statusCode);

        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
}
