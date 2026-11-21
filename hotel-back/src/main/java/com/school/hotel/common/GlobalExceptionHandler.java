package com.school.hotel.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public Result<Void> business(BusinessException ex) { return Result.error(ex.getCode(), ex.getMessage()); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return Result.error(400, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> validation(ConstraintViolationException ex) { return Result.error(400, ex.getMessage()); }

    @ExceptionHandler(org.springframework.web.method.annotation.HandlerMethodValidationException.class)
    public Result<Void> methodValidation(org.springframework.web.method.annotation.HandlerMethodValidationException ex) {
        return Result.error(400,"请求参数超出允许范围");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public Result<Void> credentials(BadCredentialsException ex) { return Result.error(401, "账号或密码错误"); }

    @ExceptionHandler({org.springframework.web.bind.MissingServletRequestParameterException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public Result<Void> malformedQuery(Exception ex) {
        return Result.error(400,"请求参数缺失或格式错误，请检查后重试");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> denied(AccessDeniedException ex) { return Result.error(403, "当前账号没有访问权限"); }

    @ExceptionHandler(Exception.class)
    public Result<Void> fallback(Exception ex) {
        log.error("Unhandled server exception", ex);
        return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器内部错误");
    }
}
