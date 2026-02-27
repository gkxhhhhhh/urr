package com.urr.app.auth.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.urr.app.auth.AuthAppService;
import com.urr.domain.account.AccountEntity;
import com.urr.infra.auth.TokenStore;
import com.urr.infra.mapper.AccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthAppServiceImpl implements AuthAppService {

    private final AccountMapper accountMapper;
    private final TokenStore tokenStore;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public Long register(String account, String password, String registerIp) {
        // account 唯一校验（uk_account）
        Long cnt = accountMapper.selectCount(new LambdaQueryWrapper<AccountEntity>()
                .eq(AccountEntity::getAccount, account));
        if (cnt != null && cnt > 0) {
            throw new IllegalArgumentException("账号已存在");
        }

        AccountEntity e = new AccountEntity();
        e.setAccount(account);
        e.setPasswordHash(encoder.encode(password)); // BCrypt自带盐
        e.setSalt(null);                             // 允许 NULL
        e.setStatus(1);
        e.setRegisterIp(registerIp);
        e.setLastLoginIp(null);
        e.setLastLoginTime(null);

        // 审计字段若你有 MetaObjectHandler 会自动填
        accountMapper.insert(e);
        return e.getId();
    }

    @Override
    @Transactional
    public LoginResult login(String account, String password, String loginIp) {
        AccountEntity e = accountMapper.selectOne(new LambdaQueryWrapper<AccountEntity>()
                .eq(AccountEntity::getAccount, account));

        if (e == null || (e.getDeleteFlag() != null && e.getDeleteFlag() == 1)) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        if (e.getStatus() != null && e.getStatus() == 2) {
            throw new IllegalArgumentException("账号已封禁");
        }
        if (!encoder.matches(password, e.getPasswordHash())) {
            throw new IllegalArgumentException("账号或密码错误");
        }

        e.setLastLoginIp(loginIp);
        e.setLastLoginTime(LocalDateTime.now());
        accountMapper.updateById(e);

        String token = tokenStore.issueToken(e.getId());
        return new LoginResult(e.getId(), token);
    }

    @Override
    public void logout(String token) {
        tokenStore.revoke(token);
    }

    @Override
    public LoginResult getUser(String account, String password, String loginIp) {
        AccountEntity e = accountMapper.selectOne(new LambdaQueryWrapper<AccountEntity>()
                .eq(AccountEntity::getAccount, account));
        if(e == null){
            return null;
        }
        String token = tokenStore.issueToken(e.getId());
        return new LoginResult(e.getId(), token);
    }
}