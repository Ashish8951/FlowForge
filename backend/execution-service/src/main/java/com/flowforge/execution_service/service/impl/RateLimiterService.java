package com.flowforge.execution_service.service.impl;

import com.flowforge.execution_service.config.RedisKeys;
import com.flowforge.execution_service.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiterService {

    private static final int MAX_TRIGGERS_PER_MINUTE = 10;

    private final StringRedisTemplate redis;

    public void checkRateLimit(Long userId) {
        String key = RedisKeys.rateLimitKey(userId);
        try {
            Long count = redis.opsForValue().increment(key);
            if (Long.valueOf(1L).equals(count)) {
                redis.expire(key, RedisKeys.RATE_LIMIT_TTL_SEC, TimeUnit.SECONDS);
            }
            if (count != null && count > MAX_TRIGGERS_PER_MINUTE) {
                log.warn("Rate limit exceeded for userId={} — count={}/{}", userId, count, MAX_TRIGGERS_PER_MINUTE);
                throw new RateLimitExceededException(
                        "Rate limit exceeded: max " + MAX_TRIGGERS_PER_MINUTE
                                + " trigger(s) per minute per user. Retry after 60 seconds.");
            }
        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Rate limit check failed for userId={} — proceeding without limit: {}", userId, e.getMessage());
        }
    }
}
