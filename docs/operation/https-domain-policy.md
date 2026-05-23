# HTTPS와 도메인 정책

운영 Google OAuth 로그인과 Refresh Token cookie 보안은 HTTPS를 전제로 합니다.
운영 환경에서는 하나의 공개 도메인을 NGINX 앞단에 두고, backend-api, PostgreSQL, RabbitMQ, MinIO 직접 포트는 외부에 공개하지 않습니다.

## 권장 구성

```text
사용자 브라우저
  -> https://YOUR_DOMAIN
  -> TLS 종료 지점 또는 클라우드 로드밸런서
  -> Compose NGINX container HTTP :80
  -> frontend / backend-api / MinIO bucket proxy
```

현재 production compose override는 NGINX만 외부에 노출합니다. backend-api, PostgreSQL, RabbitMQ, MinIO 직접 포트는 `docker-compose.prod.yml`에서 제거합니다.

## TLS 처리 방식

### 선택지 A. Cloudflare 또는 Load Balancer에서 TLS 종료

MVP 첫 운영 배포에 가장 단순한 방식입니다.

- 브라우저는 `https://YOUR_DOMAIN`으로 접속합니다.
- Cloudflare 또는 로드밸런서가 TLS를 종료합니다.
- 서버의 NGINX container로 HTTP를 전달합니다.
- `NGINX_HTTP_PORT=80`을 사용하거나 로드밸런서 구성에 맞춰 내부 포트로 바인딩합니다.

### 선택지 B. 서버 호스트 NGINX/Caddy가 TLS 종료

서버에 host-level reverse proxy를 두고 Let's Encrypt를 사용합니다.

```text
host nginx/caddy :443
  -> localhost:8088
  -> compose nginx :80
```

이 경우 `.env.prod` 예시:

```text
NGINX_HTTP_PORT=127.0.0.1:8088
```

### 선택지 C. Compose NGINX container 안에서 TLS 처리

아직 구현하지 않은 hardening 작업입니다. 인증서 volume mount, 443 포트, 갱신 자동화, production NGINX server block이 필요합니다.

## Google OAuth 필수 값

Google Console:

```text
Authorized JavaScript origin: https://YOUR_DOMAIN
Authorized redirect URI:      https://YOUR_DOMAIN/login/oauth2/code/google
```

`.env.prod`:

```text
OAUTH2_SUCCESS_REDIRECT_URI=https://YOUR_DOMAIN/oauth2/success
CORS_ALLOWED_ORIGINS=https://YOUR_DOMAIN
REFRESH_TOKEN_COOKIE_SECURE=true
REFRESH_TOKEN_COOKIE_SAME_SITE=Lax
```

## Object Storage URL 정책

Compose 내부 MinIO MVP 기준:

```text
MINIO_ENDPOINT=http://minio:9000
MINIO_PUBLIC_ENDPOINT=https://YOUR_DOMAIN
MINIO_BUCKET=image-preprocess-prod
```

프론트엔드는 public endpoint가 들어간 presigned URL을 받습니다. NGINX는 아래 bucket prefix를 proxy합니다.

```text
/image-preprocess-prod/
```

bucket 이름을 바꾸면 아래 파일에도 같은 prefix를 추가해야 합니다.

```text
infra/nginx/conf.d/app.conf
```

## 공개 포트 정책

공개 진입점은 하나만 둡니다.

| 포트 | 공개 여부 | 이유 |
| --- | --- | --- |
| `80` 또는 `443` | 예 | NGINX 또는 TLS 종료 지점 |
| `8080` backend-api | 아니오 | NGINX를 통해 접근 |
| `5432` PostgreSQL | 아니오 | 내부 전용 |
| `5672` RabbitMQ AMQP | 아니오 | 내부 전용 |
| `15672` RabbitMQ Management | 아니오 | SSH tunnel 또는 관리자망 전용 |
| `9000` MinIO API | 아니오 | bucket path를 NGINX로 proxy |
| `9001` MinIO Console | 아니오 | SSH tunnel 또는 관리자망 전용 |

## Swagger 공개 정책

현재 MVP는 아래 경로를 NGINX를 통해 접근할 수 있습니다.

```text
/swagger-ui/
/v3/api-docs
```

운영 공개 전 아래 중 하나를 결정합니다.

1. MVP 시연용으로만 공개합니다.
2. 로드밸런서나 방화벽에서 IP를 제한합니다.
3. Basic Auth를 붙이거나 NGINX route를 막습니다.

실제 사용자 데이터가 올라가는 환경에서는 Swagger 공개 범위를 제한하는 것이 안전합니다.
