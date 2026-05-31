# 이슈 137. Kubernetes MVP 런타임 의존 서비스

## 목적

실제 Kubernetes 클러스터에 DocPrep Cloud MVP를 배포하기 위해 PostgreSQL, RabbitMQ, MinIO, OTel Collector를 클러스터 내부 Deployment/Service로 추가합니다.

## 작업 범위

1. PostgreSQL Deployment/Service 추가
2. RabbitMQ ConfigMap/Deployment/Service 추가
3. MinIO Deployment/Service/bucket init Job 추가
4. OTel Collector ConfigMap/Deployment/Service 추가
5. `kustomization.yml`에서 ExternalName placeholder 제거
6. RabbitMQ 기본 사용자 생성 lifecycle 추가
7. KEDA가 사용할 RabbitMQ FQDN secret 예시 반영
8. MVP 한계와 운영 전 교체 필요사항 문서화

## 설계 결정

현재 클러스터에는 StorageClass가 없으므로 `emptyDir`를 사용합니다.

이 결정은 빠른 MVP 검증을 위한 임시 선택입니다. 운영 공개 전에는 아래 중 하나로 바꿔야 합니다.

1. PVC와 StorageClass
2. PostgreSQL/RabbitMQ/MinIO operator
3. 관리형 PostgreSQL, 관리형 RabbitMQ, S3 호환 Object Storage

## 완료 기준

- `kubectl kustomize infra/k8s`가 통과합니다.
- 렌더링 결과에 `example.internal` placeholder가 남지 않습니다.
- `docprep-cloud` namespace에 runtime dependency가 배포될 수 있습니다.
- MinIO bucket init Job이 포함됩니다.
- RabbitMQ는 `load_definitions` 사용 시 기본 사용자가 자동 생성되지 않으므로 `postStart`에서 secret 기반 계정과 vhost 권한을 보장합니다.
- KEDA는 `rabbitmq.docprep-cloud.svc.cluster.local` 주소로 RabbitMQ에 연결합니다.
