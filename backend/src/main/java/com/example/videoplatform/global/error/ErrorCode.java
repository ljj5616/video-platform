package com.example.videoplatform.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    REQUIRED_FIELD_MISSING(1001, HttpStatus.BAD_REQUEST, "필수값이 누락되었습니다."),
    INVALID_EMAIL_FORMAT(1002, HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다."),
    INVALID_PASSWORD_FORMAT(1003, HttpStatus.BAD_REQUEST, "비밀번호 형식이 올바르지 않습니다."),
    INVALID_NICKNAME_FORMAT(1004, HttpStatus.BAD_REQUEST, "닉네임 형식이 올바르지 않습니다."),
    INVALID_PHONE_FORMAT(1007, HttpStatus.BAD_REQUEST, "휴대전화 번호 형식이 올바르지 않습니다."),
    INVALID_CREDENTIALS(2001, HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),
    INVALID_REFRESH_TOKEN(2002, HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 Refresh Token입니다."),
    INCORRECT_PASSWORD(2003, HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    USER_NOT_FOUND(2101, HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    DUPLICATE_EMAIL(2102, HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATE_NICKNAME(2103, HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    DUPLICATE_PHONE(2104, HttpStatus.CONFLICT, "이미 사용 중인 휴대전화 번호입니다."),
    FIND_ID_USER_NOT_FOUND(2201, HttpStatus.NOT_FOUND, "일치하는 사용자 정보가 없습니다."),
    VERIFICATION_SEND_LIMIT_EXCEEDED(2202, HttpStatus.TOO_MANY_REQUESTS, "인증번호 발송 횟수를 초과했습니다."),
    INVALID_VERIFICATION_CODE(2203, HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않습니다."),
    VERIFICATION_CODE_EXPIRED(2204, HttpStatus.BAD_REQUEST, "인증번호가 만료되었습니다."),
    VERIFICATION_ATTEMPT_LIMIT_EXCEEDED(2205, HttpStatus.TOO_MANY_REQUESTS, "인증번호 확인 횟수를 초과했습니다."),
    PASSWORD_RESET_SEND_LIMIT_EXCEEDED(2301, HttpStatus.TOO_MANY_REQUESTS, "비밀번호 재설정 인증번호 발송 횟수를 초과했습니다."),
    INVALID_PASSWORD_RESET_CODE(2302, HttpStatus.BAD_REQUEST, "인증번호가 일치하지 않습니다."),
    PASSWORD_RESET_CODE_EXPIRED(2303, HttpStatus.BAD_REQUEST, "인증번호가 만료되었습니다."),
    PASSWORD_RESET_VERIFY_LIMIT_EXCEEDED(2304, HttpStatus.TOO_MANY_REQUESTS, "인증번호 확인 횟수를 초과했습니다."),
    INVALID_RESET_TOKEN(2305, HttpStatus.BAD_REQUEST, "유효하지 않은 Reset Token입니다."),
    EXPIRED_RESET_TOKEN(2306, HttpStatus.BAD_REQUEST, "만료된 Reset Token입니다."),
    USED_RESET_TOKEN(2307, HttpStatus.BAD_REQUEST, "이미 사용된 Reset Token입니다."),
    INTERNAL_SERVER_ERROR(9000, HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    SMS_SEND_FAILED(9001, HttpStatus.SERVICE_UNAVAILABLE, "인증번호 발송에 실패했습니다."),
    EMAIL_SEND_FAILED(9002, HttpStatus.SERVICE_UNAVAILABLE, "이메일 발송에 실패했습니다.");

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
