# 이슈 94. 로컬 E2E 스모크 스크립트

## 목적

브라우저 수동 조작 없이 API 기반으로 로컬 MVP 전체 흐름을 반복 검증합니다.

## 작업 범위

1. synthetic 문서 이미지 생성
2. ZIP 생성
3. presigned URL 업로드
4. upload complete 호출
5. Job 생성
6. Worker 완료 대기
7. 처리 이미지와 ZIP 다운로드

## 완료 기준

1. 스크립트는 인증된 Access Token을 사용합니다.
2. 개발용 인증 우회를 만들지 않습니다.
3. 결과 artifact를 `out/local-e2e-smoke/{runId}`에 저장합니다.
