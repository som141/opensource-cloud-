# Kubernetes GitHub Actions 배포

이 문서는 GitHub Actions에서 Kubernetes manifest를 렌더링하고 실제 cluster에 배포하는 방법을 정리한다.

## 현재 방식

`Deploy Kubernetes` workflow는 GitHub hosted runner가 아니라 cluster 내부 self-hosted runner에서 실행한다.

```text
runs-on: [self-hosted, Linux, X64, docprep-k8s]
```

이유는 배포 대상 Kubernetes API가 사설망 주소이기 때문이다.

```text
Kubernetes API: 10.0.1.6:6443
```

GitHub hosted runner는 이 주소에 접근할 수 없다. 따라서 `github-actions` namespace에 올라간 `docprep-k8s-runner` Pod가 배포 job을 수행한다.

## 배포 workflow 파일

```text
.github/workflows/deploy-k8s.yml
```

## 실행 방법

수동 실행은 GitHub에서 아래 순서로 실행한다.

```text
Actions -> Deploy Kubernetes -> Run workflow
```

자동 실행은 아래 흐름으로 동작한다.

```text
main push 또는 PR merge
-> Build GHCR Images workflow 성공
-> Deploy Kubernetes workflow 자동 실행
-> self-hosted runner에서 kubectl apply
```

자동 실행 시에는 수동 입력값을 받을 수 없으므로 `production` environment variables를 사용한다.

| Variable | 현재 값 예시 |
| --- | --- |
| `K8S_IMAGE_NAMESPACE` | `ghcr.io/som141/docprep-cloud` |
| `PROD_DOMAIN` | 현재 ngrok 도메인 |
| `K8S_TLS_SECRET` | `docprep-cloud-tls` |
| `POSTGRES_EXTERNAL_NAME` | `postgres` |
| `RABBITMQ_EXTERNAL_NAME` | `rabbitmq` |
| `MINIO_EXTERNAL_NAME` | `minio` |
| `OTEL_COLLECTOR_EXTERNAL_NAME` | `otel-collector` |
| `K8S_APPLY_MODE` | `apply` |
| `K8S_ROLLOUT_TIMEOUT` | `180s` |
| `CONFIGURE_GHCR_PULL_SECRET` | `false` |

수동 배포 입력값 예시는 다음과 같다.

| 입력값 | 값 |
| --- | --- |
| `image_tag` | 배포할 GHCR 이미지 태그 |
| `image_namespace` | `ghcr.io/som141/docprep-cloud` |
| `domain` | 현재 ngrok 도메인 |
| `tls_secret` | `docprep-cloud-tls` |
| `postgres_external_name` | `postgres` |
| `rabbitmq_external_name` | `rabbitmq` |
| `minio_external_name` | `minio` |
| `otel_collector_external_name` | `otel-collector` |
| `apply_mode` | `apply` |
| `kube_context` | 비워둠 |
| `rollout_timeout` | `180s` |
| `configure_ghcr_pull_secret` | GHCR package가 public이면 `false` |

## 필요한 GitHub secret

`production` environment에 아래 secret이 필요하다.

| Secret | 필수 | 설명 |
| --- | --- | --- |
| `KUBE_CONFIG_B64` | O | kubeconfig 파일을 Base64로 인코딩한 값 |
| `GHCR_USERNAME` | 선택 | GHCR private image pull secret 생성 시 사용 |
| `GHCR_TOKEN` | 선택 | GHCR private package read 권한 token |

PowerShell에서 `KUBE_CONFIG_B64`를 만들려면 다음 명령을 사용한다.

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\Downloads\kube (1).conf"))
```

## self-hosted runner 상태 확인

runner는 Kubernetes 안에 설치한다.

```bash
kubectl -n github-actions get pods
kubectl -n github-actions logs deployment/docprep-k8s-runner --tail=100
```

GitHub에서는 아래 위치에서 확인한다.

```text
Repository -> Settings -> Actions -> Runners
```

정상 상태는 다음과 같다.

```text
status: online
labels: self-hosted, Linux, X64, docprep-k8s, k8s, deploy
```

## 배포 전제조건

workflow는 배포 전에 아래 항목을 확인한다.

| 항목 | 확인 명령 |
| --- | --- |
| namespace | `kubectl get namespace docprep-cloud` |
| KEDA ScaledObject CRD | `kubectl get crd scaledobjects.keda.sh` |
| KEDA TriggerAuthentication CRD | `kubectl get crd triggerauthentications.keda.sh` |
| NGINX IngressClass | `kubectl get ingressclass nginx` |
| backend secret | `kubectl -n docprep-cloud get secret backend-api-secret` |
| worker secret | `kubectl -n docprep-cloud get secret preprocess-worker-secret` |
| TLS secret | `kubectl -n docprep-cloud get secret docprep-cloud-tls` |

## 배포 후 확인

배포가 성공하면 workflow가 아래 rollout을 기다린다.

```text
deployment/backend-api
deployment/frontend
deployment/nginx
```

`preprocess-worker`는 KEDA scale-to-zero 구조라서 queue가 비어 있으면 replica가 `0`일 수 있다.

수동 확인 명령은 다음과 같다.

```bash
kubectl -n docprep-cloud get pods
kubectl -n docprep-cloud get svc
kubectl -n docprep-cloud get ingress
kubectl -n docprep-cloud get scaledobject
```

## 자동 CI/CD 흐름

현재 자동 배포는 `workflow_run` trigger를 사용한다.

```text
main merge
-> Build GHCR Images 성공
-> Deploy Kubernetes 자동 실행
-> rollout 확인
```

`Deploy Kubernetes`는 `Build GHCR Images`의 `head_sha` 앞 12자리를 image tag로 사용한다. 따라서 이미지 빌드와 Kubernetes 배포가 같은 commit SHA 기준으로 맞춰진다.

## 현재 제약

현재 self-hosted runner는 registration token 기반 Deployment다. Pod가 재시작된 뒤 registration token이 만료되어 있으면 재등록에 실패할 수 있다.

장기 운영에서는 아래 중 하나로 전환한다.

1. Actions Runner Controller를 설치한다.
2. GitHub App 또는 PAT 기반으로 runner registration token을 Pod 시작 시 자동 발급한다.
3. 마스터 노드 OS에 runner systemd service를 직접 설치한다.
