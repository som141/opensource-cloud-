# Issue 129. Kubernetes manifest 렌더링 workflow

## 목적

GHCR 이미지 태그를 Kubernetes manifest에 주입하고, `kubectl kustomize` 결과를 artifact로 남기는 GitHub Actions workflow를 추가한다.

## 작업 범위

1. `Render Kubernetes Manifests` workflow 추가
2. `image_tag`, `image_namespace`, `domain`, `tls_secret` 입력값 정의
3. `infra/k8s` manifest를 임시 디렉터리로 복사
4. image/domain/TLS placeholder 치환
5. `kubectl kustomize` 결과 생성
6. 렌더링 결과 artifact 업로드
7. 운영 문서와 체크리스트 연결

## 제외 범위

1. 실제 Kubernetes cluster apply
2. kubeconfig secret
3. SSH key
4. 운영 secret 생성
5. GHCR image pull secret 생성

## 사용자에게 나중에 받을 값

실제 Kubernetes CD 단계로 넘어갈 때 아래 값이 필요하다.

1. kubeconfig 또는 cluster 접근 방식
2. 운영 namespace
3. 실제 domain
4. TLS secret
5. GHCR image pull token 또는 public package 정책
6. 운영 secret 주입 방식

이번 렌더링 workflow에는 위 값이 없어도 된다.

## 완료 기준

1. `kubectl kustomize`를 사용해 최종 YAML을 만든다.
2. 이미지 placeholder가 렌더링 결과에 남지 않는다.
3. workflow artifact로 `docprep-cloud-k8s.yml`을 업로드한다.
4. 실제 `kubectl apply`는 수행하지 않는다.
