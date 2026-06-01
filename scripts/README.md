# scripts

이 디렉터리는 로컬 개발, 검증, 운영 준비용 보조 스크립트를 둡니다.

## 현재 스크립트

- `local-e2e-smoke.ps1`: Docker Compose local MVP 흐름을 HTTP API로 검증합니다. 테스트 문서 이미지를 만들고, ZIP 확장 동작을 흉내 낸 뒤, presigned URL 업로드, 전처리 Job 생성, Worker 처리 완료 대기, 처리 결과 다운로드까지 확인합니다.
- `docker-compose-preflight.ps1`: 브라우저 테스트나 E2E 테스트 전에 Docker Compose 설정, 컨테이너 상태, NGINX 라우팅, backend health, Swagger/OpenAPI, MinIO health, RabbitMQ queue topology를 확인합니다.
- `k8s-generate-secrets.ps1`: 환경변수에서 운영 값을 읽어 gitignore된 Kubernetes Secret manifest를 생성합니다.
- `k8s-preflight.ps1`: 실제 Kubernetes 배포 전 현재 `kubectl` context의 namespace, secret, KEDA CRD, IngressClass를 확인합니다.

레포지토리 루트에서 실행합니다.

```powershell
.\scripts\docker-compose-preflight.ps1
```

인증이 필요한 E2E smoke flow는 다음처럼 실행합니다.

```powershell
.\scripts\local-e2e-smoke.ps1 -AccessToken "<access-token>"
```

토큰은 `http://localhost/login`으로 로그인한 뒤 브라우저 DevTools에서 아래 값을 확인합니다.

```javascript
localStorage.getItem('doc-pipeline.access-token')
```

Kubernetes secret 준비는 실제 운영 값을 shell 환경변수에 넣은 뒤 실행합니다.

```powershell
.\scripts\k8s-generate-secrets.ps1 -Force
.\scripts\k8s-preflight.ps1
```

자세한 내용은 [Kubernetes Secret 준비와 Preflight](../docs/operation/kubernetes-secret-preflight.md)를 확인합니다.

## KEDA 배치 비교 실험 스크립트

Kubernetes에서 KEDA 자동 확장 방식과 고정 Worker 방식을 비교하려면 아래 스크립트를 사용합니다.

```powershell
.\scripts\k8s-scale-mode.ps1 -Mode keda-on -KubeConfig "C:\path\to\kube.conf"
.\scripts\k8s-scale-mode.ps1 -Mode keda-off-fixed -FixedReplicas 1 -KubeConfig "C:\path\to\kube.conf"
.\scripts\k8s-batch-benchmark.ps1 -Scenario "keda-on-500" -ApiBaseUrl "https://YOUR_DOMAIN/api" -AccessToken "<ACCESS_TOKEN>" -InputPath "C:\dataset\doc-images-500" -MaxFiles 500 -KubeConfig "C:\path\to\kube.conf"
```

자세한 절차는 [KEDA 500장 배치 비교 실험 가이드](../docs/operation/keda-batch-benchmark.md)를 확인합니다.
