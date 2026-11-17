package com.avexvertex.smarttext.exception;

import com.avexvertex.smarttext.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * <p>
 * 统一捕获 Controller 层抛出的异常，
 * 并转换成项目统一的 JSON 错误响应。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理请求参数校验失败。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("请求参数不合法");

        return Result.fail(400, message);
    }

    /**
     * 处理 LLM API 调用失败
     */
    @ExceptionHandler(LlmApiException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Result<Void> handleLlmApiException(LlmApiException e){

        log.error("LLM API 调用失败，Provider HTTP 状态码：{}", e.getStatusCode(), e);

        return Result.fail(502,"大模型服务调用失败，请稍后重试");
    }

    /**
     * 处理 LLM API 请求超时
     */
    @ExceptionHandler(LlmTimeoutException.class)
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    public Result<Void> handleLlmApiTimeoutException(LlmTimeoutException e){

        log.error("LLM API 请求超时", e);

        return Result.fail(504, "大模型服务响应超时，请稍后重试");
    }

    /**
     * 处理 LLM API 网络通信异常
     */
    @ExceptionHandler(LlmNetworkException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Result<Void> handleLlmNetworkException(LlmNetworkException e) {

        log.error("LLM API 网络通信失败", e);

        return Result.fail(502, "大模型服务通信失败，请稍后重试");
    }

    /**
     * 处理 LLM API 调用线程中断。
     */
    @ExceptionHandler(LlmInterruptedException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Result<Void> handleLlmInterruptedException(LlmInterruptedException e) {

        log.warn("LLM API 调用线程被中断", e);

        return Result.fail(503, "服务暂时无法完成请求，请稍后重试");
    }

    /**
     * 处理请求体 JSON 解析失败。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e) {

        log.warn("请求体格式错误", e);

        return Result.fail(400, "请求体格式不正确");
    }

    /**
     * 处理其他未预期异常。
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {

        log.error("系统发生未预期异常", e);

        return Result.fail(500, "服务器内部错误，请稍后重试");
    }
}
