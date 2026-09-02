package com.example.videoplatform.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.user.dto.UserSignUpRequest;
import com.example.videoplatform.user.entity.User;
import com.example.videoplatform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void signsUpUserWithEncodedPassword() {
        UserSignUpRequest request = request();
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = userService.signUp(request);

        assertThat(response.email()).isEqualTo(request.email());
        assertThat(response.nickname()).isEqualTo(request.nickname());
        verify(passwordEncoder).encode(request.password());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void rejectsDuplicateEmail() {
        UserSignUpRequest request = request();
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_EMAIL));
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateNickname() {
        UserSignUpRequest request = request();
        when(userRepository.existsByNickname(request.nickname())).thenReturn(true);

        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_NICKNAME));
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicatePhone() {
        UserSignUpRequest request = request();
        when(userRepository.existsByPhone(request.phone())).thenReturn(true);

        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_PHONE));
        verify(userRepository, never()).save(any());
    }

    private UserSignUpRequest request() {
        return new UserSignUpRequest("user@example.com", "Password123!", "홍길동", "홍길동", "01012345678");
    }
}
