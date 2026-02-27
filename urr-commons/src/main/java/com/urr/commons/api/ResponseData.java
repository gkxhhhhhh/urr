package com.urr.commons.api;

import lombok.Data;

/**
 * 统一返回体（前后端对接时更稳定，后续也方便做网关签名/监控）。
 */
@Data
public class ResponseData<T> {

    private int code;
    private String message;
    private T data;

    public static <T> ResponseData<T> success(T data) {
        ResponseData<T> r = new ResponseData<>();
        r.code = 0;
        r.message = "OK";
        r.data = data;
        return r;
    }

    public static <T> ResponseData<T> fail(int code, String message) {
        ResponseData<T> r = new ResponseData<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
