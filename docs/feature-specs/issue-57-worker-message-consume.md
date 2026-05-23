# 이슈 57. Worker 메시지 소비

## 목적

RabbitMQ 메시지를 실제 Worker 작업으로 소비하고 처리 결과를 backend-api로 보고합니다.

## 작업 범위

1. queue listener 연결
2. 메시지 검증
3. 원본 object download 호출
4. pipeline 실행 호출
5. 성공/실패 callback 호출
6. retry 가능 오류 분류

## 완료 기준

1. Worker는 DB에 직접 접근하지 않습니다.
2. 메시지 처리 실패는 명확한 실패 코드로 보고됩니다.
3. RabbitMQ ack는 처리 결과에 맞게 수행합니다.
