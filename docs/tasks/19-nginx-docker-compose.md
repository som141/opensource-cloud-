# 19. NGINX/Docker Compose

## 목표

로컬에서 전체 시스템을 실행할 수 있는 Docker Compose skeleton과 NGINX 단일 진입점 설정을 만든다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/04-infra-directory-skeleton.md`
4. `docs/architecture/docker-compose-architecture.md`
5. `docs/operation/local-run.md`

## 작업 범위

1. backend-api service
2. preprocess-worker service
3. frontend service
4. nginx reverse proxy
5. postgres, rabbitmq, minio
6. monitoring compose

## 작업 순서

1. `docker-compose.local.yml`을 만든다.
2. `docker-compose.observability.yml`을 만든다.
3. `.env.example`을 만든다.
4. backend-api service를 정의한다.
5. preprocess-worker service를 정의한다.
6. frontend service를 정의한다.
7. nginx service를 정의한다.
8. postgres service를 정의한다.
9. rabbitmq service를 정의한다.
10. minio service를 정의한다.
11. prometheus service를 정의한다.
12. grafana service를 정의한다.
13. otel-collector service를 정의한다.
14. jaeger service를 정의한다.
15. Docker network와 volume을 정의한다.
16. healthcheck를 추가한다.
17. `/`를 frontend로 라우팅한다.
18. `/api/*`를 backend-api로 라우팅한다.
19. `/oauth2/*`와 `/login/oauth2/*`를 backend-api로 라우팅한다.
20. `/api/v1/jobs/*/events`에 SSE 설정을 적용한다.

## 산출물

1. Docker Compose skeleton
2. NGINX config
3. `.env.example`
4. local run guide 초안

## 완료 기준

1. NGINX가 단일 진입점이다.
2. SSE 경로는 `proxy_buffering off`가 적용된다.
3. OAuth callback 경로가 backend-api로 전달된다.
4. RabbitMQ queue 이름이 문서와 일치한다.

## 금지 사항

1. API와 Frontend를 서로 다른 외부 진입점으로 강제하지 않는다.
2. secret 값을 compose 파일에 하드코딩하지 않는다.
3. 운영 HTTPS 전제를 문서에서 누락하지 않는다.
