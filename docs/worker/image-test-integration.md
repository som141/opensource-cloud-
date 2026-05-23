# image-test 통합 경계

## 목적

Worker는 `som141/image-test`의 문서 이미지 전처리 메커니즘을 통합합니다.
통합 대상은 OCR 전처리이며, OCR 텍스트 추출이 아닙니다.

## 포함 범위

- 원본 이미지 bytes를 OpenCV 이미지 표현으로 decode
- 입력 색상 형식 정규화
- 스캔 문서 방향 보정과 deskew
- scanner border 또는 배경에서 문서 영역 crop
- 스캔 노이즈 제거
- 저대비 스캔본 대비 정규화
- OCR 준비를 위한 이진화
- 작은 노이즈 제거와 획 보정을 위한 morphology cleanup
- OCR 품질 일관성을 위한 DPI 정규화
- 필요 시 최종 문서 이미지 sharpen
- 처리 이미지, preview 이미지, debug artifact, processing report metadata 생성

## 제외 범위

- Tesseract 또는 다른 OCR 엔진 실행
- 인식 텍스트 반환
- OCR 텍스트 저장
- OCR 텍스트 검색 또는 교정
- OCR 과금, 언어팩, 텍스트 검수 workflow

## 현재 통합 상태

| 이슈 | 통합 상태 |
| --- | --- |
| `#49` | Worker skeleton 경계 완성. `domain/preprocess/model`, `domain/artifact`, `domain/report`, `infra/opencv`, tracing, metrics 추가 |
| `#63` | OpenCV runtime 경계 추가. `OpenCvLoader`, `ImageCodecAdapter`, `ImageMatHolder`, `MatResourceCleaner` 추가 |
| `#65` | source bytes가 있을 때 `DecodeStep`이 실제 decode 수행 |
| `#67` | Object Storage 다운로드 bytes를 pipeline context에 연결 |
| `#69` | decoded image 색상 형식을 BGR로 정규화 |
| `#71` | 방향 보정과 deskew 구현 |
| `#73` | crop과 DPI normalize 구현 |
| `#75` | denoise, contrast normalize, binarization 구현 |
| `#77` | morphology cleanup과 optional sharpen 구현 |
| `#79` | 최종 Mat을 `processed.png`, `preview.png`, `processing-report.json`로 저장하고 backend에 성공 보고 |
| `#81` | `debug=true`일 때 단계별 PNG snapshot 저장 |

## 통합 원칙

- Worker는 API DB에 직접 접근하지 않습니다.
- Worker는 backend internal API로 상태를 보고합니다.
- API 서버는 OpenCV 처리를 수행하지 않습니다.
- OCR 텍스트 추출 기능은 제품 runtime에서 제외합니다.
- OpenCV `Mat`은 context와 holder 소유권 기준으로 명확하게 release합니다.

## 후속 구현 포인트

1. 처리 리포트에 실제 이미지 처리 결과값을 더 풍부하게 기록합니다.
2. 모든 artifact type을 검증하는 MinIO E2E smoke를 추가합니다.
3. OCR 텍스트 추출은 계속 제품 범위 밖에 둡니다.
