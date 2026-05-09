# Worker Preset Spec

## Purpose

This document defines the preset names and parameter contract that the Worker must support. The backend API validates
these names and parameter ranges before publishing preprocessing jobs.

## Required Preset Names

Worker preset names must exactly match backend names:

- `A4_SCAN_300DPI`
- `LOW_CONTRAST_SCAN`
- `RECEIPT`
- `NOISY_SCAN`
- `AUTO`

## Required Pipeline Steps

Except `AUTO`, built-in document presets describe the following OCR-oriented processing sequence:

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

These steps are not a simple resize pipeline. `DPI_NORMALIZE` is for OCR-oriented DPI normalization and must not be
implemented as a thumbnail resize shortcut.

## Common Parameters

| Parameter | Type | Range/Values | Meaning |
| --- | --- | --- | --- |
| `targetDpi` | integer | `150` to `600` | Target DPI for OCR normalization |
| `maxDeskewAngle` | decimal | `0.0` to `45.0` | Maximum allowed deskew angle |
| `binarizationMode` | enum | `otsu`, `adaptive` | Binarization strategy |
| `contrastClipLimit` | decimal | `1.0` to `4.0` | Contrast normalization strength |
| `sharpen` | boolean | `true`, `false` | Whether optional sharpen step should run |
| `debugArtifacts` | boolean | `true`, `false` | Whether to save intermediate debug images |

## Preset Defaults

| Preset | targetDpi | binarizationMode | contrastClipLimit | sharpen |
| --- | --- | --- | --- | --- |
| `A4_SCAN_300DPI` | `300` | `otsu` | `1.2` | `false` |
| `LOW_CONTRAST_SCAN` | `300` | `adaptive` | `1.6` | `true` |
| `RECEIPT` | `300` | `adaptive` | `1.4` | `true` |
| `NOISY_SCAN` | `300` | `adaptive` | `1.5` | `false` |

`AUTO` supports `debugArtifacts` and delegates final preset selection to the Worker based on image characteristics.

## Backend Contract

- Backend exposes preset list/detail through `/api/v1/preprocess/presets`.
- Backend validates parameters through `/api/v1/preprocess/presets/validate`.
- Backend does not execute OpenCV processing.
- Worker must reject messages with unknown preset names or invalid parameter payloads.

## Future Worker Work

- Implement Worker-side `PreprocessPresetRegistry`.
- Map backend preset names to Worker pipeline step parameters.
- Add `AutoPresetSelector` based on image characteristics.
- Add Worker tests that compare supported preset names with backend documentation.
