# Preprocess Preset API

## Purpose

Preprocess Preset API exposes the preprocessing parameter contract shared by the API server, Job creation flow, and
Worker. The API server validates preset names and parameters, but it does not execute OpenCV image preprocessing.

## Rules

- Built-in preset names must match Worker preset names.
- API server validates parameter shape and range before a Job is created.
- API server must not perform document preprocessing directly.
- Presets describe OCR-oriented document preprocessing, not simple thumbnail resizing.
- Custom presets are user-owned skeleton records derived from a built-in preset.

## Built-In Presets

| Name | Purpose |
| --- | --- |
| `A4_SCAN_300DPI` | General A4 document scan preset |
| `LOW_CONTRAST_SCAN` | Low contrast scan preset with stronger contrast normalization |
| `RECEIPT` | Narrow receipt-like document preset |
| `NOISY_SCAN` | Strong background noise scan preset |
| `AUTO` | Worker-side preset selection based on image characteristics |

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/v1/preprocess/presets` | List built-in presets |
| `GET` | `/api/v1/preprocess/presets/{presetName}` | Read built-in preset detail |
| `POST` | `/api/v1/preprocess/presets/validate` | Validate preset parameters |
| `POST` | `/api/v1/preprocess/presets/custom` | Create custom preset |
| `GET` | `/api/v1/preprocess/presets/custom` | List my custom presets |
| `DELETE` | `/api/v1/preprocess/presets/custom/{presetId}` | Delete my custom preset |

## List Built-In Presets

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

## Preset Detail

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
        "name": "targetDpi",
        "type": "INTEGER",
        "required": true,
        "defaultValue": "300",
        "minValue": "150",
        "maxValue": "600",
        "allowedValues": []
      }
    ]
  }
}
```

## Validate Parameters

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
      "targetDpi": "300",
      "maxDeskewAngle": "40.0",
      "binarizationMode": "otsu",
      "contrastClipLimit": "1.2",
      "sharpen": "false",
      "debugArtifacts": "false"
    }
  }
}
```

Invalid parameter values return `valid=false` with error messages in `result.errors`; unknown preset names return a
normal API error response.

## Custom Presets

Create request:

```json
{
  "name": "Library low contrast",
  "description": "For old book scans",
  "basePresetName": "LOW_CONTRAST_SCAN",
  "parameters": {
    "targetDpi": "300",
    "contrastClipLimit": "1.8"
  }
}
```

Custom presets are currently a backend skeleton. Job creation and Worker execution will connect custom presets in a
later task.

## Next Work

- Connect Job creation to preset validation.
- Add Worker internal preset lookup endpoint.
- Connect Worker preset registry to this API contract.
