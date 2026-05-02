# 대규모 문서 이미지 전처리 플랫폼 구현 계획

## 0. 기준 문서

이 계획은 현재 대화에서 정리된 두 문서를 기준으로 작성한다.

- 기능 명세: 대규모 문서 이미지 전처리 비동기 파이프라인 명세
- 디렉터리 명세: 대규모 문서 이미지 전처리 플랫폼 모노레포/멀티앱 구조 설계

현재 저장소에서 확인한 기존 문서는 다음과 같다.

- `README.md`: 저장소 기본 설명
- `.md`: 현재 Git ignore 규칙과 유사한 내용이 들어 있는 루트 문서

## 1. 작업 전 문서 읽기 규칙

모든 구현 작업은 파일을 만들거나 수정하기 전에 관련 Markdown 문서를 먼저 읽고 시작한다.

필수 순서:

1. `README.md`를 읽어 프로젝트의 현재 설명을 확인한다.
2. `docs/implementation-plan.md`를 읽어 전체 순서와 현재 단계의 작업 범위를 확인한다.
3. 루트 `.md`가 존재하면 읽고, 현재 저장소의 예외 규칙 또는 메모가 있는지 확인한다.
4. `docs/conventions/` 문서를 읽어 GitHub workflow, code convention, API response convention을 확인한다.
5. 기능별 상세 문서가 생긴 뒤에는 작업 대상에 맞는 문서를 추가로 읽는다.
6. 문서 내용과 코드가 충돌하면 임의로 고치지 말고, 먼저 문서를 기준으로 변경 방향을 정리한다.

작업 단위별 실행 문서는 `docs/tasks/`에 둔다. 실제 구현은 큰 Phase 단위가 아니라 `docs/tasks/README.md`의 순서에 따라 한 작업 파일씩 읽고 진행한다.

기능별 추가 문서 예시:

- 인증 작업 전: `docs/api/auth-api.md`
- 프로젝트 작업 전: `docs/api/project-api.md`
- 업로드 작업 전: `docs/api/upload-api.md`
- 작업 큐 작업 전: `docs/api/job-api.md`, `docs/worker/retry-policy.md`
- Worker 작업 전: `docs/worker/preprocess-pipeline.md`, `docs/worker/image-test-integration.md`
- DB 작업 전: `docs/database/table-spec.md`, `docs/database/erd.md`
- 운영/인프라 작업 전: `docs/operation/local-run.md`, `docs/architecture/docker-compose-architecture.md`

## 2. 프로젝트 정체성

이 프로젝트는 단순 이미지 리사이징 서비스가 아니다.

최종 목표는 사용자가 대량의 스캔 문서 이미지를 업로드하면 Spring REST API가 작업을 등록하고, RabbitMQ 큐를 통해 Worker가 비동기적으로 OpenCV 기반 문서 이미지 전처리 파이프라인을 수행하는 플랫폼을 만드는 것이다.

Worker는 `som141/image-test` 레포의 문서 이미지 전처리 메커니즘을 기준으로 다음 단계를 지원해야 한다.

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

핵심 아키텍처는 다음과 같다.

1. NGINX가 외부 단일 진입점 역할을 한다.
2. 프론트엔드는 NGINX를 통해 정적 파일로 제공된다.
3. `/api`, `/oauth2`, `/login/oauth2`, SSE 경로는 Spring Boot REST API로 전달된다.
4. Spring Boot API는 인증, 프로젝트, 업로드, 이미지 메타데이터, Job 등록/조회, SSE 진행률만 담당한다.
5. Worker는 RabbitMQ 메시지를 소비하고 전처리만 담당한다.
6. PostgreSQL은 사용자, 프로젝트, 이미지, 작업, 리포트 메타데이터를 저장한다.
7. MinIO/S3는 원본 이미지, 처리 결과, preview, debug artifact, report JSON을 저장한다.
8. Prometheus, Grafana, OpenTelemetry, Jaeger는 관측성을 담당한다.
9. Kubernetes 단계에서는 KEDA가 RabbitMQ queue length 기준으로 Worker를 자동 확장한다.

## 3. 전체 구현 순서

전체 구현은 문서화, 골격 생성, 기능별 API, Worker 통합, 운영 확장의 순서로 진행한다.

### Phase 0. 저장소 기준 정리

목표:

- 기존 문서를 읽고 프로젝트 방향을 고정한다.
- 이후 작업자가 항상 참고할 계획 문서를 만든다.

작업:

1. 루트 문서 확인
2. `docs` 디렉터리 생성
3. 구현 계획 문서 작성
4. 향후 상세 문서 위치 정의

완료 기준:

- `docs/implementation-plan.md`가 존재한다.
- 기능별 작업 순서가 문서에 정리되어 있다.
- 작업 전 Markdown 읽기 규칙이 문서에 포함되어 있다.

### Phase 1. 모노레포 골격 생성

목표:

- API, Worker, Frontend, Infra, Docs, Scripts를 분리한다.
- 구현 전부터 컴포넌트 책임 경계를 고정한다.

작업:

1. `backend-api/` 생성
2. `preprocess-worker/` 생성
3. `frontend/` 생성
4. `infra/` 생성
5. `scripts/` 생성
6. `.github/workflows/` 생성
7. 루트 `README.md`를 프로젝트 목적에 맞게 갱신
8. 루트 `CODEX_DIRECTORY_SPEC.md` 또는 동등한 구조 명세 문서 추가

완료 기준:

- API 서버와 Worker가 같은 Spring Boot 애플리케이션에 섞여 있지 않다.
- `infra`에 Docker Compose, NGINX, RabbitMQ, MinIO, PostgreSQL, monitoring, k8s 하위 경로가 있다.
- `docs`에 architecture, api, database, worker, operation 하위 경로가 있다.

### Phase 2. backend-api Spring 골격 생성

목표:

- Spring 도메인형 구조를 만든다.
- 계층형 최상위 `controller/service/repository/dto` 구조를 만들지 않는다.

작업:

1. `backend-api/build.gradle` 생성
2. `BackendApiApplication.java` 생성
3. `domain/auth` 골격 생성
4. `domain/user` 골격 생성
5. `domain/project` 골격 생성
6. `domain/upload` 골격 생성
7. `domain/image` 골격 생성
8. `domain/job` 골격 생성
9. `domain/preprocess` 골격 생성
10. `domain/benchmark` 골격 생성
11. `domain/notification` 골격 생성
12. `domain/admin` 골격 생성
13. `domain/audit` 골격 생성
14. `infra/oauth` 골격 생성
15. `infra/security` 골격 생성
16. `infra/storage` 포트/어댑터 골격 생성
17. `infra/rabbitmq` 메시지 발행 골격 생성
18. `infra/persistence` 설정 골격 생성
19. `infra/tracing` 골격 생성
20. `infra/metrics` 골격 생성
21. `global/error`, `global/response`, `global/support` 생성

완료 기준:

- 도메인별로 controller, service, entity, repository, dto, exception이 내부에 분리되어 있다.
- API 서버에 OpenCV 전처리 로직이 없다.
- 도메인 서비스가 MinIO SDK 또는 RabbitMQ Template에 직접 강하게 묶이지 않는다.

### Phase 3. preprocess-worker Spring 골격 생성

목표:

- Worker를 API 서버와 별도 Spring Boot 애플리케이션으로 만든다.
- Worker 내부에 `PreprocessStep` 기반 파이프라인 구조를 만든다.

작업:

1. `preprocess-worker/build.gradle` 생성
2. `PreprocessWorkerApplication.java` 생성
3. `domain/workerjob` listener, dto, service 생성
4. `domain/preprocess/pipeline` 생성
5. `domain/preprocess/preset` 생성
6. `domain/preprocess/step` 생성
7. `domain/preprocess/model` 생성
8. `domain/artifact` 생성
9. `domain/report` 생성
10. `domain/benchmark` 생성
11. `infra/rabbitmq` consumer 설정 생성
12. `infra/storage` Object Storage 포트/어댑터 생성
13. `infra/api` Backend internal API client 생성
14. `infra/opencv` OpenCV 로더와 codec adapter 생성
15. `infra/ocr` Tesseract client 골격 생성
16. `infra/tracing`, `infra/metrics` 생성

완료 기준:

- Worker에 OAuth 로그인 로직이 없다.
- Worker가 DB에 직접 접속하지 않는다.
- `DecodeStep`부터 `SharpenStep`까지 단계 클래스가 존재한다.
- `A4_SCAN_300DPI`, `LOW_CONTRAST_SCAN`, `RECEIPT`, `NOISY_SCAN`, `AUTO`를 수용할 구조가 있다.

### Phase 4. 로컬 인프라 골격 생성

목표:

- Docker Compose로 로컬 실행 가능한 최소 구조를 만든다.
- NGINX 단일 진입점 규칙을 파일로 고정한다.

작업:

1. `infra/docker-compose/docker-compose.local.yml` 생성
2. `infra/docker-compose/docker-compose.observability.yml` 생성
3. `infra/docker-compose/.env.example` 생성
4. `infra/nginx/nginx.conf` 생성
5. `infra/nginx/conf.d/app.conf` 생성
6. `infra/nginx/conf.d/api.conf` 생성
7. `infra/nginx/conf.d/sse.conf` 생성
8. `infra/nginx/conf.d/admin.conf` 생성
9. `infra/nginx/snippets/proxy.conf` 생성
10. `infra/nginx/snippets/security-headers.conf` 생성
11. `infra/rabbitmq/definitions.json` 생성
12. `infra/rabbitmq/rabbitmq.conf` 생성
13. `infra/minio/bucket-init.sh` 생성
14. `infra/postgres/init.sql` 생성
15. `infra/monitoring/prometheus/prometheus.yml` 생성
16. Grafana provisioning 및 dashboard placeholder 생성
17. Jaeger, OpenTelemetry Collector placeholder 생성

완료 기준:

- NGINX가 `/`, `/api`, `/oauth2`, `/login/oauth2`, `/api/v1/jobs/*/events`를 분리한다.
- RabbitMQ queue 이름이 문서와 일치한다.
- MinIO bucket 초기화 경로가 있다.
- 관측성 설정을 추가할 위치가 있다.

### Phase 5. Frontend 골격 생성

목표:

- 프론트엔드는 정적 파일로 빌드되어 NGINX에서 제공될 수 있게 만든다.
- Bootstrap, jQuery, AdminLTE 같은 임의 UI 템플릿을 추가하지 않는다.

작업:

1. React + Vite 또는 선택된 프론트 구조 생성
2. `frontend/Dockerfile` 생성
3. `frontend/nginx.conf` 생성
4. `src/app` 생성
5. `src/pages` 생성
6. `src/features` 생성
7. `src/entities` 생성
8. `src/shared` 생성
9. `src/styles` 생성
10. 라우팅 placeholder 생성
11. API client placeholder 생성
12. SSE client placeholder 생성

완료 기준:

- 로그인, 대시보드, 프로젝트, 업로드, 작업 상세, 이미지 상세, 벤치마크, 관리자 페이지 placeholder가 있다.
- 프론트는 API 비즈니스 로직을 직접 수행하지 않는다.
- UI 라이브러리를 임의로 추가하지 않는다.

### Phase 6. backend-api 기능 구현

목표:

- MVP에 필요한 API 기능을 기능 단위로 순차 구현한다.
- 각 기능은 entity, repository, service, controller, DTO, exception, 테스트 순서로 구현한다.

구현 순서:

1. Global error/response
2. User/Auth
3. Project
4. Upload
5. Image
6. Preprocess preset
7. Job
8. SSE event
9. Worker internal API
10. Benchmark
11. Notification
12. Admin
13. Audit

완료 기준:

- MVP API가 `/api/v1` prefix로 정리된다.
- 인증이 필요한 API와 공개 API가 분리된다.
- Worker 내부 API는 외부 공개 API처럼 노출되지 않는다.

### Phase 7. Worker 전처리 구현

목표:

- `image-test` 레포의 전처리 메커니즘을 Worker 파이프라인으로 연결한다.
- 단순 resize 구현으로 끝내지 않는다.

구현 순서:

1. 메시지 consume
2. 처리 시작 보고
3. 원본 이미지 다운로드
4. DecodeStep
5. ColorNormalizeStep
6. OrientationNormalizeStep
7. DeskewStep
8. CropStep
9. DenoiseStep
10. ContrastNormalizeStep
11. BinarizationStep
12. MorphologyCleanupStep
13. DpiNormalizeStep
14. SharpenStep
15. Processed image 저장
16. Preview 저장
17. Report JSON 생성
18. Debug artifact 조건부 저장
19. 처리 성공 보고
20. 실패 분류와 retry/DLQ 연결

완료 기준:

- `PreprocessPipelineRunner`가 프리셋에 따라 단계 목록을 실행한다.
- 각 단계 결과가 `PreprocessContext`에 누적된다.
- 리포트에 skew, crop, denoise, binarization, fallback, timing 정보가 기록된다.

### Phase 8. 관측성 구현

목표:

- API, Queue, Worker, Storage 흐름을 운영자가 확인할 수 있게 만든다.

작업:

1. API HTTP metric 추가
2. Job 생성/상태 metric 추가
3. Worker 처리량 metric 추가
4. Worker 실패율 metric 추가
5. Preset 사용량 metric 추가
6. RabbitMQ queue length dashboard 구성
7. API Overview dashboard 구성
8. Worker Overview dashboard 구성
9. Job Overview dashboard 구성
10. OpenTelemetry trace context를 API publish와 Worker consume에 연결
11. Jaeger에서 create job부터 worker complete까지 trace 확인

완료 기준:

- Prometheus가 API와 Worker metric을 수집한다.
- Grafana에서 API, Worker, Queue, Job dashboard를 확인할 수 있다.
- traceId가 RabbitMQ 메시지에 포함된다.

### Phase 9. Kubernetes/KEDA 확장

목표:

- Docker Compose 기반 MVP를 Kubernetes 배포 구조로 확장한다.
- Worker는 RabbitMQ queue length 기준으로 자동 확장한다.

작업:

1. `infra/k8s/namespace.yml` 생성
2. backend-api deployment/service/configmap/secret example 생성
3. preprocess-worker deployment/service/configmap/secret example 생성
4. preprocess-worker scaledobject 생성
5. frontend deployment/service 생성
6. nginx deployment/service/ingress 생성
7. RabbitMQ 배포 placeholder 생성
8. MinIO 배포 placeholder 생성
9. PostgreSQL 배포 placeholder 생성
10. monitoring 배포 placeholder 생성

완료 기준:

- Worker `ScaledObject`가 `image.preprocess.normal` queue length를 기준으로 동작하도록 정의된다.
- `minReplicaCount: 0`, `maxReplicaCount`를 설정할 수 있다.
- 로컬 Compose와 Kubernetes 설정의 queue 이름이 일치한다.

### Phase 10. 문서, 테스트, 운영 정리

목표:

- 기능 구현 후 사용자가 실행, 검증, 운영할 수 있는 문서를 완성한다.

작업:

1. `docs/architecture/system-overview.md` 작성
2. `docs/architecture/sequence-diagrams.md` 작성
3. API별 문서 작성
4. DB ERD와 table spec 작성
5. Worker pipeline 문서 작성
6. Preset spec 작성
7. Report JSON spec 작성
8. Retry policy 작성
9. Local run 문서 작성
10. Observability 문서 작성
11. Troubleshooting 문서 작성
12. README 실행 가이드 작성
13. CI workflow 정리

완료 기준:

- 신규 작업자가 문서만 읽고 로컬 환경을 띄울 수 있다.
- API와 Worker 책임 경계가 문서에 명확하다.
- MVP 범위와 이후 확장 범위가 구분되어 있다.

## 4. 기능 단위 상세 작업 계획

### 4.1 Global error/response

순서:

1. `BusinessException` 생성
2. `ErrorCode` 생성
3. `ErrorResponse` 생성
4. `GlobalExceptionHandler` 생성
5. `ApiResponse` 생성
6. `PageResponse` 생성
7. `BaseEntity` 생성
8. 공통 응답 포맷 문서화
9. 예외 응답 테스트 작성

주의:

- 도메인별 예외를 `global`에 몰아넣지 않는다.
- `global.util`은 진짜 공통 유틸만 둔다.

### 4.2 Auth/User

순서:

1. `User`, `SocialAccount`, `RefreshToken` entity 생성
2. `UserRole`, `UserStatus` enum 생성
3. `UserRepository`, `SocialAccountRepository`, `RefreshTokenRepository` 생성
4. Google OAuth2 user info adapter 생성
5. Kakao OAuth2 user info adapter 생성
6. OAuth2 provider factory 생성
7. Spring Security config 생성
8. OAuth2 login success handler 생성
9. JWT access token 발급 구현
10. Refresh token 저장 구현
11. `/api/v1/auth/me` 구현
12. `/api/v1/auth/refresh` 구현
13. `/api/v1/auth/logout` 구현
14. 소셜 계정 연결/해제 API 구현
15. 회원 탈퇴 soft delete 구현
16. 인증 API 문서 작성
17. 인증 테스트 작성

주의:

- Access Token은 짧게 유지한다.
- Refresh Token은 HttpOnly Secure Cookie 기반을 우선 고려한다.
- OAuth2 state 검증과 callback URL을 NGINX 라우팅과 맞춘다.

### 4.3 Project

순서:

1. `Project`, `ProjectMember` entity 생성
2. `ProjectRole`, `ProjectStatus` enum 생성
3. repository 생성
4. 프로젝트 생성 request/response DTO 생성
5. 프로젝트 목록 조회 구현
6. 프로젝트 상세 조회 구현
7. 프로젝트 수정 구현
8. 프로젝트 soft delete 구현
9. 프로젝트 멤버 초대 구현
10. 프로젝트 멤버 목록 구현
11. 프로젝트 멤버 제거 구현
12. `ProjectPermissionService` 구현
13. 프로젝트 summary 구현
14. 프로젝트 API 문서 작성
15. 프로젝트 권한 테스트 작성

주의:

- 모든 이미지, 업로드, Job은 프로젝트 권한을 기준으로 접근을 제한한다.
- owner, editor, viewer 권한을 명확히 나눈다.

### 4.4 Upload

순서:

1. `UploadSession`, `UploadSessionFile` entity 생성
2. `UploadSessionStatus` enum 생성
3. upload repository 생성
4. `ObjectStoragePort` 정의
5. `PresignedUrlGenerator` 정의
6. 업로드 세션 생성 API 구현
7. presigned upload URL 발급 API 구현
8. 파일 확장자 검증 구현
9. content type 검증 구현
10. size limit 검증 구현
11. checksum 수집 구조 구현
12. 업로드 완료 API 구현
13. object 존재 여부 검증 구현
14. 이미지 메타데이터 생성 요청 연결
15. 중복 파일 hash 확인 구현
16. 업로드 취소 구현
17. 업로드 API 문서 작성
18. 업로드 테스트 작성

주의:

- 대용량 업로드는 Spring API가 파일 본문을 직접 받지 않는 presigned URL 방식을 기본으로 한다.
- ZIP 업로드는 별도 단계에서 확장한다.

### 4.5 Image

순서:

1. `Image`, `ImageArtifact` entity 생성
2. `ImageStatus`, `ImageFormat` enum 생성
3. image repository 생성
4. 이미지 목록 조회 구현
5. 이미지 상세 조회 구현
6. 원본 다운로드 URL 발급 구현
7. 처리 결과 다운로드 URL 발급 구현
8. preview 다운로드 URL 발급 구현
9. report 조회 구현
10. debug artifact 목록 조회 구현
11. 이미지 삭제 soft delete 구현
12. 이미지 접근 권한 검증 구현
13. 이미지 API 문서 작성
14. 이미지 테스트 작성

주의:

- Object Storage bucket은 private을 전제로 한다.
- 사용자는 presigned download URL을 통해서만 파일에 접근한다.

### 4.6 Preprocess preset

순서:

1. `PreprocessPreset` 모델 생성
2. `PresetType`, `PreprocessPresetName` enum 정리
3. 기본 프리셋 registry 구현
4. `A4_SCAN_300DPI` 정의
5. `LOW_CONTRAST_SCAN` 정의
6. `RECEIPT` 정의
7. `NOISY_SCAN` 정의
8. `AUTO` 정의
9. 프리셋 목록 API 구현
10. 프리셋 상세 API 구현
11. 파라미터 검증 API 구현
12. custom preset entity 생성
13. custom preset 생성 API 구현
14. custom preset 목록 API 구현
15. custom preset 삭제 API 구현
16. preset 문서 작성
17. preset 테스트 작성

주의:

- API 서버는 프리셋과 파라미터 명세를 관리하고, 실제 OpenCV 처리는 Worker가 수행한다.
- `NOISY_SCAN`은 강한 배경 노이즈 문서 분리를 위해 필요하다.

### 4.7 Job

순서:

1. `Job`, `JobItem` entity 생성
2. `JobStatus`, `JobItemStatus`, `JobPriority` enum 생성
3. repository 생성
4. Job 생성 request/response DTO 생성
5. Job 생성 서비스 구현
6. imageIds 권한 및 상태 검증 구현
7. 이미지 단위 JobItem 생성 구현
8. RabbitMQ message DTO 생성
9. `JobMessagePublisher` 포트/어댑터 생성
10. priority별 queue 선택 구현
11. 작업 목록 조회 구현
12. 작업 상세 조회 구현
13. 작업 item 목록 조회 구현
14. 작업 summary 구현
15. 작업 취소 구현
16. 실패 item retry 구현
17. 전체 rerun 구현
18. 결과 artifact 목록 조회 구현
19. ZIP 다운로드 skeleton 구현
20. Job API 문서 작성
21. Job 테스트 작성

주의:

- 메시지는 이미지 한 장당 하나를 발행한다.
- queue는 `image.preprocess.high`, `image.preprocess.normal`, `image.preprocess.retry`, `image.preprocess.dlq`를 기준으로 한다.

### 4.8 SSE 진행률

순서:

1. `JobEventController` 생성
2. `JobEventService` 생성
3. Job progress event DTO 생성
4. Job 상태 변경 이벤트 발행 구조 생성
5. SSE emitter registry 구현
6. 연결 timeout 설정
7. 연결 해제 cleanup 구현
8. heartbeat event 구현
9. `JOB_PROGRESS` event 구현
10. `JOB_COMPLETED` event 구현
11. `JOB_FAILED` event 구현
12. NGINX SSE buffering off 설정 연결
13. SSE API 문서 작성
14. SSE 테스트 작성

주의:

- SSE 경로는 NGINX에서 `proxy_buffering off`가 필요하다.
- 프론트는 polling보다 SSE를 기본으로 사용한다.

### 4.9 Internal Worker API

순서:

1. Worker service token 검증 구조 생성
2. internal API controller 생성
3. item started API 구현
4. item heartbeat API 구현
5. item succeeded API 구현
6. item failed API 구현
7. artifact 등록 API 구현
8. Worker용 preset 조회 API 구현
9. 실패 코드 저장 구현
10. 상태 전이 검증 구현
11. internal API 문서 작성
12. internal API 테스트 작성

주의:

- 이 API는 외부 공개 API가 아니다.
- Worker는 API DB에 직접 접속하지 않고 이 API를 통해 상태를 보고한다.

### 4.10 Worker job consume

순서:

1. RabbitMQ connection 설정
2. preprocess queue listener 생성
3. benchmark queue listener 생성
4. 메시지 JSON 역직렬화 구현
5. traceId 추출 구현
6. message validation 구현
7. 중복 메시지 처리 정책 구현
8. 처리 시작 API 호출
9. WorkerJobService 연결
10. 성공 시 ack 처리
11. retry 가능한 실패 처리
12. retry 불가능한 실패 처리
13. DLQ 이동 정책 연결
14. Worker consume 테스트 작성

주의:

- Worker 프로세스가 죽으면 RabbitMQ ack가 되지 않아 재전달되어야 한다.
- decode 실패 같은 영구 실패는 무한 retry하지 않는다.

### 4.11 Worker preprocess pipeline

순서:

1. `PreprocessStep` interface 정의
2. `PreprocessContext` 정의
3. `PreprocessResult` 정의
4. `PreprocessPipeline` 정의
5. `PreprocessPipelineRunner` 구현
6. `DecodeStep` 구현
7. `ColorNormalizeStep` 구현
8. `OrientationNormalizeStep` 구현
9. `DeskewStep` 구현
10. `CropStep` 구현
11. `DenoiseStep` 구현
12. `ContrastNormalizeStep` 구현
13. `BinarizationStep` 구현
14. `MorphologyCleanupStep` 구현
15. `DpiNormalizeStep` 구현
16. `SharpenStep` 구현
17. step timing 측정 구현
18. fallback note 수집 구현
19. debug artifact hook 구현
20. pipeline 테스트 작성

주의:

- `DpiNormalizeStep`은 OCR 품질 정규화를 위한 단계다.
- 썸네일 resize와 혼동하지 않는다.

### 4.12 Worker preset

순서:

1. `PreprocessPresetName` enum 생성
2. `PreprocessPreset` interface 또는 abstract class 생성
3. `PreprocessPresetRegistry` 구현
4. `A4Scan300DpiPreset` 구현
5. `LowContrastScanPreset` 구현
6. `ReceiptPreset` 구현
7. `NoisyScanPreset` 구현
8. `AutoPresetSelector` 구현
9. 프리셋별 step parameter 연결
10. Worker preset 테스트 작성

주의:

- `AUTO`는 이미지 특징을 보고 프리셋을 선택하는 선택 기능이다.
- API 서버의 preset 명세와 Worker registry 이름이 일치해야 한다.

### 4.13 Artifact/report

순서:

1. artifact type 정의
2. object storage path builder 구현
3. processed image 저장 구현
4. preview image 저장 구현
5. report JSON 저장 구현
6. debug artifact 저장 구현
7. debug flag가 false이면 debug 저장 생략
8. artifact 등록 internal API 호출
9. `ProcessingReport` DTO 구현
10. step report 구현
11. timing report 구현
12. memory usage sampling 구현
13. fallback summary 구현
14. report JSON 문서 작성
15. artifact 테스트 작성

저장 경로 기준:

```text
processed/{projectId}/{jobId}/{itemId}/processed.png
processed/{projectId}/{jobId}/{itemId}/preview.png
processed/{projectId}/{jobId}/{itemId}/processing-report.json
processed/{projectId}/{jobId}/{itemId}/debug/{step}.png
```

### 4.14 OCR benchmark

순서:

1. benchmark entity 생성
2. benchmark item entity 생성
3. benchmark message DTO 생성
4. benchmark queue publisher 구현
5. Worker benchmark listener 구현
6. Raw OCR 실행 구현
7. Preprocessed OCR 실행 구현
8. confidence 계산 구현
9. garbageRatio 계산 구현
10. CER 계산 구현
11. WER 계산 구현
12. wallMillis/cpuMillis/peakMemory 비교 구현
13. 개선/동일/악화 판정 구현
14. JSON summary 생성
15. CSV summary 생성
16. Markdown summary 생성
17. benchmark API 문서 작성
18. benchmark 테스트 작성

주의:

- MVP 필수는 아니지만 프로젝트 차별화 기능이다.
- `image-test` 레포의 OCR benchmark 방향과 맞춘다.

### 4.15 Notification

순서:

1. notification entity 생성
2. notification type/status enum 생성
3. repository 생성
4. 작업 완료 알림 생성
5. 작업 실패 알림 생성
6. 관리자 경고 알림 생성
7. 알림 목록 API 구현
8. 읽음 처리 API 구현
9. 전체 읽음 API 구현
10. 삭제 API 구현
11. notification 문서 작성
12. notification 테스트 작성

주의:

- 이메일 발송은 후순위로 두고, MVP에서는 인앱 알림을 먼저 구현한다.

### 4.16 Admin

순서:

1. admin 권한 검증 구현
2. overview API 구현
3. queue status API 구현
4. worker status API 구현
5. 전체 job 조회 API 구현
6. force retry API 구현
7. force cancel API 구현
8. storage usage API 구현
9. audit log 조회 API 구현
10. admin 문서 작성
11. admin 테스트 작성

주의:

- Grafana와 Jaeger 경로는 운영에서 admin only 접근을 전제로 한다.

### 4.17 Audit

순서:

1. `AuditLog` entity 생성
2. `AuditAction` enum 생성
3. repository 생성
4. audit log service 생성
5. 로그인 기록 연결
6. 다운로드 기록 연결
7. 이미지 삭제 기록 연결
8. 작업 생성 기록 연결
9. 작업 취소 기록 연결
10. 관리자 감사 로그 조회 연결
11. audit 문서 작성
12. audit 테스트 작성

주의:

- 사용자의 중요 행위는 추적 가능해야 한다.
- 개인정보는 필요한 최소한만 저장한다.

### 4.18 Frontend

순서:

1. app provider 구성
2. router 구성
3. API client 구성
4. auth feature 구성
5. Google login button 연결
6. Kakao login button 연결
7. dashboard page 구성
8. project list page 구성
9. project detail page 구성
10. upload page 구성
11. upload progress 표시
12. job create form 구성
13. job detail page 구성
14. SSE progress 연결
15. image detail page 구성
16. 원본/결과 비교 뷰 구성
17. processing report 표시
18. failed item retry 버튼 구성
19. benchmark page 구성
20. admin page 구성
21. notification 표시
22. frontend 문서 작성
23. frontend 테스트 작성

주의:

- 프론트는 화면 표시와 API 호출만 담당한다.
- UI 템플릿을 임의로 추가하지 않는다.

### 4.19 NGINX

순서:

1. `/`를 frontend로 proxy
2. `/assets/*`를 frontend static asset 경로로 proxy
3. `/api/*`를 backend-api로 proxy
4. `/oauth2/*`를 backend-api로 proxy
5. `/login/oauth2/*`를 backend-api로 proxy
6. `/api/v1/jobs/*/events`에 SSE 설정 적용
7. `/grafana/*` admin only proxy placeholder 구성
8. `/jaeger/*` admin only proxy placeholder 구성
9. `client_max_body_size 2g` 설정
10. `proxy_read_timeout` 설정
11. `proxy_buffering off` 설정
12. security headers snippet 적용
13. nginx 문서 작성
14. nginx config 테스트 작성

주의:

- 운영에서는 HTTPS가 전제다.
- OAuth callback URL과 NGINX external host 설정이 맞아야 한다.

### 4.20 Docker Compose

순서:

1. backend-api service 정의
2. preprocess-worker service 정의
3. frontend service 정의
4. nginx service 정의
5. postgres service 정의
6. rabbitmq service 정의
7. minio service 정의
8. prometheus service 정의
9. grafana service 정의
10. otel-collector service 정의
11. jaeger service 정의
12. network 정의
13. volume 정의
14. healthcheck 정의
15. `.env.example` 작성
16. local up/down script 작성
17. local run 문서 작성

주의:

- API와 Worker는 같은 DB를 직접 공유하지 않는다. Worker는 API internal endpoint를 통해 상태를 보고한다.
- local 환경에서도 queue 이름은 운영 설계와 동일하게 둔다.

### 4.21 Kubernetes/KEDA

순서:

1. namespace 작성
2. backend-api deployment 작성
3. backend-api service 작성
4. backend-api configmap 작성
5. backend-api secret example 작성
6. preprocess-worker deployment 작성
7. preprocess-worker service 작성
8. preprocess-worker configmap 작성
9. preprocess-worker secret example 작성
10. preprocess-worker scaledobject 작성
11. frontend deployment 작성
12. frontend service 작성
13. nginx deployment 작성
14. nginx service 작성
15. ingress 작성
16. RabbitMQ manifest placeholder 작성
17. MinIO manifest placeholder 작성
18. PostgreSQL manifest placeholder 작성
19. monitoring manifest placeholder 작성
20. Kubernetes 문서 작성

주의:

- Worker scale trigger는 RabbitMQ queue length다.
- CPU 기반 확장만으로는 대량 배치 처리 특성을 잘 반영하지 못한다.

## 5. MVP 범위

### MVP 1차

포함:

1. Google/Kakao 로그인 골격
2. 사용자/소셜 계정 entity
3. 프로젝트 생성/조회
4. presigned URL 기반 이미지 업로드
5. MinIO 원본 저장
6. Job 생성
7. RabbitMQ 메시지 발행
8. Worker 메시지 consume
9. Worker 전처리 pipeline skeleton
10. processed image 저장 skeleton
11. 작업 상태 조회
12. Docker Compose local skeleton
13. NGINX routing skeleton

제외:

1. OCR benchmark 완성
2. Kubernetes 배포
3. KEDA autoscaling
4. 고급 관리자 대시보드
5. custom preset UI

### MVP 2차

포함:

1. SSE 진행률
2. 실패 항목 재시도
3. 전체 rerun
4. 결과 ZIP 다운로드
5. debug artifact 저장
6. processing-report.json 완성
7. Prometheus/Grafana 기본 대시보드
8. 관리자 overview
9. audit log

### MVP 3차

포함:

1. OCR benchmark
2. custom preset
3. `AUTO` preset selector
4. Kubernetes manifests
5. KEDA ScaledObject
6. OpenTelemetry/Jaeger trace
7. 운영 문서와 troubleshooting

## 6. 생성할 상세 문서 목록

아래 문서는 구현 중 기능별로 작성한다.

```text
docs/
├── architecture/
│   ├── system-overview.md
│   ├── docker-compose-architecture.md
│   ├── kubernetes-architecture.md
│   └── sequence-diagrams.md
├── api/
│   ├── auth-api.md
│   ├── project-api.md
│   ├── upload-api.md
│   ├── image-api.md
│   ├── job-api.md
│   ├── benchmark-api.md
│   └── admin-api.md
├── database/
│   ├── erd.md
│   ├── table-spec.md
│   └── migration-policy.md
├── worker/
│   ├── preprocess-pipeline.md
│   ├── preset-spec.md
│   ├── report-json-spec.md
│   ├── retry-policy.md
│   └── image-test-integration.md
└── operation/
    ├── local-run.md
    ├── observability.md
    ├── queue-operation.md
    ├── storage-operation.md
    └── troubleshooting.md
```

## 7. 구현 중 금지 사항

1. Worker를 단순 이미지 리사이징 서비스로 축소하지 않는다.
2. API 서버에 OpenCV 전처리 로직을 넣지 않는다.
3. Worker에 OAuth 로그인 로직을 넣지 않는다.
4. Worker가 API DB에 직접 접속하지 않는다.
5. Spring 패키지를 최상위 계층형 `controller/service/repository/dto` 구조로 만들지 않는다.
6. `global` 또는 `common`에 도메인 로직을 몰아넣지 않는다.
7. MinIO SDK, RabbitMQ Template 같은 외부 SDK를 도메인 서비스에 직접 강하게 묶지 않는다.
8. Bootstrap, jQuery, AdminLTE 같은 임의 UI 템플릿을 추가하지 않는다.
9. NGINX 경로와 OAuth callback 경로를 따로 놀게 만들지 않는다.
10. queue 이름을 문서와 다르게 임의 변경하지 않는다.

## 8. 작업 체크리스트

초기 골격:

- [ ] `backend-api`와 `preprocess-worker` 분리
- [ ] Spring 도메인형 패키지 생성
- [ ] `global` 최소화
- [ ] `infra`에 외부 시스템 어댑터 분리
- [ ] `frontend` 정적 배포 구조 생성
- [ ] `infra/docker-compose` 생성
- [ ] `infra/nginx` 생성
- [ ] `infra/k8s` 생성
- [ ] `docs` 상세 문서 경로 생성

API:

- [ ] Auth API
- [ ] User API
- [ ] Project API
- [ ] Upload API
- [ ] Image API
- [ ] Preprocess Preset API
- [ ] Job API
- [ ] SSE Event API
- [ ] Benchmark API
- [ ] Notification API
- [ ] Admin API
- [ ] Internal Worker API

Worker:

- [ ] RabbitMQ listener
- [ ] Backend internal API client
- [ ] Object Storage client
- [ ] Preprocess pipeline
- [ ] Preset registry
- [ ] Decode step
- [ ] Color normalize step
- [ ] Orientation normalize step
- [ ] Deskew step
- [ ] Crop step
- [ ] Denoise step
- [ ] Contrast normalize step
- [ ] Binarization step
- [ ] Morphology cleanup step
- [ ] DPI normalize step
- [ ] Sharpen step
- [ ] Artifact save
- [ ] Report JSON
- [ ] Retry/DLQ
- [ ] OCR benchmark

Infra:

- [ ] NGINX reverse proxy
- [ ] PostgreSQL
- [ ] RabbitMQ
- [ ] MinIO
- [ ] Prometheus
- [ ] Grafana
- [ ] OpenTelemetry Collector
- [ ] Jaeger
- [ ] Docker Compose local
- [ ] Kubernetes manifests
- [ ] KEDA ScaledObject

문서:

- [ ] Architecture overview
- [ ] API docs
- [ ] DB table spec
- [ ] Worker pipeline spec
- [ ] Preset spec
- [ ] Report JSON spec
- [ ] Retry policy
- [ ] Local run guide
- [ ] Observability guide
- [ ] Troubleshooting guide

## 9. 다음 작업 권장 순서

바로 다음 구현 작업은 아래 순서로 진행한다.

1. `docs/tasks/README.md`를 읽고 작업 단위 순서를 확인한다.
2. `docs/tasks/00-repository-baseline.md`로 현재 저장소 기준을 확인한다.
3. `docs/tasks/01-monorepo-skeleton.md`에 따라 루트 모노레포 디렉터리를 생성한다.
4. `docs/tasks/02-backend-api-skeleton.md`에 따라 `backend-api` Spring skeleton을 생성한다.
5. `docs/tasks/03-worker-skeleton.md`에 따라 `preprocess-worker` Spring skeleton을 생성한다.
6. `docs/tasks/04-infra-directory-skeleton.md`와 `docs/tasks/19-nginx-docker-compose.md`에 따라 infra skeleton을 생성한다.
7. `docs/tasks/05-frontend-skeleton.md`에 따라 frontend skeleton을 생성한다.
8. `docs/tasks/06-global-error-response.md`부터 기능별 API와 Worker 작업을 순서대로 진행한다.
9. 각 작업 완료 후 해당 작업 문서의 완료 기준을 확인한다.

작업 단위 문서:

```text
docs/tasks/
├── README.md
├── 00-repository-baseline.md
├── 01-monorepo-skeleton.md
├── 02-backend-api-skeleton.md
├── 03-worker-skeleton.md
├── 04-infra-directory-skeleton.md
├── 05-frontend-skeleton.md
├── 06-global-error-response.md
├── 07-auth-user.md
├── 08-project.md
├── 09-upload.md
├── 10-image.md
├── 11-preprocess-preset.md
├── 12-job.md
├── 13-sse-progress.md
├── 14-internal-worker-api.md
├── 15-worker-message-consume.md
├── 16-worker-preprocess-pipeline.md
├── 17-worker-preset.md
├── 18-artifact-report.md
├── 19-nginx-docker-compose.md
├── 20-observability.md
├── 21-ocr-benchmark.md
├── 22-notification.md
├── 23-admin-audit.md
├── 24-kubernetes-keda.md
└── 25-final-docs-tests.md
```
