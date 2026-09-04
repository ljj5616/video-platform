package com.example.videoplatform.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    REQUIRED_FIELD_MISSING(1001, HttpStatus.BAD_REQUEST, "필수값이 누락되었습니다."),
    INVALID_EMAIL_FORMAT(1002, HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다."),
    INVALID_PASSWORD_FORMAT(1003, HttpStatus.BAD_REQUEST, "비밀번호 형식이 올바르지 않습니다."),
    INVALID_NICKNAME_FORMAT(1004, HttpStatus.BAD_REQUEST, "닉네임 형식이 올바르지 않습니다."),
    INVALID_PHONE_FORMAT(1007, HttpStatus.BAD_REQUEST, "휴대전화 번호 형식이 올바르지 않습니다."),
    UNSUPPORTED_VIDEO_FORMAT(3201, HttpStatus.BAD_REQUEST, "지원하지 않는 영상 파일 형식이거나 파일 크기를 초과했습니다."),
    UNSUPPORTED_THUMBNAIL_FORMAT(3202, HttpStatus.BAD_REQUEST, "지원하지 않는 썸네일 파일 형식이거나 파일 크기를 초과했습니다."),
    INVALID_VIDEO_TITLE(3203, HttpStatus.BAD_REQUEST, "영상 제목 길이가 올바르지 않습니다."),
    INVALID_VIDEO_DESCRIPTION(3204, HttpStatus.BAD_REQUEST, "영상 설명 길이가 올바르지 않습니다."),
    INVALID_VIDEO_VISIBILITY(3205, HttpStatus.BAD_REQUEST, "지원하지 않는 공개 범위입니다."),
    UPLOAD_SIZE_EXCEEDED(3206, HttpStatus.PAYLOAD_TOO_LARGE, "업로드 파일 크기 제한을 초과했습니다."),
    CATEGORY_NOT_FOUND(4001, HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    INVALID_PAGE_NUMBER(3001, HttpStatus.BAD_REQUEST, "페이지 번호 형식 또는 범위가 올바르지 않습니다."),
    INVALID_PAGE_SIZE(3002, HttpStatus.BAD_REQUEST, "페이지 크기 형식 또는 범위가 올바르지 않습니다."),
    INVALID_VIDEO_ID(3101, HttpStatus.BAD_REQUEST, "영상 ID 형식 또는 범위가 올바르지 않습니다."),
    VIDEO_ACCESS_DENIED(3102, HttpStatus.FORBIDDEN, "영상 조회 권한이 없습니다."),
    VIDEO_NOT_FOUND(3103, HttpStatus.NOT_FOUND, "영상을 찾을 수 없습니다."),
    VIDEO_NOT_READY(3104, HttpStatus.CONFLICT, "영상이 아직 처리 중이어서 재생할 수 없습니다."),
    VIDEO_NOT_EDITABLE(3105, HttpStatus.CONFLICT, "영상이 처리 중이어서 수정할 수 없습니다."),
    VIDEO_ALREADY_LIKED(5001, HttpStatus.CONFLICT, "이미 좋아요를 등록한 영상입니다."),
    VIDEO_LIKE_NOT_FOUND(5002, HttpStatus.NOT_FOUND, "등록된 좋아요가 없습니다."),
    VIDEO_ALREADY_BOOKMARKED(6001, HttpStatus.CONFLICT, "이미 저장 목록에 추가된 영상입니다."),
    VIDEO_BOOKMARK_NOT_FOUND(6002, HttpStatus.NOT_FOUND, "저장 목록에 존재하지 않는 영상입니다."),
    INVALID_COMMENT_CONTENT(7002, HttpStatus.BAD_REQUEST, "댓글 내용 길이가 올바르지 않습니다."),
    INVALID_COMMENT_ID(7003, HttpStatus.BAD_REQUEST, "댓글 ID 형식 또는 범위가 올바르지 않습니다."),
    COMMENT_ACCESS_DENIED(7004, HttpStatus.FORBIDDEN, "댓글 수정 또는 삭제 권한이 없습니다."),
    COMMENT_NOT_FOUND(7001, HttpStatus.NOT_FOUND, "댓글을 찾을 수 없습니다."),
    VIDEO_ALREADY_REPORTED(8001, HttpStatus.CONFLICT, "이미 신고한 콘텐츠입니다."),
    INVALID_REPORT_REASON(8002, HttpStatus.BAD_REQUEST, "지원하지 않는 신고 사유입니다."),
    REPORT_DESCRIPTION_REQUIRED(8003, HttpStatus.BAD_REQUEST, "기타 신고 사유의 상세 내용이 필요합니다."),
    INVALID_REPORT_DESCRIPTION(8004, HttpStatus.BAD_REQUEST, "신고 상세 내용 길이가 올바르지 않습니다."),
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
    EMAIL_SEND_FAILED(9002, HttpStatus.SERVICE_UNAVAILABLE, "이메일 발송에 실패했습니다."),
    FILE_STORAGE_FAILED(9003, HttpStatus.SERVICE_UNAVAILABLE, "파일 저장소 처리에 실패했습니다.");

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
