# 운영 문서 인덱스

이 디렉터리는 로컬 실행, 운영 배포, 환경변수, 백업, 최종 검증 절차를 다룹니다.  
배포 전에는 아래 순서대로 읽는 것을 기준으로 합니다.

## 권장 읽기 순서

1. [운영 배포 가이드](production-deployment-guide.md)
2. [운영 환경변수와 Secret 주입](production-env.md)
3. [GitHub Actions 배포](github-actions-deployment.md)
4. [GHCR 이미지 빌드/푸시](ghcr-image-workflow.md)
5. [Kubernetes manifest 렌더링](kubernetes-manifest-render-workflow.md)
6. [HTTPS와 도메인 정책](https-domain-policy.md)
7. [관측성 로컬 실행](observability.md)
8. [Kubernetes/KEDA 배포](kubernetes-deployment.md)
9. [배포 체크리스트](deployment-checklist.md)
10. [최종 E2E 검증](final-e2e-verification.md)
11. [백업/복구](backup-restore.md)

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
| [운영 배포 가이드](production-deployment-guide.md) | 서버 준비부터 post-deploy까지 전체 흐름 |
| [운영 환경변수](production-env.md) | `.env.prod` 작성과 secret 관리 |
| [GitHub Actions 배포](github-actions-deployment.md) | `Deploy Production` workflow 사용 |
| [GHCR 이미지 빌드/푸시](ghcr-image-workflow.md) | Kubernetes 배포용 이미지 생성 |
| [Kubernetes manifest 렌더링](kubernetes-manifest-render-workflow.md) | 이미지 태그를 주입한 K8s YAML artifact 생성 |
| [HTTPS/도메인 정책](https-domain-policy.md) | TLS 종료, OAuth redirect, cookie 정책 |
| [관측성 로컬 실행](observability.md) | 배포 전 metric과 trace 검증 |
| [Kubernetes/KEDA 배포](kubernetes-deployment.md) | Kubernetes skeleton 적용과 Worker autoscaling |
| [배포 체크리스트](deployment-checklist.md) | 공개 전 확인 목록 |
| [최종 E2E 검증](final-e2e-verification.md) | 로그인부터 결과 다운로드까지 검증 |
| [백업/복구](backup-restore.md) | PostgreSQL과 Object Storage 백업 |

## 운영 원칙

- 운영 secret은 Git에 커밋하지 않습니다.
- 서버의 실제 값은 `/opt/image-preprocess/shared/.env.prod`에 둡니다.
- GitHub Actions secrets에는 SSH 배포에 필요한 값만 둡니다.
- Google OAuth 운영 redirect URI는 HTTPS 도메인 기준으로 등록합니다.
- Swagger는 MVP 검증에는 유용하지만 공개 서비스 전에는 접근 제한 여부를 결정해야 합니다.
