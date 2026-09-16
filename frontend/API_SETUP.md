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
| 영상 업로드 | `POST /videos` (`multipart/form-data`, Bearer 인증) |
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
내 영상/좋아요 목록/시청 기록 화면, 재생 위치 저장 및 복원은 아직 연결하지 않았습니다.

## 영상 업로드

- 헤더의 영상 업로드 링크 또는 `/#/upload`로 진입합니다. 로그인이 필요합니다.
- `videoFile`, `title`, `description`, `categoryId`, `visibility`를 전송하며, 선택한 썸네일이 있으면 `thumbnailFile`을 함께 전송합니다.
- 현재 백엔드 설정과 동일하게 영상은 최대 1GB의 MP4/MOV/WebM, 썸네일은 최대 10MB의 JPG/PNG/WebP를 허용합니다. 서버 제한을 변경하면 프론트 검증 및 안내도 함께 변경해야 합니다.
- 브라우저에서 영상의 20%, 50%, 80% 지점 썸네일 후보를 생성합니다. 지원하지 않는 코덱은 자동 생성을 생략하고 직접 이미지를 선택할 수 있습니다. 썸네일을 선택하지 않으면 서버의 기본 설정을 사용합니다.
- 실제 파일 전송 이벤트로 진행률과 예상 남은 시간을 표시합니다. 100% 전송 이후에도 서버 응답이 올 때까지 접수 대기 상태를 표시합니다.
- 취소 시 요청을 중단하고 입력값을 유지합니다. 페이지를 떠나면 전송이 중단되며, 서버가 이미 받은 업로드의 삭제/변환 취소까지 보장하지는 않습니다.
- `202` 응답은 업로드 접수이며 변환 완료가 아닙니다. 변환 상태를 주기적으로 조회하는 API는 연결하지 않았습니다. 완료 안내의 링크는 업로드 응답 `videoId`에 해당하는 상세 화면으로 이동합니다.
- 비공개 영상은 변환 후에도 비공개로 유지됩니다. 실제 저장소 업로드 및 FFmpeg 변환에는 백엔드의 저장소/FFmpeg 설정이 필요합니다.

## 검증

- `npm run build`: TypeScript 및 배포 빌드
- `npm run lint`: 코드 검사
- `npm test`: 세션, 인증 오류, 빈 201/204 응답 처리
- `npm run test:e2e`: 테스트용 API 응답으로 상세 진입, 반응 토글, 댓글 CRUD, 신고, 페이지 이동, 오류 재시도, 모바일 너비 및 플레이어 유지 검증
- `npm run test:e2e -- upload.spec.ts`: 업로드 multipart 필드·인증·썸네일 전송, 형식 검증, 실패 후 재시도, 전송 취소, 인증 만료, 데스크톱·모바일 화면 검증
- 백엔드: `./gradlew test --tests com.example.videoplatform.video.service.VideoDetailServiceTest`

브라우저 테스트는 설치된 Microsoft Edge를 사용하며 5174 포트로 개발 서버를 실행합니다.
자동 테스트는 실제 저장소 영상의 디코딩이나 운영 백엔드 통신 성공을 보장하지 않습니다. 실제 계정과 게시된 영상으로 별도 확인해야 합니다.
