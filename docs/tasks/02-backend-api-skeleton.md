# 02. backend-api Spring 골격

## 목표

Spring Boot REST API 서버를 도메인형 구조로 생성한다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/01-monorepo-skeleton.md`
4. `docs/api/auth-api.md`
5. `docs/api/job-api.md`

## 작업 범위

1. Gradle 프로젝트 골격
2. Spring Boot entrypoint
3. 도메인별 패키지
4. infra 패키지
5. global 패키지

## 작업 순서

1. `backend-api/build.gradle`을 생성한다.
2. `BackendApiApplication.java`를 생성한다.
3. `domain/auth`를 생성한다.
4. `domain/user`를 생성한다.
5. `domain/project`를 생성한다.
6. `domain/upload`를 생성한다.
7. `domain/image`를 생성한다.
8. `domain/job`를 생성한다.
9. `domain/preprocess`를 생성한다.
10. `domain/benchmark`를 생성한다.
11. `domain/notification`를 생성한다.
12. `domain/admin`을 생성한다.
13. `domain/audit`를 생성한다.
14. `infra/oauth`, `infra/security`, `infra/storage`, `infra/rabbitmq`, `infra/persistence`, `infra/tracing`, `infra/metrics`를 생성한다.
15. `global/error`, `global/response`, `global/config`, `global/support`, `global/util`을 생성한다.

## 산출물

1. API 서버 Spring Boot skeleton
2. 도메인형 패키지 구조
3. 최소 application 설정 파일
4. API 서버 Dockerfile placeholder

## 완료 기준

1. 최상위 `controller`, `service`, `repository`, `dto` 패키지가 없다.
2. 각 도메인이 자기 내부에 controller, service, entity, repository, dto, exception을 가질 수 있다.
3. API 서버에 OpenCV 전처리 클래스가 없다.

## 금지 사항

1. Worker 코드를 API 서버에 넣지 않는다.
2. 도메인 서비스가 MinIO SDK나 RabbitMQ Template을 직접 의존하지 않는다.
3. 인증 로직을 `global`에 몰아넣지 않는다.
