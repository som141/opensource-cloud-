# Kubernetes/KEDA 아키텍처

이 문서는 DocPrep Cloud의 현재 Kubernetes 배포 구조를 설명한다. Docker Compose MVP에서 출발했지만, 현재 운영 실험 기준은 `docprep-cloud` namespace에 애플리케이션, 런타임 의존성, 관측성 리소스를 함께 배포하는 구조다.

## 배포 단위

| 영역 | Kubernetes 리소스 | 설명 |
| --- | --- | --- |
| 진입점 | `Ingress`, `nginx` Deployment/Service/ConfigMap | 외부 트래픽을 프론트, API, OAuth, object route, Grafana로 분기 |
| Frontend | `frontend` Deployment/Service | React/Vite 정적 파일 제공 |
| API | `backend-api` Deployment/Service/ConfigMap/Secret | 인증, 프로젝트, 업로드, 이미지, Job API |
| Worker | `preprocess-worker` Deployment/Service/ConfigMap/Secret | RabbitMQ 메시지 소비와 OpenCV 전처리 |
| Queue | `rabbitmq` Deployment/Service/ConfigMap | 전처리 JobItem 메시지 전달 |
| DB | `postgres` Deployment/Service | MVP용 PostgreSQL |
| Object Storage | `minio` Deployment/Service, bucket init Job | 원본/결과 이미지 저장 |
| Autoscaling | KEDA `ScaledObject`, `TriggerAuthentication`, HPA | RabbitMQ queue length 기반 Worker 확장 |
| Observability | `prometheus`, `grafana`, `kube-state-metrics`, `otel-collector` | metric, dashboard, trace 수집 |
| CI/CD Runner | `github-actions/docprep-k8s-runner` | 사설망 Kubernetes API에 접근하는 self-hosted runner |

## 네트워크 구조

```text
Internet
  |
  v
ngrok 또는 운영 도메인
  |
  v
Ingress Controller
  |
  v
nginx Service
  |-- /                         -> frontend Service
  |-- /api/*                    -> backend-api Service
  |-- /oauth2/*                 -> backend-api Service
  |-- /login/oauth2/*           -> backend-api Service
  |-- /swagger-ui/*             -> backend-api Service
  |-- /v3/api-docs/*            -> backend-api Service
  |-- /image-preprocess-prod/*  -> minio Service
  |-- /grafana/*                -> grafana Service
```

NGINX는 단일 진입점 역할만 수행한다. 사용자 인증, Job 상태 판단, object 권한 검증은 backend-api의 책임이다.

## Namespace 기준

| Namespace | 용도 |
| --- | --- |
| `docprep-cloud` | 애플리케이션, DB, Queue, Object Storage, 관측성 |
| `keda` | KEDA operator와 metrics API |
| `ingress-nginx` | NGINX Ingress Controller |
| `github-actions` | self-hosted runner |
| `kubernetes-dashboard` | Kubernetes Dashboard GUI |
| `ngrok` | ngrok tunnel workload |

기본 운영 확인은 `docprep-cloud` namespace를 기준으로 한다.

```powershell
kubectl -n docprep-cloud get pods,deploy,svc,ingress,scaledobject,hpa
```

## Worker 확장 구조

Worker는 일반 Deployment지만 replica를 사람이 직접 늘리는 것이 아니라 KEDA가 조절한다.

| 항목 | 현재 값 |
| --- | --- |
| scale target | `deployment/preprocess-worker` |
| polling interval | 10초 |
| cooldown period | 300초 |
| min replica | 0 |
| max replica | 20 |
| normal queue | `image.preprocess.normal` |
| normal queue threshold | Worker 1개당 대기 메시지 25개 |
| high queue | `image.preprocess.high` |
| high queue threshold | Worker 1개당 대기 메시지 10개 |

queue가 비어 있으면 `preprocess-worker`가 `0/0`으로 보이는 것이 정상이다.

```powershell
kubectl -n docprep-cloud get deploy preprocess-worker
kubectl -n docprep-cloud get scaledobject preprocess-worker
kubectl -n docprep-cloud get hpa keda-hpa-preprocess-worker
```

## KEDA와 HPA CPU의 차이

| 방식 | 확장 기준 | 장점 | 한계 |
| --- | --- | --- | --- |
| KEDA min 0 | RabbitMQ queue length, 0개에서 시작 | 유휴 비용 최소화 | cold start 대기 발생 |
| KEDA min 1 | RabbitMQ queue length, 1개 유지 | 빠른 시작과 큐 기반 확장 | 최소 Worker 비용 발생 |
| HPA CPU | Pod CPU 사용률 | Kubernetes 기본 기능 | 큐 적체를 직접 보지 못함 |
| Fixed Worker | replica 고정 | 예측 쉬움 | 트래픽 변동에 비용 또는 지연 발생 |

500장 실험에서는 같은 입력 기준으로 KEDA min 1이 HPA CPU보다 처리 구간 기준 더 빠르게 동작했다. 이유는 HPA CPU가 CPU 사용률이 올라간 뒤 반응하는 반면, KEDA는 queue backlog를 보고 Worker를 직접 늘리기 때문이다.

## 현재 실험 결과 요약

| 방식 | 큐 대기 | 처리 시간 | 전체 시간 | 성공/전체 | 관측 replica |
| --- | ---: | ---: | ---: | ---: | ---: |
| Fixed 1 | 2.716초 | 98.1초 | 100.815초 | 500/500 | 1 |
| HPA CPU | 2.145초 | 91.736초 | 93.881초 | 500/500 | 3 |
| KEDA min 1 | 2.209초 | 59.728초 | 61.937초 | 500/500 | 10 |
| KEDA min 0 | 29.183초 | 52.572초 | 81.755초 | 500/500 | 20 |

운영 기본값은 비용 절감을 우선해 `minReplicaCount=0`으로 복구했다. 사용자 체감 지연을 줄이는 것이 더 중요하면 `minReplicaCount=1`로 바꾸는 것이 합리적이다.

## 런타임 의존성 운영 상태

현재 manifest는 PostgreSQL, RabbitMQ, MinIO를 클러스터 내부 Deployment로 실행한다. 단, 현재 설정은 MVP/테스트 기준이다.

| 컴포넌트 | 현재 저장소 | 운영 권장 |
| --- | --- | --- |
| PostgreSQL | `emptyDir` | PVC, managed DB, 또는 PostgreSQL operator |
| RabbitMQ | `emptyDir` | PVC, managed RabbitMQ, 또는 RabbitMQ cluster operator |
| MinIO | `emptyDir` | PVC, MinIO operator, 또는 관리형 S3 |
| Prometheus | `emptyDir` | PVC 또는 외부 장기 저장소 |

`emptyDir`는 Pod가 재생성되면 데이터가 사라진다. 운영 장기 사용 전에는 반드시 영속 저장소로 교체해야 한다.

## CI/CD 구조

```text
main merge
  -> Build GHCR Images
  -> ghcr.io/som141/docprep-cloud/backend-api:<short-sha>
  -> ghcr.io/som141/docprep-cloud/preprocess-worker:<short-sha>
  -> ghcr.io/som141/docprep-cloud/frontend:<short-sha>
  -> Deploy Kubernetes
  -> Kubernetes 내부 self-hosted runner
  -> kubectl apply -k rendered manifests
  -> backend-api/frontend/nginx rollout 확인
```

Worker는 KEDA scale-to-zero 대상이므로 배포 후 rollout 확인 대상에서 제외하거나, queue가 있을 때만 Pod가 뜨는 것으로 판단한다.

## 정상 상태 기준

| 항목 | 정상 기준 |
| --- | --- |
| `backend-api` | `2/2 Running`, `/actuator/health` ready |
| `frontend` | `2/2 Running` |
| `nginx` | `2/2 Running`, Ingress host 연결 |
| `postgres` | `1/1 Running` |
| `rabbitmq` | `1/1 Running`, management/AMQP/Prometheus port open |
| `minio` | `1/1 Running`, bucket init Job Completed |
| `preprocess-worker` | queue 없음: `0/0`, queue 있음: KEDA가 replica 증가 |
| `grafana` | `1/1 Running`, `/grafana/` 접근 가능 |
| `prometheus` | `1/1 Running`, targets UP |
| `kube-state-metrics` | `1/1 Running` |
| `metrics-server` | `kubectl top nodes` 동작 |

## 관련 문서

- [시스템 개요](system-overview.md)
- [런타임 자원 정책](runtime-resource-policy.md)
- [Kubernetes 배포 가이드](../operation/kubernetes-deployment.md)
- [Kubernetes GitHub Actions 배포](../operation/kubernetes-github-actions-deploy.md)
- [KEDA 배치 비교 실험](../operation/keda-batch-benchmark.md)
