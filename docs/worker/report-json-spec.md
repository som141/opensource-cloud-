# Processing Report JSON Spec

## Purpose

`processing-report.json` records how the Worker transformed one document image before OCR. It is an operational and
debugging artifact, not an OCR text result.

## Object Key

```text
processed/{projectId}/{jobId}/{itemId}/processing-report.json
```

## Envelope

```json
{
  "schemaVersion": "1.0",
  "fileName": "processing-report.json",
  "report": {
    "jobId": 1,
    "itemId": 2,
    "presetName": "LOW_CONTRAST_SCAN",
    "steps": [],
    "timing": {},
    "memoryUsage": {},
    "fallbackSummary": {},
    "debugArtifacts": [],
    "success": true,
    "errorMessage": null
  }
}
```

## Step Report

Each step entry contains:

| Field | Meaning |
|---|---|
| `stepName` | Pipeline step enum such as `DESKEW` or `BINARIZATION` |
| `note` | Human-readable execution note |
| `timing.wallTime` | Step wall-clock duration |
| `timing.cpuTime` | Reserved CPU duration field, currently zero when not sampled |

## Fallback Summary

Fallback notes are emitted when a step intentionally chooses a safe fallback instead of failing the whole item.

Examples:

- Source DPI is missing, so DPI normalization is skipped.
- Unsupported binarization mode falls back to Otsu.
- Insufficient foreground points cause deskew or crop to skip.

## Current Limits

1. Memory usage is currently not sampled and remains `0`.
2. Debug artifact entries are metadata-only until real debug image generation is implemented.
3. OCR text, confidence, CER, WER, and recognized text are out of scope.
