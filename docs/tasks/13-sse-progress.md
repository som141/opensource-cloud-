# 13. SSE Progress

## 목표

프론트엔드가 작업 진행률을 실시간으로 받을 수 있는 SSE API를 구현한다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/12-job.md`
4. `docs/api/job-api.md`
5. `docs/architecture/sequence-diagrams.md`

## 작업 범위

1. Job event controller
2. SSE emitter registry
3. Job progress event
4. heartbeat
5. NGINX buffering off 연동

## 작업 순서

1. `JobEventController`를 만든다.
2. `JobEventService`를 만든다.
3. Job progress event DTO를 만든다.
4. SSE emitter registry를 구현한다.
5. 연결 timeout을 설정한다.
6. 연결 해제 cleanup을 구현한다.
7. heartbeat event를 구현한다.
8. `JOB_PROGRESS` event를 구현한다.
9. `JOB_COMPLETED` event를 구현한다.
10. `JOB_FAILED` event를 구현한다.
11. Job 상태 변경 시 이벤트를 발행한다.
12. NGINX SSE 경로에 `proxy_buffering off`를 적용한다.
13. 프론트 SSE client와 연결한다.

## 산출물

1. SSE API
2. Job progress event DTO
3. SSE client 연결 기준
4. NGINX SSE 설정

## 완료 기준

1. 작업 상세 화면이 새로고침 없이 진행률을 받을 수 있다.
2. 연결 종료 시 서버 리소스가 정리된다.
3. NGINX가 SSE 응답을 버퍼링하지 않는다.

## 금지 사항

1. 진행률 조회를 polling만으로 고정하지 않는다.
2. SSE 연결마다 무제한 리소스를 점유하지 않는다.
3. 인증 없는 사용자가 다른 사용자의 Job event를 구독하게 하지 않는다.
