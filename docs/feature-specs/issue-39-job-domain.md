# 이슈 39. Job 도메인

## 목적

전처리 작업을 생성하고 이미지 단위 `JobItem` 상태를 관리합니다.

## 작업 범위

1. `Job` entity
2. `JobItem` entity
3. 작업 생성 API
4. 이미지별 JobItem 생성
5. RabbitMQ 메시지 발행 요청
6. 작업 상태 조회
7. 실패 항목 재시도와 취소 기반 구조

## 완료 기준

1. 이미지 한 장당 하나의 JobItem으로 분산 처리할 수 있습니다.
2. API 서버는 전처리를 직접 실행하지 않습니다.
3. Worker callback이 상태를 갱신합니다.
