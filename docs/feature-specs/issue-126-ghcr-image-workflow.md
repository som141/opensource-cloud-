# Issue 126. GHCR 이미지 빌드/푸시 workflow skeleton

## 목적

GitHub Actions에서 backend-api, preprocess-worker, frontend 이미지를 GHCR로 빌드하고 푸시하는 workflow skeleton을 추가한다.

## 작업 범위

1. GHCR build/push workflow 추가
2. 세 서비스 이미지 matrix 구성
3. `GITHUB_TOKEN` 기반 GHCR login
4. commit SHA, Git tag, 수동 입력 tag 규칙 정의
5. workflow summary에 이미지 주소 출력
6. 운영 문서와 체크리스트 연결

## 제외 범위

1. Kubernetes cluster apply
2. kubeconfig secret 등록
3. SSH 기반 VM 배포
4. GHCR pull secret 생성
5. image tag 자동 patch 후 배포

## 이미지 이름

```text
ghcr.io/som141/docprep-cloud/backend-api:<tag>
ghcr.io/som141/docprep-cloud/preprocess-worker:<tag>
ghcr.io/som141/docprep-cloud/frontend:<tag>
```

## 사용자에게 나중에 받을 값

이번 작업에는 필요 없지만 실제 Kubernetes 배포 단계에서는 아래 값이 필요하다.

1. kubeconfig 또는 cluster 접근 방식
2. GHCR private image pull token
3. 실제 운영 도메인
4. TLS secret
5. 운영 secret

## 검증 기준

1. workflow YAML 문법이 유효해야 한다.
2. `packages: write` 권한이 있어야 한다.
3. 실제 secret 값이 workflow에 없어야 한다.
4. 문서에 GHCR 권한 설정과 이미지 이름 규칙이 적혀 있어야 한다.
