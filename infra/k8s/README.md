# Kubernetes/KEDA 배포 skeleton

이 디렉터리는 DocPrep Cloud를 Kubernetes로 옮기기 위한 manifest skeleton을 담는다.

## 현재 범위

1. `backend-api`, `preprocess-worker`, `frontend`, `nginx` 기본 Deployment/Service
2. RabbitMQ queue length 기반 KEDA `ScaledObject`
3. Secret 예시 파일
4. PostgreSQL, RabbitMQ, MinIO, OTel Collector MVP용 in-cluster Deployment/Service
5. NGINX 단일 진입점 Ingress skeleton

## 실제 적용 전에 필요한 값

아래 값은 사용자가 실제 배포 전에 준비해야 한다.

1. 운영 이미지 주소와 tag
2. 운영 도메인
3. TLS secret
4. Google OAuth 운영 Client ID/Secret
5. DB, RabbitMQ, MinIO 접속 정보
6. KEDA 설치 여부
7. Ingress Controller 설치 여부

## Secret 처리

`secret.example.yml`은 형식 예시만 제공한다. 실제 값은 `secret.yml`로 복사해서 클러스터에 적용하고 Git에는 커밋하지 않는다.

```powershell
Copy-Item infra/k8s/backend-api/secret.example.yml infra/k8s/backend-api/secret.yml
Copy-Item infra/k8s/preprocess-worker/secret.example.yml infra/k8s/preprocess-worker/secret.yml
```

`.gitignore`는 `infra/k8s/**/secret.yml`을 제외하도록 설정되어 있다.

## 적용 순서

```powershell
kubectl apply -f infra/k8s/namespace.yml
kubectl apply -f infra/k8s/backend-api/secret.yml
kubectl apply -f infra/k8s/preprocess-worker/secret.yml
kubectl apply -k infra/k8s
```

Secret 파일은 환경변수 기반으로 생성할 수 있다.

```powershell
.\scripts\k8s-generate-secrets.ps1 -Force
```

클러스터에 실제 배포하기 전에는 preflight를 먼저 실행한다.

```powershell
.\scripts\k8s-preflight.ps1
```

자세한 순서는 [Kubernetes Secret 준비와 Preflight](../../docs/operation/kubernetes-secret-preflight.md)를 따른다.

## 주의사항

- `postgres`, `rabbitmq`, `minio`는 현재 `emptyDir` 기반이다.
- Pod가 재생성되면 데이터가 사라질 수 있으므로 운영에서는 PVC, operator, 또는 관리형 서비스로 교체해야 한다.
- Worker autoscaling은 CPU가 아니라 RabbitMQ queue length 기준이다.
- `preprocess-worker` Deployment는 `replicas: 0`으로 시작하고 KEDA가 queue 상태에 따라 scale out한다.

## KEDA 비교 실험용 관측성 리소스

`infra/k8s/monitoring/`에는 KEDA 500장 배치 비교 실험을 위한 Prometheus, Grafana, kube-state-metrics manifest가 포함됩니다.

배포 후 Grafana는 NGINX의 `/grafana/` 경로로 접근합니다.

```text
https://YOUR_DOMAIN/grafana/
```

자세한 실험 절차는 [KEDA 500장 배치 비교 실험 가이드](../../docs/operation/keda-batch-benchmark.md)를 확인합니다.
