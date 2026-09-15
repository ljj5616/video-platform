# 프론트엔드 API 연동

## 실행

1. 백엔드를 8080 포트로 실행합니다. 상세 응답에 likeCount, liked, bookmarked가 추가되었으므로 최신 코드로 재시작합니다.
2. frontend에서 `npm install`, `npm run dev`를 실행합니다.
3. 로그인하고 홈의 영상 카드를 선택합니다. 상세 주소는 `/#/videos/{videoId}`입니다.

Vite는 /api 요청을 http://localhost:8080으로 전달합니다. 배포 서버에도 별도 프록시 설정이 필요합니다.
현재 백엔드 SecurityConfig는 영상 조회에도 인증을 요구합니다. 인증 정책은 변경하지 않았습니다.

## 연결된 기능

공통 경로: `/api/v1`

| 화면 | API |
| --- | --- |
| 홈 검색·필터·정렬·페이지 | `GET /videos` |
| 홈 및 상세 우측 추천 | `GET /videos/recommendations` |
| 카테고리 | `GET /categories` |
| 회원가입 / 로그인 / 로그아웃 / 탈퇴 | `POST /users`, `POST /auth/login`, `POST /auth/logout`, `DELETE /users/me` |
| 영상 상세 | `GET /videos/{id}` |
| HLS 재생 | `GET /videos/{id}/playback` |
| 로그인 사용자의 재생 시작 시 조회 기록 | `POST /videos/{id}/views` |
| 좋아요 / 취소 | `POST`, `DELETE /videos/{id}/likes` |
| 북마크 / 취소 | `POST`, `DELETE /videos/{id}/bookmarks` |
| 영상 신고 | `POST /videos/{id}/reports` |
| 댓글 조회·작성 | `GET`, `POST /videos/{id}/comments` |
| 본인 댓글 수정·삭제 | `PATCH`, `DELETE /comments/{id}` |
| 다른 사용자 댓글 신고 | `POST /comments/{id}/reports` |

상세에는 제목, 작성자, 프로필 이미지, 카테고리, 조회수, 등록일, 설명, 좋아요 수와 내 반응 상태를 표시합니다.
구독자 수는 API에 없어 표시하지 않습니다. 우측 목록은 기존 추천 API이며 현재 영상은 제외합니다.
재생은 브라우저 기본 HLS 지원 또는 hls.js를 사용합니다. 영상 호스트에서 재생 목록과 세그먼트의 CORS 접근을 허용해야 합니다.
업로드 화면, 내 영상/좋아요 목록/시청 기록 화면, 재생 위치 저장 및 복원은 이번 상세 화면 범위에 포함하지 않습니다.

## 검증

- `npm run build`: TypeScript 및 배포 빌드
- `npm run lint`: 코드 검사
- `npm test`: 세션, 인증 오류, 빈 201/204 응답 처리
- `npm run test:e2e`: 테스트용 API 응답으로 상세 진입, 반응 토글, 댓글 CRUD, 신고, 페이지 이동, 오류 재시도, 모바일 너비 및 플레이어 유지 검증
- 백엔드: `./gradlew test --tests com.example.videoplatform.video.service.VideoDetailServiceTest`

브라우저 테스트는 설치된 Microsoft Edge를 사용하며 5174 포트로 개발 서버를 실행합니다.
자동 테스트는 실제 저장소 영상의 디코딩이나 운영 백엔드 통신 성공을 보장하지 않습니다. 실제 계정과 게시된 영상으로 별도 확인해야 합니다.
