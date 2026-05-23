# 이슈 75. Quality 1: 노이즈, 대비, 이진화

## 목적

문서 이미지 품질을 개선하기 위해 denoise, contrast normalize, binarization step을 구현합니다.

## 작업 범위

1. `DenoiseStep`
2. median blur
3. bilateral filtering
4. `ContrastNormalizeStep`
5. CLAHE 적용
6. `BinarizationStep`
7. Otsu threshold
8. adaptive threshold
9. 지원하지 않는 mode fallback

## 완료 기준

1. 텍스트 경계를 과하게 훼손하지 않습니다.
2. mode 오류는 실패 대신 fallback 가능한 기본값을 사용합니다.
3. morphology와 sharpen은 후속 이슈에서 구현합니다.
