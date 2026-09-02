package com.example.videoplatform.auth.verification;

public interface VerificationStore {

    boolean reserveSendAttempt(String phone, int maxSendCount, long windowSeconds);

    void releaseSendAttempt(String phone);

    void saveCode(String phone, String codeHash, long expirationSeconds, long expiredMarkerSeconds);

    VerificationResult verify(String phone, String codeHash, int maxVerifyAttempts);

    enum VerificationResult {
        VERIFIED,
        INVALID,
        EXPIRED,
        ATTEMPT_LIMIT_EXCEEDED
    }
}
