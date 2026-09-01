package com.example.videoplatform.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.videoplatform.auth.dto.LoginRequest;
import com.example.videoplatform.auth.entity.RefreshToken;
import com.example.videoplatform.auth.repository.RefreshTokenRepository;
import com.example.videoplatform.auth.token.JwtTokenProvider;
import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.user.entity.User;
import com.example.videoplatform.user.repository.UserRepository;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtTokenProvider);
    }

    @Test
    void loginIssuesTokensAndStoresRefreshToken() {
        LoginRequest request = new LoginRequest("user@example.com", "Password123!");
        User user = user();
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(1L)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(1L)).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(3600L);
        when(jwtTokenProvider.getRefreshTokenExpirationSeconds()).thenReturn(1209600L);
        when(refreshTokenRepository.findByUserId(1L)).thenReturn(Optional.empty());

        var response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void loginRejectsWrongPassword() {
        LoginRequest request = new LoginRequest("user@example.com", "wrong-password");
        User user = user();
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void logoutDeletesMatchingRefreshToken() {
        RefreshToken stored = RefreshToken.create(1L,
                sha256("refresh-token"),
                java.time.LocalDateTime.now().plusDays(14));
        when(jwtTokenProvider.getRefreshTokenUserId("refresh-token")).thenReturn(1L);
        when(refreshTokenRepository.findByUserId(1L)).thenReturn(Optional.of(stored));

        authService.logout("refresh-token");

        verify(refreshTokenRepository).delete(stored);
    }

    @Test
    void logoutRejectsReplacedRefreshToken() {
        RefreshToken stored = RefreshToken.create(1L, "different-hash", java.time.LocalDateTime.now().plusDays(14));
        when(jwtTokenProvider.getRefreshTokenUserId("old-token")).thenReturn(1L);
        when(refreshTokenRepository.findByUserId(1L)).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.logout("old-token"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN));
    }

    private User user() {
        User user = User.create("user@example.com", "encoded-password", "nickname", "name");
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
