package com.example.videoplatform.auth.service;

import com.example.videoplatform.auth.dto.LoginRequest;
import com.example.videoplatform.auth.dto.LoginResponse;
import com.example.videoplatform.auth.entity.RefreshToken;
import com.example.videoplatform.auth.repository.RefreshTokenRepository;
import com.example.videoplatform.auth.token.JwtTokenProvider;
import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.user.entity.User;
import com.example.videoplatform.user.entity.UserStatus;
import com.example.videoplatform.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .filter(found -> found.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshTokenValue = jwtTokenProvider.createRefreshToken(user.getId());
        String refreshTokenHash = hash(refreshTokenValue);
        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                java.time.Instant.now().plusSeconds(jwtTokenProvider.getRefreshTokenExpirationSeconds()),
                ZoneOffset.UTC
        );

        RefreshToken savedToken = refreshTokenRepository.findByUserId(user.getId())
                .map(existing -> {
                    existing.replace(refreshTokenHash, expiresAt);
                    return existing;
                })
                .orElseGet(() -> RefreshToken.create(user.getId(), refreshTokenHash, expiresAt));
        refreshTokenRepository.save(savedToken);

        return new LoginResponse(
                accessToken,
                refreshTokenValue,
                "Bearer",
                jwtTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        Long userId;
        try {
            userId = jwtTokenProvider.getRefreshTokenUserId(refreshTokenValue);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken storedToken = refreshTokenRepository.findByUserId(userId)
                .filter(token -> MessageDigest.isEqual(
                        token.getTokenHash().getBytes(StandardCharsets.UTF_8),
                        hash(refreshTokenValue).getBytes(StandardCharsets.UTF_8)
                ))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        refreshTokenRepository.delete(storedToken);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
