# Issue 149. Kubernetes 배포 workflow self-hosted runner 전환

## 목적

GitHub hosted runner는 사설망 Kubernetes API에 접근할 수 없다.

```text
Kubernetes API: 10.0.1.6:6443
```

따라서 Kubernetes cluster 내부에 self-hosted runner를 설치하고, `Deploy Kubernetes` workflow가 해당 runner에서 실행되도록 변경한다.

## 작업 내용

1. `github-actions` namespace에 `docprep-k8s-runner` Deployment를 설치한다.
2. GitHub repository runner 등록 상태를 확인한다.
3. `Deploy Kubernetes` workflow의 `runs-on`을 self-hosted runner 라벨로 변경한다.
4. Kubernetes GitHub Actions 배포 문서를 한글로 갱신한다.

## runner 라벨

```text
self-hosted
Linux
X64
docprep-k8s
k8s
deploy
```

workflow는 아래 라벨을 사용한다.

```yaml
runs-on: [self-hosted, Linux, X64, docprep-k8s]
```

## 완료 조건

- GitHub runner 목록에서 `docprep-k8s` runner가 `online` 상태다.
- `Deploy Kubernetes` workflow가 GitHub hosted runner가 아닌 self-hosted runner에서 실행된다.
- 관련 운영 문서가 갱신된다.

## 운영 주의사항

현재 runner는 registration token 기반으로 등록된다. Pod 재시작 시 token이 만료되어 있으면 재등록에 실패할 수 있다.

장기 운영에서는 Actions Runner Controller 또는 GitHub App/PAT 기반 token refresh 구조로 전환한다.
