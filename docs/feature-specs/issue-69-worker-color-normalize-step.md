# 이슈 69. ColorNormalizeStep

## 목적

입력 이미지 색상 channel을 pipeline이 처리하기 쉬운 BGR 형태로 정규화합니다.

## 작업 범위

1. Gray 입력을 BGR로 변환
2. BGRA 입력을 BGR로 변환
3. BGR 입력은 no-op 처리
4. 변환 후 이전 Mat release
5. 지원하지 않는 channel layout 실패 처리

## 완료 기준

1. downstream step은 BGR 기준으로 동작할 수 있습니다.
2. resource leak 없이 이전 Mat을 정리합니다.
3. orientation, deskew, crop 등 후속 단계는 별도 이슈에서 구현합니다.
