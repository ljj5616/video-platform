package com.example.videoplatform.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    REQUIRED_FIELD_MISSING(1001, HttpStatus.BAD_REQUEST, "필수값이 누락되었습니다."),
    INVALID_EMAIL_FORMAT(1002, HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다."),
    INVALID_PASSWORD_FORMAT(1003, HttpStatus.BAD_REQUEST, "비밀번호 형식이 올바르지 않습니다."),
    INVALID_NICKNAME_FORMAT(1004, HttpStatus.BAD_REQUEST, "닉네임 형식이 올바르지 않습니다."),
    DUPLICATE_EMAIL(1005, HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(1006, HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    INTERNAL_SERVER_ERROR(9000, HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final int code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(int code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
