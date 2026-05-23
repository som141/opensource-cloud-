# API 문서 인덱스

이 디렉터리는 backend-api가 제공하는 REST API 문서를 도메인별로 정리합니다.  
모든 API 응답은 공통 응답 형식을 따릅니다.

## 공통 응답 형식

성공:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "요청에 성공했습니다.",
  "result": {}
}
```

실패:

```json
{
  "isSuccess": false,
  "code": "TOKEN401",
  "message": "액세스 토큰이 만료되었습니다.",
  "result": null
}
```

## API 문서

| 문서 | 설명 |
| --- | --- |
| [인증 API](auth-api.md) | Google OAuth, 현재 사용자, token refresh |
| [인증 토큰 흐름](auth-token-flow.md) | Access Token과 Refresh Token 처리 흐름 |
| [프로젝트 API](project-api.md) | 프로젝트 생성, 목록, 상세, 수정 |
| [업로드 API](upload-api.md) | 업로드 세션, presigned URL, 업로드 완료 |
| [이미지 API](image-api.md) | 이미지 메타데이터, 처리 결과 다운로드 |
| [Job API](job-api.md) | 전처리 Job 생성, 상태 조회, 결과 ZIP |
| [전처리 프리셋 API](preprocess-preset-api.md) | Worker preset 목록과 파라미터 |
| [Swagger/OpenAPI](swagger-openapi.md) | Swagger UI와 OpenAPI 확인 방법 |

## Swagger 주소

로컬:

```text
http://localhost/swagger-ui/index.html
```

운영:

```text
https://YOUR_DOMAIN/swagger-ui/index.html
```

운영에서 Swagger를 공개할지는 배포 전 보안 정책으로 결정합니다.
