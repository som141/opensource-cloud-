# Kubernetes Secret 준비와 Preflight

이 문서는 Kubernetes 배포 직전에 필요한 application secret manifest를 로컬에서 생성하고, 클러스터 사전 조건을 확인하는 방법을 정리합니다.

실제 secret 값은 Git에 커밋하지 않습니다. 생성되는 파일은 `.gitignore` 대상입니다.

## 사용하는 스크립트

| 스크립트 | 용도 |
| --- | --- |
| `scripts/k8s-generate-secrets.ps1` | 환경변수에서 값을 읽어 `backend-api-secret`, `preprocess-worker-secret` manifest 생성 |
| `scripts/k8s-preflight.ps1` | 현재 `kubectl` context의 namespace, secret, KEDA, IngressClass 존재 여부 확인 |

## 생성되는 파일

```text
infra/k8s/backend-api/secret.yml
infra/k8s/preprocess-worker/secret.yml
```

두 파일은 `.gitignore`에 포함되어 있으므로 원격 저장소에 올라가지 않습니다.

## 필요한 환경변수

`k8s-generate-secrets.ps1` 실행 전 아래 값을 현재 shell 환경변수로 넣습니다.

| 환경변수 | 사용처 |
| --- | --- |
| `DB_USERNAME` | backend-api DB 접속 계정 |
| `DB_PASSWORD` | backend-api DB 접속 비밀번호 |
| `SPRING_RABBITMQ_USERNAME` | API/Worker RabbitMQ 계정 |
| `SPRING_RABBITMQ_PASSWORD` | API/Worker RabbitMQ 비밀번호 |
| `RABBITMQ_AMQP_URI` | KEDA와 Worker가 사용할 AMQP URI |
| `GOOGLE_CLIENT_ID` | Google OAuth 운영 Client ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth 운영 Client Secret |
| `JWT_SECRET` | Access Token 서명 secret. 32자 이상 |
| `MINIO_ACCESS_KEY` | S3/MinIO access key |
| `MINIO_SECRET_KEY` | S3/MinIO secret key |
| `WORKER_INTERNAL_TOKEN` | backend-api와 worker 내부 통신 토큰 |

예시:

```powershell
$env:DB_USERNAME = "CHANGE_THIS_IN_LOCAL_SHELL"
$env:DB_PASSWORD = "CHANGE_THIS_IN_LOCAL_SHELL"
$env:SPRING_RABBITMQ_USERNAME = "CHANGE_THIS_IN_LOCAL_SHELL"
$env:SPRING_RABBITMQ_PASSWORD = "CHANGE_THIS_IN_LOCAL_SHELL"
$env:RABBITMQ_AMQP_URI = "amqp://USER:PASSWORD@rabbitmq:5672/%2F"
$env:GOOGLE_CLIENT_ID = "CHANGE_THIS_IN_LOCAL_SHELL"
$env:GOOGLE_CLIENT_SECRET = "CHANGE_THIS_IN_LOCAL_SHELL"
$env:JWT_SECRET = "CHANGE_THIS_TO_AT_LEAST_32_CHARACTERS"
$env:MINIO_ACCESS_KEY = "CHANGE_THIS_IN_LOCAL_SHELL"
$env:MINIO_SECRET_KEY = "CHANGE_THIS_IN_LOCAL_SHELL"
$env:WORKER_INTERNAL_TOKEN = "CHANGE_THIS_TO_A_LONG_RANDOM_VALUE"
```

위 예시는 형식만 보여줍니다. 실제 값은 문서나 PR에 적지 않습니다.

## Secret manifest 생성

레포지토리 루트에서 실행합니다.

```powershell
.\scripts\k8s-generate-secrets.ps1
```

이미 파일이 있으면 덮어쓰지 않습니다. 다시 생성하려면 `-Force`를 붙입니다.

```powershell
.\scripts\k8s-generate-secrets.ps1 -Force
```

현재 `kubectl` context에 바로 적용하려면 `-Apply`를 붙입니다.

```powershell
.\scripts\k8s-generate-secrets.ps1 -Force -Apply
```

`-Apply`는 현재 `kubectl` context에 secret을 적용합니다. 실행 전에 반드시 아래 명령으로 대상 cluster를 확인합니다.

```powershell
kubectl config current-context
```

## 클러스터 preflight

Kubernetes 사전 조건을 확인합니다.

```powershell
.\scripts\k8s-preflight.ps1
```

특정 context를 선택하려면 다음처럼 실행합니다.

```powershell
.\scripts\k8s-preflight.ps1 -KubeContext "YOUR_CONTEXT"
```

GHCR package가 private이고 image pull secret을 직접 준비했다면 다음 옵션을 추가합니다.

```powershell
.\scripts\k8s-preflight.ps1 -RequireGhcrPullSecret
```

## 확인하는 항목

`k8s-preflight.ps1`은 읽기 전용으로 아래 항목을 확인합니다.

| 항목 | 확인 명령 |
| --- | --- |
| kubectl 설치 | `kubectl` command |
| 현재 context | `kubectl config current-context` |
| cluster 연결 | `kubectl cluster-info` |
| namespace | `kubectl get namespace docprep-cloud` |
| backend secret | `kubectl -n docprep-cloud get secret backend-api-secret` |
| worker secret | `kubectl -n docprep-cloud get secret preprocess-worker-secret` |
| TLS secret | `kubectl -n docprep-cloud get secret docprep-cloud-tls` |
| KEDA CRD | `kubectl get crd scaledobjects.keda.sh` |
| KEDA auth CRD | `kubectl get crd triggerauthentications.keda.sh` |
| IngressClass | `kubectl get ingressclass nginx` |

## 실제 배포 전 순서

권장 순서는 아래와 같습니다.

1. 운영 secret 값을 로컬 shell 환경변수로만 설정합니다.
2. `k8s-generate-secrets.ps1 -Force`로 secret manifest를 만듭니다.
3. `kubectl config current-context`로 대상 cluster를 확인합니다.
4. namespace가 없으면 먼저 생성합니다.
5. `kubectl apply -f infra/k8s/backend-api/secret.yml`을 실행합니다.
6. `kubectl apply -f infra/k8s/preprocess-worker/secret.yml`을 실행합니다.
7. TLS secret을 생성하거나 기존 secret 이름을 확인합니다.
8. `k8s-preflight.ps1`을 통과시킵니다.
9. `Render Kubernetes Manifests` workflow로 YAML을 검토합니다.
10. `Deploy Kubernetes` workflow를 `dry-run`으로 실행합니다.
11. 문제가 없으면 `Deploy Kubernetes` workflow를 `apply`로 실행합니다.

## 주의사항

- `secret.yml` 파일은 로컬에 남기지 않아도 됩니다. 필요할 때 다시 생성하면 됩니다.
- 생성된 secret 값을 PR, 문서, 이슈에 붙여넣지 않습니다.
- `RABBITMQ_AMQP_URI`에 비밀번호가 들어가므로 터미널 공유 시 주의합니다.
- 운영 cluster에 적용하기 전 `kubectl config current-context`를 반드시 확인합니다.
