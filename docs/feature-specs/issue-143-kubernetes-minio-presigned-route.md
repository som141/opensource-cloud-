# Issue 143. Kubernetes MinIO presigned URL 업로드 405 수정

## 문제

Kubernetes 배포 환경에서 업로드 화면으로 이미지를 선택한 뒤 전처리 작업을 실행하면 원본 업로드 단계에서 `HTTP 405`가 발생했다.

```text
Upload originals to object storage failed. MinIO upload failed ... HTTP 405.
```

## 원인

`backend-api`는 브라우저가 접근 가능한 presigned URL을 만들기 위해 `MINIO_PUBLIC_ENDPOINT`를 사용한다.

운영형 Kubernetes 환경에서는 이 값이 공개 도메인이다.

```text
MINIO_PUBLIC_ENDPOINT=https://{공개 도메인}
```

따라서 업로드 URL은 아래처럼 생성된다.

```text
https://{공개 도메인}/image-preprocess-prod/{objectKey}?X-Amz-...
```

하지만 Kubernetes NGINX ConfigMap에는 `/image-preprocess-prod/` bucket path를 `minio:9000`으로 보내는 라우트가 없었다. 그 결과 `PUT` 요청이 프론트엔드 NGINX로 전달되어 정적 파일 서버가 `405 Method Not Allowed`를 반환했다.

## 수정 내용

Kubernetes NGINX ConfigMap에 bucket path 라우트를 추가했다.

```nginx
location ~ ^/(image-preprocess-local|image-preprocess-prod)/ {
    proxy_set_header Host $host;
    proxy_request_buffering off;
    proxy_buffering off;
    proxy_pass http://minio:9000;
}
```

## 중요한 점

presigned URL은 서명에 요청 host와 path가 포함된다. 그래서 NGINX가 MinIO로 넘길 때 `Host`를 내부 서비스명 `minio:9000`으로 바꾸면 서명이 맞지 않을 수 있다.

이 설정은 공개 도메인의 `Host` 값을 그대로 유지한다.

## 검증 기준

- 외부 URL에서 `PUT /image-preprocess-prod/{objectKey}` 요청이 더 이상 프론트의 405를 반환하지 않는다.
- 서명 없는 테스트 요청은 MinIO의 XML 오류를 반환한다.
- 실제 프론트 업로드는 presigned URL을 사용하므로 원본 업로드 단계가 통과해야 한다.

