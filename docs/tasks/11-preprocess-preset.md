# 11. Preprocess Preset

## 목표

API 서버에서 전처리 프리셋 명세와 파라미터 검증 구조를 구현한다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/10-image.md`
4. `docs/api/job-api.md`
5. `docs/worker/preset-spec.md`

## 작업 범위

1. 기본 프리셋 목록
2. 프리셋 상세 조회
3. 파라미터 검증
4. Custom preset skeleton

## 작업 순서

1. `PreprocessPreset` 모델을 만든다.
2. `PresetType` enum을 만든다.
3. 기본 preset registry를 만든다.
4. `A4_SCAN_300DPI`를 정의한다.
5. `LOW_CONTRAST_SCAN`을 정의한다.
6. `RECEIPT`를 정의한다.
7. `NOISY_SCAN`을 정의한다.
8. `AUTO`를 정의한다.
9. 프리셋 목록 API를 구현한다.
10. 프리셋 상세 API를 구현한다.
11. 파라미터 검증 API를 구현한다.
12. `CustomPreprocessPreset` entity를 만든다.
13. custom preset 생성 API를 구현한다.
14. custom preset 목록 API를 구현한다.
15. custom preset 삭제 API를 구현한다.

## 산출물

1. Preprocess preset domain 클래스
2. Preset API
3. Parameter validator
4. Custom preset skeleton

## 완료 기준

1. API 서버와 Worker가 같은 preset name을 사용한다.
2. Worker가 실제 처리할 파라미터가 API에서 검증된다.
3. 단순 resize preset만 존재하지 않는다.

## 금지 사항

1. API 서버에서 OpenCV 처리를 수행하지 않는다.
2. `NOISY_SCAN` 필요성을 제거하지 않는다.
3. `DPI Normalize`를 썸네일 resize로 표현하지 않는다.
