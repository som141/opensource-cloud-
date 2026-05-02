# 14. Internal Worker API

## 목표

Worker가 처리 상태와 artifact를 API 서버에 보고할 수 있는 내부 API를 구현한다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/12-job.md`
4. `docs/tasks/13-sse-progress.md`
5. `docs/api/job-api.md`

## 작업 범위

1. Worker token 검증
2. item started
3. item heartbeat
4. item succeeded
5. item failed
6. artifact 등록
7. Worker preset 조회

## 작업 순서

1. Worker service token 검증 필터를 만든다.
2. internal API controller를 만든다.
3. item started API를 구현한다.
4. item heartbeat API를 구현한다.
5. item succeeded API를 구현한다.
6. item failed API를 구현한다.
7. artifact 등록 API를 구현한다.
8. Worker용 preset 조회 API를 구현한다.
9. 실패 코드 저장을 구현한다.
10. 상태 전이 검증을 구현한다.
11. SSE progress 갱신과 연결한다.
12. audit 필요 항목을 연결한다.

## 산출물

1. Internal Worker API
2. Worker token 검증 구조
3. JobItem 상태 보고 로직
4. Artifact 등록 로직

## 완료 기준

1. Worker가 DB에 직접 접속하지 않는다.
2. 외부 사용자가 internal API를 호출할 수 없다.
3. item 상태 전이가 일관된다.

## 금지 사항

1. Internal API를 공개 API 문서와 같은 성격으로 노출하지 않는다.
2. 사용자 access token으로 Worker API를 호출하지 않는다.
3. Worker 실패를 성공 상태로 덮어쓰지 않는다.
