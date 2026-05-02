# 04. Infra 디렉터리 골격

## 목표

로컬 실행, 관측성, Kubernetes 확장을 위한 인프라 설정 경로를 만든다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/architecture/docker-compose-architecture.md`
4. `docs/operation/local-run.md`

## 작업 범위

1. Docker Compose 경로
2. NGINX 경로
3. RabbitMQ 경로
4. MinIO 경로
5. PostgreSQL 경로
6. Monitoring 경로
7. Kubernetes 경로

## 작업 순서

1. `infra/docker-compose`를 생성한다.
2. `infra/nginx/conf.d`를 생성한다.
3. `infra/nginx/snippets`를 생성한다.
4. `infra/rabbitmq`를 생성한다.
5. `infra/minio`를 생성한다.
6. `infra/postgres`를 생성한다.
7. `infra/monitoring/prometheus`를 생성한다.
8. `infra/monitoring/grafana/dashboards`를 생성한다.
9. `infra/monitoring/grafana/provisioning`을 생성한다.
10. `infra/monitoring/jaeger`를 생성한다.
11. `infra/monitoring/otel-collector`를 생성한다.
12. `infra/k8s` 하위에 backend-api, preprocess-worker, frontend, nginx, rabbitmq, minio, postgres, monitoring 경로를 만든다.

## 산출물

1. 인프라 디렉터리 구조
2. 로컬과 운영 설정을 분리할 수 있는 경로
3. Kubernetes/KEDA 설정을 넣을 위치

## 완료 기준

1. `infra` 하위에 외부 시스템별 경로가 분리되어 있다.
2. NGINX, RabbitMQ, MinIO, PostgreSQL 설정 경로가 존재한다.
3. 관측성 도구 설정 경로가 존재한다.

## 금지 사항

1. 인프라 설정을 API 서버 리소스 디렉터리에 몰아넣지 않는다.
2. 운영 secret 값을 커밋하지 않는다.
3. queue 이름을 문서와 다르게 임의 변경하지 않는다.
