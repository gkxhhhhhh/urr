package com.urr.app.auth;

public interface AuthAppService {

    Long register(String account, String password, String registerIp);

    LoginResult login(String account, String password, String loginIp);

    void logout(String token);

    LoginResult getUser(String account, String password, String loginIp);

    class LoginResult {
        public Long accountId;
        public String token;

        public LoginResult(Long accountId, String token) {
            this.accountId = accountId;
            this.token = token;
        }
    }
}