package com.urr.api.support;

import com.urr.commons.api.ResponseData;
import com.urr.commons.exception.BizException;
import com.urr.commons.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseData<Void> handleBiz(BizException e) {
        return ResponseData.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, IllegalArgumentException.class})
    public ResponseData<Void> handleBadRequest(Exception e) {
        String msg = e.getMessage();
        if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;
            if (ex.getBindingResult().getFieldError() != null) {
                msg = ex.getBindingResult().getFieldError().getDefaultMessage();
            }
        }
        return ResponseData.fail(ErrorCode.PARAM_INVALID, msg == null ? "参数错误" : msg);
    }

    @ExceptionHandler(Exception.class)
    public ResponseData<Void> handleUnknown(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseData.fail(ErrorCode.INTERNAL_ERROR, "系统异常");
    }
}
