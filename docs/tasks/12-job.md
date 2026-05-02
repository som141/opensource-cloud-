# 12. Job

## 목표

전처리 작업 생성, 이미지 단위 JobItem 생성, RabbitMQ 메시지 발행, 상태 조회를 구현한다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/11-preprocess-preset.md`
4. `docs/api/job-api.md`
5. `docs/worker/retry-policy.md`

## 작업 범위

1. Job entity
2. JobItem entity
3. 상태 전이
4. 메시지 발행
5. 취소와 재시도

## 작업 순서

1. `Job` entity를 만든다.
2. `JobItem` entity를 만든다.
3. `JobStatus` enum을 만든다.
4. `JobItemStatus` enum을 만든다.
5. `JobPriority` enum을 만든다.
6. repository를 만든다.
7. Job 생성 DTO를 만든다.
8. Job 생성 API를 구현한다.
9. imageIds 권한과 상태를 검증한다.
10. 이미지 한 장당 JobItem을 생성한다.
11. `PreprocessJobMessage`를 만든다.
12. `JobMessagePublisher`를 만든다.
13. priority별 queue 선택을 구현한다.
14. 작업 목록 API를 구현한다.
15. 작업 상세 API를 구현한다.
16. 작업 item 목록 API를 구현한다.
17. 작업 summary API를 구현한다.
18. 작업 취소 API를 구현한다.
19. 실패 item retry API를 구현한다.
20. 전체 rerun API를 구현한다.
21. 결과 artifact 목록 API를 구현한다.
22. ZIP 다운로드 skeleton을 구현한다.

## 산출물

1. Job domain 클래스
2. RabbitMQ message DTO
3. Job API
4. Job 상태 전이 로직

## 완료 기준

1. 메시지는 이미지 한 장당 하나다.
2. queue 이름은 문서와 일치한다.
3. 취소와 재시도는 상태 전이를 검증한다.

## 금지 사항

1. Job 생성 시 Worker를 직접 호출하지 않는다.
2. 전체 이미지를 하나의 메시지로 묶지 않는다.
3. 실패한 item과 성공한 item을 구분 없이 재처리하지 않는다.
