package com.urr.api.support;

import com.urr.commons.api.ResponseData;
import com.urr.commons.exception.BizException;
import com.urr.commons.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Iterator;

/**
 * 全局异常处理。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常。
     *
     * @param e 业务异常
     * @return 统一返回
     */
    @ExceptionHandler(BizException.class)
    public ResponseData handleBiz(BizException e) {
        return ResponseData.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理表单和基础参数错误。
     *
     * @param e 异常
     * @return 统一返回
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            IllegalArgumentException.class,
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseData handleBadRequest(Exception e) {
        return ResponseData.fail(ErrorCode.PARAM_INVALID, resolveBadRequestMessage(e));
    }

    /**
     * 处理未知异常。
     *
     * @param e 未知异常
     * @return 统一返回
     */
    @ExceptionHandler(Exception.class)
    public ResponseData handleUnknown(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseData.fail(ErrorCode.INTERNAL_ERROR, "系统异常");
    }

    /**
     * 解析参数错误消息。
     *
     * @param e 异常
     * @return 错误消息
     */
    private String resolveBadRequestMessage(Exception e) {
        if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;
            FieldError fieldError = ex.getBindingResult().getFieldError();
            if (fieldError != null && fieldError.getDefaultMessage() != null) {
                return fieldError.getDefaultMessage();
            }
        }

        if (e instanceof BindException) {
            BindException ex = (BindException) e;
            FieldError fieldError = ex.getBindingResult().getFieldError();
            if (fieldError != null && fieldError.getDefaultMessage() != null) {
                return fieldError.getDefaultMessage();
            }
        }

        if (e instanceof ConstraintViolationException) {
            ConstraintViolationException ex = (ConstraintViolationException) e;
            Iterator<ConstraintViolation<?>> iterator = ex.getConstraintViolations().iterator();
            if (iterator.hasNext()) {
                ConstraintViolation<?> violation = iterator.next();
                if (violation.getMessage() != null) {
                    return violation.getMessage();
                }
            }
        }

        if (e instanceof MissingServletRequestParameterException) {
            MissingServletRequestParameterException ex = (MissingServletRequestParameterException) e;
            return ex.getParameterName() + "不能为空";
        }

        if (e instanceof MethodArgumentTypeMismatchException) {
            MethodArgumentTypeMismatchException ex = (MethodArgumentTypeMismatchException) e;
            return ex.getName() + "参数格式错误";
        }

        if (e instanceof HttpMessageNotReadableException) {
            String message = e.getMessage();
            if (message != null && message.contains("Required request body is missing")) {
                return "请求体不能为空";
            }
            return "请求体格式错误";
        }

        if (e.getMessage() == null || e.getMessage().trim().isEmpty()) {
            return "参数错误";
        }
        return e.getMessage();
    }
}
