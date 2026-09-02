package com.example.videoplatform.auth.service;

import com.example.videoplatform.auth.dto.PasswordResetRequest;
import com.example.videoplatform.auth.dto.PasswordResetVerifyRequest;
import com.example.videoplatform.auth.dto.PasswordResetVerifyResponse;
import com.example.videoplatform.auth.dto.PasswordUpdateRequest;
import com.example.videoplatform.auth.email.EmailSender;
import com.example.videoplatform.auth.repository.RefreshTokenRepository;
import com.example.videoplatform.auth.verification.PasswordResetStore;
import com.example.videoplatform.auth.verification.VerificationCodeGenerator;
import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.user.entity.User;
import com.example.videoplatform.user.entity.UserStatus;
import com.example.videoplatform.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PasswordResetService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetStore store;
    private final VerificationCodeGenerator codeGenerator;
    private final EmailSender emailSender;
    private final PasswordEncoder passwordEncoder;
    private final long codeExpirationSeconds;
    private final long sendWindowSeconds;
    private final int maxSendCount;
    private final int maxVerifyAttempts;
    private final long tokenExpirationSeconds;

    public PasswordResetService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
            PasswordResetStore store, VerificationCodeGenerator codeGenerator, EmailSender emailSender,
            PasswordEncoder passwordEncoder,
            @Value("${verification.code-expiration-seconds:180}") long codeExpirationSeconds,
            @Value("${verification.send-window-seconds:3600}") long sendWindowSeconds,
            @Value("${verification.max-send-count:5}") int maxSendCount,
            @Value("${verification.max-verify-attempts:5}") int maxVerifyAttempts,
            @Value("${password-reset.token-expiration-seconds:600}") long tokenExpirationSeconds) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.store = store;
        this.codeGenerator = codeGenerator;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
        this.codeExpirationSeconds = codeExpirationSeconds;
        this.sendWindowSeconds = sendWindowSeconds;
        this.maxSendCount = maxSendCount;
        this.maxVerifyAttempts = maxVerifyAttempts;
        this.tokenExpirationSeconds = tokenExpirationSeconds;
    }

    public void sendCode(PasswordResetRequest request) {
        if (userRepository.findByEmailAndStatus(request.email(), UserStatus.ACTIVE).isEmpty()) return;
        if (!store.reserveSendAttempt(request.email(), maxSendCount, sendWindowSeconds)) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_SEND_LIMIT_EXCEEDED);
        }
        String code = codeGenerator.generate();
        try {
            emailSender.sendPasswordResetCode(request.email(), code);
        } catch (RuntimeException exception) {
            store.releaseSendAttempt(request.email());
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
        try {
            store.saveCode(request.email(), hash(code), codeExpirationSeconds, codeExpirationSeconds + sendWindowSeconds);
        } catch (RuntimeException exception) {
            store.releaseSendAttempt(request.email());
            throw exception;
        }
    }

    public PasswordResetVerifyResponse verify(PasswordResetVerifyRequest request) {
        if (userRepository.findByEmailAndStatus(request.email(), UserStatus.ACTIVE).isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_RESET_CODE);
        }
        PasswordResetStore.CodeVerificationResult result = store.verifyCode(
                request.email(), hash(request.verificationCode()), maxVerifyAttempts);
        switch (result) {
            case INVALID -> throw new BusinessException(ErrorCode.INVALID_PASSWORD_RESET_CODE);
            case EXPIRED -> throw new BusinessException(ErrorCode.PASSWORD_RESET_CODE_EXPIRED);
            case ATTEMPT_LIMIT_EXCEEDED -> throw new BusinessException(ErrorCode.PASSWORD_RESET_VERIFY_LIMIT_EXCEEDED);
            case VERIFIED -> {
                String token = UUID.randomUUID().toString();
                store.saveResetToken(hash(token), request.email(), tokenExpirationSeconds,
                        tokenExpirationSeconds + sendWindowSeconds);
                return new PasswordResetVerifyResponse(token, tokenExpirationSeconds);
            }
        }
        throw new IllegalStateException("Unhandled password reset verification result: " + result);
    }

    @Transactional
    public void resetPassword(PasswordUpdateRequest request) {
        PasswordResetStore.TokenConsumptionResult result = store.consumeResetToken(hash(request.resetToken()));
        switch (result.status()) {
            case INVALID -> throw new BusinessException(ErrorCode.INVALID_RESET_TOKEN);
            case EXPIRED -> throw new BusinessException(ErrorCode.EXPIRED_RESET_TOKEN);
            case USED -> throw new BusinessException(ErrorCode.USED_RESET_TOKEN);
            case CONSUMED -> {
                User user = userRepository.findByEmailAndStatus(result.email(), UserStatus.ACTIVE)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_RESET_TOKEN));
                user.changePassword(passwordEncoder.encode(request.newPassword()));
                refreshTokenRepository.deleteByUserId(user.getId());
            }
        }
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
