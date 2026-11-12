package com.avexvertex.smarttext.exception;

import com.avexvertex.smarttext.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

        // 记录上游 Provider 返回的 HTTP 状态码。
        log.error("LLM API 调用失败，Provider HTTP 状态码：{}", e.getStatusCode(), e);

        return Result.fail(502,"大模型服务调用失败，请稍后重试");
    }

}
