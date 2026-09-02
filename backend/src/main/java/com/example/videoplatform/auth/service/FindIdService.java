package com.example.videoplatform.auth.service;

import com.example.videoplatform.auth.dto.FindIdCodeRequest;
import com.example.videoplatform.auth.dto.FindIdResponse;
import com.example.videoplatform.auth.dto.FindIdVerifyRequest;
import com.example.videoplatform.auth.sms.SmsSender;
import com.example.videoplatform.auth.verification.VerificationCodeGenerator;
import com.example.videoplatform.auth.verification.VerificationStore;
import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.user.entity.User;
import com.example.videoplatform.user.entity.UserStatus;
import com.example.videoplatform.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FindIdService {

    private final UserRepository userRepository;
    private final VerificationStore verificationStore;
    private final VerificationCodeGenerator codeGenerator;
    private final SmsSender smsSender;
    private final long codeExpirationSeconds;
    private final long sendWindowSeconds;
    private final int maxSendCount;
    private final int maxVerifyAttempts;

    public FindIdService(
            UserRepository userRepository,
            VerificationStore verificationStore,
            VerificationCodeGenerator codeGenerator,
            SmsSender smsSender,
            @Value("${verification.code-expiration-seconds:180}") long codeExpirationSeconds,
            @Value("${verification.send-window-seconds:3600}") long sendWindowSeconds,
            @Value("${verification.max-send-count:5}") int maxSendCount,
            @Value("${verification.max-verify-attempts:5}") int maxVerifyAttempts
    ) {
        this.userRepository = userRepository;
        this.verificationStore = verificationStore;
        this.codeGenerator = codeGenerator;
        this.smsSender = smsSender;
        this.codeExpirationSeconds = codeExpirationSeconds;
        this.sendWindowSeconds = sendWindowSeconds;
        this.maxSendCount = maxSendCount;
        this.maxVerifyAttempts = maxVerifyAttempts;
    }

    @Transactional
    public void sendCode(FindIdCodeRequest request) {
        findActiveUser(request.name(), request.phone());
        if (!verificationStore.reserveSendAttempt(request.phone(), maxSendCount, sendWindowSeconds)) {
            throw new BusinessException(ErrorCode.VERIFICATION_SEND_LIMIT_EXCEEDED);
        }
        String code = codeGenerator.generate();
        try {
            smsSender.sendVerificationCode(request.phone(), code);
        } catch (RuntimeException exception) {
            verificationStore.releaseSendAttempt(request.phone());
            throw new BusinessException(ErrorCode.SMS_SEND_FAILED);
        }
        try {
            verificationStore.saveCode(
                    request.phone(), hash(code), codeExpirationSeconds, codeExpirationSeconds + sendWindowSeconds);
        } catch (RuntimeException exception) {
            verificationStore.releaseSendAttempt(request.phone());
            throw exception;
        }
    }

    @Transactional
    public FindIdResponse verify(FindIdVerifyRequest request) {
        User user = findActiveUser(request.name(), request.phone());
        VerificationStore.VerificationResult result = verificationStore.verify(
                request.phone(), hash(request.verificationCode()), maxVerifyAttempts);
        switch (result) {
            case INVALID -> throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
            case EXPIRED -> throw new BusinessException(ErrorCode.VERIFICATION_CODE_EXPIRED);
            case ATTEMPT_LIMIT_EXCEEDED ->
                    throw new BusinessException(ErrorCode.VERIFICATION_ATTEMPT_LIMIT_EXCEEDED);
            case VERIFIED -> {
                return new FindIdResponse(maskEmail(user.getEmail()));
            }
        }
        throw new IllegalStateException("Unhandled verification result: " + result);
    }

    private User findActiveUser(String name, String phone) {
        return userRepository.findByNameAndPhoneAndStatus(name, phone, UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.FIND_ID_USER_NOT_FOUND));
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        String localPart = email.substring(0, atIndex);
        int visibleLength = Math.min(2, Math.max(0, localPart.length() - 1));
        return localPart.substring(0, visibleLength)
                + "*".repeat(localPart.length() - visibleLength)
                + email.substring(atIndex);
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
