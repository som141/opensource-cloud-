# Kubernetes GitHub Actions 배포

이 문서는 GitHub Actions에서 Kubernetes manifest를 렌더링한 뒤 실제 클러스터에 `dry-run` 또는 `apply` 하는 수동 CD workflow를 설명합니다.

## 목적

기존 `Render Kubernetes Manifests` workflow는 YAML artifact만 만들고 클러스터에는 적용하지 않습니다.

`Deploy Kubernetes` workflow는 다음 단계입니다.

1. GHCR 이미지 태그를 Kubernetes manifest에 주입합니다.
2. 운영 도메인과 TLS secret 이름을 주입합니다.
3. GitHub production secret의 kubeconfig로 클러스터에 접속합니다.
4. KEDA, IngressClass, namespace, application secret을 사전 확인합니다.
5. `dry-run`으로 서버 검증하거나 `apply`로 실제 배포합니다.

## Workflow 파일

```text
.github/workflows/deploy-k8s.yml
```

## 실행 조건

수동 실행만 지원합니다.

```text
Actions -> Deploy Kubernetes -> Run workflow
```

`main` push 자동 배포는 아직 사용하지 않습니다. 실제 클러스터, 도메인, TLS, secret, 백업 정책이 안정화되기 전에는 수동 배포가 안전합니다.

## GitHub production secrets

GitHub repository의 `production` Environment에 아래 값을 등록합니다.

| Secret | 필수 | 설명 |
| --- | --- | --- |
| `KUBE_CONFIG_B64` | O | kubeconfig 파일을 base64로 인코딩한 값 |
| `GHCR_USERNAME` | 선택 | GHCR private image pull secret을 workflow에서 만들 때 사용 |
| `GHCR_TOKEN` | 선택 | GHCR package read 권한이 있는 token |

`KUBE_CONFIG_B64` 생성 예시:

```bash
base64 -w 0 ~/.kube/config
```

PowerShell 예시:

```powershell
[Convert]::ToBase64String([System.IO.File]::ReadAllBytes("$HOME\.kube\config"))
```

## Workflow 입력값

| 입력값 | 기본값 | 설명 |
| --- | --- | --- |
| `image_tag` | commit SHA 앞 12자리 | 배포할 이미지 태그 |
| `image_namespace` | `ghcr.io/som141/docprep-cloud` | 이미지 namespace |
| `domain` | `YOUR_DOMAIN` | 운영 도메인. 실제 배포에서는 반드시 교체 |
| `tls_secret` | `docprep-cloud-tls` | 클러스터에 이미 존재해야 하는 TLS secret |
| `postgres_external_name` | `postgres.example.internal` | PostgreSQL로 연결되는 실제 DNS 이름 |
| `rabbitmq_external_name` | `rabbitmq.example.internal` | RabbitMQ로 연결되는 실제 DNS 이름 |
| `minio_external_name` | `minio.example.internal` | MinIO/S3 API로 연결되는 실제 DNS 이름 |
| `otel_collector_external_name` | `otel-collector.example.internal` | OpenTelemetry Collector로 연결되는 실제 DNS 이름 |
| `apply_mode` | `dry-run` | `dry-run` 또는 `apply` |
| `kube_context` | 빈 값 | kubeconfig 안의 특정 context를 선택할 때 사용 |
| `rollout_timeout` | `180s` | deployment rollout 대기 시간 |
| `configure_ghcr_pull_secret` | `false` | GHCR pull secret을 workflow가 만들고 default service account에 연결할지 여부 |

`domain`, `postgres_external_name`, `rabbitmq_external_name`, `minio_external_name`, `otel_collector_external_name`에 기본 placeholder가 남아 있으면 workflow는 배포를 중단합니다.

## 클러스터 사전 조건

실행 전에 아래 항목이 준비되어야 합니다.

| 항목 | 확인 명령 |
| --- | --- |
| 대상 namespace | `kubectl get namespace docprep-cloud` |
| KEDA CRD | `kubectl get crd scaledobjects.keda.sh` |
| KEDA TriggerAuthentication CRD | `kubectl get crd triggerauthentications.keda.sh` |
| NGINX IngressClass | `kubectl get ingressclass nginx` |
| backend-api secret | `kubectl -n docprep-cloud get secret backend-api-secret` |
| preprocess-worker secret | `kubectl -n docprep-cloud get secret preprocess-worker-secret` |
| TLS secret | `kubectl -n docprep-cloud get secret docprep-cloud-tls` |

`apply_mode=apply`일 때 namespace가 없으면 workflow가 `infra/k8s/namespace.yml`을 먼저 적용합니다.  
하지만 application secret과 TLS secret은 실제 운영 값을 담기 때문에 workflow가 자동 생성하지 않습니다.

## 실행 순서

권장 순서는 아래와 같습니다.

1. `Build GHCR Images` workflow로 이미지 생성
2. `Render Kubernetes Manifests` workflow로 YAML artifact 검토
3. 클러스터에 namespace와 secret 준비
4. DB, RabbitMQ, MinIO, OTel Collector의 실제 DNS 이름을 입력해 `Deploy Kubernetes` workflow를 `apply_mode=dry-run`으로 실행
5. dry-run 성공 후 `apply_mode=apply`로 실행
6. 운영 도메인 접속과 E2E 검증 수행

## GHCR private image 처리

GHCR package가 public이면 별도 image pull secret이 필요하지 않습니다.

GHCR package가 private이면 아래 중 하나를 선택합니다.

1. GHCR package visibility를 public으로 변경합니다.
2. `GHCR_USERNAME`, `GHCR_TOKEN`을 production secret에 등록하고 `configure_ghcr_pull_secret=true`로 실행합니다.

두 번째 방식을 사용하면 workflow가 아래 작업을 수행합니다.

1. `ghcr-pull-secret` docker-registry secret 생성 또는 갱신
2. `default` service account에 `imagePullSecrets` 연결

## 배포 후 확인

workflow가 `apply` 모드로 실행되면 아래 rollout을 기다립니다.

```text
deployment/backend-api
deployment/frontend
deployment/nginx
```

`preprocess-worker`는 KEDA scale-to-zero 구조라 queue가 비어 있으면 replica `0`이 정상입니다.

수동 확인 명령:

```bash
kubectl -n docprep-cloud get pods
kubectl -n docprep-cloud get svc
kubectl -n docprep-cloud get ingress
kubectl -n docprep-cloud get scaledobject
```

브라우저 검증:

1. 운영 도메인 접속
2. Google 로그인
3. 프로젝트 생성
4. 이미지 또는 ZIP 업로드
5. 전처리 Job 생성
6. 처리된 이미지 또는 ZIP 다운로드

## 사용자가 값을 줘야 하는 시점

이번 workflow 작성 단계에서는 실제 값을 줄 필요가 없습니다.

실제 `Deploy Kubernetes` workflow를 실행하기 직전에는 아래 값을 받아야 합니다.

1. 운영 클러스터 kubeconfig 또는 배포 전용 service account kubeconfig
2. 운영 도메인
3. TLS secret 이름과 생성 방식
4. PostgreSQL, RabbitMQ, MinIO, OTel Collector의 Kubernetes 접근 DNS 이름
5. backend-api, preprocess-worker Kubernetes secret의 실제 값
6. GHCR package가 private일 경우 GHCR read token
7. Google OAuth 운영 Client ID/Secret과 redirect URI

## 현재 한계

1. PostgreSQL, RabbitMQ, MinIO는 아직 placeholder service 기준입니다.
2. TLS secret 자동 발급은 포함하지 않습니다.
3. DB migration 전용 Job은 아직 없습니다.
4. Prometheus Operator용 ServiceMonitor는 아직 없습니다.
5. 실제 운영 환경에서는 첫 apply 전에 네트워크, DNS, storage endpoint를 별도로 점검해야 합니다.
