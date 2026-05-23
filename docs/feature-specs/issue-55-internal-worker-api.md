# 이슈 55. Worker internal API

## 목적

Worker가 JobItem 처리 상태를 backend-api에 보고할 수 있는 내부 API를 구현합니다.

## 작업 범위

1. 시작 보고
2. heartbeat 보고
3. 성공 보고
4. 실패 보고
5. artifact key 등록
6. Worker token 인증

## 완료 기준

1. Worker 내부 API는 일반 사용자 API와 분리됩니다.
2. 실패 callback은 error code와 message를 저장합니다.
3. 성공 callback은 artifact key를 포함합니다.
