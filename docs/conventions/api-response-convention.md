# API 응답 컨벤션

## 목적

backend-api의 모든 REST API 응답 형식을 통일한다.

## 기본 응답 형식

성공 응답:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "요청에 성공했습니다.",
  "result": {
  }
}
```

실패 응답:

```json
{
  "isSuccess": false,
  "code": "TOKEN401",
  "message": "액세스 토큰이 만료되었습니다.",
  "result": null
}
```

## Java 응답 wrapper

```java
public record ApiResponse<T>(
    boolean isSuccess,
    String code,
    String message,
    T result
) {
    public static <T> ApiResponse<T> success(String code, String message, T result) {
        return new ApiResponse<>(true, code, message, result);
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
```

## 코드 규칙

공통 코드:

| 코드 | 의미 |
|---|---|
| `common200` | 요청 성공 |
| `common201` | 생성 성공 |
| `common204` | 삭제 또는 응답 본문 없는 성공 |
| `common400` | 잘못된 요청 |
| `common401` | 인증 실패 |
| `common403` | 권한 없음 |
| `common404` | 리소스 없음 |
| `common409` | 상태 충돌 |
| `common500` | 서버 오류 |

도메인별 코드 prefix:

| Prefix | 도메인 |
|---|---|
| `AUTH` | 인증 |
| `TOKEN` | 토큰 |
| `USER` | 사용자 |
| `PROJECT` | 프로젝트 |
| `UPLOAD` | 업로드 |
| `IMAGE` | 이미지 |
| `JOB` | 작업 |
| `PRESET` | 전처리 프리셋 |
| `WORKER` | Worker internal API |
| `BENCHMARK` | OCR 벤치마크 |
| `ADMIN` | 관리자 |

예시:

| 코드 | 의미 |
|---|---|
| `AUTH401` | OAuth2 로그인 실패 |
| `TOKEN401` | Access Token 만료 |
| `PROJECT403` | 프로젝트 접근 권한 없음 |
| `IMAGE415` | 지원하지 않는 이미지 형식 |
| `JOB409` | 완료된 작업은 취소할 수 없음 |
| `WORKER401` | Worker token 인증 실패 |

## 페이지 응답

목록 조회는 `PageResponse` 또는 `SliceResponse`를 사용한다.

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "요청에 성공했습니다.",
  "result": {
    "content": [],
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
    "hasNext": false
  }
}
```

## 예외 처리 규칙

1. controller에서 try-catch로 반복 처리하지 않는다.
2. 도메인 예외는 `BusinessException` 계열로 변환한다.
3. `GlobalExceptionHandler`에서 공통 실패 응답으로 변환한다.
4. validation 오류는 필드 오류 정보를 포함할 수 있다.
5. 내부 에러 메시지나 stack trace를 사용자 응답에 노출하지 않는다.

## Swagger 문서화 규칙

API를 추가하거나 수정하면 Swagger에서 아래를 확인한다.

1. endpoint가 노출되는지
2. 인증이 필요한 API에 JWT Bearer 설정이 있는지
3. 성공 응답 예시가 있는지
4. 실패 응답 예시가 있는지
5. DTO 필드 설명이 있는지
