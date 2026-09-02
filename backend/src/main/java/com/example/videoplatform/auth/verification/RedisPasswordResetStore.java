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
public class RedisPasswordResetStore implements PasswordResetStore {
    private static final String PREFIX = "password-reset:";
    private static final DefaultRedisScript<Long> RESERVE = new DefaultRedisScript<>("""
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            if current >= tonumber(ARGV[1]) then return 0 end
            current = redis.call('INCR', KEYS[1])
            if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[2]) end
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>("""
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            if current <= 1 then redis.call('DEL', KEYS[1]) else redis.call('DECR', KEYS[1]) end
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> SAVE_CODE = new DefaultRedisScript<>("""
            redis.call('HSET', KEYS[1], 'codeHash', ARGV[1], 'attempts', '0')
            redis.call('EXPIRE', KEYS[1], ARGV[2])
            redis.call('SET', KEYS[2], '1', 'EX', ARGV[3])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> VERIFY_CODE = new DefaultRedisScript<>("""
            if redis.call('EXISTS', KEYS[1]) == 0 then
                if redis.call('EXISTS', KEYS[2]) == 1 then return -2 end
                return -1
            end
            local attempts = tonumber(redis.call('HGET', KEYS[1], 'attempts') or '0')
            if attempts >= tonumber(ARGV[2]) then return -3 end
            if redis.call('HGET', KEYS[1], 'codeHash') ~= ARGV[1] then
                redis.call('HINCRBY', KEYS[1], 'attempts', 1)
                return 0
            end
            redis.call('DEL', KEYS[1], KEYS[2])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<List> CONSUME_TOKEN = new DefaultRedisScript<>("""
            local email = redis.call('GET', KEYS[1])
            if email then
                redis.call('DEL', KEYS[1])
                redis.call('SET', KEYS[2], 'used', 'KEEPTTL')
                return {'consumed', email}
            end
            local marker = redis.call('GET', KEYS[2])
            if marker == 'used' then return {'used', ''} end
            if marker == 'issued' then return {'expired', ''} end
            return {'invalid', ''}
            """, List.class);

    private final StringRedisTemplate redisTemplate;
    public RedisPasswordResetStore(StringRedisTemplate redisTemplate) { this.redisTemplate = redisTemplate; }

    public boolean reserveSendAttempt(String email, int max, long seconds) {
        return Long.valueOf(1).equals(redisTemplate.execute(RESERVE, List.of(sendKey(email)), Integer.toString(max), Long.toString(seconds)));
    }
    public void releaseSendAttempt(String email) { redisTemplate.execute(RELEASE, List.of(sendKey(email))); }
    public void saveCode(String email, String codeHash, long expiry, long marker) {
        redisTemplate.execute(SAVE_CODE, List.of(codeKey(email), codeMarkerKey(email)), codeHash, Long.toString(expiry), Long.toString(marker));
    }
    public CodeVerificationResult verifyCode(String email, String codeHash, int max) {
        Long value = redisTemplate.execute(VERIFY_CODE, List.of(codeKey(email), codeMarkerKey(email)), codeHash, Integer.toString(max));
        if (value == null) throw new IllegalStateException("Redis password reset script returned no result");
        return switch (value.intValue()) {
            case 1 -> CodeVerificationResult.VERIFIED;
            case -2 -> CodeVerificationResult.EXPIRED;
            case -3 -> CodeVerificationResult.ATTEMPT_LIMIT_EXCEEDED;
            case 0, -1 -> CodeVerificationResult.INVALID;
            default -> throw new IllegalStateException("Unexpected password reset result: " + value);
        };
    }
    public void saveResetToken(String tokenHash, String email, long expiry, long marker) {
        redisTemplate.opsForValue().set(tokenKey(tokenHash), email, java.time.Duration.ofSeconds(expiry));
        redisTemplate.opsForValue().set(tokenMarkerKey(tokenHash), "issued", java.time.Duration.ofSeconds(marker));
    }
    @SuppressWarnings("unchecked")
    public TokenConsumptionResult consumeResetToken(String tokenHash) {
        List<String> result = (List<String>) redisTemplate.execute(CONSUME_TOKEN, List.of(tokenKey(tokenHash), tokenMarkerKey(tokenHash)));
        if (result == null || result.isEmpty()) throw new IllegalStateException("Redis reset token script returned no result");
        return switch (result.get(0)) {
            case "consumed" -> new TokenConsumptionResult(TokenStatus.CONSUMED, result.get(1));
            case "expired" -> new TokenConsumptionResult(TokenStatus.EXPIRED, null);
            case "used" -> new TokenConsumptionResult(TokenStatus.USED, null);
            default -> new TokenConsumptionResult(TokenStatus.INVALID, null);
        };
    }
    private String codeKey(String email) { return PREFIX + "code:" + hash(email); }
    private String codeMarkerKey(String email) { return PREFIX + "code-issued:" + hash(email); }
    private String sendKey(String email) { return PREFIX + "send-limit:" + hash(email); }
    private String tokenKey(String hash) { return PREFIX + "token:" + hash; }
    private String tokenMarkerKey(String hash) { return PREFIX + "token-state:" + hash; }
    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 is not available", e); }
    }
}
