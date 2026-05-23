# 이슈 73. Geometry 2: Crop과 DPI 정규화

## 목적

문서 영역을 잘라내고 OCR 품질을 위한 DPI 기준 크기 보정을 수행합니다.

## 작업 범위

1. `CropStep`
2. foreground bounds 탐지
3. `cropMarginPixels` 적용
4. 유효하지 않은 crop fallback
5. `DpiNormalizeStep`
6. `sourceDpi`와 `targetDpi` 기반 scale 계산
7. min/max scale 제한

## 완료 기준

1. crop 실패 시 원본을 유지하고 fallback note를 기록합니다.
2. source DPI가 없으면 DPI normalize를 skip합니다.
3. DPI normalize는 thumbnail resize가 아니라 OCR 전처리 단계입니다.
