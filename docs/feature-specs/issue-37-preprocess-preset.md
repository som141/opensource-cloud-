# 이슈 37. 전처리 프리셋

## 목적

Worker에 전달할 문서 이미지 전처리 preset과 파라미터를 관리합니다.

## 작업 범위

1. `A4_SCAN_300DPI`
2. `LOW_CONTRAST_SCAN`
3. `RECEIPT`
4. `NOISY_SCAN`
5. preset 조회 API
6. 파라미터 검증

## 완료 기준

1. 프리셋은 단순 resize 옵션이 아닙니다.
2. denoise, contrast, binarization, morphology, sharpen 파라미터를 표현할 수 있습니다.
3. Worker와 API가 같은 preset 이름을 사용합니다.
