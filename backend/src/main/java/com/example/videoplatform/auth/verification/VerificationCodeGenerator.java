package com.example.videoplatform.auth.verification;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class VerificationCodeGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        return "%06d".formatted(secureRandom.nextInt(1_000_000));
    }
}
