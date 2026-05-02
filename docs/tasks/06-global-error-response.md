# 06. Global Error/Response

## 목표

API 서버 전체에서 사용할 최소 공통 예외와 응답 구조를 만든다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/02-backend-api-skeleton.md`

## 작업 범위

1. 공통 예외 base
2. 에러 코드
3. 에러 응답
4. 성공 응답 wrapper
5. paging 응답
6. JPA base entity

## 작업 순서

1. `BusinessException`을 만든다.
2. `ErrorCode`를 만든다.
3. `ErrorResponse`를 만든다.
4. `GlobalExceptionHandler`를 만든다.
5. `ApiResponse`를 만든다.
6. `PageResponse`를 만든다.
7. `SliceResponse`를 만든다.
8. `BaseEntity`를 만든다.
9. `CurrentUser` annotation placeholder를 만든다.
10. 예외 응답 테스트를 작성한다.

## 산출물

1. `global/error` 클래스
2. `global/response` 클래스
3. `global/support` 클래스
4. 공통 응답 규칙 문서 초안

## 완료 기준

1. 도메인 전용 예외가 `global`에 섞이지 않는다.
2. controller가 일관된 응답 형식을 반환할 수 있다.
3. validation 예외와 business 예외가 구분된다.

## 금지 사항

1. `global.util`에 도메인 로직을 넣지 않는다.
2. 모든 예외를 하나의 RuntimeException으로 뭉개지 않는다.
3. HTTP status와 내부 error code를 혼동하지 않는다.
