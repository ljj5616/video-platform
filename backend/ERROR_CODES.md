# API 오류 코드

오류 번호는 하나의 의미로만 사용하며, 삭제된 번호도 다른 의미로 재사용하지 않는다.
새 오류를 추가할 때는 `ErrorCodeTest.numericCodesAreUnique`로 숫자 중복을 검사한다.

## 번호 범위

| 범위 | 영역 |
|---|---|
| 1000-1099 | 요청값 검증 |
| 2000-2099 | 로그인 및 토큰 |
| 2100-2199 | 회원 |
| 2200-2299 | 본인 인증 및 아이디 찾기 |
| 2300-2399 | 비밀번호 찾기 및 변경 |
| 3000-8999 | 기능별 도메인(추후 배정) |
| 9000-9099 | 서버 및 외부 인프라 |

## 현재 코드

| 코드 | enum | HTTP | 의미 |
|---:|---|---:|---|
| 1001 | REQUIRED_FIELD_MISSING | 400 | 필수값 누락 |
| 1002 | INVALID_EMAIL_FORMAT | 400 | 이메일 형식 오류 |
| 1003 | INVALID_PASSWORD_FORMAT | 400 | 비밀번호 형식 오류 |
| 1004 | INVALID_NICKNAME_FORMAT | 400 | 닉네임 형식 오류 |
| 1007 | INVALID_PHONE_FORMAT | 400 | 휴대전화 번호 형식 오류 |
| 2001 | INVALID_CREDENTIALS | 401 | 로그인 정보 불일치 |
| 2002 | INVALID_REFRESH_TOKEN | 401 | Refresh Token 오류 |
| 2003 | INCORRECT_PASSWORD | 401 | 비밀번호 불일치 |
| 2101 | USER_NOT_FOUND | 404 | 회원 없음 |
| 2102 | DUPLICATE_EMAIL | 409 | 이메일 중복 |
| 2103 | DUPLICATE_NICKNAME | 409 | 닉네임 중복 |
| 2104 | DUPLICATE_PHONE | 409 | 휴대전화 번호 중복 |
| 2201 | FIND_ID_USER_NOT_FOUND | 404 | 아이디 찾기 사용자 정보 불일치 |
| 2202 | VERIFICATION_SEND_LIMIT_EXCEEDED | 429 | 인증번호 발송 횟수 초과 |
| 2203 | INVALID_VERIFICATION_CODE | 400 | 인증번호 형식 또는 값 불일치 |
| 2204 | VERIFICATION_CODE_EXPIRED | 400 | 인증번호 만료 |
| 2205 | VERIFICATION_ATTEMPT_LIMIT_EXCEEDED | 429 | 인증번호 확인 횟수 초과 |
| 2301 | PASSWORD_RESET_SEND_LIMIT_EXCEEDED | 429 | 비밀번호 재설정 인증번호 발송 횟수 초과 |
| 2302 | INVALID_PASSWORD_RESET_CODE | 400 | 비밀번호 재설정 인증번호 불일치 |
| 2303 | PASSWORD_RESET_CODE_EXPIRED | 400 | 비밀번호 재설정 인증번호 만료 |
| 2304 | PASSWORD_RESET_VERIFY_LIMIT_EXCEEDED | 429 | 비밀번호 재설정 인증번호 확인 횟수 초과 |
| 2305 | INVALID_RESET_TOKEN | 400 | 유효하지 않은 Reset Token |
| 2306 | EXPIRED_RESET_TOKEN | 400 | 만료된 Reset Token |
| 2307 | USED_RESET_TOKEN | 400 | 이미 사용된 Reset Token |
| 9000 | INTERNAL_SERVER_ERROR | 500 | 내부 서버 오류 |
| 9001 | SMS_SEND_FAILED | 503 | SMS 발송 실패 |
| 9002 | EMAIL_SEND_FAILED | 503 | 이메일 발송 실패 |

## 사용 중단된 코드

`1005`, `1006`, `2004`는 기존 분류를 재편하면서 사용을 중단했으며 다른 의미로 재사용하지 않는다.
