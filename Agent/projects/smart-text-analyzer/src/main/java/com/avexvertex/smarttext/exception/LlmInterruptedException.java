package com.avexvertex.smarttext.exception;

/**
 * LLM API 调用线程中断异常。
 */
public class LlmInterruptedException extends RuntimeException {

    public LlmInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }
}