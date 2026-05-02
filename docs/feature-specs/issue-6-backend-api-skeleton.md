# Issue 6. backend-api Spring Boot skeleton

## 이슈

- Issue: `#6`
- Title: `✨ [Feat] backend-api Spring Boot skeleton 추가`
- Branch: `feat/som/6`
- Base: `chore/som/3`

## 목표

`backend-api`를 Spring Boot REST API 애플리케이션으로 확장할 수 있는 최소 skeleton으로 만든다.

## 작업 범위

1. Gradle Spring Boot 프로젝트 설정 추가
2. `BackendApiApplication` entrypoint 추가
3. 최소 `application.yml` 추가
4. 도메인형 package boundary 추가
5. infra/global package boundary 추가
6. Dockerfile placeholder 추가

## 패키지 원칙

backend-api는 도메인형 구조를 사용한다.

```text
com.moonju.preprocess.api
├── domain
├── infra
└── global
```

금지 사항:

1. 최상위 `controller`, `service`, `repository`, `dto` 패키지를 만들지 않는다.
2. API 서버에 OpenCV 전처리 로직을 넣지 않는다.
3. Worker 메시지 consume 로직을 넣지 않는다.
4. 도메인 서비스가 MinIO SDK나 RabbitMQ Template을 직접 의존하지 않도록 한다.

## 범위 제외

1. 공통 응답/예외 클래스 실제 구현
2. 인증/OAuth2 설정 실제 구현
3. JPA entity 구현
4. RabbitMQ publisher 구현
5. Storage adapter 구현
6. Controller API 구현

## 완료 기준

1. Spring Boot entrypoint가 존재한다.
2. Gradle 설정이 존재한다.
3. 도메인형 package boundary가 존재한다.
4. API 서버와 Worker 책임이 섞이지 않았다.
5. 후속 PR에서 공통 응답/예외 skeleton을 추가할 수 있다.
