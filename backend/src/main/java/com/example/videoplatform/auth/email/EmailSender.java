package com.example.videoplatform.auth.email;

public interface EmailSender {
    void sendPasswordResetCode(String email, String code);
}
