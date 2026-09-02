package com.example.videoplatform.auth.verification;

public interface PasswordResetStore {
    boolean reserveSendAttempt(String email, int maxSendCount, long windowSeconds);
    void releaseSendAttempt(String email);
    void saveCode(String email, String codeHash, long expirationSeconds, long expiredMarkerSeconds);
    CodeVerificationResult verifyCode(String email, String codeHash, int maxVerifyAttempts);
    void saveResetToken(String tokenHash, String email, long expirationSeconds, long markerSeconds);
    TokenConsumptionResult consumeResetToken(String tokenHash);

    enum CodeVerificationResult { VERIFIED, INVALID, EXPIRED, ATTEMPT_LIMIT_EXCEEDED }
    record TokenConsumptionResult(TokenStatus status, String email) {}
    enum TokenStatus { CONSUMED, INVALID, EXPIRED, USED }
}
