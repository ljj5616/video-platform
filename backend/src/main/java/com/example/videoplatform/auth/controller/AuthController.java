package com.example.videoplatform.auth.controller;

import com.example.videoplatform.auth.dto.FindIdCodeRequest;
import com.example.videoplatform.auth.dto.FindIdResponse;
import com.example.videoplatform.auth.dto.FindIdVerifyRequest;
import com.example.videoplatform.auth.dto.LoginRequest;
import com.example.videoplatform.auth.dto.LoginResponse;
import com.example.videoplatform.auth.dto.LogoutRequest;
import com.example.videoplatform.auth.service.AuthService;
import com.example.videoplatform.auth.service.FindIdService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final FindIdService findIdService;

    public AuthController(AuthService authService, FindIdService findIdService) {
        this.authService = authService;
        this.findIdService = findIdService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/find-id/code")
    public ResponseEntity<Void> sendFindIdCode(@Valid @RequestBody FindIdCodeRequest request) {
        findIdService.sendCode(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/find-id/verify")
    public ResponseEntity<FindIdResponse> verifyFindIdCode(@Valid @RequestBody FindIdVerifyRequest request) {
        return ResponseEntity.ok(findIdService.verify(request));
    }
}
