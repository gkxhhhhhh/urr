package com.urr.commons.exception;

/**
 * URR 错误码（先覆盖最基础的玩家接口，后续逐步扩展：结算/制造/市场/副本）。
 */
public interface ErrorCode {
    int PARAM_INVALID = 400;
    int NOT_FOUND = 404;
    int CONFLICT = 409;
    int INTERNAL_ERROR = 500;
}
