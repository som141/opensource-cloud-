# 이슈 118. Worker 전처리 품질 파라미터 튜닝

## 목적

문서 텍스트 경계를 더 잘 보존하면서 대비와 선명도를 개선하도록 전처리 기본 파라미터를 조정합니다.

## 변경 내용

1. `BinarizationStep` adaptive `blockSize` 기본값을 `31`에서 `21`로 조정
2. `BinarizationStep` adaptive `C` 기본값을 `7.0`에서 `5.0`으로 조정
3. `ContrastNormalizeStep` CLAHE `clipLimit` 기본값을 `1.2`에서 `2.0`으로 조정
4. `SharpenStep` `amount`를 `0.6`에서 `0.8`로 조정
5. `SharpenStep` `sigma`를 `1.0`에서 `1.5`로 조정
6. `DenoiseStep` bilateral `sigmaColor`, `sigmaRange`를 파라미터로 노출
7. bilateral `sigmaColor` 기본값을 `75`에서 `25`로 낮춰 텍스트 경계 보존
8. 주요 프리셋에 누락된 denoise/morphology 파라미터를 추가
9. 주요 프리셋의 `contrastClipLimit`을 상향

## 완료 기준

1. `A4_SCAN_300DPI`, `NOISY_SCAN`, `LOW_CONTRAST_SCAN`, `RECEIPT` 프리셋이 새 파라미터를 포함합니다.
2. 기존 step 단위 테스트가 통과합니다.
3. 파라미터 의미는 `docs/worker/preset-spec.md`에 반영합니다.
