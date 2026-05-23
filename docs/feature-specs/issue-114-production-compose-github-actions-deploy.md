# 이슈 114. Production Compose와 GitHub Actions 배포

## 목적

운영 서버에 Docker Compose 기반으로 배포하고 GitHub Actions에서 배포 workflow를 실행할 수 있게 합니다.

## 작업 범위

1. 운영용 Compose override
2. image build/push
3. SSH 배포 workflow
4. 서버 디렉터리 구조
5. `.env.prod` 주입
6. 배포 후 health check

## 완료 기준

1. GitHub Actions에서 운영 배포 절차가 문서화됩니다.
2. 배포 실패 시 rollback 또는 로그 확인 지점이 명확합니다.
3. 운영 secret은 GitHub Environment secrets로 관리합니다.
