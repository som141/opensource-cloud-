# Kubernetes MVP 런타임 의존 서비스

이 문서는 Kubernetes 클러스터 안에서 DocPrep Cloud MVP를 바로 실행하기 위한 PostgreSQL, RabbitMQ, MinIO, OTel Collector manifest를 설명합니다.

## 목적

초기 skeleton은 외부 PostgreSQL, RabbitMQ, MinIO, OTel Collector를 `ExternalName` placeholder로 두었습니다.

현재 클러스터에는 관리형 DB/Object Storage/Message Broker가 없으므로, MVP 검증을 위해 아래 의존 서비스를 클러스터 내부에 배포합니다.

| 서비스 | 역할 |
| --- | --- |
| PostgreSQL | 사용자, 프로젝트, 이미지, Job 메타데이터 저장 |
| RabbitMQ | 전처리 JobItem queue |
| MinIO | 원본/처리 이미지 object storage |
| MinIO bucket init Job | `image-preprocess-prod` bucket 생성 |
| OTel Collector | backend-api/worker trace 수신용 최소 collector |

## 중요한 한계

현재 manifest는 `emptyDir`를 사용합니다.

| 서비스 | 저장 방식 | 의미 |
| --- | --- | --- |
| PostgreSQL | `emptyDir` | Pod 재생성 시 DB 데이터 손실 가능 |
| RabbitMQ | `emptyDir` | Pod 재생성 시 queue/message 손실 가능 |
| MinIO | `emptyDir` | Pod 재생성 시 object 손실 가능 |

즉 이 구성은 MVP 검증용입니다. 운영 공개 전에는 PVC, operator, 또는 관리형 서비스로 교체해야 합니다.

## RabbitMQ 사용자와 KEDA DNS

RabbitMQ는 `management.load_definitions`로 queue/exchange를 가져오면 기본 사용자 seed를 건너뜁니다. 그래서 RabbitMQ Deployment는 `postStart` lifecycle에서 `backend-api-secret`의 RabbitMQ 계정으로 사용자를 만들고 `/` vhost 권한을 부여합니다.

KEDA operator는 `keda` 네임스페이스에서 실행되므로 `rabbitmq:5672` 같은 짧은 서비스 이름을 해석하지 못합니다. Worker secret의 `RABBITMQ_AMQP_URI`는 반드시 아래처럼 namespace까지 포함한 DNS 이름을 사용합니다.

```text
amqp://<user>:<password>@rabbitmq.docprep-cloud.svc.cluster.local:5672/%2F
```

## 적용 순서

1. KEDA 설치
2. NGINX Ingress Controller 설치 또는 ngrok 직접 연결 사용
3. `docprep-cloud` namespace 생성
4. backend-api/worker secret 적용
5. `kubectl apply -k infra/k8s`
6. `minio-bucket-init` Job 완료 확인
7. backend-api, frontend, nginx rollout 확인
8. RabbitMQ queue에 메시지 적재 시 KEDA worker scale-out 확인

## 확인 명령

```powershell
kubectl -n docprep-cloud get pods
kubectl -n docprep-cloud get svc
kubectl -n docprep-cloud get job minio-bucket-init
kubectl -n docprep-cloud get scaledobject
```

Worker는 queue가 비어 있으면 replica `0`이 정상입니다.

```powershell
kubectl -n docprep-cloud get deployment preprocess-worker
```

## JPA schema 정책

MVP 첫 배포를 위해 `JPA_DDL_AUTO=update`를 사용합니다.

운영 전에는 Flyway/Liquibase 같은 migration 도구를 도입하고 `validate`로 전환해야 합니다.
