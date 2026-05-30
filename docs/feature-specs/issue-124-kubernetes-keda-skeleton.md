# Issue 124. Kubernetes/KEDA 배포 skeleton

## 목적

Docker Compose 기반 MVP를 Kubernetes로 확장할 수 있도록 기본 manifest와 KEDA Worker autoscaling skeleton을 추가한다.

## 작업 범위

1. Namespace manifest
2. `backend-api` Deployment, Service, ConfigMap, Secret 예시
3. `preprocess-worker` Deployment, Service, ConfigMap, Secret 예시
4. RabbitMQ queue length 기반 KEDA `ScaledObject`
5. KEDA `TriggerAuthentication`
6. `frontend` Deployment, Service
7. `nginx` ConfigMap, Deployment, Service, Ingress
8. PostgreSQL, RabbitMQ, MinIO, OTel Collector placeholder Service
9. Kubernetes 아키텍처 문서
10. Kubernetes 배포 운영 문서

## 제외 범위

1. 실제 Kubernetes cluster 적용
2. 실제 secret 작성
3. 이미지 registry push workflow
4. Stateful workload 운영 manifest
5. TLS 자동 발급
6. Prometheus Operator, ServiceMonitor, alert rule

## 주요 결정

| 항목 | 결정 |
| --- | --- |
| namespace | `docprep-cloud` |
| Worker scale 기준 | RabbitMQ queue length |
| Worker min replica | `0` |
| Worker max replica | `20` |
| normal queue threshold | `25` |
| high queue threshold | `10` |
| queue 이름 | Docker Compose와 동일하게 유지 |
| secret 처리 | `secret.example.yml`만 커밋 |

## 산출물

```text
infra/k8s/
├── namespace.yml
├── kustomization.yml
├── backend-api/
├── preprocess-worker/
├── frontend/
├── nginx/
├── postgres/
├── rabbitmq/
├── minio/
└── monitoring/
```

## 적용 전 사용자 준비값

실제 배포 전에 사용자는 아래 값을 준비해야 한다.

1. 운영 도메인
2. TLS secret
3. image registry 주소와 tag
4. Google OAuth 운영 Client ID/Secret
5. DB 접속 정보
6. RabbitMQ 접속 정보
7. MinIO/S3 접속 정보
8. JWT secret
9. Worker internal token

## 검증 기준

1. `secret.example.yml`에는 실제 값이 없어야 한다.
2. `secret.yml`은 Git 추적 대상에서 제외되어야 한다.
3. KEDA `ScaledObject`는 `image.preprocess.normal`, `image.preprocess.high`를 참조해야 한다.
4. manifest는 Kubernetes 리소스 기본 필드를 갖춰야 한다.
5. 운영 문서에 실제 적용 순서와 사용자 준비값이 포함되어야 한다.

## 참고

KEDA RabbitMQ scaler는 공식 문서 기준으로 `queueName`, `mode: QueueLength`, `value`, `TriggerAuthentication`을 사용한다.

- https://keda.sh/docs/2.19/scalers/rabbitmq-queue/
