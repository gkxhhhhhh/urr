package com.urr.api.auth;

public class AuthRequired {
    public static Long requireAccountId() {
        Long id = AuthContext.getAccountId();
        if (id == null) throw new IllegalArgumentException("未登录");
        return id;
    }
}