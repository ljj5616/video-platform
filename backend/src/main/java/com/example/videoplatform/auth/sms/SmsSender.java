package com.example.videoplatform.auth.sms;

public interface SmsSender {

    void sendVerificationCode(String phone, String verificationCode);
}
