# 이슈 41. Worker listener skeleton

## 목적

RabbitMQ 메시지를 소비하는 Worker listener의 기본 경계를 만듭니다.

## 작업 범위

1. `PreprocessJobListener`
2. `PreprocessJobMessage`
3. `WorkerJobService`
4. `BackendApiClient` port
5. `ObjectStoragePort` port
6. listener 활성화 환경변수

## 완료 기준

1. 기본값에서는 의도치 않게 큐를 소비하지 않습니다.
2. 유효하지 않은 메시지는 실패로 처리합니다.
3. 실제 OpenCV pipeline 연결 전에는 성공 처리로 위장하지 않습니다.
