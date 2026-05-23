# 이슈 49. Worker skeleton 보강

## 목적

초기 Worker skeleton을 실제 전처리 pipeline과 storage 연동을 붙일 수 있는 수준으로 보강합니다.

## 작업 범위

1. Worker 설정 기본값 정리
2. listener/service 책임 분리
3. 실패 코드 enum 정리
4. backend callback port 정리
5. storage port 정리

## 완료 기준

1. Worker가 인증 도메인이나 화면용 API를 갖지 않습니다.
2. 후속 OpenCV step 구현이 가능한 구조입니다.
3. 실패 처리와 retry 판단 위치가 분명합니다.
