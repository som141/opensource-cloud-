# 운영 문서 인덱스

이 디렉터리는 로컬 실행, 운영 배포, 환경변수, 백업, 최종 검증 절차를 다룹니다.  
배포 전에는 아래 순서대로 읽는 것을 기준으로 합니다.

## 권장 읽기 순서

1. [운영 원칙](operating-principles.md)
2. [Kubernetes/KEDA 배포](kubernetes-deployment.md)
3. [Kubernetes GitHub Actions 배포](kubernetes-github-actions-deploy.md)
4. [GHCR 이미지 빌드/푸시](ghcr-image-workflow.md)
5. [Kubernetes manifest 렌더링](kubernetes-manifest-render-workflow.md)
6. [운영 환경변수와 Secret 주입](production-env.md)
7. [HTTPS와 도메인 정책](https-domain-policy.md)
8. [KEDA 500장 배치 비교 실험](keda-batch-benchmark.md)
9. [관측성 로컬 실행](observability.md)
10. [배포 체크리스트](deployment-checklist.md)
11. [최종 E2E 검증](final-e2e-verification.md)
12. [백업/복구](backup-restore.md)

## 로컬 실행 문서

| 문서 | 설명 |
| --- | --- |
| [로컬 환경 설정](local-env-setup.md) | 로컬 secret과 OAuth 설정 |
| [Docker Compose local](docker-compose-local.md) | 로컬 stack 실행 방법 |
| [Docker Compose preflight](docker-compose-preflight.md) | 컨테이너와 endpoint 점검 |
| [Storage 운영](storage-operation.md) | MinIO bucket과 object 관리 |
| [관측성 로컬 실행](observability.md) | Prometheus, Grafana, Jaeger 로컬 실행과 확인 |

## 운영 배포 문서

| 문서 | 설명 |
| --- | --- |
| [운영 원칙](operating-principles.md) | 배포, Secret, 장애 대응, 관측성, KEDA 운영 기준 |
| [운영 배포 가이드](production-deployment-guide.md) | 서버 준비부터 post-deploy까지 전체 흐름 |
| [운영 환경변수](production-env.md) | `.env.prod` 작성과 secret 관리 |
| [GitHub Actions 배포](github-actions-deployment.md) | `Deploy Production` workflow 사용 |
| [GHCR 이미지 빌드/푸시](ghcr-image-workflow.md) | Kubernetes 배포용 이미지 생성 |
| [Kubernetes manifest 렌더링](kubernetes-manifest-render-workflow.md) | 이미지 태그를 주입한 K8s YAML artifact 생성 |
| [Kubernetes GitHub Actions 배포](kubernetes-github-actions-deploy.md) | kubeconfig 기반 수동 dry-run/apply workflow |
| [HTTPS/도메인 정책](https-domain-policy.md) | TLS 종료, OAuth redirect, cookie 정책 |
| [관측성 로컬 실행](observability.md) | 배포 전 metric과 trace 검증 |
| [Kubernetes/KEDA 배포](kubernetes-deployment.md) | Kubernetes 배포와 Worker autoscaling 확인 |
| [KEDA 500장 배치 비교 실험](keda-batch-benchmark.md) | KEDA, HPA CPU, 고정 Worker 성능 비교 |
| [배포 체크리스트](deployment-checklist.md) | 공개 전 확인 목록 |
| [최종 E2E 검증](final-e2e-verification.md) | 로그인부터 결과 다운로드까지 검증 |
| [백업/복구](backup-restore.md) | PostgreSQL과 Object Storage 백업 |

## 운영 원칙

- 운영 secret은 Git에 커밋하지 않습니다.
- Kubernetes 배포 secret은 GitHub `production` Environment 또는 Kubernetes Secret으로 주입합니다.
- kubeconfig는 `KUBE_CONFIG_B64`로 저장하고 문서에는 실제 값을 기록하지 않습니다.
- Google OAuth 운영 redirect URI는 현재 공개 도메인 기준으로 등록합니다.
- Worker가 `0/0`이어도 queue가 비어 있으면 KEDA scale-to-zero 정상 상태입니다.
- Swagger는 MVP 검증에는 유용하지만 공개 서비스 전에는 접근 제한 여부를 결정해야 합니다.

## KEDA 성능 비교 실험

KEDA 자동 확장과 고정 Worker 방식의 500장 이미지 전처리 성능을 비교하려면 [KEDA 500장 배치 비교 실험 가이드](keda-batch-benchmark.md)를 확인합니다.

이 문서는 Prometheus/Grafana/kube-state-metrics 배포, KEDA ON/OFF 전환, 배치 업로드 벤치마크 실행, 결과 해석 기준을 포함합니다.
