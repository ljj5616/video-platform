package com.example.videoplatform.user.controller;

import com.example.videoplatform.user.dto.UserSignUpRequest;
import com.example.videoplatform.user.dto.UserSignUpResponse;
import com.example.videoplatform.user.dto.UserWithdrawalRequest;
import com.example.videoplatform.user.service.UserService;
import com.example.videoplatform.user.service.UserWithdrawalService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserWithdrawalService userWithdrawalService;

    public UserController(UserService userService, UserWithdrawalService userWithdrawalService) {
        this.userService = userService;
        this.userWithdrawalService = userWithdrawalService;
    }

    @PostMapping
    public ResponseEntity<UserSignUpResponse> signUp(@Valid @RequestBody UserSignUpRequest request) {
        UserSignUpResponse response = userService.signUp(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + response.id())).body(response);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserWithdrawalRequest request
    ) {
        userWithdrawalService.withdraw(userId, request.password());
        return ResponseEntity.noContent().build();
    }
}
