# Issue 9. Global error and response skeleton

## 이슈

- Issue: `#9`
- Title: `✨ [Feat] backend-api 공통 응답과 예외 처리 skeleton 추가`
- Branch: `feat/som/9`
- Base: `feat/som/6`

## 목표

`backend-api` 전체 controller와 service에서 공통으로 사용할 응답 wrapper, 예외 base, error code, global exception handler를 만든다.

## 작업 범위

1. `BusinessException`
2. `ErrorCode`
3. `ErrorResponse`
4. `GlobalExceptionHandler`
5. `ApiResponse`
6. `PageResponse`
7. `SliceResponse`
8. `BaseEntity`
9. `CurrentUser`
10. 공통 응답/예외 단위 테스트

## 응답 원칙

성공 응답은 `ApiResponse<T>`를 사용한다.

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "요청에 성공했습니다.",
  "result": {}
}
```

실패 응답은 `ErrorResponse`를 사용한다.

```json
{
  "isSuccess": false,
  "code": "common400",
  "message": "잘못된 요청입니다.",
  "result": null,
  "errors": []
}
```

## 예외 처리 원칙

1. `BusinessException`은 내부 `ErrorCode`를 가진다.
2. `ErrorCode`는 HTTP status와 내부 code를 함께 가진다.
3. `GlobalExceptionHandler`는 business, validation, unknown exception을 분리한다.
4. 내부 stack trace는 사용자 응답에 노출하지 않는다.
5. 도메인 전용 예외는 각 도메인 패키지에 둔다.

## 범위 제외

1. 도메인 전용 예외 구현
2. 인증 사용자 resolver 구현
3. JPA entity 구현
4. Swagger 설정
5. 실제 controller API 구현
