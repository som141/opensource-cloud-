# 운영 원칙

이 문서는 DocPrep Cloud를 로컬 MVP에서 Kubernetes 운영 환경으로 가져갈 때 지켜야 할 기준을 정리한다.

## 기본 운영 철학

1. API 서버는 가볍게 유지하고, CPU가 큰 이미지 처리는 Worker로 분리한다.
2. Worker 확장은 CPU가 아니라 RabbitMQ queue backlog를 기준으로 판단한다.
3. secret은 코드와 문서에 기록하지 않고, GitHub Environment Secret 또는 Kubernetes Secret으로 주입한다.
4. 장애 판단은 로그 하나만 보지 않고 API health, queue length, Worker replica, Job 성공률, object storage 상태를 함께 본다.
5. MVP용 `emptyDir` 저장소는 운영 장기 데이터 보존용이 아니다.
6. 배포는 수동 `kubectl apply`보다 GitHub Actions workflow를 우선한다.

## 배포 원칙

| 항목 | 원칙 |
| --- | --- |
| 이미지 | `main` merge 후 GHCR에 commit SHA 태그로 push한다. |
| Kubernetes 배포 | `Deploy Kubernetes` workflow가 같은 SHA 태그를 manifest에 주입한다. |
| Runner | 사설 Kubernetes API 접근 때문에 cluster 내부 self-hosted runner를 사용한다. |
| Rollout 확인 | `backend-api`, `frontend`, `nginx` rollout을 확인한다. |
| Worker 확인 | Worker는 KEDA scale-to-zero 대상이므로 queue가 없으면 `0/0`이 정상이다. |
| 배포 전 검증 | PR 단계에서 build/test와 수동 workflow dispatch를 먼저 확인한다. |

자동 흐름:

```text
PR merge to main
  -> Build GHCR Images
  -> backend-api/preprocess-worker/frontend 이미지 push
  -> Deploy Kubernetes workflow_run
  -> self-hosted runner에서 kubectl apply
  -> rollout 확인
```

## 환경변수와 Secret 원칙

| 값 | 저장 위치 |
| --- | --- |
| Google OAuth Client ID/Secret | GitHub `production` secret 또는 Kubernetes Secret |
| JWT secret | Kubernetes Secret |
| Worker internal token | backend-api와 preprocess-worker Secret에 같은 값으로 주입 |
| DB/RabbitMQ/MinIO 비밀번호 | Kubernetes Secret |
| kubeconfig | GitHub `KUBE_CONFIG_B64` secret |
| ngrok token | ngrok namespace Secret 또는 노드 로컬 설정 |

문서에는 실제 secret 값을 쓰지 않는다. 필요한 값의 이름, 예시 형식, 주입 방법만 기록한다.

## Kubernetes 확인 원칙

현재 클러스터 확인은 사용자가 제공한 kubeconfig를 기준으로 한다.

```powershell
$KC="$env:USERPROFILE\Downloads\kube (1).conf"
kubectl --kubeconfig $KC -n docprep-cloud get pods,deploy,svc,ingress,scaledobject,hpa
```

자주 보는 명령:

```powershell
kubectl --kubeconfig $KC -n docprep-cloud get pods
kubectl --kubeconfig $KC -n docprep-cloud get deploy
kubectl --kubeconfig $KC -n docprep-cloud get scaledobject preprocess-worker
kubectl --kubeconfig $KC -n docprep-cloud get hpa
kubectl --kubeconfig $KC top nodes
kubectl --kubeconfig $KC -n docprep-cloud top pods
```

## Grafana 확인 원칙

Grafana는 운영 상태를 GUI로 보기 위한 기본 도구다.

외부 경로:

```text
https://<운영-도메인>/grafana/
```

로컬 port-forward:

```powershell
$KC="$env:USERPROFILE\Downloads\kube (1).conf"
kubectl --kubeconfig $KC -n docprep-cloud port-forward svc/grafana 3000:3000
```

접속:

```text
http://localhost:3000/grafana/
```

현재 manifest 기본 계정은 `admin/admin`이다. 운영 공개 전에는 Secret 기반 비밀번호로 교체한다.

Grafana에서 우선 확인할 항목:

| 항목 | 봐야 하는 이유 |
| --- | --- |
| Worker desired/ready replica | KEDA가 queue에 반응하는지 확인 |
| RabbitMQ queue length | 작업 적체 여부 확인 |
| Job 성공/실패 추세 | Worker 처리 품질 확인 |
| Pod CPU/Memory | request/limit 조정 근거 확보 |
| API request/error | 사용자 요청 장애 여부 확인 |

## KEDA 운영 원칙

현재 기본값은 비용 절감형 `minReplicaCount=0`이다.

| 상황 | 추천 설정 |
| --- | --- |
| 발표/데모/사용자 체감 속도 중요 | `keda-on-min1` |
| 유휴 시간이 길고 비용 절감 중요 | `keda-on` |
| KEDA 비교 실험 | `keda-off-fixed`, `hpa-cpu`, `keda-on-min1`, `keda-on` 순서로 비교 |
| 장애 분석 | queue length, HPA desired replica, Worker Pending 사유를 함께 확인 |

전환 명령:

```powershell
.\scripts\k8s-scale-mode.ps1 -Mode keda-on -KubeConfig "$env:USERPROFILE\Downloads\kube (1).conf"
.\scripts\k8s-scale-mode.ps1 -Mode keda-on-min1 -KubeConfig "$env:USERPROFILE\Downloads\kube (1).conf"
.\scripts\k8s-scale-mode.ps1 -Mode hpa-cpu -KubeConfig "$env:USERPROFILE\Downloads\kube (1).conf"
.\scripts\k8s-scale-mode.ps1 -Mode keda-off-fixed -FixedReplicas 1 -KubeConfig "$env:USERPROFILE\Downloads\kube (1).conf"
```

실험 후에는 원래 운영값으로 복구한다.

```powershell
.\scripts\k8s-scale-mode.ps1 -Mode keda-on -KubeConfig "$env:USERPROFILE\Downloads\kube (1).conf"
```

## 장애 대응 우선순위

### 1. 사용자가 접속하지 못함

확인 순서:

1. Ingress host와 ngrok/도메인 상태
2. `nginx` Pod와 Service
3. `frontend` Pod
4. 브라우저 Network에서 `/api` 요청 상태

### 2. 로그인 실패

확인 순서:

1. Google OAuth redirect URI가 현재 도메인과 같은지 확인
2. `backend-api-secret`의 Google Client ID/Secret 확인
3. `/login/oauth2/code/google` callback이 backend-api로 라우팅되는지 확인
4. backend-api 로그 확인

### 3. 업로드 실패

확인 순서:

1. presigned URL host가 현재 공개 도메인과 맞는지 확인
2. NGINX object route가 `/image-preprocess-prod/`를 MinIO로 보내는지 확인
3. MinIO Pod와 bucket init Job 상태 확인
4. 브라우저 Network에서 PUT 응답 코드 확인

### 4. Job이 처리되지 않음

확인 순서:

1. RabbitMQ queue에 메시지가 쌓였는지 확인
2. KEDA `ScaledObject` Ready/Active 상태 확인
3. HPA desired replica 확인
4. Worker Pod가 Pending인지 Running인지 확인
5. Worker 로그와 backend-api internal callback 응답 확인

### 5. Worker가 많이 Pending됨

확인 순서:

1. `kubectl describe pod <worker-pod>`에서 `Insufficient cpu/memory` 확인
2. `kubectl top nodes`로 노드 자원 확인
3. Worker request를 줄일지, 노드를 늘릴지 결정
4. 데모 환경이면 `maxReplicaCount`를 낮춰 과도한 Pending을 줄인다.

## 백업 원칙

현재 MVP manifest는 PostgreSQL/MinIO/RabbitMQ를 `emptyDir`로 실행한다. 운영 데이터 보존이 필요하면 반드시 PVC 또는 관리형 서비스로 전환한다.

| 데이터 | 백업 대상 |
| --- | --- |
| PostgreSQL | 사용자, 프로젝트, 이미지, Job, JobItem 메타데이터 |
| MinIO/S3 | 원본 이미지, 처리 결과, ZIP archive |
| RabbitMQ | 장기 보관 대상 아님. queue는 재처리 가능한 일시 상태로 본다. |
| Secret | password manager 또는 GitHub/Kubernetes secret export 정책 |

## 보안 원칙

1. Access Token은 URL에 노출하지 않는다.
2. Refresh Token은 HttpOnly cookie로 유지한다.
3. Object Storage bucket은 private으로 둔다.
4. Worker callback API는 internal token으로 보호한다.
5. Swagger는 운영 공개 전에 접근 제한 여부를 결정한다.
6. Grafana/RabbitMQ/MinIO 관리 콘솔은 공개 인터넷에 직접 열지 않는다.

## 문서 운영 원칙

1. 공개 문서는 `README.md`와 `docs/`에 둔다.
2. AI/Codex 하네스나 개인 설정 문서는 Git 추적 대상에서 제외한다.
3. 실제 secret 값은 문서에 기록하지 않는다.
4. 배포나 실험 결과가 바뀌면 `docs/operation`과 `docs/feature-specs`를 함께 갱신한다.
5. 새 스크립트를 만들면 `scripts/README.md`에도 사용법을 추가한다.

## 관련 문서

- [시스템 개요](../architecture/system-overview.md)
- [Kubernetes/KEDA 아키텍처](../architecture/kubernetes-architecture.md)
- [런타임 자원 정책](../architecture/runtime-resource-policy.md)
- [Kubernetes GitHub Actions 배포](kubernetes-github-actions-deploy.md)
- [KEDA 배치 비교 실험](keda-batch-benchmark.md)
