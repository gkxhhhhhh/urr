package com.urr.api.auth;

import com.urr.app.auth.AuthAppService;
import com.urr.api.util.IpUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    @Resource
    private AuthAppService authAppService;

    @PostMapping("/register")
    public Long register(@RequestBody RegisterReq req, HttpServletRequest request) {
        String ip = IpUtils.getClientIp(request);
        return authAppService.register(req.account, req.password, ip);
    }

    @PostMapping("/guestRegister")
    public Long guestRegister(@RequestBody RegisterReq req, HttpServletRequest request) {
        String ip = IpUtils.getClientIp(request);
        return authAppService.register(req.account, req.password, ip);
    }

    @PostMapping("/guestLogin")
    public AuthAppService.LoginResult guestLogin(@RequestBody RegisterReq req, HttpServletRequest request) {
        String ip = IpUtils.getClientIp(request);
        req.setPassword("guestLogin");
        AuthAppService.LoginResult login = authAppService.getUser(req.account, req.password, ip);
        if(login == null){
            authAppService.register(req.account, req.password, ip);
        }
        return authAppService.login(req.account, req.password, ip);
    }

    @PostMapping("/login")
    public AuthAppService.LoginResult login(@RequestBody LoginReq req, HttpServletRequest request) {
        String ip = IpUtils.getClientIp(request);
        return authAppService.login(req.account, req.password, ip);
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (auth == null || !auth.startsWith("Bearer ")) return;
        authAppService.logout(auth.substring("Bearer ".length()).trim());
    }

    @GetMapping("/me")
    public Long me() {
        Long accountId = AuthContext.getAccountId();
        if (accountId == null) throw new IllegalArgumentException("未登录");
        return accountId;
    }

    @Data
    public static class RegisterReq {
        @NotBlank public String account;
        @NotBlank public String password;
    }

    @Data
    public static class LoginReq {
        @NotBlank public String account;
        @NotBlank public String password;
    }
}