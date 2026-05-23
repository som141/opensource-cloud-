# 이슈 47. NGINX routing skeleton

## 목적

프론트엔드와 Spring API를 NGINX 단일 진입점으로 연결합니다.

## 작업 범위

1. `/`를 frontend로 proxy
2. `/api/*`를 backend-api로 proxy
3. `/oauth2/*`와 `/login/oauth2/*`를 backend-api로 proxy
4. Swagger 경로 proxy
5. SSE buffering 비활성화
6. MinIO local upload 경로 proxy

## 완료 기준

1. 브라우저는 `http://localhost`만 진입점으로 사용합니다.
2. Google OAuth callback이 NGINX 경유로 동작합니다.
3. SSE 진행률이 buffering 없이 전달됩니다.
