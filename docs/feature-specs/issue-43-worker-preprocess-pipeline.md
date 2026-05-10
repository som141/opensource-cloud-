# Issue 43. Worker Preprocess Pipeline Step Skeleton

## Goal

Add the Worker-side preprocessing pipeline contract. This is not a simple resize worker. The pipeline explicitly models
document image preprocessing steps needed before OCR and keeps actual OpenCV implementation deferred to later tasks.

## Overall Order

1. Define `PreprocessStepName`.
2. Define `PreprocessStep`.
3. Add skeleton step classes for every required document preprocessing stage.
4. Add `PreprocessStepCatalog`.
5. Define `PreprocessPresetName`.
6. Add built-in preset definitions.
7. Add `PreprocessPresetRegistry`.
8. Add `PreprocessContext`.
9. Add `PreprocessPipeline`.
10. Add `PreprocessPipelineRunner`.
11. Connect `WorkerJobService` to execute the pipeline skeleton.
12. Keep Worker result as failed with `PIPELINE_NOT_IMPLEMENTED` until OpenCV and artifact integration exist.
13. Add unit tests for preset names, step catalog, execution order, and worker service boundary.
14. Document the Worker pipeline behavior.

## Functional Units

### Step Contract

Every step implements `PreprocessStep`:

- `name()`
- `execute(PreprocessContext context)`

Current steps record skeleton execution notes only. They do not process image bytes yet.

### Required Steps

The catalog registers these steps:

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

### Presets

The registry supports:

- `A4_SCAN_300DPI`
- `LOW_CONTRAST_SCAN`
- `RECEIPT`
- `NOISY_SCAN`
- `AUTO`

`AUTO` currently reserves the Worker-side selection boundary. Actual image feature analysis is deferred.

### Worker Service Boundary

`WorkerJobService` now:

1. Validates the message.
2. Reports started.
3. Prepares object storage download.
4. Executes the pipeline skeleton.
5. Reports `PIPELINE_NOT_IMPLEMENTED`.

The Worker intentionally does not report success because OpenCV processing and artifact upload are not implemented yet.

## Out Of Scope

- OpenCV `Mat` loading and memory management.
- image-test repository integration.
- Actual deskew, crop, denoise, contrast, binarization, morphology, DPI, or sharpen algorithms.
- Debug artifact generation.
- Processed image upload.
- Backend success callback.

## Verification

- `PreprocessStepCatalogTests` verifies every required step is registered.
- `PreprocessPresetRegistryTests` verifies required preset names and full document step sequence.
- `PreprocessPipelineRunnerTests` verifies execution order.
- `WorkerJobServiceTests` verifies the Worker executes the skeleton and still reports the not-implemented boundary.
