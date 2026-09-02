package com.example.videoplatform.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.videoplatform.auth.dto.PasswordResetRequest;
import com.example.videoplatform.auth.dto.PasswordResetVerifyRequest;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {
    private static final String EMAIL = "user@example.com";
    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordResetStore store;
    @Mock VerificationCodeGenerator codeGenerator;
    @Mock EmailSender emailSender;
    @Mock PasswordEncoder passwordEncoder;
    private PasswordResetService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(userRepository, refreshTokenRepository, store, codeGenerator,
                emailSender, passwordEncoder, 180, 3600, 5, 5, 600);
        user = User.create(EMAIL, "old-hash", "nickname", "홍길동", "01012345678");
    }

    @Test
    void hidesWhetherUnregisteredEmailExists() {
        service.sendCode(new PasswordResetRequest("missing@example.com"));
        verify(emailSender, never()).sendPasswordResetCode(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void sendsAndStoresCodeForActiveUser() {
        when(userRepository.findByEmailAndStatus(EMAIL, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(store.reserveSendAttempt(EMAIL, 5, 3600)).thenReturn(true);
        when(codeGenerator.generate()).thenReturn("123456");
        service.sendCode(new PasswordResetRequest(EMAIL));
        verify(emailSender).sendPasswordResetCode(EMAIL, "123456");
        verify(store).saveCode(EMAIL, hash("123456"), 180, 3780);
    }

    @Test
    void issuesTenMinuteOneTimeTokenAfterVerification() {
        when(userRepository.findByEmailAndStatus(EMAIL, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(store.verifyCode(EMAIL, hash("123456"), 5))
                .thenReturn(PasswordResetStore.CodeVerificationResult.VERIFIED);
        var response = service.verify(new PasswordResetVerifyRequest(EMAIL, "123456"));
        assertThat(response.resetToken()).isNotBlank();
        assertThat(response.expiresIn()).isEqualTo(600);
        verify(store).saveResetToken(hash(response.resetToken()), EMAIL, 600, 4200);
    }

    @Test
    void changesPasswordAndRevokesRefreshToken() {
        String token = "a8f30c1d-9a25-4b27-aaaa-bbbbbbbbbbbb";
        when(store.consumeResetToken(hash(token))).thenReturn(new PasswordResetStore.TokenConsumptionResult(
                PasswordResetStore.TokenStatus.CONSUMED, EMAIL));
        when(userRepository.findByEmailAndStatus(EMAIL, UserStatus.ACTIVE)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("new-hash");
        service.resetPassword(new PasswordUpdateRequest(token, "NewPassword123!"));
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(refreshTokenRepository).deleteByUserId(user.getId());
    }

    @Test
    void rejectsUsedResetToken() {
        when(store.consumeResetToken(hash("used-token"))).thenReturn(new PasswordResetStore.TokenConsumptionResult(
                PasswordResetStore.TokenStatus.USED, null));
        assertThatThrownBy(() -> service.resetPassword(new PasswordUpdateRequest("used-token", "NewPassword123!")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USED_RESET_TOKEN));
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
