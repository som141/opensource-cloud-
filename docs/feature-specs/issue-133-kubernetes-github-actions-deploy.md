# 이슈 133. Kubernetes 수동 배포 workflow

## 목적

Kubernetes manifest 렌더링 단계 다음으로, GitHub Actions에서 실제 클러스터에 `dry-run` 또는 `apply`를 수행할 수 있는 수동 CD workflow를 추가합니다.

## 작업 범위

1. `Deploy Kubernetes` workflow 추가
2. GHCR 이미지 태그와 namespace 주입
3. 운영 도메인과 TLS secret 주입
4. PostgreSQL, RabbitMQ, MinIO, OTel Collector ExternalName 주입
5. `KUBE_CONFIG_B64` 기반 kubeconfig 설정
6. KEDA CRD, IngressClass, namespace, application secret 사전 확인
7. `dry-run`과 실제 `apply` 모드 분리
8. GHCR private image pull secret 선택 생성
9. 운영 문서 갱신

## 사용자에게 값을 요구하는 시점

이번 작업은 workflow와 문서 추가이므로 사용자의 실제 운영 값이 필요하지 않습니다.

실제 GitHub Actions에서 `Deploy Kubernetes`를 실행하기 직전에는 아래 값이 필요합니다.

1. `KUBE_CONFIG_B64`
2. 운영 도메인
3. TLS secret 이름
4. PostgreSQL, RabbitMQ, MinIO, OTel Collector DNS 이름
5. `backend-api-secret`
6. `preprocess-worker-secret`
7. GHCR private image를 사용할 경우 `GHCR_USERNAME`, `GHCR_TOKEN`

## 완료 기준

- `.github/workflows/deploy-k8s.yml`이 추가됩니다.
- `kubectl kustomize infra/k8s`가 통과합니다.
- 렌더링 결과에 `YOUR_REGISTRY`, `CHANGE_ME`, `YOUR_DOMAIN`, `example.internal`이 남지 않는지 로컬 검증합니다.
- 운영 문서에 실행 순서와 필수 준비값이 기록됩니다.
