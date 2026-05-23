# 이슈 63. OpenCV loader와 codec boundary

## 목적

Worker 전용 OpenCV 로딩과 이미지 byte decode 경계를 만듭니다.

## 작업 범위

1. Worker 전용 OpenCV dependency
2. `OpenCvLoader`
3. `ImageCodecAdapter`
4. `ImageMatHolder`
5. `MatResourceCleaner`
6. decode 실패 처리

## 완료 기준

1. OpenCV native loading은 idempotent하게 동작합니다.
2. 빈 byte나 지원하지 않는 이미지 byte는 decode 실패로 처리합니다.
3. Mat resource release 규칙이 명확합니다.
