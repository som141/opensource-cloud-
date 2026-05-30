# 20. 관측성

## 목표

API, Queue, Worker 흐름을 Prometheus, Grafana, OpenTelemetry, Jaeger로 관측할 수 있게 한다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/19-nginx-docker-compose.md`
4. `docs/operation/observability.md`
5. `docs/feature-specs/issue-122-observability-docker-compose.md`

## 작업 범위

1. API metric 노출
2. Worker metric 기록
3. RabbitMQ metric 수집
4. Grafana dashboard 구성
5. OpenTelemetry trace 수집
6. Jaeger trace 조회

## 작업 순서

1. API Actuator Prometheus endpoint를 노출한다.
2. Worker Actuator Prometheus endpoint를 노출한다.
3. Worker 처리량, 실패 코드, 처리 시간 metric을 추가한다.
4. RabbitMQ Prometheus plugin을 활성화한다.
5. Prometheus scrape 설정을 추가한다.
6. Grafana datasource와 dashboard provider를 추가한다.
7. 기본 Grafana dashboard를 만든다.
8. OpenTelemetry Collector를 추가한다.
9. API와 Worker trace를 OTel Collector로 전송한다.
10. Jaeger에서 trace를 조회한다.
11. NGINX에서 `/grafana/`, `/jaeger/` 경로를 연결한다.
12. 운영 문서에 실행과 검증 절차를 기록한다.

## 산출물

1. `infra/docker-compose/docker-compose.observability.yml`
2. `infra/monitoring/prometheus/prometheus.yml`
3. `infra/monitoring/grafana/provisioning/**`
4. `infra/monitoring/grafana/dashboards/docprep-overview.json`
5. `infra/monitoring/otel-collector/otel-collector-config.yml`
6. `docs/operation/observability.md`

## 완료 기준

1. API와 Worker metric이 Prometheus에 수집된다.
2. RabbitMQ queue metric이 Prometheus에 수집된다.
3. Grafana 기본 dashboard가 자동 등록된다.
4. Worker 처리 후 `worker_job_processed_total`과 `worker_processing_seconds`가 증가한다.
5. Jaeger에서 API 또는 Worker trace를 조회할 수 있다.

## 금지 사항

1. metric 이름을 작업마다 임의로 바꾸지 않는다.
2. 사용자 이메일, 토큰, 원본 파일명 같은 민감 정보를 trace attribute에 넣지 않는다.
3. Worker 실패를 로그에만 남기고 metric에 반영하지 않는 상태로 두지 않는다.
4. 관측성 컨테이너 없이는 기본 로컬 Compose가 뜨지 않는 구조를 만들지 않는다.
