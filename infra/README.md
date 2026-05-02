# infra

로컬 실행, 운영 배포, 관측성 설정을 관리하는 경로다.

## 하위 경로

| 경로 | 책임 |
|---|---|
| `docker-compose/` | 로컬 실행용 Compose 설정 |
| `nginx/` | 단일 진입점 reverse proxy 설정 |
| `rabbitmq/` | Queue와 exchange 설정 |
| `minio/` | Object Storage bucket 초기화 |
| `postgres/` | DB 초기화와 백업 스크립트 |
| `monitoring/` | Prometheus, Grafana, OpenTelemetry, Jaeger 설정 |
| `k8s/` | Kubernetes와 KEDA 설정 |

## 금지 사항

1. 운영 secret 값을 커밋하지 않는다.
2. queue 이름을 문서와 다르게 임의 변경하지 않는다.
3. API와 frontend를 별도 외부 진입점으로 강제하지 않는다.
