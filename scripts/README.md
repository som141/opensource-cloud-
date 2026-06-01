# scripts

로컬 개발, Docker Compose 검증, Kubernetes 배포 전 점검, KEDA 배치 성능 실험에 사용하는 보조 스크립트 모음이다.

## 로컬 검증 스크립트

| 스크립트 | 용도 |
| --- | --- |
| `local-e2e-smoke.ps1` | Docker Compose local MVP 흐름을 HTTP API로 검증한다. 테스트 문서 이미지를 만들고 presigned URL 업로드, 전처리 Job 생성, Worker 완료 대기, 처리 결과 다운로드까지 확인한다. |
| `docker-compose-preflight.ps1` | 브라우저 테스트나 E2E 테스트 전에 Docker Compose 설정, 컨테이너 상태, NGINX 라우팅, backend health, Swagger/OpenAPI, MinIO health, RabbitMQ queue topology를 확인한다. |

기본 실행 예시는 다음과 같다.

```powershell
.\scripts\docker-compose-preflight.ps1
```

인증이 필요한 E2E smoke flow는 Access Token을 전달해서 실행한다.

```powershell
.\scripts\local-e2e-smoke.ps1 -AccessToken "<ACCESS_TOKEN>"
```

## Kubernetes 운영 스크립트

| 스크립트 | 용도 |
| --- | --- |
| `k8s-generate-secrets.ps1` | 환경변수에서 운영 값을 읽어 gitignore 대상 Kubernetes Secret manifest를 생성한다. |
| `k8s-preflight.ps1` | 실제 Kubernetes 배포 전 현재 `kubectl` context, namespace, secret, KEDA CRD, IngressClass를 확인한다. |
| `k8s-scale-mode.ps1` | KEDA 자동 확장 모드와 고정 Worker 모드를 전환한다. |

Secret 준비와 배포 전 점검은 다음 순서로 실행한다.

```powershell
.\scripts\k8s-generate-secrets.ps1 -Force
.\scripts\k8s-preflight.ps1
```

자세한 내용은 [Kubernetes Secret 준비와 Preflight](../docs/operation/kubernetes-secret-preflight.md)를 확인한다.

## KEDA 500장 배치 실험

Kubernetes에서 KEDA 자동 확장 방식과 고정 Worker 방식을 비교하려면 아래 스크립트를 사용한다.

```powershell
.\scripts\k8s-scale-mode.ps1 -Mode keda-on -KubeConfig "C:\path\to\kube.conf"

.\scripts\k8s-batch-benchmark.ps1 `
  -Scenario "keda-on-500" `
  -ApiBaseUrl "https://YOUR_DOMAIN/api" `
  -AccessToken "<ACCESS_TOKEN>" `
  -InputPath "C:\dataset\doc-images-500" `
  -MaxFiles 500 `
  -KubeConfig "C:\path\to\kube.conf" `
  -NgrokSkipBrowserWarning
```

고정 Worker 1개 기준 실험은 다음과 같이 전환한 뒤 같은 벤치마크 스크립트를 실행한다.

```powershell
.\scripts\k8s-scale-mode.ps1 -Mode keda-off-fixed -FixedReplicas 1 -KubeConfig "C:\path\to\kube.conf"
```

ngrok 무료 도메인을 API 앞단에 둔 경우에는 `-NgrokSkipBrowserWarning` 옵션을 사용한다. 이 옵션은 API 호출과 presigned 업로드 요청에 `ngrok-skip-browser-warning: true` 헤더를 추가해 HTML 경고 페이지가 JSON 응답을 대체하지 않도록 한다.

자세한 절차는 [KEDA 500장 배치 비교 실험 가이드](../docs/operation/keda-batch-benchmark.md)를 확인한다.
