package com.urr.commons.exception;

/**
 * 业务异常：用于可预期的业务校验失败（昵称重复、角色不存在等）。
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
