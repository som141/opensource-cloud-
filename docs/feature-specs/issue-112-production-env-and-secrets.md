# 이슈 112. 운영 환경변수와 Secrets

## 목적

운영 배포에 필요한 환경변수와 secret 주입 방식을 문서화합니다.

## 작업 범위

1. production `.env` 템플릿
2. Google OAuth secret
3. JWT secret
4. DB credential
5. RabbitMQ credential
6. MinIO/S3 credential
7. GitHub Actions secrets

## 완료 기준

1. 실제 secret은 Git에 올라가지 않습니다.
2. 사용자가 어떤 값을 발급받아야 하는지 알 수 있습니다.
3. 로컬 값과 운영 값을 분리합니다.
