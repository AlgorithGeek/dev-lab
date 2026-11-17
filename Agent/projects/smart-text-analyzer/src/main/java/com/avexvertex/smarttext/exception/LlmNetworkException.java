package com.avexvertex.smarttext.exception;

/**
 * LLM API 网络通信异常。
 *
 * 用于表示调用 LLM API 时发生的网络 I/O 异常。
 */
public class LlmNetworkException extends RuntimeException {

    public LlmNetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}