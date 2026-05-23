# Worker 프리셋 명세

이 문서는 backend-api와 preprocess-worker가 공유하는 전처리 프리셋 이름과 파라미터 계약을 정의합니다.
API 서버는 프리셋 이름과 파라미터 범위를 검증하고, 실제 OpenCV 문서 전처리는 Worker가 수행합니다.

## 지원 프리셋

| 프리셋 | 용도 |
| --- | --- |
| `A4_SCAN_300DPI` | 일반 A4 300 DPI 스캔 문서 |
| `LOW_CONTRAST_SCAN` | 저대비 문서 |
| `RECEIPT` | 영수증처럼 폭이 좁은 문서 |
| `NOISY_SCAN` | 배경 노이즈가 강한 문서 |
| `AUTO` | Worker가 이미지 특성에 따라 프리셋 선택 |

## 처리 단계

`AUTO`를 제외한 기본 문서 프리셋은 아래 순서를 따릅니다.

1. `DECODE`
2. `COLOR_NORMALIZE`
3. `ORIENTATION_NORMALIZE`
4. `DESKEW`
5. `CROP`
6. `DENOISE`
7. `CONTRAST_NORMALIZE`
8. `BINARIZATION`
9. `MORPHOLOGY_CLEANUP`
10. `DPI_NORMALIZE`
11. `OPTIONAL_SHARPEN`

이 파이프라인은 단순 resize가 아닙니다. `DPI_NORMALIZE`는 OCR 품질을 맞추기 위한 DPI 정규화 단계입니다.

## 공통 파라미터

| 파라미터 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `targetDpi` | integer | `300` | OCR용 목표 DPI |
| `maxDeskewAngle` | decimal | `40.0` | 허용할 최대 deskew 각도 |
| `binarizationMode` | enum | 프리셋별 | `otsu`, `adaptive` |
| `adaptiveBlockSize` | integer | `21` | adaptive threshold block size. 홀수로 보정됨 |
| `adaptiveC` | decimal | `5.0` | adaptive threshold 평균에서 뺄 C 값 |
| `contrastClipLimit` | decimal | 프리셋별 | CLAHE clip limit |
| `contrastTileGridSize` | integer | `8` | CLAHE tile grid size |
| `denoiseMode` | enum | 프리셋별 | `median`, `bilateral`, `none`, `off`, `false` |
| `denoiseKernelSize` | integer | `3` | median blur kernel size |
| `denoiseDiameter` | integer | `5` | bilateral filter diameter |
| `denoiseSigmaColor` | decimal | `25.0` | bilateral filter color sigma. 텍스트 경계 보존을 위해 낮춤 |
| `denoiseSigmaRange` | decimal | `75.0` | bilateral filter spatial sigma range |
| `morphologyMode` | enum | `open_close` | `open`, `close`, `open_close`, `none`, `off`, `false` |
| `morphologyKernelSize` | integer | `2` | morphology kernel size |
| `sharpen` | boolean | 프리셋별 | optional sharpen 실행 여부 |
| `sharpenAmount` | decimal | `0.8` | unsharp mask weight |
| `sharpenSigma` | decimal | `1.5` | unsharp mask Gaussian sigma |
| `debugArtifacts` | boolean | `false` | 단계별 debug artifact 저장 여부 |

## 이번 튜닝 기준

| 항목 | 이전 값 | 변경 값 | 의도 |
| --- | --- | --- | --- |
| `adaptiveBlockSize` | `31` | `21` | 지역 threshold가 더 작은 글자 영역에 반응하도록 조정 |
| `adaptiveC` | `7.0` | `5.0` | 얇은 획 손실을 줄이기 위해 threshold offset 완화 |
| `contrastClipLimit` 기본값 | `1.2` | `2.0` | 저대비 문서 대비 개선 강화 |
| `sharpenAmount` | `0.6` | `0.8` | OCR 전 획 선명도 강화 |
| `sharpenSigma` | `1.0` | `1.5` | 노이즈보다 문자 획 중심으로 sharpening 조정 |
| `denoiseSigmaColor` | `75.0` | `25.0` | bilateral denoise에서 텍스트 경계 보존 |

## 프리셋 기본값

| 프리셋 | binarizationMode | contrastClipLimit | denoiseMode | morphologyMode | sharpen |
| --- | --- | --- | --- | --- | --- |
| `A4_SCAN_300DPI` | `otsu` | `2.0` | `median` | `open_close` | `false` |
| `LOW_CONTRAST_SCAN` | `adaptive` | `2.4` | `median` | `open_close` | `true` |
| `RECEIPT` | `adaptive` | `2.2` | `median` | `open_close` | `true` |
| `NOISY_SCAN` | `adaptive` | `2.0` | `bilateral` | `open_close` | `false` |

모든 built-in preset은 `adaptiveBlockSize=21`, `adaptiveC=5.0`, `denoiseSigmaColor=25.0`,
`denoiseSigmaRange=75.0`, `morphologyKernelSize=2`, `sharpenAmount=0.8`, `sharpenSigma=1.5`를 포함합니다.

## Backend 계약

- backend-api는 `/api/v1/preprocess/presets`에서 프리셋 목록과 파라미터를 노출합니다.
- backend-api는 Job 생성 전 파라미터 이름과 범위를 검증합니다.
- backend-api는 OpenCV 전처리를 직접 수행하지 않습니다.
- Worker는 알 수 없는 프리셋 이름이나 잘못된 파라미터 payload를 거부해야 합니다.
