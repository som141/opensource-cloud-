# GHCR 이미지 빌드/푸시 workflow

이 문서는 GitHub Actions에서 backend-api, preprocess-worker, frontend 이미지를 GHCR로 빌드하고 푸시하는 방법을 정리한다.

## 목적

Kubernetes 배포에서는 컨테이너 이미지가 먼저 registry에 올라가 있어야 한다. 이 프로젝트는 별도 Docker Hub 가입 없이 GitHub Container Registry를 기본 이미지 저장소로 사용한다.

## Workflow 파일

```text
.github/workflows/build-ghcr-images.yml
```

## 실행 조건

| 조건 | 동작 |
| --- | --- |
| `push` to `main` | 세 서비스 이미지를 commit SHA tag와 `latest`로 푸시 |
| `push` tag `v*.*.*` | 세 서비스 이미지를 Git tag와 `latest`로 푸시 |
| `workflow_dispatch` | 수동 실행. 선택적으로 `image_tag` 입력 가능 |

## 이미지 이름 규칙

기본 이미지 이름은 아래와 같다.

```text
ghcr.io/som141/docprep-cloud/backend-api:<tag>
ghcr.io/som141/docprep-cloud/preprocess-worker:<tag>
ghcr.io/som141/docprep-cloud/frontend:<tag>
```

`tag` 결정 규칙:

1. 수동 실행에서 `image_tag`를 넣으면 그 값을 사용한다.
2. Git tag로 실행되면 Git tag 이름을 사용한다.
3. 그 외에는 commit SHA 앞 12자리를 사용한다.

모든 실행은 같은 이미지에 `latest` tag도 함께 푸시한다.

## 권한 설정

Workflow는 아래 권한을 사용한다.

```yaml
permissions:
  contents: read
  packages: write
```

GitHub repository 설정에서 Actions 권한이 읽기/쓰기 가능해야 한다.

```text
Settings -> Actions -> General -> Workflow permissions -> Read and write permissions
```

별도 가입은 필요 없다. GHCR은 GitHub 계정과 repository에 연결된 Container Registry다.

## Kubernetes manifest에 반영하는 방법

이미지가 푸시된 뒤에는 Kubernetes manifest의 image placeholder를 실제 값으로 교체한다.

| 파일 | 교체 대상 |
| --- | --- |
| `infra/k8s/backend-api/deployment.yml` | `YOUR_REGISTRY/docprep-backend-api:CHANGE_ME` |
| `infra/k8s/preprocess-worker/deployment.yml` | `YOUR_REGISTRY/docprep-preprocess-worker:CHANGE_ME` |
| `infra/k8s/frontend/deployment.yml` | `YOUR_REGISTRY/docprep-frontend:CHANGE_ME` |

예시:

```text
YOUR_REGISTRY/docprep-backend-api:CHANGE_ME
-> ghcr.io/som141/docprep-cloud/backend-api:abc123def456
```

향후 자동 배포 단계에서는 GitHub Actions가 이 값을 `kustomize set image` 또는 manifest patch로 주입하도록 확장한다.

## Private package pull

GHCR package가 private이면 Kubernetes cluster에서 이미지를 pull할 때 `imagePullSecret`이 필요할 수 있다.

그 단계에서 사용자에게 필요한 값:

1. GHCR pull 권한이 있는 GitHub PAT 또는 배포 전용 token
2. registry username
3. cluster namespace

현재 workflow skeleton 작업에는 이 값들이 필요 없다.

## 수동 실행 절차

1. GitHub repository의 `Actions` 탭으로 이동한다.
2. `Build GHCR Images` workflow를 선택한다.
3. `Run workflow`를 누른다.
4. 필요하면 `image_tag`를 입력한다.
5. 실행 완료 후 workflow summary에서 이미지 주소를 확인한다.

## 검증 방법

GitHub Packages 또는 GHCR에서 이미지가 보이는지 확인한다.

```text
https://github.com/som141?tab=packages
```

로컬 Docker가 사용 가능하면 아래처럼 pull 테스트를 할 수 있다.

```bash
docker pull ghcr.io/som141/docprep-cloud/backend-api:<tag>
docker pull ghcr.io/som141/docprep-cloud/preprocess-worker:<tag>
docker pull ghcr.io/som141/docprep-cloud/frontend:<tag>
```

## 다음 단계

1. K8s manifest image placeholder 자동 치환
2. `kubectl apply` 또는 GitOps 기반 배포 workflow
3. GHCR private image pull secret 생성
4. 운영 cluster smoke test 자동화
