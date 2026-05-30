# Kubernetes/KEDA 배포 가이드

이 문서는 DocPrep Cloud를 Kubernetes에 배포하기 위한 skeleton 적용 절차를 정리한다.

현재 문서는 실제 운영 클러스터에 바로 적용하는 완성본이 아니라, 배포 전 필요한 값과 교체 지점을 명확히 하기 위한 기준 문서다.

## 1. 사전 준비

실제 적용 전에 아래 항목을 준비해야 한다.

| 항목 | 설명 |
| --- | --- |
| Kubernetes cluster | 운영 또는 테스트 클러스터 |
| `kubectl` context | 배포 대상 cluster를 가리키는 context |
| Ingress Controller | `ingressClassName: nginx`를 처리할 controller |
| KEDA | `ScaledObject`, `TriggerAuthentication` CRD |
| 이미지 registry | backend-api, preprocess-worker, frontend 이미지 저장소 |
| 운영 도메인 | Ingress host에 사용할 도메인 |
| TLS secret | `docprep-cloud-tls` 또는 운영 환경에 맞는 secret |
| PostgreSQL | 관리형 DB, operator, 또는 StatefulSet |
| RabbitMQ | 관리형 RabbitMQ, operator, 또는 StatefulSet |
| Object Storage | S3 호환 저장소 또는 MinIO |
| Google OAuth | 운영 Client ID와 Client Secret |

## 2. 사용자에게 미리 받아야 할 값

Kubernetes 실제 적용 작업을 시작하기 전에 아래 값을 사용자에게 받아야 한다.

| 값 | 예시 |
| --- | --- |
| 운영 도메인 | `https://YOUR_DOMAIN` |
| backend-api 이미지 | `ghcr.io/ORG/docprep-backend-api:TAG` |
| preprocess-worker 이미지 | `ghcr.io/ORG/docprep-preprocess-worker:TAG` |
| frontend 이미지 | `ghcr.io/ORG/docprep-frontend:TAG` |
| DB URL | `jdbc:postgresql://postgres:5432/image_preprocess` |
| DB username/password | 실제 secret |
| RabbitMQ username/password | 실제 secret |
| RabbitMQ AMQP URI | `amqp://USER:PASSWORD@rabbitmq:5672/%2F` |
| MinIO/S3 endpoint/access key/secret key | 실제 secret |
| Google OAuth Client ID/Secret | 운영용 값 |
| JWT secret | 32 bytes 이상 |
| Worker internal token | backend-api와 worker가 공유하는 내부 토큰 |

이 값들은 Git에 커밋하지 않는다.

## 3. Secret 파일 작성

예시 파일을 실제 secret 파일로 복사한다.

```powershell
Copy-Item infra/k8s/backend-api/secret.example.yml infra/k8s/backend-api/secret.yml
Copy-Item infra/k8s/preprocess-worker/secret.example.yml infra/k8s/preprocess-worker/secret.yml
```

`secret.yml` 안의 `CHANGE_ME` 값을 실제 값으로 바꾼다.

`.gitignore`는 아래 파일을 제외한다.

```text
infra/k8s/**/secret.yml
infra/k8s/**/*.local.yml
```

## 4. 이미지와 도메인 교체

아래 파일의 placeholder를 실제 값으로 교체한다.

| 파일 | 교체 값 |
| --- | --- |
| `infra/k8s/backend-api/deployment.yml` | `YOUR_REGISTRY/docprep-backend-api:CHANGE_ME` |
| `infra/k8s/preprocess-worker/deployment.yml` | `YOUR_REGISTRY/docprep-preprocess-worker:CHANGE_ME` |
| `infra/k8s/frontend/deployment.yml` | `YOUR_REGISTRY/docprep-frontend:CHANGE_ME` |
| `infra/k8s/nginx/ingress.yml` | `YOUR_DOMAIN`, `docprep-cloud-tls` |
| `infra/k8s/backend-api/configmap.yml` | `https://YOUR_DOMAIN`, storage endpoint |

실제 운영에서는 `kustomize overlay` 또는 GitHub Actions에서 image tag를 주입하는 방식을 권장한다.

## 5. 외부 서비스 placeholder 교체

현재 skeleton은 PostgreSQL, RabbitMQ, MinIO, OTel Collector를 `ExternalName` Service로 둔다.

| 파일 | 교체 대상 |
| --- | --- |
| `infra/k8s/postgres/service-placeholder.yml` | 실제 PostgreSQL Service 또는 관리형 DB endpoint |
| `infra/k8s/rabbitmq/service-placeholder.yml` | 실제 RabbitMQ Service 또는 관리형 RabbitMQ endpoint |
| `infra/k8s/minio/service-placeholder.yml` | 실제 MinIO/S3 endpoint |
| `infra/k8s/monitoring/service-placeholder.yml` | 실제 OTel Collector Service |

Kubernetes 내부에 직접 Stateful workload를 둘 경우 operator 또는 별도 StatefulSet manifest를 추가한다.

## 6. KEDA 설치 확인

KEDA CRD가 설치되어 있는지 확인한다.

```bash
kubectl get crd scaledobjects.keda.sh
kubectl get crd triggerauthentications.keda.sh
```

없으면 KEDA를 먼저 설치한다. 설치 방법은 클러스터 운영 방식에 따라 Helm 또는 manifest 적용 중 하나를 선택한다.

## 7. 적용 순서

namespace를 먼저 만든다.

```bash
kubectl apply -f infra/k8s/namespace.yml
```

secret을 적용한다.

```bash
kubectl apply -f infra/k8s/backend-api/secret.yml
kubectl apply -f infra/k8s/preprocess-worker/secret.yml
```

나머지 manifest를 적용한다.

```bash
kubectl apply -k infra/k8s
```

## 8. 상태 확인

```bash
kubectl -n docprep-cloud get pods
kubectl -n docprep-cloud get svc
kubectl -n docprep-cloud get ingress
kubectl -n docprep-cloud get scaledobject
```

Worker는 queue가 비어 있으면 `0` replica가 정상이다.

```bash
kubectl -n docprep-cloud get deployment preprocess-worker
```

## 9. KEDA 동작 확인

RabbitMQ의 `image.preprocess.normal` 또는 `image.preprocess.high` queue에 메시지가 쌓이면 KEDA가 Worker replica를 늘려야 한다.

```bash
kubectl -n docprep-cloud describe scaledobject preprocess-worker
kubectl -n docprep-cloud get hpa
```

기본 기준:

| Queue | threshold |
| --- | --- |
| `image.preprocess.normal` | Worker 1개당 25개 |
| `image.preprocess.high` | Worker 1개당 10개 |

## 10. E2E 확인

1. 운영 도메인 접속
2. Google 로그인
3. 프로젝트 생성
4. 이미지 또는 ZIP 업로드
5. 전처리 Job 생성
6. RabbitMQ queue 증가 확인
7. Worker replica 증가 확인
8. 처리 완료 후 Worker replica 감소 확인
9. 처리된 이미지 또는 결과 ZIP 다운로드

## 11. 현재 skeleton의 한계

1. Stateful 서비스 운영 정책은 아직 포함하지 않는다.
2. TLS 발급 자동화는 포함하지 않는다.
3. GitHub Actions에서 image tag를 자동 주입하는 CD workflow는 별도 작업이다.
4. 운영 alert rule, ServiceMonitor, Prometheus Operator 설정은 별도 작업이다.

## 12. 관련 문서

- [Kubernetes/KEDA 아키텍처](../architecture/kubernetes-architecture.md)
- [관측성 로컬 실행](observability.md)
- [운영 배포 가이드](production-deployment-guide.md)
- [배포 체크리스트](deployment-checklist.md)
