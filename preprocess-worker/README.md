# preprocess-worker

RabbitMQ 메시지를 소비하고 문서 이미지 전처리 pipeline을 실행하는 Worker 경로다.

## 책임

1. RabbitMQ 메시지 consume
2. Object Storage에서 원본 이미지 다운로드
3. image-test 기반 OpenCV 문서 이미지 전처리 pipeline 실행
4. processed image, preview, debug artifact, processing report 저장
5. backend-api internal API로 처리 상태 보고

## 금지 사항

1. OAuth 로그인 로직을 넣지 않는다.
2. 사용자 화면용 API를 만들지 않는다.
3. API DB에 직접 접속하지 않는다.
4. 단순 resize Worker로 축소하지 않는다.
