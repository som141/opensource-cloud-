# Preprocess Preset API

## 목적

Preprocess Preset API는 Job 생성 전에 사용할 수 있는 전처리 프리셋과 파라미터 범위를 제공한다.
API 서버는 파라미터 이름과 범위를 검증하지만 OpenCV 이미지 전처리는 직접 수행하지 않는다.
실제 처리는 RabbitMQ 메시지를 받은 Worker가 수행한다.

## 규칙

- built-in preset 이름은 Worker preset 이름과 일치해야 한다.
- Job 생성 시 API 서버는 프리셋 파라미터를 검증하고 기본값을 채운다.
- 검증된 파라미터는 Job에 저장되고 RabbitMQ 메시지로 Worker에 전달된다.
- API 서버는 문서 이미지 전처리를 직접 수행하지 않는다.
- 이 API는 단순 resize가 아니라 OCR 전 문서 이미지 전처리 파이프라인의 계약을 설명한다.

## Built-in Preset

| 이름 | 용도 |
| --- | --- |
| `A4_SCAN_300DPI` | 일반 A4 300 DPI 스캔 문서 |
| `LOW_CONTRAST_SCAN` | 저대비 스캔 문서 |
| `RECEIPT` | 영수증처럼 폭이 좁은 문서 |
| `NOISY_SCAN` | 배경 노이즈가 강한 문서 |
| `AUTO` | Worker가 이미지 특성에 따라 프리셋 선택 |

## Endpoint

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/api/v1/preprocess/presets` | built-in preset 목록 조회 |
| `GET` | `/api/v1/preprocess/presets/{presetName}` | built-in preset 상세 조회 |
| `POST` | `/api/v1/preprocess/presets/validate` | preset 파라미터 검증 |
| `POST` | `/api/v1/preprocess/presets/custom` | custom preset 생성 |
| `GET` | `/api/v1/preprocess/presets/custom` | 내 custom preset 목록 조회 |
| `DELETE` | `/api/v1/preprocess/presets/custom/{presetId}` | custom preset 삭제 |

## Preset 상세 응답 예시

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "Request succeeded.",
  "result": {
    "name": "A4_SCAN_300DPI",
    "type": "BUILT_IN",
    "displayName": "A4 300 DPI scan",
    "description": "General A4 document scan preset for OCR preprocessing.",
    "supportsDebug": true,
    "steps": [
      "DECODE",
      "COLOR_NORMALIZE",
      "ORIENTATION_NORMALIZE",
      "DESKEW",
      "CROP",
      "DENOISE",
      "CONTRAST_NORMALIZE",
      "BINARIZATION",
      "MORPHOLOGY_CLEANUP",
      "DPI_NORMALIZE",
      "OPTIONAL_SHARPEN"
    ],
    "parameters": [
      {
        "name": "contrastClipLimit",
        "type": "DECIMAL",
        "required": true,
        "defaultValue": "2.5",
        "minValue": "1.0",
        "maxValue": "4.0",
        "allowedValues": []
      }
    ]
  }
}
```

## 파라미터 검증

Request:

```json
{
  "presetName": "A4_SCAN_300DPI",
  "parameters": {
    "targetDpi": "300",
    "binarizationMode": "otsu",
    "debugArtifacts": "false"
  }
}
```

Response:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "Request succeeded.",
  "result": {
    "presetName": "A4_SCAN_300DPI",
    "valid": true,
    "errors": [],
    "resolvedParameters": {
      "grayscale": "true",
      "targetDpi": "300",
      "referenceWidthInches": "8.27",
      "referenceHeightInches": "11.69",
      "fallbackSourceDpi": "300",
      "maxDeskewAngle": "40.0",
      "binarizationMode": "otsu",
      "adaptiveBlockSize": "31",
      "adaptiveC": "15.0",
      "contrastNormalize": "false",
      "contrastClipLimit": "2.5",
      "contrastTileGridSize": "8",
      "denoiseMode": "median",
      "denoiseKernelSize": "3",
      "denoiseDiameter": "7",
      "denoiseSigmaColor": "50.0",
      "denoiseSigmaRange": "50.0",
      "morphologyMode": "open",
      "morphologyKernelSize": "2",
      "sharpen": "false",
      "sharpenAmount": "0.25",
      "sharpenSigma": "1.2",
      "debugArtifacts": "false"
    }
  }
}
```

## 주요 기본값

| 프리셋 | binarizationMode | contrastNormalize | contrastClipLimit | denoiseMode | morphologyMode | sharpen |
| --- | --- | --- | --- | --- | --- | --- |
| `A4_SCAN_300DPI` | `otsu` | `false` | `2.5` | `median` | `open` | `false` |
| `LOW_CONTRAST_SCAN` | `adaptive` | `true` | `2.5` | `median` | `close` | `true` |
| `RECEIPT` | `adaptive` | `true` | `2.5` | `median` | `close` | `true` |
| `NOISY_SCAN` | `adaptive` | `false` | `2.5` | `bilateral` | `open` | `false` |

공통 기본값은 `grayscale=true`, `fallbackSourceDpi=300`, `adaptiveBlockSize=31`, `adaptiveC=15.0`,
`denoiseDiameter=7`, `denoiseSigmaColor=50.0`, `denoiseSigmaRange=50.0`,
`morphologyKernelSize=2`, `sharpenAmount=0.25`, `sharpenSigma=1.2`다.

## 오류

알 수 없는 파라미터나 범위를 벗어난 값은 `valid=false`와 `result.errors`로 반환된다.
존재하지 않는 preset 이름은 공통 실패 응답으로 반환된다.

## 다음 작업

- custom preset 실제 저장/조회 기능은 별도 작업에서 구현한다.
- Worker registry와 API registry의 기본값이 달라지지 않도록 테스트를 유지한다.
