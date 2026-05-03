# backend-api

Spring Boot REST API 서버 경로다.

## 책임

1. Google 소셜 로그인
2. 사용자와 프로젝트 관리
3. 업로드 세션과 presigned URL 발급
4. 이미지 메타데이터 관리
5. Job 등록과 상태 조회
6. SSE 진행률 제공
7. Worker internal API 제공

## 금지 사항

1. OpenCV 문서 이미지 전처리 로직을 넣지 않는다.
2. Worker 전용 메시지 consume 로직을 넣지 않는다.
3. 최상위 계층형 `controller/service/repository/dto` 구조를 만들지 않는다.
