# 이슈 9. 공통 응답과 예외 처리

## 목적

모든 API가 동일한 성공/실패 응답 구조를 사용하도록 공통 응답 wrapper와 예외 처리 skeleton을 추가합니다.

## 작업 범위

1. `ApiResponse` 추가
2. `BusinessException` 추가
3. `ErrorCode` 추가
4. `GlobalExceptionHandler` 추가
5. validation 오류 응답 형식 정리

## 완료 기준

1. 성공 응답은 `isSuccess=true`와 `result`를 포함합니다.
2. 실패 응답은 error code와 message를 포함합니다.
3. 내부 stack trace는 사용자 응답에 노출하지 않습니다.
