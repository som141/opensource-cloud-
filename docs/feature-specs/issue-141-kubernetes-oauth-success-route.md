# Issue 141. Kubernetes OAuth 성공 라우팅 수정

## 문제

Kubernetes 배포 환경에서 Google OAuth 로그인 성공 후 `/oauth2/success?login=success`로 이동하면 프론트엔드 SPA가 아니라 `backend-api`로 프록시되어 `common500` 응답이 표시되었다.

## 원인

프론트엔드는 `/oauth2/success` 경로에서 OAuth 성공 후 세션을 확인하도록 구현되어 있다. 하지만 Kubernetes NGINX ConfigMap에는 `/oauth2/` 전체를 `backend-api:8080`으로 보내는 prefix 라우트만 있었고, `/oauth2/success`를 프론트로 보내는 exact 라우트가 없었다.

로컬 NGINX 설정에는 이미 아래 정책이 있었지만 Kubernetes 설정에는 누락되어 있었다.

```text
/oauth2/success -> frontend
/oauth2/*       -> backend-api
```

## 수정 내용

`infra/k8s/nginx/configmap.yml`에 exact match 라우트를 추가했다.

```nginx
location = /oauth2/success {
    proxy_pass http://frontend:80;
}
```

OAuth 시작과 콜백 경로는 기존처럼 백엔드로 유지한다.

```text
/oauth2/authorization/google -> backend-api
/login/oauth2/code/google    -> backend-api
```

## 검증 기준

- `https://{도메인}/oauth2/success?login=success`가 200 HTML을 반환한다.
- `https://{도메인}/oauth2/authorization/google`은 Google OAuth로 302 리다이렉트된다.
- `https://{도메인}/v3/api-docs`는 기존처럼 backend-api에서 200을 반환한다.

