# 런타임 자원 정책

이 문서는 Kubernetes manifest 기준 컨테이너 자원 설정과 운영 원칙을 정리한다. 자원 값은 `infra/k8s/**/deployment.yml`에 정의된 현재 값이다.

## 기본 원칙

1. API 서버와 Worker는 같은 노드에 떠도 책임은 분리한다.
2. CPU를 많이 쓰는 OpenCV 처리는 Worker에만 둔다.
3. API, Frontend, NGINX는 고정 replica 2개로 시작해 기본 가용성을 확보한다.
4. Worker는 RabbitMQ queue length 기준으로 KEDA가 자동 확장한다.
5. DB, Queue, Object Storage는 MVP에서는 Deployment로 두지만 운영 장기 사용 전에는 영속 저장소가 필요하다.
6. `limits`는 폭주 방지용이고, 성능 판단은 `requests`, 실제 사용량, queue backlog를 함께 본다.

## 애플리케이션 자원 설정

| 컴포넌트 | Replica | CPU request | Memory request | CPU limit | Memory limit | 설명 |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| `backend-api` | 2 | 250m | 512Mi | 1 | 1Gi | 인증, 업로드, Job API. 이미지 전처리는 하지 않는다. |
| `frontend` | 2 | 100m | 128Mi | 500m | 256Mi | React/Vite 정적 파일 제공. |
| `nginx` | 2 | 100m | 128Mi | 500m | 256Mi | 단일 진입점, reverse proxy, object route. |
| `preprocess-worker` | 0~20 | 500m | 1Gi | 2 | 2Gi | OpenCV 전처리. KEDA가 replica를 조절한다. |

## 런타임 의존성 자원 설정

| 컴포넌트 | Replica | CPU request | Memory request | CPU limit | Memory limit | 저장소 |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| `postgres` | 1 | 100m | 256Mi | 500m | 512Mi | `emptyDir` |
| `rabbitmq` | 1 | 100m | 256Mi | 500m | 512Mi | `emptyDir` |
| `minio` | 1 | 100m | 256Mi | 500m | 512Mi | `emptyDir` |

현재 `emptyDir`는 테스트/MVP 기준이다. Pod가 삭제되면 데이터가 사라진다. 운영 데이터 보존이 필요하면 아래 중 하나로 교체한다.

| 대상 | 권장 대안 |
| --- | --- |
| PostgreSQL | 관리형 PostgreSQL, PostgreSQL operator, StatefulSet + PVC |
| RabbitMQ | 관리형 RabbitMQ, RabbitMQ cluster operator, StatefulSet + PVC |
| MinIO | 관리형 S3, MinIO operator, StatefulSet + PVC |

## 관측성 자원 설정

| 컴포넌트 | Replica | CPU request | Memory request | CPU limit | Memory limit | 설명 |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| `prometheus` | 1 | 200m | 512Mi | 1 | 1Gi | metric 수집, 7일 retention |
| `grafana` | 1 | 100m | 256Mi | 500m | 512Mi | dashboard 제공 |
| `kube-state-metrics` | 1 | 100m | 128Mi | 500m | 512Mi | Deployment/HPA/Pod metric 제공 |
| `otel-collector` | 1 | 50m | 128Mi | 250m | 256Mi | OTLP trace 수집 |

Grafana 현재 기본 계정은 manifest 기준 `admin/admin`이다. 공개 운영 전에는 반드시 Secret 기반으로 교체한다.

## Worker autoscaling 정책

| 항목 | 값 |
| --- | --- |
| ScaledObject | `preprocess-worker` |
| target | `deployment/preprocess-worker` |
| polling interval | 10초 |
| cooldown period | 300초 |
| min replica | 0 |
| max replica | 20 |
| normal queue | `image.preprocess.normal` |
| normal threshold | 25 |
| high queue | `image.preprocess.high` |
| high threshold | 10 |

KEDA 계산 예시는 다음과 같다.

| Queue backlog | normal threshold 25 기준 예상 replica |
| ---: | ---: |
| 0 | 0 |
| 1~25 | 1 |
| 100 | 4 |
| 250 | 10 |
| 500 | 20 |

`maxReplicaCount=20`이므로 backlog가 500개를 초과해도 Worker는 20개까지만 늘어난다.

## KEDA min 0과 min 1 선택 기준

| 기준 | `min=0` | `min=1` |
| --- | --- | --- |
| 유휴 비용 | 가장 낮음 | Worker 1개 비용 상시 발생 |
| 첫 작업 지연 | scale-up cold start 발생 | 거의 없음 |
| 500장 실험 전체 시간 | 81.755초 | 61.937초 |
| 추천 상황 | 비용 절감, 비정기 배치 | 사용자 대기 시간이 중요한 서비스 |

현재 운영 복구 기본값은 `min=0`이다. 데모나 사용자 테스트처럼 응답 체감이 중요하면 아래 명령으로 임시 전환한다.

```powershell
.\scripts\k8s-scale-mode.ps1 `
  -Mode keda-on-min1 `
  -KubeConfig "$env:USERPROFILE\Downloads\kube (1).conf"
```

다시 비용 절감 모드로 복구한다.

```powershell
.\scripts\k8s-scale-mode.ps1 `
  -Mode keda-on `
  -KubeConfig "$env:USERPROFILE\Downloads\kube (1).conf"
```

## Probe 정책

| 컴포넌트 | Readiness | Liveness |
| --- | --- | --- |
| `backend-api` | `/actuator/health` | `/actuator/health` |
| `preprocess-worker` | `/actuator/health` on management port 8081 | `/actuator/health` on management port 8081 |
| `frontend` | `/` | `/` |
| `nginx` | `/` | `/` |
| `postgres` | `pg_isready` | `pg_isready` |
| `rabbitmq` | `rabbitmq-diagnostics check_running` | `rabbitmq-diagnostics check_running` |
| `minio` | `/minio/health/ready` | `/minio/health/live` |
| `prometheus` | `/-/ready` | `/-/healthy` |
| `grafana` | `/api/health` | `/api/health` |

Probe가 실패하면 Kubernetes가 트래픽 전달 또는 컨테이너 재시작을 조절한다. Worker는 queue가 비어 있을 때 Pod가 없어도 장애가 아니다.

## 노드 자원 해석

500장 실험에서 KEDA는 desired replica를 20까지 요청했지만, 실제 ready Worker는 클러스터 자원 부족으로 더 낮게 관측될 수 있었다. 이 경우 KEDA가 실패한 것이 아니라, scheduler가 CPU/메모리 request를 만족하는 노드를 찾지 못한 것이다.

확인 명령:

```powershell
kubectl top nodes
kubectl -n docprep-cloud get pods
kubectl -n docprep-cloud describe pod <pending-worker-pod>
```

Pending 사유가 `Insufficient cpu`, `Insufficient memory`, `control-plane taint`이면 노드 증설 또는 request 조정이 필요하다.

## 운영 전 필수 변경

| 항목 | 현재 | 운영 전 조치 |
| --- | --- | --- |
| Grafana password | `admin/admin` | Secret 기반 강한 비밀번호 |
| PostgreSQL storage | `emptyDir` | PVC 또는 managed DB |
| RabbitMQ storage | `emptyDir` | PVC 또는 managed queue |
| MinIO storage | `emptyDir` | PVC 또는 managed S3 |
| TLS | ngrok 또는 수동 secret | 운영 도메인 + 인증서 자동화 |
| OAuth redirect | 현재 도메인 기준 | 운영 도메인 callback 등록 |
| Worker min replica | 0 | 비용/지연 기준으로 0 또는 1 선택 |
| Alert | dashboard 중심 | Prometheus alert rule 추가 |

## 관련 문서

- [Kubernetes/KEDA 아키텍처](kubernetes-architecture.md)
- [Kubernetes 배포 가이드](../operation/kubernetes-deployment.md)
- [KEDA 배치 비교 실험](../operation/keda-batch-benchmark.md)
- [운영 원칙](../operation/operating-principles.md)
