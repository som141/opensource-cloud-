# Worker 전처리 파이프라인

## 목적

Worker 전처리 파이프라인은 OCR 전에 문서 이미지를 정규화하는 OpenCV 기반 처리 흐름입니다.
이 파이프라인은 썸네일 생성이나 단순 resize가 아니라, 스캔 품질, 방향, 기울기, crop, 노이즈, 대비, 이진화, morphology, DPI를 OCR에 맞게 보정하는 구조입니다.

이 플랫폼은 OCR 텍스트 추출 서비스를 제공하지 않습니다. Worker는 OCR 엔진에 넣기 전의 문서 이미지 전처리만 담당합니다.

## 현재 구현 요약

| 이슈 | 구현 내용 |
| --- | --- |
| `#43` | `PreprocessContext`, `PreprocessPipeline`, `PreprocessPipelineRunner`, `PreprocessResult`, step catalog, preset registry skeleton 추가 |
| `#59` | 단계별 시작/종료 시각, wall time, 성공/실패, fallback note, 전체 처리 시간 hook 추가 |
| `#61` | debug artifact metadata hook 추가 |
| `#63` | OpenCV loader, image codec adapter, `ImageMatHolder`, Mat 정리 경계 추가 |
| `#65` | source bytes가 있을 때 `DecodeStep`이 실제 decode 수행 |
| `#67` | Object Storage에서 원본 bytes를 다운로드해 context에 연결 |
| `#69` | `ColorNormalizeStep` 구현. `GRAY`, `BGRA` 입력을 `BGR` 처리 형식으로 정규화 |
| `#71` | `OrientationNormalizeStep`, `DeskewStep` 구현 |
| `#73` | `CropStep`, `DpiNormalizeStep` 구현 |
| `#75` | `DenoiseStep`, `ContrastNormalizeStep`, `BinarizationStep` 구현 |
| `#77` | `MorphologyCleanupStep`, `SharpenStep` 구현 |
| `#79` | `processed.png`, `preview.png`, `processing-report.json` 저장과 success callback 연결 |
| `#81` | `debug=true`일 때 단계별 debug PNG snapshot 저장 |

## 실행 순서

모든 built-in 문서 프리셋은 아래 순서로 실행됩니다.

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

`DPI_NORMALIZE`는 OCR 품질을 위한 DPI 보정 단계입니다. 썸네일 resize로 축소해서 구현하면 안 됩니다.

## 주요 단계 설명

| 단계 | 설명 |
| --- | --- |
| `DECODE` | 원본 bytes를 OpenCV `Mat`으로 decode |
| `COLOR_NORMALIZE` | 입력 channel을 downstream step이 처리하기 쉬운 형식으로 정규화 |
| `ORIENTATION_NORMALIZE` | 가로로 누운 스캔본을 세로 방향으로 회전 |
| `DESKEW` | foreground pixel geometry를 기준으로 기울기 보정 |
| `CROP` | 문서 영역을 찾아 scanner border나 배경을 제거 |
| `DENOISE` | median 또는 bilateral filter로 스캔 노이즈 완화 |
| `CONTRAST_NORMALIZE` | CLAHE로 저대비 문서의 대비 개선 |
| `BINARIZATION` | Otsu 또는 adaptive threshold로 단일 channel binary image 생성 |
| `MORPHOLOGY_CLEANUP` | open/close 연산으로 작은 노이즈와 끊긴 획 보정 |
| `DPI_NORMALIZE` | source DPI metadata가 있을 때 target DPI로 정규화 |
| `OPTIONAL_SHARPEN` | preset이 켠 경우 unsharp mask 적용 |

## 지원 프리셋

- `A4_SCAN_300DPI`
- `LOW_CONTRAST_SCAN`
- `RECEIPT`
- `NOISY_SCAN`
- `AUTO`

프리셋 이름은 backend `/api/v1/preprocess/presets` 계약과 반드시 일치해야 합니다.

## Worker 실행 경계

Worker는 아래 artifact가 Object Storage에 업로드된 뒤에만 성공을 보고합니다.

- `processed/{projectId}/{jobId}/{itemId}/processed.png`
- `processed/{projectId}/{jobId}/{itemId}/preview.png`
- `processed/{projectId}/{jobId}/{itemId}/processing-report.json`

`debug=true`인 경우 아래 debug artifact도 업로드합니다.

- `processed/{projectId}/{jobId}/{itemId}/debug/00_decoded.png`
- `processed/{projectId}/{jobId}/{itemId}/debug/01_normalized.png`
- `processed/{projectId}/{jobId}/{itemId}/debug/02_orientation.png`
- `processed/{projectId}/{jobId}/{itemId}/debug/03_deskew.png`
- `processed/{projectId}/{jobId}/{itemId}/debug/04_crop.png`
- `processed/{projectId}/{jobId}/{itemId}/debug/05_denoise.png`
- `processed/{projectId}/{jobId}/{itemId}/debug/06_contrast.png`
- `processed/{projectId}/{jobId}/{itemId}/debug/07_binarized.png`
- `processed/{projectId}/{jobId}/{itemId}/debug/08_morphology.png`
- `processed/{projectId}/{jobId}/{itemId}/debug/09_dpi.png`
- `processed/{projectId}/{jobId}/{itemId}/debug/10_sharpen.png`

## 오류 매핑

| 상황 | Worker 실패 코드 |
| --- | --- |
| 실제 output image 없이 pipeline 종료 | `PIPELINE_NOT_IMPLEMENTED` |
| OpenCV step 실행 실패 | `PIPELINE_EXECUTION_FAILED` |
| artifact upload 실패 | `ARTIFACT_UPLOAD_FAILED` |
| backend internal API 보고 실패 | `BACKEND_REPORT_FAILED` |

## 후속 작업

1. skew angle, crop bounds, binarization strategy, DPI normalization 결과를 report에 더 자세히 남깁니다.
2. MinIO에 저장된 artifact를 직접 확인하는 Docker smoke 검증을 보강합니다.
3. OCR 텍스트 추출은 Worker runtime 범위에 넣지 않습니다.
