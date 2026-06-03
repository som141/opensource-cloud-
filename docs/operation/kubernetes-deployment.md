# Kubernetes/KEDA 배포 가이드

이 문서는 DocPrep Cloud를 Kubernetes에 배포하고 상태를 확인하는 절차를 정리한다. 현재 배포 기준은 `docprep-cloud` namespace에 애플리케이션, 런타임 의존성, 관측성 리소스를 함께 배포하는 MVP 운영 구조다.

## 1. 사전 준비

| 항목 | 설명 |
| --- | --- |
| Kubernetes cluster | 테스트 또는 운영 클러스터 |
| kubeconfig | 배포 대상 cluster 접근 정보 |
| Ingress Controller | `ingressClassName: nginx` 처리 |
| KEDA | `ScaledObject`, `TriggerAuthentication` CRD |
| metrics-server | HPA CPU 비교와 `kubectl top` 확인 |
| GHCR 이미지 | backend-api, preprocess-worker, frontend 이미지 |
| 공개 도메인 | ngrok 도메인 또는 운영 도메인 |
| Google OAuth | 현재 도메인 기준 redirect URI 등록 |
| Kubernetes Secrets | backend-api, preprocess-worker, TLS, 필요 시 GHCR pull secret |

## 2. 배포 방식

권장 배포 방식은 GitHub Actions다.

```text
main merge
  -> Build GHCR Images
  -> Deploy Kubernetes
  -> self-hosted runner
  -> kubectl apply
```

수동으로 manifest만 확인하려면 [Kubernetes manifest 렌더링](kubernetes-manifest-render-workflow.md)을 먼저 실행한다. 실제 배포는 [Kubernetes GitHub Actions 배포](kubernetes-github-actions-deploy.md)를 기준으로 한다.

## 3. 수동 적용 순서

GitHub Actions가 아니라 로컬에서 직접 적용할 때의 기본 순서는 아래와 같다.

```powershell
$KC="$env:USERPROFILE\Downloads\kube (1).conf"
kubectl --kubeconfig $KC apply -f infra/k8s/namespace.yml
kubectl --kubeconfig $KC apply -k infra/k8s
```

실제 secret 값은 Git에 없는 `secret.yml` 또는 GitHub Actions secret에서 주입한다. `secret.example.yml`은 양식 확인용이다.

## 4. 현재 namespace 구조

| Namespace | 역할 |
| --- | --- |
| `docprep-cloud` | 애플리케이션, 런타임 의존성, 관측성 |
| `keda` | KEDA operator |
| `ingress-nginx` | Ingress Controller |
| `github-actions` | self-hosted runner |
| `ngrok` | ngrok tunnel |
| `kubernetes-dashboard` | Dashboard GUI |

상태 확인:

```powershell
kubectl --kubeconfig $KC get ns
kubectl --kubeconfig $KC -n docprep-cloud get pods,deploy,svc,ingress,scaledobject,hpa
```

## 5. 배포된 컴포넌트

| 컴포넌트 | 기대 상태 |
| --- | --- |
| `backend-api` | `2/2 Running` |
| `frontend` | `2/2 Running` |
| `nginx` | `2/2 Running` |
| `postgres` | `1/1 Running` |
| `rabbitmq` | `1/1 Running` |
| `minio` | `1/1 Running` |
| `minio-bucket-init` | `Completed` |
| `prometheus` | `1/1 Running` |
| `grafana` | `1/1 Running` |
| `kube-state-metrics` | `1/1 Running` |
| `otel-collector` | `1/1 Running` |
| `preprocess-worker` | queue 없음: `0/0`, queue 있음: KEDA 확장 |

Worker가 `0/0`이어도 queue가 비어 있으면 정상이다.

## 6. KEDA 확인

```powershell
kubectl --kubeconfig $KC -n docprep-cloud get scaledobject preprocess-worker
kubectl --kubeconfig $KC -n docprep-cloud describe scaledobject preprocess-worker
kubectl --kubeconfig $KC -n docprep-cloud get hpa
```

현재 KEDA 기준:

| 항목 | 값 |
| --- | --- |
| min replica | 0 |
| max replica | 20 |
| normal queue | `image.preprocess.normal`, threshold 25 |
| high queue | `image.preprocess.high`, threshold 10 |
| cooldown | 300초 |

데모 또는 사용자 체감 테스트에서는 `min=1`이 더 적합할 수 있다.

```powershell
.\scripts\k8s-scale-mode.ps1 -Mode keda-on-min1 -KubeConfig $KC
```

실험 후 기본값으로 복구:

```powershell
.\scripts\k8s-scale-mode.ps1 -Mode keda-on -KubeConfig $KC
```

## 7. Grafana 확인

외부 경로:

```text
https://<운영-도메인>/grafana/
```

로컬 port-forward:

```powershell
kubectl --kubeconfig $KC -n docprep-cloud port-forward svc/grafana 3000:3000
```

접속:

```text
http://localhost:3000/grafana/
```

현재 기본 계정은 `admin/admin`이다. 운영 공개 전에는 Secret 기반 비밀번호로 교체한다.

## 8. Dashboard GUI 확인

```powershell
kubectl --kubeconfig $KC -n kubernetes-dashboard port-forward svc/kubernetes-dashboard 8443:443
```

접속:

```text
https://localhost:8443
```

토큰 발급:

```powershell
kubectl --kubeconfig $KC -n kubernetes-dashboard create token docprep-dashboard-viewer
```

## 9. E2E 확인

1. 공개 도메인 접속
2. Google 로그인
3. 프로젝트 생성
4. 이미지 또는 ZIP 업로드
5. 전처리 Job 생성
6. RabbitMQ queue 증가 확인
7. KEDA Worker replica 증가 확인
8. 처리 완료 후 Worker replica 감소 확인
9. 처리된 이미지 또는 Job 결과 ZIP 다운로드

## 10. 운영 전 반드시 바꿀 항목

| 항목 | 현재 | 운영 전 조치 |
| --- | --- | --- |
| PostgreSQL 저장소 | `emptyDir` | PVC 또는 managed DB |
| RabbitMQ 저장소 | `emptyDir` | PVC 또는 managed queue |
| MinIO 저장소 | `emptyDir` | PVC 또는 managed S3 |
| Grafana 계정 | `admin/admin` | Secret 기반 강한 비밀번호 |
| TLS | 수동 secret 또는 ngrok | 운영 도메인과 인증서 자동화 |
| OAuth redirect | 현재 도메인 | 운영 도메인 callback 등록 |
| Alert | dashboard 중심 | Prometheus alert rule 추가 |

## 11. 관련 문서

- [Kubernetes/KEDA 아키텍처](../architecture/kubernetes-architecture.md)
- [런타임 자원 정책](../architecture/runtime-resource-policy.md)
- [운영 원칙](operating-principles.md)
- [Kubernetes GitHub Actions 배포](kubernetes-github-actions-deploy.md)
- [KEDA 500장 배치 비교 실험](keda-batch-benchmark.md)
