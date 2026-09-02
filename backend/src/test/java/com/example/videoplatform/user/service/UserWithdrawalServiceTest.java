package com.example.videoplatform.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.videoplatform.auth.repository.RefreshTokenRepository;
import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.user.entity.User;
import com.example.videoplatform.user.entity.UserStatus;
import com.example.videoplatform.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserWithdrawalServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;

    private UserWithdrawalService userWithdrawalService;

    @BeforeEach
    void setUp() {
        userWithdrawalService = new UserWithdrawalService(
                userRepository,
                refreshTokenRepository,
                passwordEncoder
        );
    }

    @Test
    void withdrawsUserAndDeletesRefreshToken() {
        User user = user();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", user.getPasswordHash())).thenReturn(true);

        userWithdrawalService.withdraw(1L, "Password123!");

        assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(user.getDeletedAt()).isNotNull();
        verify(refreshTokenRepository).deleteByUserId(1L);
    }

    @Test
    void rejectsIncorrectPassword() {
        User user = user();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> userWithdrawalService.withdraw(1L, "wrong-password"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INCORRECT_PASSWORD));
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(refreshTokenRepository, never()).deleteByUserId(1L);
    }

    @Test
    void rejectsAlreadyWithdrawnUser() {
        User user = user();
        user.withdraw(java.time.LocalDateTime.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userWithdrawalService.withdraw(1L, "Password123!"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));
        verify(passwordEncoder, never()).matches(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private User user() {
        return User.create("user@example.com", "encoded-password", "nickname", "name", "01012345678");
    }
}
