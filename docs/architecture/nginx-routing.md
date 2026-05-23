# NGINX 라우팅

## 목적

NGINX는 브라우저 기준 단일 진입점입니다.
프론트엔드 정적 파일은 frontend NGINX 컨테이너로 전달하고, API/OAuth/Swagger/SSE 요청은 `backend-api`로 reverse proxy합니다.

## 로컬 진입 경로

| 경로 | 대상 |
| --- | --- |
| `/` | `frontend:80` |
| `/assets/*` | `frontend:80` |
| `/api/*` | `backend-api:8080` |
| `/oauth2/*` | `backend-api:8080` |
| `/login/oauth2/*` | `backend-api:8080` |
| `/v3/api-docs` | `backend-api:8080` |
| `/swagger-ui/*` | `backend-api:8080` |
| `/api/v1/jobs/*/events` | `backend-api:8080`, SSE buffering 비활성화 |
| `/grafana/*` | 관측성 stack 추가 전 placeholder |
| `/jaeger/*` | 관측성 stack 추가 전 placeholder |

## SSE 규칙

Job event 경로는 응답 buffering을 끕니다.

```nginx
proxy_buffering off;
proxy_cache off;
proxy_read_timeout 1h;
add_header X-Accel-Buffering no always;
```

이 설정이 없으면 브라우저가 진행률 이벤트를 실시간으로 받지 못하고 한 번에 몰아서 받을 수 있습니다.

## OAuth 규칙

NGINX를 통해 Google 로그인을 테스트할 때 Google Console에는 아래 callback을 등록합니다.

```text
http://localhost/login/oauth2/code/google
```

backend-api를 직접 호출해 테스트할 때는 아래 callback도 사용할 수 있습니다.

```text
http://localhost:8080/login/oauth2/code/google
```

프론트엔드는 OAuth 성공 뒤 URL에 Access Token을 노출하지 않는 방향을 기준으로 합니다.
Refresh Token은 `HttpOnly` cookie로 유지하고, Access Token은 `/api/v1/auth/refresh`로 재발급받습니다.

## 보안 헤더

로컬 skeleton은 기본 브라우저 보안 헤더를 적용합니다.

- `X-Frame-Options`
- `X-Content-Type-Options`
- `Referrer-Policy`
- `Permissions-Policy`

운영 환경에서는 HTTPS, HSTS, CSP, admin route 인증을 배포 환경에 맞춰 추가로 조정합니다.
