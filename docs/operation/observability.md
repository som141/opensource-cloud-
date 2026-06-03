# 관측성 로컬 실행 가이드

이 문서는 Docker Compose 로컬 환경과 Kubernetes 환경에서 Prometheus, Grafana, OpenTelemetry Collector를 사용해 API, Worker, RabbitMQ, KEDA 상태를 확인하는 방법을 정리한다.

## 목적

관측성 스택은 배포 전 로컬에서 다음 항목을 검증하기 위해 사용한다.

1. `backend-api`와 `preprocess-worker`의 Actuator Prometheus metric 노출
2. RabbitMQ queue length와 consumer 상태 수집
3. Worker 처리량, 실패 코드, 처리 시간 수집
4. OpenTelemetry trace를 Jaeger에서 확인
5. NGINX 단일 진입점에서 `/grafana/`, `/jaeger/` 경로 접근
6. Kubernetes에서 KEDA/HPA desired replica와 Worker ready replica 확인

## 구성

| 컴포넌트 | 역할 | 기본 주소 |
| --- | --- | --- |
| Prometheus | metric 수집과 PromQL 조회 | `http://localhost:9090` |
| Grafana | dashboard 시각화 | `http://localhost/grafana/`, `http://localhost:3000/grafana/` |
| OpenTelemetry Collector | API/Worker trace 수신 후 Jaeger로 전달 | `http://localhost:4318` |
| Jaeger | trace 검색과 확인 | `http://localhost/jaeger/`, `http://localhost:16686/jaeger/` |
| RabbitMQ Prometheus plugin | queue metric 노출 | `http://localhost:15692/metrics` |

## Kubernetes 관측성 구성

Kubernetes에서는 `docprep-cloud` namespace에 아래 리소스를 배포한다.

| 컴포넌트 | 역할 |
| --- | --- |
| `prometheus` | backend-api, preprocess-worker, RabbitMQ, kube-state-metrics metric 수집 |
| `grafana` | KEDA 배치 비교와 운영 dashboard 제공 |
| `kube-state-metrics` | Deployment, HPA, Pod, Node 상태 metric 제공 |
| `node-exporter` | 노드별 CPU, 메모리, 파일시스템 실제 사용량 metric 제공 |
| `otel-collector` | API/Worker trace 수집 endpoint |
| `metrics-server` | `kubectl top`, HPA CPU 비교 실험에 필요 |

상태 확인:

```powershell
$KC="$env:USERPROFILE\Downloads\kube (1).conf"
kubectl --kubeconfig $KC -n docprep-cloud get pods,svc | Select-String "prometheus|grafana|kube-state|otel"
kubectl --kubeconfig $KC top nodes
kubectl --kubeconfig $KC -n docprep-cloud top pods
```

Grafana 외부 접근:

```text
https://<운영-도메인>/grafana/
```

Grafana local port-forward:

```powershell
kubectl --kubeconfig $KC -n docprep-cloud port-forward svc/grafana 3000:3000
```

접속:

```text
http://localhost:3000/grafana/
```

현재 manifest 기본 계정은 `admin/admin`이다. 공개 운영 전에는 반드시 Secret 기반 비밀번호로 교체한다.

Grafana에서 우선 볼 dashboard 항목:

| 항목 | 이유 |
| --- | --- |
| Worker desired replica | KEDA가 queue backlog에 반응하는지 확인 |
| Worker ready replica | 실제 스케줄링된 Worker 수 확인 |
| RabbitMQ queue length | 작업 적체 확인 |
| Job 처리량과 실패율 | 전처리 품질과 장애 확인 |
| Pod CPU/Memory | Worker request/limit 조정 근거 확보 |
| Node CPU/Memory | KEDA 확장 시 노드 리소스 병목 확인 |

추가 Grafana dashboard:

| 대시보드 | 목적 |
| --- | --- |
| `DocPrep KEDA 500장 배치 비교` | RabbitMQ queue, Worker replica, consumer, 처리량 확인 |
| `DocPrep Node Resource Overview` | 노드별 CPU, 메모리, load, 파일시스템, Worker Pod 분산 확인 |

노드 리소스 관측 방법은 [노드 리소스 관측 운영 문서](node-resource-observability.md)를 참고한다.

## 실행 방법

기본 로컬 스택에 관측성 override 파일을 추가해서 실행한다.

```powershell
docker compose `
  -f infra/docker-compose/docker-compose.local.yml `
  -f infra/docker-compose/docker-compose.observability.yml `
  --env-file infra/docker-compose/.env `
  up -d --build
```

`.env`가 아직 없으면 아래 명령으로 예시 파일을 복사한다.

```powershell
Copy-Item infra/docker-compose/.env.example infra/docker-compose/.env
```

관측성만 추가할 때 별도 사용자 secret은 필요 없다. Grafana 초기 계정은 로컬 기본값으로 `admin` / `admin`을 사용한다.

## 주요 환경변수

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `PROMETHEUS_PORT` | `9090` | Prometheus host port |
| `GRAFANA_PORT` | `3000` | Grafana host port |
| `GRAFANA_ADMIN_USER` | `admin` | Grafana 관리자 계정 |
| `GRAFANA_ADMIN_PASSWORD` | `admin` | Grafana 관리자 비밀번호 |
| `JAEGER_UI_PORT` | `16686` | Jaeger UI host port |
| `OTEL_GRPC_PORT` | `4317` | OTLP gRPC receiver port |
| `OTEL_HTTP_PORT` | `4318` | OTLP HTTP receiver port |
| `OTEL_TRACES_SAMPLER_PROBABILITY` | `1.0` | 로컬 trace sampling 비율 |
| `RABBITMQ_PROMETHEUS_PORT` | `15692` | RabbitMQ metric host port |

운영 환경에서는 `GRAFANA_ADMIN_PASSWORD`를 반드시 별도 secret으로 바꾼다.

## 수집 대상

Prometheus scrape 대상은 `infra/monitoring/prometheus/prometheus.yml`에서 관리한다.

| job | 대상 |
| --- | --- |
| `prometheus` | `prometheus:9090` |
| `backend-api` | `backend-api:8080/actuator/prometheus` |
| `preprocess-worker` | `preprocess-worker:8081/actuator/prometheus` |
| `rabbitmq` | `rabbitmq:15692/metrics` |

Worker는 metric endpoint를 열기 위해 로컬 관측성 실행 시 `WORKER_MANAGEMENT_PORT=8081`로 실행한다. 이 포트는 Compose 내부 네트워크에서 Prometheus가 접근하는 용도이며, 일반 사용자 API가 아니다.

## 확인 절차

1. 컨테이너 상태를 확인한다.

```powershell
docker compose `
  -f infra/docker-compose/docker-compose.local.yml `
  -f infra/docker-compose/docker-compose.observability.yml `
  --env-file infra/docker-compose/.env `
  ps
```

2. Prometheus target 상태를 확인한다.

```text
http://localhost:9090/targets
```

`backend-api`, `preprocess-worker`, `rabbitmq`가 `UP`이면 정상이다.

3. Prometheus에서 아래 query를 실행한다.

```promql
up
http_server_requests_seconds_count
worker_job_processed_total
rabbitmq_queue_messages_ready
```

4. Grafana dashboard를 확인한다.

```text
http://localhost/grafana/
```

로그인 후 `DocPrep Cloud / DocPrep Cloud Overview` dashboard를 연다.

5. Jaeger trace를 확인한다.

```text
http://localhost/jaeger/
```

서비스 목록에서 `backend-api` 또는 `preprocess-worker`를 선택한다. 로컬 trace sampling은 기본 `1.0`이라 요청이 발생하면 trace가 남아야 한다.

## NGINX 경로

NGINX는 아래 경로를 관측성 UI로 연결한다.

```text
/grafana/ -> grafana:3000
/jaeger/  -> jaeger:16686
```

기본 로컬 Compose만 실행한 경우 Grafana/Jaeger 컨테이너가 없으므로 해당 경로는 사용할 수 없다. NGINX 설정은 Docker DNS resolver를 사용하므로 관측성 컨테이너가 없어도 NGINX 자체는 시작 가능해야 한다.

## 종료 방법

```powershell
docker compose `
  -f infra/docker-compose/docker-compose.local.yml `
  -f infra/docker-compose/docker-compose.observability.yml `
  --env-file infra/docker-compose/.env `
  down
```

metric 저장 데이터까지 삭제하려면 volume을 포함해서 내린다.

```powershell
docker compose `
  -f infra/docker-compose/docker-compose.local.yml `
  -f infra/docker-compose/docker-compose.observability.yml `
  --env-file infra/docker-compose/.env `
  down -v
```

## 문제 해결

| 증상 | 확인할 것 |
| --- | --- |
| Prometheus target이 `DOWN` | 해당 컨테이너 로그와 `/actuator/prometheus` 노출 여부 확인 |
| Worker metric이 보이지 않음 | Worker가 실제 JobItem을 처리했는지, `preprocess-worker:8081` scrape가 `UP`인지 확인 |
| RabbitMQ metric이 보이지 않음 | `rabbitmq_prometheus` plugin 활성화와 `15692` port 확인 |
| Grafana dashboard가 비어 있음 | Prometheus datasource `Prometheus`가 정상인지 확인 |
| Jaeger trace가 없음 | `OTEL_TRACES_SAMPLER_PROBABILITY`, `otel-collector` 로그, API 요청 발생 여부 확인 |

## 완료 기준

1. Compose override를 포함해 스택이 기동된다.
2. Prometheus에서 API, Worker, RabbitMQ target이 `UP`이다.
3. Grafana 기본 dashboard가 자동 등록된다.
4. Worker 처리 후 `worker_job_processed_total`, `worker_processing_seconds`가 증가한다.
5. Jaeger에서 API 또는 Worker trace를 조회할 수 있다.
