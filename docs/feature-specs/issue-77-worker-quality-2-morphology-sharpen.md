# 이슈 77. Quality 2: Morphology와 Sharpen

## 목적

이진화 이후 작은 노이즈와 끊긴 획을 보정하고 필요 시 선명화를 적용합니다.

## 작업 범위

1. `MorphologyCleanupStep`
2. `open`, `close`, `open_close` mode
3. binary image 내부 반전 처리
4. 지원하지 않는 mode fallback
5. `SharpenStep`
6. unsharp mask 적용
7. preset 기반 sharpen 활성화

## 완료 기준

1. 기본 preset에서 불필요한 sharpen을 강제하지 않습니다.
2. morphology fallback은 `open_close`를 사용합니다.
3. artifact upload와 success callback은 후속 이슈에서 구현합니다.
