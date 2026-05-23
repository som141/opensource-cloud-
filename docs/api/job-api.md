# Job API

## 목적

Job API는 대량 문서 이미지 전처리 작업을 등록합니다.
프로젝트 접근 권한, 선택된 이미지, 전처리 프리셋 파라미터를 검증한 뒤 이미지 단위 RabbitMQ 메시지를 발행해 Worker가 처리하게 합니다.

API 서버는 OpenCV 전처리를 직접 실행하지 않습니다. 실제 이미지 처리와 artifact 저장은 preprocess-worker가 담당합니다.

## 상태 모델

### Job 상태

| 상태 | 의미 |
| --- | --- |
| `CREATED` | Job entity가 생성됨 |
| `QUEUED` | JobItem이 queue에 등록되고 메시지가 발행됨 |
| `RUNNING` | 하나 이상의 Worker가 item을 처리 중 |
| `PARTIAL_SUCCEEDED` | 일부 item 성공, 일부 item 실패 |
| `SUCCEEDED` | 모든 item 성공 |
| `FAILED` | 성공 없이 Job 실패 |
| `CANCEL_REQUESTED` | 사용자가 취소를 요청함 |
| `CANCELLED` | Job이 취소됨 |
| `RETRYING` | 실패 또는 선택된 item이 재등록됨 |

### JobItem 상태

| 상태 | 의미 |
| --- | --- |
| `PENDING` | item은 있지만 아직 queue에 등록되지 않음 |
| `QUEUED` | Worker가 소비할 수 있는 상태 |
| `PROCESSING` | Worker가 이미지 처리 중 |
| `SUCCEEDED` | Worker 처리 성공 |
| `FAILED` | Worker가 실패 보고 |
| `SKIPPED` | 의도적으로 건너뜀 |
| `CANCELLED` | 처리 전 취소됨 |
| `RETRYING` | 재등록됨 |
| `DEAD_LETTERED` | 재시도 한도를 넘거나 DLQ로 이동 |

## Endpoint

모든 사용자용 endpoint는 `Authorization: Bearer <access-token>`이 필요합니다.

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/api/v1/jobs` | 전처리 Job 생성 |
| `GET` | `/api/v1/jobs` | 내 Job 목록 |
| `GET` | `/api/v1/jobs/{jobId}` | Job 상세 |
| `GET` | `/api/v1/jobs/{jobId}/items` | 이미지 단위 JobItem 목록 |
| `GET` | `/api/v1/jobs/{jobId}/items/{itemId}/download?type=processed` | 처리 결과 이미지 다운로드 URL |
| `GET` | `/api/v1/jobs/{jobId}/items/{itemId}/download?type=preview` | preview 다운로드 URL |
| `GET` | `/api/v1/jobs/{jobId}/items/{itemId}/download?type=report` | 처리 리포트 다운로드 URL |
| `GET` | `/api/v1/jobs/{jobId}/summary` | 진행률 counter 조회 |
| `GET` | `/api/v1/jobs/{jobId}/events` | 진행률 SSE stream |
| `POST` | `/api/v1/jobs/{jobId}/cancel` | 취소 요청 |
| `POST` | `/api/v1/jobs/{jobId}/retry` | 실패 item 재시도 |
| `POST` | `/api/v1/jobs/{jobId}/rerun` | 처리 중이 아닌 item 전체 재등록 |
| `GET` | `/api/v1/jobs/{jobId}/artifacts` | 처리 결과 artifact 목록 |
| `GET` | `/api/v1/jobs/{jobId}/download.zip` | 처리 결과 ZIP 다운로드 URL |

Internal Worker API는 사용자용 API가 아닙니다. `/internal/v1/**` 하위에 있고 `X-Worker-Token`이 필요합니다.

## Job 생성

요청:

```json
{
  "projectId": 1,
  "imageIds": [100, 101, 102],
  "preset": "LOW_CONTRAST_SCAN",
  "presetParameters": {
    "targetDpi": "300",
    "binarizationMode": "adaptive"
  },
  "debug": false,
  "priority": "NORMAL",
  "outputOptions": {
    "saveProcessedImage": true,
    "savePreview": true,
    "saveReportJson": true,
    "saveDebugArtifacts": false
  }
}
```

응답:

```json
{
  "isSuccess": true,
  "code": "common201",
  "message": "생성되었습니다.",
  "result": {
    "jobId": 1,
    "status": "QUEUED",
    "totalImages": 3,
    "queuedImages": 3,
    "createdAt": "2026-05-10T12:00:00"
  }
}
```

규칙:

- `imageIds`는 비어 있을 수 없고 중복될 수 없습니다.
- 모든 이미지는 요청한 프로젝트에 속해야 합니다.
- 삭제된 이미지는 거부합니다.
- preset 파라미터는 메시지 발행 전에 preset validation 로직으로 검증합니다.
- API는 이미지마다 RabbitMQ 메시지 하나를 발행합니다.

## RabbitMQ 라우팅

| 우선순위 | Queue |
| --- | --- |
| `HIGH` | `image.preprocess.high` |
| `LOW` | `image.preprocess.normal` |
| `NORMAL` | `image.preprocess.normal` |

재시도 요청은 `image.preprocess.retry`로 발행합니다.

## 메시지 계약

```json
{
  "messageId": "msg-uuid",
  "jobId": 1,
  "itemId": 10,
  "projectId": 1,
  "imageId": 100,
  "userId": 20,
  "originalObjectKey": "originals/1/1/100/scan.png",
  "preset": "LOW_CONTRAST_SCAN",
  "presetParameters": {
    "targetDpi": "300"
  },
  "debug": false,
  "priority": "NORMAL",
  "attempt": 1,
  "traceId": "trace-uuid",
  "createdAt": "2026-05-10T03:00:00Z"
}
```

## 목록과 상세

```text
GET /api/v1/jobs?page=0&size=20
GET /api/v1/jobs/{jobId}
GET /api/v1/jobs/{jobId}/items?page=0&size=50
```

`GET /api/v1/jobs`는 현재 사용자가 생성한 Job을 조회합니다. 상세와 item API는 프로젝트 조회 권한을 검증합니다.

## JobItem artifact 다운로드

```text
GET /api/v1/jobs/{jobId}/items/{itemId}/download?type=processed
GET /api/v1/jobs/{jobId}/items/{itemId}/download?type=preview
GET /api/v1/jobs/{jobId}/items/{itemId}/download?type=report
```

API는 현재 사용자가 Job의 프로젝트를 조회할 수 있는지 확인하고, 요청한 JobItem에서 artifact type에 맞는 object key를 찾아 임시 다운로드 URL을 반환합니다.

응답:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "요청에 성공했습니다.",
  "result": {
    "jobId": 1,
    "itemId": 10,
    "type": "PROCESSED",
    "objectKey": "processed/1/1/10/processed.png",
    "downloadUrl": "http://localhost:9000/image-preprocess-local/processed/1/1/10/processed.png?...",
    "expiresAt": "2026-05-15T09:00:00Z"
  }
}
```

규칙:

- `type`은 `processed`, `preview`, `report` 중 하나입니다.
- Worker가 아직 object key를 등록하지 않았다면 `common404`를 반환합니다.
- debug artifact 확장은 이 item-level 다운로드 흐름과 분리합니다.

## 처리 결과 artifact 목록

```text
GET /api/v1/jobs/{jobId}/artifacts
```

API는 프로젝트 조회 권한을 확인하고, 성공한 JobItem 중 `processedObjectKey`가 있는 항목만 반환합니다. MVP 사용자 화면에서는 preview, report, debug artifact URL을 숨깁니다.

응답:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "요청에 성공했습니다.",
  "result": {
    "jobId": 1,
    "totalItems": 3,
    "processedReadyCount": 2,
    "processedArtifacts": [
      {
        "itemId": 10,
        "imageId": 100,
        "status": "SUCCEEDED",
        "objectKey": "processed/1/1/10/processed.png",
        "downloadUrl": "http://localhost:9000/image-preprocess-local/processed/1/1/10/processed.png?...",
        "expiresAt": "2026-05-15T09:00:00Z"
      }
    ]
  }
}
```

## 처리 결과 ZIP 다운로드

```text
GET /api/v1/jobs/{jobId}/download.zip
```

API는 `SUCCEEDED` 상태이고 `processedObjectKey`가 있는 JobItem의 처리 결과 이미지만 ZIP으로 묶습니다. preview, report, debug artifact는 MVP ZIP 다운로드에 포함하지 않습니다.

Archive object key:

```text
archives/{projectId}/{jobId}/processed-results.zip
```

응답:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "요청에 성공했습니다.",
  "result": {
    "jobId": 1,
    "fileCount": 3,
    "objectKey": "archives/10/1/processed-results.zip",
    "downloadUrl": "http://localhost/image-preprocess-local/archives/10/1/processed-results.zip?...",
    "expiresAt": "2026-05-16T00:00:00Z"
  }
}
```

규칙:

- 현재 사용자가 Job의 프로젝트를 조회할 수 있어야 합니다.
- 성공한 item 중 처리 결과 object key가 있는 항목만 포함합니다.
- 처리된 이미지가 없으면 `common404`를 반환합니다.
- ZIP은 요청 시점마다 다시 생성해 최신 결과를 반영합니다.

## 요약

응답:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "요청에 성공했습니다.",
  "result": {
    "jobId": 1,
    "total": 1000,
    "queued": 120,
    "processing": 20,
    "succeeded": 850,
    "failed": 10,
    "progressPercent": 86.0
  }
}
```

`progressPercent`는 `(succeeded + failed) / total * 100`으로 계산합니다.

## SSE 진행률 stream

```text
GET /api/v1/jobs/{jobId}/events
Accept: text/event-stream
```

연결 전에 일반 Job 조회 권한을 검증합니다. 구독이 시작되면 heartbeat event와 현재 `JOB_PROGRESS` snapshot을 전송합니다.

| Event | 설명 |
| --- | --- |
| `HEARTBEAT` | 연결 유지와 상태 확인 |
| `JOB_PROGRESS` | 현재 진행률 counter 전송 |
| `JOB_COMPLETED` | Job 완료 전환 예약 event |
| `JOB_FAILED` | Job 실패 전환 예약 event |

예시 payload:

```json
{
  "eventType": "JOB_PROGRESS",
  "jobId": 1,
  "total": 1000,
  "queued": 120,
  "processing": 20,
  "succeeded": 850,
  "failed": 10,
  "progressPercent": 86.0,
  "emittedAt": "2026-05-10T00:00:00Z"
}
```

Worker 상태 보고는 Internal Worker API를 통해 Job counter를 갱신하고 SSE 진행률 event를 발행합니다.

## Internal Worker API

인증:

```http
X-Worker-Token: <WORKER_INTERNAL_TOKEN>
```

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/internal/v1/jobs/{jobId}/items/{itemId}/started` | item을 `PROCESSING`으로 변경 |
| `POST` | `/internal/v1/jobs/{jobId}/items/{itemId}/heartbeat` | 처리 중 heartbeat 갱신 |
| `POST` | `/internal/v1/jobs/{jobId}/items/{itemId}/succeeded` | item을 `SUCCEEDED`로 바꾸고 결과 key 저장 |
| `POST` | `/internal/v1/jobs/{jobId}/items/{itemId}/failed` | item을 `FAILED`로 바꾸고 오류 정보 저장 |
| `POST` | `/internal/v1/jobs/{jobId}/items/{itemId}/artifacts` | artifact object key 등록 |
| `GET` | `/internal/v1/preprocess/presets` | Worker용 built-in preset 반환 |

규칙:

- 사용자 Access Token은 `/internal/**`를 인증하지 못합니다.
- `X-Worker-Token`이 없거나 틀리면 `WORKER401`을 반환합니다.
- 잘못된 item 상태 전환은 `WORKER409`를 반환합니다.
- API는 DB 상태만 갱신하고 실제 OpenCV 처리는 `preprocess-worker`가 수행합니다.

## 취소

```text
POST /api/v1/jobs/{jobId}/cancel
```

취소는 cooperative 방식입니다. API는 Job을 `CANCEL_REQUESTED`로 바꾸고 아직 시작하지 않은 item만 취소합니다. 처리 중 item은 Worker가 안전한 지점에서 끝내거나 멈출 수 있도록 그대로 둡니다.

## 재시도

```text
POST /api/v1/jobs/{jobId}/retry
POST /api/v1/jobs/{jobId}/rerun
```

- `retry`는 `FAILED`, `DEAD_LETTERED` item만 재등록합니다.
- `rerun`은 `PROCESSING`이 아닌 모든 item을 재등록합니다.
- 재시도할 때마다 `attempt`가 증가합니다.
- 재시도 메시지는 `image.preprocess.retry`로 발행합니다.

## 현재 제한

- 세부 debug artifact row 확장은 후속 artifact domain 작업으로 남깁니다.
