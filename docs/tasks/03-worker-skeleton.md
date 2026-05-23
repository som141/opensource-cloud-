# 03. Worker 골격

## 목표

`preprocess-worker`를 `backend-api`와 분리된 Spring Boot 애플리케이션으로 구성합니다.
Worker는 RabbitMQ 메시지를 소비하고 문서 이미지 전처리만 담당합니다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/architecture/system-overview.md`
3. `docs/worker/listener-skeleton.md`

## 작업 범위

1. `PreprocessWorkerApplication` 생성
2. Worker 도메인형 패키지 구조 생성
3. RabbitMQ listener 패키지 생성
4. Object Storage port 생성
5. backend internal API client port 생성
6. 전처리 pipeline 패키지 골격 생성
7. 안전한 로컬 기본 설정 추가

## 완료 기준

1. Worker가 API 서버 패키지 안에 들어가지 않습니다.
2. Worker에 OAuth 로그인이나 사용자 화면용 API가 없습니다.
3. RabbitMQ, storage, backend API 접근은 `infra` adapter 또는 port로 분리됩니다.
4. `preprocess-worker` build/test가 통과합니다.
