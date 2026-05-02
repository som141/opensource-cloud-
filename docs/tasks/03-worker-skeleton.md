# 03. preprocess-worker Spring 골격

## 목표

RabbitMQ 메시지를 소비하고 OpenCV 기반 문서 이미지 전처리를 수행할 Worker 애플리케이션 골격을 만든다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/02-backend-api-skeleton.md`
4. `docs/worker/preprocess-pipeline.md`
5. `docs/worker/image-test-integration.md`

## 작업 범위

1. Worker Gradle 프로젝트
2. Worker entrypoint
3. 메시지 consume 도메인
4. 전처리 pipeline 도메인
5. artifact/report 도메인
6. storage/api/opencv infra

## 작업 순서

1. `preprocess-worker/build.gradle`을 생성한다.
2. `PreprocessWorkerApplication.java`를 생성한다.
3. `domain/workerjob`를 생성한다.
4. `domain/preprocess/pipeline`을 생성한다.
5. `domain/preprocess/preset`을 생성한다.
6. `domain/preprocess/step`을 생성한다.
7. `domain/preprocess/model`을 생성한다.
8. `domain/artifact`를 생성한다.
9. `domain/report`를 생성한다.
10. `domain/benchmark`를 생성한다.
11. `infra/rabbitmq`를 생성한다.
12. `infra/storage`를 생성한다.
13. `infra/api`를 생성한다.
14. `infra/opencv`를 생성한다.
15. `infra/ocr`를 생성한다.
16. `infra/tracing`, `infra/metrics`를 생성한다.

## 산출물

1. Worker Spring Boot skeleton
2. `PreprocessStep` 기반 패키지 구조
3. Worker Dockerfile placeholder
4. Worker application 설정 파일

## 완료 기준

1. Worker에 OAuth 로그인 로직이 없다.
2. Worker가 API DB에 직접 접속하지 않는다.
3. `DecodeStep`부터 `SharpenStep`까지 들어갈 구조가 있다.
4. `image-test` 메커니즘을 이식하거나 연결할 위치가 명확하다.

## 금지 사항

1. Worker를 단순 resize 작업자로 축소하지 않는다.
2. Worker에 외부 공개 controller를 만들지 않는다.
3. Worker가 사용자 권한 판단을 직접 하지 않는다.
