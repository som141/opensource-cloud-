# Worker 프리셋 명세

이 문서는 `backend-api`와 `preprocess-worker`가 공유하는 문서 이미지 전처리 프리셋 이름과 파라미터 계약을 정의한다.
API 서버는 Job 생성 전에 파라미터 이름과 범위를 검증하고, 실제 OpenCV 기반 전처리는 Worker가 수행한다.

## 지원 프리셋

| 프리셋 | 용도 |
| --- | --- |
| `A4_SCAN_300DPI` | 일반 A4 300 DPI 스캔 문서 |
| `LOW_CONTRAST_SCAN` | 저대비 스캔 문서 |
| `RECEIPT` | 영수증처럼 폭이 좁은 문서 |
| `NOISY_SCAN` | 배경 노이즈가 강한 문서 |
| `AUTO` | Worker가 이미지 특성에 따라 프리셋 선택 |

## 공통 처리 순서

`AUTO`를 제외한 기본 문서 프리셋은 아래 순서로 실행된다.

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

이 파이프라인은 단순 resize가 아니다. `DPI_NORMALIZE`는 OCR 품질을 일정하게 맞추기 위한 DPI 보정 단계다.

## 현재 기준값

`A4_SCAN_300DPI`는 이전 `image-test` 기준에 맞춰 보수적인 OCR 전처리값을 사용한다.

| 파라미터 | A4 기본값 | 설명 |
| --- | --- | --- |
| `grayscale` | `true` | Color Normalize 단계에서 BGR/BGRA 입력을 그레이스케일로 통일 |
| `targetDpi` | `300` | OCR 대상 DPI |
| `referenceWidthInches` | `8.27` | DPI 메타데이터가 없을 때 A4 기준 너비로 source DPI 추정 |
| `referenceHeightInches` | `11.69` | DPI 메타데이터가 없을 때 A4 기준 높이로 source DPI 추정 |
| `fallbackSourceDpi` | `300` | 메타데이터와 기준 크기 추정이 모두 없을 때 사용할 source DPI |
| `maxDeskewAngle` | `40.0` | 이 각도를 넘는 deskew는 안전 범위 밖으로 보고 회전하지 않음 |
| `binarizationMode` | `otsu` | A4 스캔 문서는 기본 Otsu 이진화 사용 |
| `adaptiveBlockSize` | `31` | adaptive threshold 사용 시 block size |
| `adaptiveC` | `15.0` | adaptive threshold 평균에서 뺄 C 값 |
| `contrastNormalize` | `false` | A4 기본값은 CLAHE 대비 정규화를 건너뜀 |
| `contrastClipLimit` | `2.5` | CLAHE clip limit |
| `contrastTileGridSize` | `8` | CLAHE tile grid size |
| `denoiseMode` | `median` | 기본 노이즈 제거 전략 |
| `denoiseKernelSize` | `3` | median blur 커널 |
| `denoiseDiameter` | `7` | bilateral filter 직경 |
| `denoiseSigmaColor` | `50.0` | bilateral color sigma |
| `denoiseSigmaRange` | `50.0` | bilateral spatial sigma |
| `morphologyMode` | `open` | 작은 노이즈 제거 중심 |
| `morphologyKernelSize` | `2` | morphology 2x2 직사각 커널 |
| `sharpen` | `false` | A4 기본값은 sharpen 비활성화 |
| `sharpenAmount` | `0.25` | 활성화 시 unsharp mask 원본/blur 가중치가 1.25/-0.25가 되도록 설정 |
| `sharpenSigma` | `1.2` | 활성화 시 Gaussian sigma |
| `debugArtifacts` | `false` | 단계별 debug artifact 저장 여부 |

## 프리셋별 차이

| 프리셋 | binarizationMode | contrastNormalize | contrastClipLimit | denoiseMode | morphologyMode | sharpen |
| --- | --- | --- | --- | --- | --- | --- |
| `A4_SCAN_300DPI` | `otsu` | `false` | `2.5` | `median` | `open` | `false` |
| `LOW_CONTRAST_SCAN` | `adaptive` | `true` | `2.5` | `median` | `close` | `true` |
| `RECEIPT` | `adaptive` | `true` | `2.5` | `median` | `close` | `true` |
| `NOISY_SCAN` | `adaptive` | `false` | `2.5` | `bilateral` | `open` | `false` |

모든 built-in preset은 `grayscale=true`, `fallbackSourceDpi=300`, `adaptiveBlockSize=31`, `adaptiveC=15.0`,
`denoiseDiameter=7`, `denoiseSigmaColor=50.0`, `denoiseSigmaRange=50.0`,
`morphologyKernelSize=2`, `sharpenAmount=0.25`, `sharpenSigma=1.2`를 포함한다.

## 방향 보정 기준

`ORIENTATION_NORMALIZE`는 이미지가 가로로 누워 있다고 판단될 때만 90도 반시계 방향으로 회전한다.

```text
조건: cols > rows * 1.15
회전: Core.ROTATE_90_COUNTERCLOCKWISE
리포트 note: grossRotationDegrees=-90.0
```

이 조건을 만족하지 않는 약한 가로형 이미지는 무리하게 회전하지 않는다.

## DPI Normalize 기준

`DPI_NORMALIZE`는 다음 순서로 source DPI를 결정한다.

1. 입력 메타데이터 또는 Job 파라미터의 `sourceDpi`, `sourceDpiX`, `sourceDpiY`
2. `referenceWidthInches`, `referenceHeightInches`와 현재 이미지 크기를 이용한 휴리스틱 추정
3. `fallbackSourceDpi`

스케일은 과도한 확대/축소를 막기 위해 `0.75x`부터 `2.5x` 사이로 제한한다.
확대 시 `INTER_CUBIC`, 축소 시 `INTER_AREA`를 사용한다.

## Backend 계약

- `/api/v1/preprocess/presets`는 위 프리셋과 파라미터 기본값을 노출한다.
- Job 생성 시 backend-api는 프리셋 파라미터를 검증하고 resolved parameter를 Job과 RabbitMQ 메시지에 저장한다.
- Worker는 메시지에 포함된 파라미터를 우선 사용한다.
- 따라서 프리셋 기본값 변경 시 `backend-api`와 `preprocess-worker` 값을 함께 맞춰야 한다.
