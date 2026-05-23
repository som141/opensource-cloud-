# 이슈 6. backend-api skeleton

## 목적

Spring Boot REST API 서버의 기본 골격을 도메인형 구조로 구성합니다.

## 작업 범위

1. `BackendApiApplication` 생성
2. `domain`, `infra`, `global` 패키지 생성
3. Gradle 설정과 기본 profile 구성
4. 기본 health endpoint 확인

## 완료 기준

1. 최상위 `controller/service/repository/dto` 계층형 구조를 만들지 않습니다.
2. API 서버에는 OpenCV 전처리 로직이 없습니다.
3. backend-api test/build가 통과합니다.
