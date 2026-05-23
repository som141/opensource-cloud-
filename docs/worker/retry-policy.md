# Worker 재시도 정책

## 목적

이 문서는 backend Job 도메인과 Worker 구현이 공유하는 재시도 계약을 정의합니다.

## Queue 구성

| Queue | 용도 |
| --- | --- |
| `image.preprocess.high` | 높은 우선순위 전처리 작업 |
| `image.preprocess.normal` | 일반 또는 낮은 우선순위 전처리 작업 |
| `image.preprocess.retry` | 명시적 retry와 rerun 작업 |
| `image.preprocess.dlq` | 재시도 한도를 넘은 최종 실패 메시지 |

## 재시도 가능한 실패

Worker는 아래 실패를 재시도 가능한 오류로 취급합니다.

- 일시적인 Object Storage 다운로드 실패
- 일시적인 Object Storage 업로드 실패
- backend internal API timeout 발생
- ack 전 RabbitMQ delivery 중단
- message ack 전 Worker process crash 발생

## 재시도하지 않는 실패

아래 실패는 사용자가 수동 rerun하지 않는 한 반복 재시도하지 않습니다.

- 이미지 decode 실패
- metadata는 통과했지만 실제 file content가 지원되지 않는 경우
- 알 수 없는 preset 이름
- 잘못된 preset parameter payload
- 손상된 image bytes

## backend 동작

- `POST /api/v1/jobs/{jobId}/retry`는 `FAILED`, `DEAD_LETTERED` item만 재등록합니다.
- `POST /api/v1/jobs/{jobId}/rerun`은 `PROCESSING`이 아닌 모든 item을 재등록합니다.
- 재시도 item은 `RETRYING`으로 이동합니다.
- 재시도할 때 `attempt`가 증가합니다.
- retry 메시지는 `image.preprocess.retry`로 발행합니다.

## Worker 동작

1. 선택된 queue에서 메시지를 consume합니다.
2. internal Worker API로 item started를 보고합니다.
3. Object Storage port로 원본 이미지를 다운로드합니다.
4. internal Worker API로 heartbeat를 보고합니다.
5. 전처리 pipeline을 실행합니다.
6. 성공 시 artifact를 저장하고 success callback을 호출합니다.
7. 재시도 가능한 실패는 requeue하고, 재시도하지 않는 실패는 reject합니다.

## DLQ 규칙

자동 재시도 횟수는 RabbitMQ queue 설정에서 별도로 조정합니다. 현재 listener 정책은 아래와 같습니다.

- 재시도 가능한 실패는 `ImmediateRequeueAmqpException`을 던집니다.
- 재시도하지 않는 실패는 `AmqpRejectAndDontRequeueException`을 던집니다.
- RabbitMQ DLQ binding이 있으면 메시지는 `image.preprocess.dlq`로 이동합니다.
- `jobId`, `itemId`, `imageId`, `attempt`, `traceId`, 실패 원인을 유지합니다.
- backend 수동 retry는 새 메시지를 `image.preprocess.retry`로 발행합니다.
