package com.urr.api.auth;

public class AuthContext {
    private static final ThreadLocal<Long> ACCOUNT_ID = new ThreadLocal<>();

    public static void setAccountId(Long id) { ACCOUNT_ID.set(id); }
    public static Long getAccountId() { return ACCOUNT_ID.get(); }
    public static void clear() { ACCOUNT_ID.remove(); }
}