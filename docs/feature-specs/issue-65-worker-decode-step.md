# 이슈 65. DecodeStep 구현

## 목적

`DecodeStep`을 skeleton에서 실제 이미지 decode 단계로 교체합니다.

## 작업 범위

1. `ImageDecodePort`
2. `ImageCodecAdapter` port 구현
3. source image bytes context 저장
4. decoded `ImageMatHolder` 저장
5. pipeline 종료 후 Mat release
6. decoded width/height/color space 기록

## 완료 기준

1. source bytes가 없으면 pipeline 호환성을 위해 필요한 방식으로 처리합니다.
2. 잘못된 bytes는 decode step 실패로 처리합니다.
3. downstream step은 decoded Mat을 기준으로 동작합니다.
