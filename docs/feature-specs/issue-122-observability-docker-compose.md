# Issue 122. Observability Docker Compose MVP

## 목적

Docker Compose 로컬 스택에 관측성 계층을 추가해 배포 전 API, Worker, Queue 상태를 확인할 수 있게 한다.

이번 범위는 Kubernetes/KEDA 운영 배포가 아니라 로컬 Docker Compose 기준 MVP다. Kubernetes 관측성 확장은 이후 작업에서 별도 이슈로 다룬다.

## 작업 범위

1. `backend-api`와 `preprocess-worker`에 Prometheus registry와 OTLP trace exporter 의존성을 추가한다.
2. Actuator Prometheus endpoint를 노출한다.
3. Worker 처리 결과 metric을 기록한다.
4. RabbitMQ Prometheus plugin을 활성화한다.
5. Prometheus scrape 설정을 추가한다.
6. Grafana datasource, dashboard provider, 기본 dashboard를 추가한다.
7. OpenTelemetry Collector와 Jaeger를 Compose override로 추가한다.
8. NGINX에서 `/grafana/`, `/jaeger/` 경로를 관측성 UI로 연결한다.
9. 운영 문서에 실행, 확인, 종료 절차를 남긴다.

## 제외 범위

1. Kubernetes Prometheus Operator 배포
2. KEDA ScaledObject 배포
3. 외부 SaaS APM 연동
4. 운영 Grafana 권한 체계
5. 운영 alert rule

## 변경 파일

| 위치 | 내용 |
| --- | --- |
| `backend-api/build.gradle` | Prometheus registry, OTel exporter 의존성 추가 |
| `preprocess-worker/build.gradle` | Actuator web endpoint와 metric/trace 의존성 추가 |
| `backend-api/src/main/resources/application.yml` | metric tag, Prometheus exposure, OTLP endpoint 설정 |
| `preprocess-worker/src/main/resources/application.yml` | Worker management port와 metric/trace 설정 |
| `WorkerMetricsRecorder` | Worker JobItem 처리량과 처리 시간 metric 기록 |
| `docker-compose.observability.yml` | Prometheus, Grafana, Jaeger, OTel Collector service 추가 |
| `infra/monitoring/prometheus/prometheus.yml` | scrape target 정의 |
| `infra/monitoring/grafana/**` | datasource, dashboard provider, 기본 dashboard |
| `infra/monitoring/otel-collector/otel-collector-config.yml` | OTLP trace 수신 후 Jaeger export |
| `infra/rabbitmq/enabled_plugins` | RabbitMQ management/prometheus plugin 활성화 |
| `docs/operation/observability.md` | 로컬 관측성 실행 가이드 |

## 주요 metric

| metric | 설명 |
| --- | --- |
| `http_server_requests_seconds_count` | API HTTP 요청 수 |
| `worker_job_processed_total` | Worker가 처리한 JobItem 수 |
| `worker_processing_seconds` | Worker JobItem 처리 시간 |
| `worker_pipeline_skeleton_executions_total` | 전처리 skeleton 실행 수 |
| `worker_artifact_prepared_total` | artifact 준비 수 |
| `rabbitmq_queue_messages_ready` | RabbitMQ queue 대기 메시지 수 |

## 실행 방법

```powershell
docker compose `
  -f infra/docker-compose/docker-compose.local.yml `
  -f infra/docker-compose/docker-compose.observability.yml `
  --env-file infra/docker-compose/.env `
  up -d --build
```

## 검증 방법

1. `http://localhost:9090/targets`에서 target 상태를 확인한다.
2. `http://localhost/grafana/`에서 Grafana dashboard를 확인한다.
3. `http://localhost/jaeger/`에서 Jaeger trace를 확인한다.
4. 이미지 전처리 작업을 실행한 뒤 `worker_job_processed_total`이 증가하는지 확인한다.

## 완료 조건

1. Compose config가 정상 생성된다.
2. Backend/Worker 테스트가 통과한다.
3. Worker metric 단위 테스트가 추가된다.
4. 관측성 실행 문서가 추가된다.
5. 기본 Grafana dashboard가 자동 provision된다.
