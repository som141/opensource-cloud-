# Preprocess Preset API

## 목적

Preprocess Preset API는 backend-api, Job 생성 흐름, Worker가 공유하는 전처리 파라미터 계약을 제공합니다.
API 서버는 프리셋 이름과 파라미터 범위를 검증하지만 OpenCV 이미지 전처리를 직접 수행하지 않습니다.

## 규칙

- built-in preset 이름은 Worker preset 이름과 정확히 일치해야 합니다.
- API 서버는 Job 생성 전에 파라미터 형태와 범위를 검증합니다.
- API 서버는 문서 이미지 전처리를 직접 수행하지 않습니다.
- 프리셋은 단순 썸네일 resize가 아니라 OCR용 문서 전처리 파이프라인을 설명합니다.
- custom preset은 built-in preset에서 파생되는 사용자 소유 skeleton record입니다.

## Built-in preset

| 이름 | 용도 |
| --- | --- |
| `A4_SCAN_300DPI` | 일반 A4 문서 스캔 |
| `LOW_CONTRAST_SCAN` | 저대비 문서 |
| `RECEIPT` | 영수증처럼 폭이 좁은 문서 |
| `NOISY_SCAN` | 배경 노이즈가 강한 문서 |
| `AUTO` | Worker가 이미지 특성에 따라 프리셋 선택 |

## Endpoint

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/api/v1/preprocess/presets` | built-in preset 목록 |
| `GET` | `/api/v1/preprocess/presets/{presetName}` | built-in preset 상세 |
| `POST` | `/api/v1/preprocess/presets/validate` | preset 파라미터 검증 |
| `POST` | `/api/v1/preprocess/presets/custom` | custom preset 생성 |
| `GET` | `/api/v1/preprocess/presets/custom` | 내 custom preset 목록 |
| `DELETE` | `/api/v1/preprocess/presets/custom/{presetId}` | custom preset 삭제 |

## built-in preset 목록

Response:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "Request succeeded.",
  "result": [
    {
      "name": "A4_SCAN_300DPI",
      "type": "BUILT_IN",
      "displayName": "A4 300 DPI scan",
      "description": "General A4 document scan preset for OCR preprocessing.",
      "supportsDebug": true
    }
  ]
}
```

## preset 상세

Response:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "Request succeeded.",
  "result": {
    "name": "LOW_CONTRAST_SCAN",
    "type": "BUILT_IN",
    "displayName": "Low contrast scan",
    "description": "Improves low contrast scans using stronger contrast normalization before binarization.",
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
        "defaultValue": "2.4",
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
    "contrastClipLimit": "2.0",
    "denoiseMode": "median",
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
      "targetDpi": "300",
      "maxDeskewAngle": "40.0",
      "binarizationMode": "otsu",
      "adaptiveBlockSize": "21",
      "adaptiveC": "5.0",
      "contrastClipLimit": "2.0",
      "contrastTileGridSize": "8",
      "denoiseMode": "median",
      "denoiseKernelSize": "3",
      "denoiseDiameter": "5",
      "denoiseSigmaColor": "25.0",
      "denoiseSigmaRange": "75.0",
      "morphologyMode": "open_close",
      "morphologyKernelSize": "2",
      "sharpen": "false",
      "sharpenAmount": "0.8",
      "sharpenSigma": "1.5",
      "debugArtifacts": "false"
    }
  }
}
```

잘못된 파라미터 값은 `valid=false`와 `result.errors`로 반환합니다. 알 수 없는 preset 이름은 일반 API 오류 응답을 반환합니다.

## custom preset

Create request:

```json
{
  "name": "Library low contrast",
  "description": "For old book scans",
  "basePresetName": "LOW_CONTRAST_SCAN",
  "parameters": {
    "targetDpi": "300",
    "contrastClipLimit": "2.4",
    "adaptiveBlockSize": "21",
    "adaptiveC": "5.0"
  }
}
```

custom preset은 현재 backend skeleton입니다. Job 생성과 Worker 실행 연결은 후속 작업에서 처리합니다.

## 다음 작업

- Job 생성 시 preset validation 연결 상태를 계속 유지합니다.
- Worker internal preset lookup endpoint가 필요하면 별도 이슈에서 추가합니다.
- Worker preset registry와 API contract가 다른 값으로 갈라지지 않도록 테스트를 유지합니다.
