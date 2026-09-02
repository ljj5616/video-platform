package com.example.videoplatform.auth.verification;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class RedisVerificationStore implements VerificationStore {

    private static final String KEY_PREFIX = "find-id:";

    private static final DefaultRedisScript<Long> RESERVE_SEND_SCRIPT = new DefaultRedisScript<>("""
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            if current >= tonumber(ARGV[1]) then
                return 0
            end
            current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[2])
            end
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> RELEASE_SEND_SCRIPT = new DefaultRedisScript<>("""
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            if current <= 1 then
                redis.call('DEL', KEYS[1])
            else
                redis.call('DECR', KEYS[1])
            end
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> SAVE_CODE_SCRIPT = new DefaultRedisScript<>("""
            redis.call('HSET', KEYS[1], 'codeHash', ARGV[1], 'attempts', '0')
            redis.call('EXPIRE', KEYS[1], ARGV[2])
            redis.call('SET', KEYS[2], '1', 'EX', ARGV[3])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> VERIFY_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 0 then
                if redis.call('EXISTS', KEYS[2]) == 1 then
                    return -2
                end
                return -1
            end
            local attempts = tonumber(redis.call('HGET', KEYS[1], 'attempts') or '0')
            if attempts >= tonumber(ARGV[2]) then
                return -3
            end
            if redis.call('HGET', KEYS[1], 'codeHash') ~= ARGV[1] then
                redis.call('HINCRBY', KEYS[1], 'attempts', 1)
                return 0
            end
            redis.call('DEL', KEYS[1], KEYS[2])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisVerificationStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean reserveSendAttempt(String phone, int maxSendCount, long windowSeconds) {
        Long result = redisTemplate.execute(
                RESERVE_SEND_SCRIPT,
                List.of(sendLimitKey(phone)),
                Integer.toString(maxSendCount),
                Long.toString(windowSeconds)
        );
        return Long.valueOf(1).equals(result);
    }

    @Override
    public void releaseSendAttempt(String phone) {
        redisTemplate.execute(RELEASE_SEND_SCRIPT, List.of(sendLimitKey(phone)));
    }

    @Override
    public void saveCode(String phone, String codeHash, long expirationSeconds, long expiredMarkerSeconds) {
        redisTemplate.execute(
                SAVE_CODE_SCRIPT,
                List.of(codeKey(phone), expiredMarkerKey(phone)),
                codeHash,
                Long.toString(expirationSeconds),
                Long.toString(expiredMarkerSeconds)
        );
    }

    @Override
    public VerificationResult verify(String phone, String codeHash, int maxVerifyAttempts) {
        Long result = redisTemplate.execute(
                VERIFY_SCRIPT,
                List.of(codeKey(phone), expiredMarkerKey(phone)),
                codeHash,
                Integer.toString(maxVerifyAttempts)
        );
        if (result == null) {
            throw new IllegalStateException("Redis verification script returned no result");
        }
        return switch (result.intValue()) {
            case 1 -> VerificationResult.VERIFIED;
            case -2 -> VerificationResult.EXPIRED;
            case -3 -> VerificationResult.ATTEMPT_LIMIT_EXCEEDED;
            case 0, -1 -> VerificationResult.INVALID;
            default -> throw new IllegalStateException("Unexpected Redis verification result: " + result);
        };
    }

    private String codeKey(String phone) {
        return KEY_PREFIX + "code:" + hash(phone);
    }

    private String expiredMarkerKey(String phone) {
        return KEY_PREFIX + "issued:" + hash(phone);
    }

    private String sendLimitKey(String phone) {
        return KEY_PREFIX + "send-limit:" + hash(phone);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
