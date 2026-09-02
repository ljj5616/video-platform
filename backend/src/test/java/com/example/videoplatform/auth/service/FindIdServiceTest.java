package com.example.videoplatform.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.videoplatform.auth.dto.FindIdCodeRequest;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindIdServiceTest {

    private static final String PHONE = "01012345678";

    @Mock UserRepository userRepository;
    @Mock VerificationStore verificationStore;
    @Mock VerificationCodeGenerator codeGenerator;
    @Mock SmsSender smsSender;

    private FindIdService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new FindIdService(userRepository, verificationStore, codeGenerator, smsSender,
                180, 3600, 5, 5);
        user = User.create("user@example.com", "hash", "nickname", "홍길동", PHONE);
    }

    @Test
    void sendsAndStoresHashedVerificationCodeInRedisStore() {
        stubUser();
        when(verificationStore.reserveSendAttempt(PHONE, 5, 3600)).thenReturn(true);
        when(codeGenerator.generate()).thenReturn("123456");

        service.sendCode(new FindIdCodeRequest("홍길동", PHONE));

        verify(smsSender).sendVerificationCode(PHONE, "123456");
        verify(verificationStore).saveCode(PHONE, hash("123456"), 180, 3780);
    }

    @Test
    void rejectsSendWhenRateLimitIsExhausted() {
        stubUser();
        when(verificationStore.reserveSendAttempt(PHONE, 5, 3600)).thenReturn(false);

        assertThatThrownBy(() -> service.sendCode(new FindIdCodeRequest("홍길동", PHONE)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.VERIFICATION_SEND_LIMIT_EXCEEDED));
    }

    @Test
    void releasesSendAttemptWhenSmsDeliveryFails() {
        stubUser();
        when(verificationStore.reserveSendAttempt(PHONE, 5, 3600)).thenReturn(true);
        when(codeGenerator.generate()).thenReturn("123456");
        org.mockito.Mockito.doThrow(new IllegalStateException("delivery failed"))
                .when(smsSender).sendVerificationCode(PHONE, "123456");

        assertThatThrownBy(() -> service.sendCode(new FindIdCodeRequest("홍길동", PHONE)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SMS_SEND_FAILED));
        verify(verificationStore).releaseSendAttempt(PHONE);
    }

    @Test
    void verifiesCodeAndReturnsMaskedEmail() {
        stubUser();
        when(verificationStore.verify(PHONE, hash("123456"), 5))
                .thenReturn(VerificationStore.VerificationResult.VERIFIED);

        var response = service.verify(new FindIdVerifyRequest("홍길동", PHONE, "123456"));

        assertThat(response.email()).isEqualTo("us**@example.com");
    }

    @Test
    void mapsExpiredRedisCodeToBusinessError() {
        stubUser();
        when(verificationStore.verify(PHONE, hash("123456"), 5))
                .thenReturn(VerificationStore.VerificationResult.EXPIRED);

        assertThatThrownBy(() -> service.verify(new FindIdVerifyRequest("홍길동", PHONE, "123456")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VERIFICATION_CODE_EXPIRED));
    }

    private void stubUser() {
        when(userRepository.findByNameAndPhoneAndStatus("홍길동", PHONE, UserStatus.ACTIVE))
                .thenReturn(Optional.of(user));
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
