# 이슈 71. Geometry 1: 방향 보정과 기울기 보정

## 목적

문서 이미지의 큰 방향과 기울기를 보정합니다.

## 작업 범위

1. `OrientationNormalizeStep`
2. 가로 이미지의 세로 방향 회전
3. `DeskewStep`
4. grayscale 변환
5. Otsu inverse threshold
6. foreground point 기반 `minAreaRect`
7. 허용 각도 내 `warpAffine`
8. fallback note 기록

## 완료 기준

1. foreground가 부족하면 안전하게 skip합니다.
2. `maxDeskewAngle`을 넘으면 보정을 적용하지 않습니다.
3. crop, DPI, 품질 step은 후속 이슈에서 구현합니다.
