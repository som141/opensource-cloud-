# 20. Observability

## 목표

API, Queue, Worker, Storage 흐름을 Prometheus, Grafana, OpenTelemetry, Jaeger로 관측할 수 있게 한다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/19-nginx-docker-compose.md`
4. `docs/operation/observability.md`
5. `docs/architecture/sequence-diagrams.md`

## 작업 범위

1. API metric
2. Worker metric
3. RabbitMQ metric
4. Grafana dashboard
5. Distributed tracing

## 작업 순서

1. API HTTP metric을 노출한다.
2. Job 생성 metric을 추가한다.
3. Upload session metric을 추가한다.
4. Auth login success/failure metric을 추가한다.
5. Worker processed total metric을 추가한다.
6. Worker failed total metric을 추가한다.
7. Worker processing seconds metric을 추가한다.
8. Worker preset usage metric을 추가한다.
9. RabbitMQ queue length 수집을 연결한다.
10. API Overview dashboard를 만든다.
11. Worker Overview dashboard를 만든다.
12. Queue Overview dashboard를 만든다.
13. Job Overview dashboard를 만든다.
14. API에서 RabbitMQ publish 시 trace context를 넣는다.
15. Worker consume 시 trace context를 추출한다.
16. Jaeger에서 create job부터 worker complete까지 추적한다.

## 산출물

1. Prometheus 설정
2. Grafana dashboard
3. OpenTelemetry 설정
4. Jaeger trace 확인 흐름

## 완료 기준

1. API와 Worker metric이 Prometheus에 수집된다.
2. queue length와 DLQ 상태를 볼 수 있다.
3. traceId가 API, RabbitMQ, Worker 구간에 연결된다.

## 금지 사항

1. 메트릭 이름을 매번 임의로 바꾸지 않는다.
2. 민감한 사용자 정보를 trace attribute에 넣지 않는다.
3. Worker 실패를 로그에만 남기고 metric에 반영하지 않는 상태로 두지 않는다.
