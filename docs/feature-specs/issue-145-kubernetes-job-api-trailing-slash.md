# Issue 145. Kubernetes Job API trailing slash 301 수정

## 문제

Kubernetes 배포 환경에서 프론트가 전처리 작업을 생성할 때 `POST /api/v1/jobs`를 호출하면 `HTTP 0 non-JSON response` 오류가 표시되었다.

```text
Create preprocessing job failed. API returned non-JSON response with HTTP 0
```

대시보드에서도 일부 Job 관련 API 호출이 같은 방식으로 실패할 수 있다.

## 원인

Kubernetes NGINX ConfigMap에는 SSE와 Job detail 계열 경로를 위해 아래 prefix location이 있었다.

```nginx
location /api/v1/jobs/ {
    proxy_pass http://backend-api:8080;
}
```

NGINX는 slash로 끝나는 proxy prefix location이 있을 때 slash 없는 exact path 요청을 slash path로 301 리다이렉트할 수 있다.

실제 로그는 다음과 같았다.

```text
POST /api/v1/jobs HTTP/1.1 301
```

프론트 API client는 보안상 `redirect: manual`을 사용한다. 이 경우 브라우저 fetch는 수동 리다이렉트 응답을 opaque redirect로 처리할 수 있고, 프론트에서는 status가 `0`인 non-JSON 응답처럼 보인다.

## 수정 내용

`/api/v1/jobs` exact route를 추가해 slash 없는 Job collection API가 리다이렉트 없이 backend-api로 전달되게 했다.

```nginx
location = /api/v1/jobs {
    proxy_pass http://backend-api:8080;
}
```

기존 `/api/v1/jobs/` prefix route는 SSE와 하위 Job 경로를 위해 유지한다.

## 검증 기준

- `POST /api/v1/jobs`가 301을 반환하지 않는다.
- 인증이 없으면 JSON `401`이 반환된다.
- 인증이 있으면 Job 생성 API가 JSON 응답을 반환한다.
- 업로드 화면에서 원본 업로드 완료 후 Job 생성 단계로 넘어간다.

