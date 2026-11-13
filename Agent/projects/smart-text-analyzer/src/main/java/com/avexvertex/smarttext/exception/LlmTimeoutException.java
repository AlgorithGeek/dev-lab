package com.avexvertex.smarttext.exception;

/**
 * LLM API 请求超时异常。
 */
public class LlmTimeoutException extends RuntimeException {

    public LlmTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

}
