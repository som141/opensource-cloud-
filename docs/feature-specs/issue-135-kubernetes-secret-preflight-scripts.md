# 이슈 135. Kubernetes secret 준비와 preflight 스크립트

## 목적

Kubernetes 실제 배포 전에 사용자가 직접 준비해야 하는 application secret과 cluster 사전 조건을 명확히 확인할 수 있도록 로컬 스크립트와 문서를 추가합니다.

## 작업 범위

1. 환경변수 기반 Kubernetes Secret manifest 생성 스크립트 추가
2. 생성 결과를 gitignore된 `infra/k8s/**/secret.yml`에 저장
3. 현재 kubectl context의 namespace, secret, KEDA CRD, IngressClass 확인 스크립트 추가
4. 필요한 환경변수 목록과 실행 순서 문서화

## 추가 스크립트

| 스크립트 | 역할 |
| --- | --- |
| `scripts/k8s-generate-secrets.ps1` | 운영 값을 환경변수로 받아 `backend-api-secret`, `preprocess-worker-secret` manifest 생성 |
| `scripts/k8s-preflight.ps1` | 실제 cluster에 배포 전 필수 리소스 존재 여부 확인 |

## 완료 기준

- PowerShell parser 기준 문법 오류가 없습니다.
- dummy 환경변수로 secret manifest 생성이 가능합니다.
- 생성된 secret manifest 경로가 `.gitignore` 대상입니다.
- 실제 secret 값은 저장소에 포함하지 않습니다.
- 운영 문서에 실행 순서가 남아 있습니다.

## 사용자에게 값을 요구하는 시점

이번 작업은 스크립트와 문서 추가라 실제 운영 값이 필요하지 않습니다.

실제 배포 직전에는 아래 값을 받아야 합니다.

1. DB 계정과 비밀번호
2. RabbitMQ 계정, 비밀번호, AMQP URI
3. Google OAuth 운영 Client ID/Secret
4. JWT secret
5. MinIO/S3 access key와 secret key
6. Worker internal token
7. TLS secret 이름과 생성 방식
8. Kubernetes context 또는 kubeconfig
