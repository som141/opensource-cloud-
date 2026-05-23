# 시스템 개요

DocPrep Cloud는 대량 스캔 문서 이미지를 OCR 전에 정규화하는 비동기 전처리 플랫폼입니다.  
핵심은 API 서버와 이미지 처리 Worker를 분리해 웹 요청 부하와 CPU 중심 이미지 처리 부하가 서로 영향을 주지 않도록 만드는 것입니다.

## 컴포넌트

| 컴포넌트 | 책임 |
| --- | --- |
| Frontend | 프로젝트, 업로드, Job, 결과 다운로드 화면 제공 |
| NGINX | 프론트 정적 파일과 API/OAuth 경로를 단일 진입점으로 라우팅 |
| backend-api | 인증, 프로젝트, 업로드 세션, 이미지 메타데이터, Job 등록/조회 담당 |
| PostgreSQL | 사용자, 프로젝트, 이미지, Job, JobItem 메타데이터 저장 |
| MinIO/S3 | 원본 이미지와 처리된 이미지 파일 저장 |
| RabbitMQ | 이미지 전처리 작업 메시지 전달 |
| preprocess-worker | RabbitMQ 메시지를 소비하고 OpenCV 문서 전처리 파이프라인 실행 |
| GitHub Actions | 운영 서버에 Docker Compose 기반 production stack 배포 |

## 요청 흐름

```text
사용자 브라우저
  -> NGINX
  -> backend-api
  -> PostgreSQL / MinIO / RabbitMQ
  -> preprocess-worker
  -> MinIO 결과 저장
  -> backend-api internal callback
  -> Frontend 결과 조회/다운로드
```

## 인증 흐름

1. 사용자가 Google 로그인 버튼을 누릅니다.
2. 브라우저가 `/oauth2/authorization/google`로 이동합니다.
3. Spring Security OAuth2가 Google callback을 처리합니다.
4. backend-api가 사용자 정보를 저장하거나 기존 사용자를 찾습니다.
5. Access Token은 프론트에서 API 호출에 사용합니다.
6. Refresh Token은 HttpOnly cookie로 저장합니다.
7. Access Token이 만료되면 `/api/v1/auth/refresh`로 재발급합니다.

## 업로드와 전처리 흐름

1. 프론트가 업로드 세션을 생성합니다.
2. backend-api가 파일별 presigned URL을 발급합니다.
3. 브라우저가 MinIO/S3에 원본 파일을 업로드합니다.
4. 업로드 완료 API가 이미지 메타데이터를 DB에 저장합니다.
5. 사용자가 전처리 Job을 생성합니다.
6. backend-api가 이미지 단위 JobItem을 만들고 RabbitMQ 메시지를 발행합니다.
7. Worker가 원본 파일을 다운로드해 전처리합니다.
8. Worker가 처리된 이미지를 Object Storage에 저장하고 backend-api에 성공/실패를 보고합니다.
9. 사용자는 처리된 이미지 또는 Job 결과 ZIP을 다운로드합니다.

## 전처리 범위

Worker는 단순 resize가 아니라 문서 OCR 품질을 높이기 위한 전처리를 수행합니다.

- Decode
- Color Normalize
- Orientation Normalize
- Deskew
- Crop
- Denoise
- Contrast Normalize
- Binarization
- Morphology Cleanup
- DPI Normalize
- Optional Sharpen

## 운영 관점의 경계

- API 서버는 OpenCV 처리를 수행하지 않습니다.
- Worker는 OAuth 로그인과 화면용 API를 갖지 않습니다.
- Object Storage bucket은 private을 기본값으로 합니다.
- 운영 secret은 GitHub 저장소가 아니라 서버 `.env.prod` 또는 배포 플랫폼 secret store에 둡니다.
- 공개 진입점은 NGINX 하나로 유지합니다.
