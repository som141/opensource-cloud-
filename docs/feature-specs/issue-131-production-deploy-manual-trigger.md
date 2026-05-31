# 이슈 131. 운영 배포 workflow 수동 실행 전용 전환

## 목적

`Deploy Production` workflow가 `main` push마다 자동 실행되어 운영 서버 시크릿이 준비되지 않은 상태에서도 실패 체크를 만드는 문제를 막습니다.

## 변경 사항

1. `.github/workflows/deploy-production.yml`에서 `push` 트리거를 제거합니다.
2. 운영 배포는 `workflow_dispatch` 수동 실행으로만 수행합니다.
3. 수동 실행 시 필요한 GitHub Environment secret을 먼저 검사합니다.
4. 누락된 secret이 있으면 SSH 접속 전에 명확한 에러 메시지로 실패합니다.
5. 운영 배포 문서에 수동 실행 정책과 필수 secret 검증 방식을 반영합니다.

## 필수 GitHub production secrets

| Secret | 설명 |
| --- | --- |
| `DEPLOY_HOST` | 운영 서버 IP 또는 host |
| `DEPLOY_USER` | SSH 배포 유저 |
| `DEPLOY_SSH_PRIVATE_KEY` | 배포 유저 private key |
| `DEPLOY_PATH` | 서버 배포 경로 |
| `PROD_BASE_URL` | 배포 후 health/API docs 확인용 base URL |
| `DEPLOY_SSH_PORT` | SSH 포트. 선택값이며 없으면 `22` 사용 |

## 사용자가 값을 제공해야 하는 시점

아직은 코드와 문서 작업만 가능하므로 실제 서버 값이 없어도 됩니다.

운영 배포를 실제로 실행하기 직전에는 아래 값이 필요합니다.

1. 운영 서버 접속 정보: host, SSH port, deploy user
2. 배포용 SSH private key
3. 서버 배포 경로
4. 운영 도메인과 `PROD_BASE_URL`
5. 서버의 `/opt/image-preprocess/shared/.env.prod`
6. Google OAuth 운영 Client ID/Secret과 redirect URI

## 완료 조건

- `Deploy Production`이 `main` push로 자동 실행되지 않습니다.
- 수동 실행 시 누락된 secret을 명확히 알려줍니다.
- 운영 배포 문서가 현재 정책과 일치합니다.
