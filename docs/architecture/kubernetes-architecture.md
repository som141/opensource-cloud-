# Kubernetes/KEDA 아키텍처

이 문서는 Docker Compose 기반 MVP를 Kubernetes로 확장할 때의 목표 구조를 설명한다.

## 목표 구조

```text
User Browser
  |
  v
Ingress Controller
  |
  v
nginx Service
  |--------------------------> frontend Service
  |
  | /api, /oauth2, /login/oauth2, /v3/api-docs
  v
backend-api Service
  |--------------------------> PostgreSQL Service 또는 관리형 DB
  |--------------------------> MinIO/S3 Service 또는 관리형 Object Storage
  |--------------------------> RabbitMQ Service 또는 관리형 Queue
                                      |
                                      v
                              preprocess-worker Deployment
                              KEDA ScaledObject
```

## 컴포넌트 책임

| 컴포넌트 | Kubernetes 리소스 | 책임 |
| --- | --- | --- |
| Ingress | `Ingress` | 운영 도메인과 TLS 진입점 |
| NGINX | `Deployment`, `Service`, `ConfigMap` | 프론트와 API 경로를 단일 진입점으로 분기 |
| Frontend | `Deployment`, `Service` | React/Vite build 결과 제공 |
| Backend API | `Deployment`, `Service`, `ConfigMap`, `Secret` | 인증, 프로젝트, 업로드, 이미지, Job API |
| Worker | `Deployment`, `Service`, `ConfigMap`, `Secret` | RabbitMQ 메시지 소비와 OpenCV 전처리 |
| KEDA | `ScaledObject`, `TriggerAuthentication` | RabbitMQ queue length 기반 Worker autoscaling |
| PostgreSQL | `Service` placeholder | 실제 운영 DB 또는 operator로 교체 |
| RabbitMQ | `Service` placeholder | 실제 운영 queue 또는 operator로 교체 |
| MinIO/S3 | `Service` placeholder | 실제 Object Storage로 교체 |
| OTel Collector | `Service` placeholder | trace 수집 endpoint |

## Namespace

모든 리소스는 기본적으로 `docprep-cloud` namespace에 둔다.

```text
infra/k8s/namespace.yml
```

## API 서버 배포

`backend-api`는 최소 2개 replica로 시작한다.

주요 설정:

| 항목 | 값 |
| --- | --- |
| container port | `8080` |
| readiness/liveness | `/actuator/health` |
| secret | `backend-api-secret` |
| configmap | `backend-api-config` |
| service | `backend-api:8080` |

API 서버는 OpenCV 작업을 수행하지 않는다. 이미지 전처리는 RabbitMQ 메시지 발행 후 Worker가 처리한다.

## Worker 배포와 KEDA

`preprocess-worker`는 기본 `replicas: 0`으로 시작한다. KEDA가 RabbitMQ queue length를 기준으로 replica 수를 조절한다.

주요 설정:

| 항목 | 값 |
| --- | --- |
| target Deployment | `preprocess-worker` |
| min replica | `0` |
| max replica | `20` |
| normal queue | `image.preprocess.normal` |
| high queue | `image.preprocess.high` |
| normal threshold | Worker 1개당 대기 메시지 `25`개 |
| high threshold | Worker 1개당 대기 메시지 `10`개 |
| cooldown | `300`초 |

RabbitMQ 접속 URI는 `preprocess-worker-secret`의 `RABBITMQ_AMQP_URI`에 둔다. KEDA `TriggerAuthentication`은 이 값을 `host` parameter로 참조한다.

## Queue 기준

Docker Compose와 Kubernetes의 queue 이름은 동일하게 유지한다.

```text
image.preprocess.high
image.preprocess.normal
image.preprocess.retry
image.preprocess.dlq
```

## Storage 기준

Kubernetes manifest는 `minio`라는 내부 Service 이름을 기준으로 한다.

실제 운영에서는 아래 중 하나로 교체한다.

1. 관리형 S3 호환 Object Storage
2. MinIO Operator
3. 직접 구성한 MinIO StatefulSet

Object Storage bucket은 private을 기본값으로 한다.

## Ingress와 NGINX

Ingress는 운영 도메인을 `nginx` Service로 연결한다. 실제 path routing은 NGINX ConfigMap에서 수행한다.

```text
/                    -> frontend
/api/*               -> backend-api
/oauth2/*            -> backend-api
/login/oauth2/*      -> backend-api
/swagger-ui/*        -> backend-api
/v3/api-docs/*       -> backend-api
```

SSE 성격의 Job event 경로는 `proxy_buffering off`와 긴 read timeout을 사용한다.

## Secret 원칙

Git에는 실제 secret을 커밋하지 않는다.

커밋 가능한 파일:

```text
secret.example.yml
```

커밋하면 안 되는 파일:

```text
secret.yml
*.local.yml
```

## 현재 skeleton의 한계

1. PostgreSQL, RabbitMQ, MinIO는 실제 Stateful workload가 아니라 `ExternalName` placeholder다.
2. 이미지 registry 주소는 `YOUR_REGISTRY/...:CHANGE_ME` placeholder다.
3. TLS secret과 운영 도메인은 실제 값으로 교체해야 한다.
4. KEDA CRD가 클러스터에 설치되어 있어야 `ScaledObject`를 적용할 수 있다.
5. 운영 backup, migration, persistent volume 정책은 별도 작업이 필요하다.

## 참고

KEDA RabbitMQ scaler는 공식 문서 기준으로 `queueName`, `mode: QueueLength`, `value`, `TriggerAuthentication`을 사용한다.

- KEDA RabbitMQ Queue scaler: https://keda.sh/docs/2.19/scalers/rabbitmq-queue/
