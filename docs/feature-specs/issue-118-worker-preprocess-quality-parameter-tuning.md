# Issue 118. Worker 전처리 품질 파라미터 튜닝

## 목적

문서 OCR 전처리 품질을 높이기 위해 Worker의 품질 관련 기본 파라미터를 조정한다.  
API 서버는 전처리를 직접 수행하지 않고, preset validation contract만 Worker와 맞춘다.

## 변경 파라미터

| 대상 | 이전 값 | 변경 값 |
| --- | --- | --- |
| `BinarizationStep.DEFAULT_ADAPTIVE_BLOCK_SIZE` | `31` | `21` |
| `BinarizationStep.DEFAULT_ADAPTIVE_C` | `7.0` | `5.0` |
| `ContrastNormalizeStep.DEFAULT_CLIP_LIMIT` | `1.2` | `2.0` |
| `SharpenStep.DEFAULT_SHARPEN_AMOUNT` | `0.6` | `0.8` |
| `SharpenStep.DEFAULT_SHARPEN_SIGMA` | `1.0` | `1.5` |
| `DenoiseStep` bilateral sigmaColor | `75.0` | `25.0` |
| `DenoiseStep` bilateral sigmaRange | 고정 `75.0` | `denoiseSigmaRange` 파라미터로 노출 |

## 추가 파라미터

| 파라미터 | 기본값 | 설명 |
| --- | --- | --- |
| `denoiseSigmaColor` | `25.0` | bilateral filter color sigma |
| `denoiseSigmaRange` | `75.0` | bilateral filter spatial sigma |
| `adaptiveBlockSize` | `21` | adaptive threshold block size |
| `adaptiveC` | `5.0` | adaptive threshold C |
| `morphologyMode` | `open_close` | morphology cleanup strategy |
| `morphologyKernelSize` | `2` | morphology kernel size |
| `sharpenAmount` | `0.8` | unsharp mask amount |
| `sharpenSigma` | `1.5` | unsharp mask sigma |

## 프리셋 기본값

| 프리셋 | contrastClipLimit | denoiseMode | morphologyMode | sharpen |
| --- | --- | --- | --- | --- |
| `A4_SCAN_300DPI` | `2.0` | `median` | `open_close` | `false` |
| `LOW_CONTRAST_SCAN` | `2.4` | `median` | `open_close` | `true` |
| `RECEIPT` | `2.2` | `median` | `open_close` | `true` |
| `NOISY_SCAN` | `2.0` | `bilateral` | `open_close` | `false` |

## 검증 기준

- Worker step 테스트가 기본값 변경을 확인한다.
- Worker preset registry 테스트가 모든 built-in preset의 품질 파라미터를 확인한다.
- backend-api preset validation 테스트가 새 파라미터 기본값을 확인한다.
- `docs/worker/preset-spec.md`와 `docs/api/preprocess-preset-api.md`가 같은 contract를 설명한다.
