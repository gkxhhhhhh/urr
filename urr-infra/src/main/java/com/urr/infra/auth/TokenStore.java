package com.urr.infra.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TokenStore {

    private final StringRedisTemplate redis;

    private static final String KEY_PREFIX = "urr:token:";
    // 7天有效期
    private static final Duration TTL = Duration.ofDays(7);

    public String issueToken(Long accountId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(KEY_PREFIX + token, String.valueOf(accountId), TTL);
        return token;
    }

    public Long getAccountId(String token) {
        String v = redis.opsForValue().get(KEY_PREFIX + token);
        return v == null ? null : Long.valueOf(v);
    }

    public void revoke(String token) {
        redis.delete(KEY_PREFIX + token);
    }
}