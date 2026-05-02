# 코드 컨벤션

## 목적

이 문서는 backend-api, preprocess-worker, frontend, infra 작업에서 지켜야 할 구조와 코드 작성 규칙을 정의한다.

## Spring 구조

Spring Boot 애플리케이션은 도메인형 구조를 사용한다.

허용 구조:

```text
src/main/java/com/moonju/preprocess/api/
├── domain/
│   ├── auth/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── repository/
│   │   ├── service/
│   │   └── exception/
│   └── job/
│       ├── controller/
│       ├── dto/
│       ├── entity/
│       ├── repository/
│       ├── service/
│       ├── event/
│       └── exception/
├── infra/
└── global/
```

금지 구조:

```text
src/main/java/...
├── controller/
├── service/
├── repository/
├── dto/
└── entity/
```

## 컴포넌트 책임

backend-api:

1. 인증
2. 프로젝트 관리
3. 업로드 세션과 presigned URL
4. 이미지 메타데이터
5. Job 등록과 상태 조회
6. SSE 진행률
7. Worker internal API

preprocess-worker:

1. RabbitMQ 메시지 consume
2. Object Storage 원본 다운로드
3. OpenCV 문서 이미지 전처리 pipeline 실행
4. 결과 artifact 저장
5. 처리 상태를 API internal endpoint로 보고

frontend:

1. 화면 표시
2. API 호출
3. 업로드 진행률 표시
4. SSE 진행률 표시
5. 원본/결과 비교 UI

infra:

1. NGINX
2. Docker Compose
3. RabbitMQ
4. MinIO/S3
5. PostgreSQL
6. Prometheus/Grafana
7. OpenTelemetry/Jaeger
8. Kubernetes/KEDA

## 이름 규칙

| 대상 | 규칙 |
|---|---|
| 패키지명 | 소문자 |
| 클래스명 | PascalCase |
| 메서드명 | camelCase |
| 변수명 | camelCase |
| 상수 | UPPER_SNAKE_CASE |
| enum 값 | UPPER_SNAKE_CASE |

## Java 포맷팅

1. 들여쓰기는 4칸 공백을 사용한다.
2. 최대 줄 길이는 120자를 기준으로 한다.
3. 파일 끝에는 빈 줄을 둔다.
4. 중괄호는 같은 줄에서 시작한다.
5. 연산자 앞뒤에 공백을 둔다.

## 주석 규칙

1. 코드만 봐도 명확하면 주석을 쓰지 않는다.
2. 복잡한 상태 전이, retry 정책, 보안 의도는 짧게 설명한다.
3. 의미 없는 주석을 추가하지 않는다.

예시:

```java
// Worker 재전달을 위해 처리 완료 전에는 ack하지 않는다.
public void handle(PreprocessJobMessage message) {
    ...
}
```

## Worker 전처리 규칙

Worker는 단순 이미지 리사이징 서비스가 아니다.

필수 pipeline 단계:

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

## 테스트 규칙

1. 새 service 로직이 추가되면 service test를 작성한다.
2. 새 controller API가 추가되면 controller test 또는 Swagger 확인 결과를 남긴다.
3. repository query가 추가되면 repository test를 작성한다.
4. Worker pipeline step이 추가되면 step 단위 테스트를 작성한다.
5. 버그 수정 PR은 재발 방지 테스트를 추가한다.
6. 테스트가 불가능하면 PR 본문에 이유와 대체 검증 방법을 적는다.

## Frontend 규칙

1. Bootstrap, jQuery, AdminLTE를 임의로 추가하지 않는다.
2. 프론트엔드는 화면 표시와 API 호출만 담당한다.
3. Object Storage secret을 프론트에 노출하지 않는다.
4. `/api` 호출은 NGINX reverse proxy를 전제로 한다.
