# 17. Worker Preset

## 목표

Worker에서 프리셋별 전처리 파라미터와 단계 구성을 관리한다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/16-worker-preprocess-pipeline.md`
4. `docs/worker/preset-spec.md`
5. `docs/api/job-api.md`

## 작업 범위

1. Preset name enum
2. Preset registry
3. 기본 프리셋 구현
4. AUTO selector
5. API preset 명세와 이름 일치

## 작업 순서

1. `PreprocessPresetName` enum을 만든다.
2. `PreprocessPreset` interface를 만든다.
3. `PreprocessPresetRegistry`를 만든다.
4. `A4Scan300DpiPreset`을 구현한다.
5. `LowContrastScanPreset`을 구현한다.
6. `ReceiptPreset`을 구현한다.
7. `NoisyScanPreset`을 구현한다.
8. `AutoPresetSelector`를 구현한다.
9. 프리셋별 step parameter를 연결한다.
10. API에서 전달된 preset name을 registry와 매핑한다.
11. 지원하지 않는 preset 예외를 구현한다.
12. 프리셋 테스트를 작성한다.

## 산출물

1. Worker preset registry
2. 기본 preset 클래스
3. AUTO preset selector skeleton
4. Preset 테스트

## 완료 기준

1. `A4_SCAN_300DPI`, `LOW_CONTRAST_SCAN`, `RECEIPT`, `NOISY_SCAN`, `AUTO`가 처리 가능하다.
2. API 서버의 preset name과 Worker enum이 일치한다.
3. 프리셋별 단계와 파라미터가 분리되어 있다.

## 금지 사항

1. `NOISY_SCAN`을 `LOW_CONTRAST_SCAN`과 완전히 동일하게 방치하지 않는다.
2. AUTO를 단순 기본값 alias로만 처리하지 않는다.
3. 프리셋 이름을 임의로 변경하지 않는다.
