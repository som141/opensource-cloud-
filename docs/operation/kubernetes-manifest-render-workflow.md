# Kubernetes manifest 렌더링 workflow

이 문서는 GHCR 이미지 태그를 Kubernetes manifest에 주입하고, 최종 YAML을 artifact로 남기는 GitHub Actions workflow를 설명한다.

## 목적

실제 클러스터에 배포하기 전에 아래 항목을 확인한다.

1. backend-api, preprocess-worker, frontend 이미지 태그가 올바르게 들어가는지
2. Ingress domain과 TLS secret 이름이 원하는 값으로 들어가는지
3. `kubectl kustomize infra/k8s` 결과가 하나의 배포 YAML로 생성되는지
4. 실제 `kubectl apply` 없이 배포 파일을 리뷰할 수 있는지

## Workflow 파일

```text
.github/workflows/render-k8s-manifests.yml
```

## 실행 조건

현재는 수동 실행만 지원한다.

```text
Actions -> Render Kubernetes Manifests -> Run workflow
```

## 입력값

| 입력값 | 기본값 | 설명 |
| --- | --- | --- |
| `image_tag` | commit SHA 앞 12자리 | 세 서비스에 공통으로 주입할 이미지 태그 |
| `image_namespace` | `ghcr.io/som141/docprep-cloud` | GHCR image namespace |
| `domain` | `YOUR_DOMAIN` | Ingress host와 backend config의 public domain |
| `tls_secret` | `docprep-cloud-tls` | Ingress TLS secret 이름 |

## 이미지 주입 규칙

렌더링 workflow는 아래 placeholder를 실제 이미지로 바꾼다.

| 기존 placeholder | 렌더링 결과 예시 |
| --- | --- |
| `YOUR_REGISTRY/docprep-backend-api:CHANGE_ME` | `ghcr.io/som141/docprep-cloud/backend-api:abc123def456` |
| `YOUR_REGISTRY/docprep-preprocess-worker:CHANGE_ME` | `ghcr.io/som141/docprep-cloud/preprocess-worker:abc123def456` |
| `YOUR_REGISTRY/docprep-frontend:CHANGE_ME` | `ghcr.io/som141/docprep-cloud/frontend:abc123def456` |

## 결과물

Workflow는 아래 artifact를 생성한다.

```text
docprep-cloud-k8s-{image_tag}
└── docprep-cloud-k8s.yml
```

이 파일은 리뷰용 렌더링 결과다. 실제 클러스터 적용은 하지 않는다.

## 실제 배포와의 차이

이 workflow는 다음을 하지 않는다.

1. kubeconfig를 사용하지 않는다.
2. SSH key를 사용하지 않는다.
3. 운영 secret을 만들지 않는다.
4. `kubectl apply`를 실행하지 않는다.
5. GHCR private image pull secret을 만들지 않는다.

위 작업은 실제 Kubernetes CD 단계에서 별도 workflow로 추가한다.

## 검증 기준

1. workflow가 성공한다.
2. artifact에 `docprep-cloud-k8s.yml`이 있다.
3. 렌더링 결과에 `YOUR_REGISTRY`, `CHANGE_ME`가 남아 있지 않다.
4. `YOUR_DOMAIN`은 입력값을 넣은 경우 실제 domain으로 바뀐다.
5. secret 값은 artifact에 포함되지 않는다.

## 다음 단계

1. GHCR 이미지 push workflow와 연결
2. image tag를 자동으로 render workflow에 전달
3. [Kubernetes GitHub Actions 배포](kubernetes-github-actions-deploy.md)로 kubeconfig 기반 `dry-run` 또는 `apply` 수행
4. 배포 후 health check와 smoke test 추가
