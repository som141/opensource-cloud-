# 시스템 개요

DocPrep Cloud는 대량의 스캔 문서 이미지를 OCR 전에 안정적으로 전처리하기 위한 비동기 이미지 전처리 플랫폼이다. 핵심은 이미지 리사이징 서비스가 아니라, 문서 이미지의 방향, 기울기, 여백, 노이즈, 대비, 이진화 품질을 Worker가 OpenCV 기반 파이프라인으로 보정하는 구조다.

## 목표

| 목표 | 설명 |
| --- | --- |
| 대량 처리 | 여러 이미지 또는 ZIP 파일을 업로드하면 이미지 단위 `JobItem`으로 분리해 큐에 넣는다. |
| 책임 분리 | API 서버는 인증, 업로드, Job 등록/조회만 담당하고, Worker가 실제 전처리를 수행한다. |
| 단일 진입점 | 외부 사용자는 NGINX/Ingress 하나만 바라보고 프론트, API, OAuth, object download 경로를 사용한다. |
| 운영 가능성 | PostgreSQL, RabbitMQ, MinIO/S3, Prometheus, Grafana, KEDA, GitHub Actions 배포 흐름을 포함한다. |
| 보안 기본값 | Google OAuth, HttpOnly refresh token cookie, private object storage, Worker internal token을 사용한다. |

## 전체 구조

```text
User Browser
  |
  v
Ingress / NGINX
  |-- /                         -> frontend
  |-- /api/*                    -> backend-api
  |-- /oauth2/*                 -> backend-api OAuth entry
  |-- /login/oauth2/*           -> backend-api OAuth callback
  |-- /image-preprocess-prod/*  -> MinIO/S3 object route
  |-- /grafana/*                -> Grafana
  |
  v
backend-api
  |-- PostgreSQL                -> users, projects, images, jobs, job_items
  |-- RabbitMQ                  -> image.preprocess.normal/high queue
  |-- MinIO/S3                  -> originals, processed outputs, archives
  |
  v
preprocess-worker
  |-- RabbitMQ consume
  |-- MinIO/S3 download original
  |-- OpenCV document preprocessing pipeline
  |-- MinIO/S3 upload processed image
  |-- backend-api internal callback
```

## 컴포넌트 책임

| 컴포넌트 | 책임 |
| --- | --- |
| Frontend | 로그인 진입, 프로젝트/업로드/Job/이미지 화면, API 호출, 결과 다운로드 링크 표시 |
| NGINX | 정적 프론트와 API/OAuth/Object Storage/Grafana 경로를 단일 진입점으로 라우팅 |
| backend-api | Google OAuth, JWT/refresh token, 프로젝트, 업로드 세션, 이미지 메타데이터, Job 생성/조회, Worker callback |
| PostgreSQL | 사용자, 프로젝트, 업로드 세션, 이미지, Job, JobItem, artifact 메타데이터 저장 |
| RabbitMQ | 이미지 전처리 요청을 queue로 보관하고 Worker에 전달 |
| MinIO/S3 | 원본 이미지, 처리된 이미지, Job 결과 ZIP 저장 |
| preprocess-worker | RabbitMQ 메시지 소비, 문서 이미지 전처리, 처리 결과 저장, 성공/실패 callback |
| Prometheus | API, Worker, RabbitMQ, Kubernetes 상태 metric 수집 |
| Grafana | Worker replica, queue, Job 처리 상태, 리소스 사용량 시각화 |
| KEDA | RabbitMQ queue length 기준으로 Worker replica 자동 조절 |
| GitHub Actions | GHCR 이미지 빌드, Kubernetes manifest 렌더링, self-hosted runner 기반 배포 |

## 사용자 요청 흐름

### 1. 로그인

```text
Browser
  -> /oauth2/authorization/google
  -> Google OAuth
  -> /login/oauth2/code/google
  -> backend-api OAuth success handler
  -> refresh token HttpOnly cookie 저장
  -> frontend /oauth2/success?login=success
  -> frontend가 /api/v1/auth/refresh 또는 /api/v1/auth/me 호출
```

Access Token은 URL에 노출하지 않는 것이 원칙이다. Refresh Token은 `HttpOnly`, `Secure`, `SameSite` 정책이 적용된 cookie로 관리한다.

### 2. 이미지 업로드

```text
Frontend
  -> upload session 생성
  -> 파일별 SHA-256 계산
  -> presigned upload URL 요청
  -> MinIO/S3에 원본 직접 PUT
  -> upload complete 요청
  -> backend-api가 이미지 메타데이터 생성
```

대용량 파일을 API 서버 메모리로 직접 받지 않기 위해 presigned URL 방식을 사용한다. ZIP 업로드는 프론트에서 이미지 파일로 펼친 뒤 각 이미지를 별도 객체로 업로드한다.

### 3. 전처리 Job

```text
Frontend
  -> Job 생성 요청

backend-api
  -> Job 생성
  -> 이미지별 JobItem 생성
  -> RabbitMQ message publish

preprocess-worker
  -> message consume
  -> 원본 download
  -> 전처리 pipeline 실행
  -> processed image upload
  -> backend-api internal callback
```

이미지 한 장이 하나의 `JobItem`이다. 이 구조 때문에 Worker가 여러 개로 늘어나도 같은 Job 안의 이미지를 병렬 처리할 수 있다.

## Worker 전처리 범위

Worker는 단순 resize만 수행하지 않는다. 현재 전처리 단계는 아래 순서가 기준이다.

1. Decode
2. Color Normalize
3. Orientation Normalize
4. Deskew
5. Crop
6. Denoise
7. Contrast Normalize
8. Binarization
9. Morphology Cleanup
10. DPI Normalize
11. Optional Sharpen

결과물은 사용자에게 처리된 이미지와 Job 결과 ZIP 중심으로 제공한다. 디버그 artifact와 리포트는 내부 검증과 개발 용도이며, 운영 UI에서는 필요할 때만 노출한다.

## 운영 경계

| 경계 | 원칙 |
| --- | --- |
| API와 Worker | API는 OpenCV 작업을 직접 하지 않는다. Worker는 사용자 인증과 화면 API를 갖지 않는다. |
| Queue와 DB | RabbitMQ는 처리 요청 전달용이다. 최종 상태와 조회 기준은 PostgreSQL이다. |
| Object Storage | 원본과 결과 파일은 private bucket에 저장하고, 다운로드는 API가 권한을 확인한 뒤 URL을 발급한다. |
| OAuth | Google OAuth만 지원한다. Kakao OAuth는 현재 범위에서 제외한다. |
| OCR | 이 프로젝트는 OCR 엔진 자체가 아니라 OCR 전 이미지 전처리에 집중한다. |
| 관측성 | 운영 판단은 로그만 보지 않고 metric, replica, queue length, Job 성공률을 함께 본다. |

## 현재 배포 기준

현재 Kubernetes 배포 기준은 다음과 같다.

| 항목 | 기준 |
| --- | --- |
| Namespace | `docprep-cloud` |
| 공개 도메인 | 현재 ngrok 도메인 또는 운영 도메인 |
| 이미지 registry | `ghcr.io/som141/docprep-cloud/*` |
| 배포 방식 | `Build GHCR Images` 성공 후 `Deploy Kubernetes` 자동 실행 |
| 배포 runner | Kubernetes 내부 self-hosted runner |
| Worker 확장 | KEDA RabbitMQ queue length 기반 `min=0`, `max=20` |
| 관측성 | Prometheus, Grafana, kube-state-metrics, metrics-server |

## 관련 문서

- [Kubernetes/KEDA 아키텍처](kubernetes-architecture.md)
- [런타임 자원 정책](runtime-resource-policy.md)
- [NGINX 라우팅](nginx-routing.md)
- [Worker 전처리 파이프라인](../worker/preprocess-pipeline.md)
- [운영 원칙](../operation/operating-principles.md)
