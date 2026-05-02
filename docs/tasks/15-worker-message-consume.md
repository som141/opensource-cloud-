# 15. Worker Message Consume

## 목표

Worker가 RabbitMQ 메시지를 안정적으로 consume하고 처리 시작/성공/실패를 API에 보고한다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/03-worker-skeleton.md`
4. `docs/tasks/14-internal-worker-api.md`
5. `docs/worker/retry-policy.md`

## 작업 범위

1. RabbitMQ listener
2. 메시지 역직렬화
3. traceId 전파
4. ack/nack
5. retry/DLQ 정책

## 작업 순서

1. Worker RabbitMQ 설정을 만든다.
2. preprocess queue listener를 만든다.
3. benchmark queue listener를 만든다.
4. `PreprocessJobMessage` DTO를 만든다.
5. 메시지 JSON 역직렬화를 구현한다.
6. 메시지 validation을 구현한다.
7. traceId 추출을 구현한다.
8. 처리 시작 internal API 호출을 구현한다.
9. `WorkerJobService`에 처리를 위임한다.
10. 성공 시 ack 처리한다.
11. retry 가능한 실패를 분류한다.
12. retry 불가능한 실패를 분류한다.
13. DLQ 이동 정책을 연결한다.
14. 처리 실패 internal API 호출을 구현한다.

## 산출물

1. RabbitMQ listener
2. WorkerJobService
3. Retry/DLQ 연결
4. BackendApiClient 호출 흐름

## 완료 기준

1. Worker가 죽으면 ack되지 않은 메시지가 재전달된다.
2. decode 실패 같은 영구 실패가 무한 retry되지 않는다.
3. traceId가 Worker 처리 로그와 API 보고에 연결된다.

## 금지 사항

1. 메시지 consume 중 DB에 직접 접속하지 않는다.
2. 실패를 모두 같은 종류로 처리하지 않는다.
3. ack를 처리 완료 전에 보내지 않는다.
