# Issue 59. Worker Pipeline Timing And Failure Hook

## Feature Summary

This task adds execution metadata to the Worker preprocessing pipeline. It prepares the pipeline for actual OpenCV
implementation by recording per-step timing, fallback notes, and failed-step metadata.

## Implemented Units

1. `PreprocessStepExecution` now records:
   - step name
   - note
   - startedAt
   - endedAt
   - wallTime
   - success
   - errorMessage
2. `PreprocessContext` collects pending step notes, completed step executions, and fallback notes.
3. `PreprocessPipelineRunner` measures per-step and total wall time.
4. `PreprocessPipelineRunner` stops on the first failed step and returns a failed `PreprocessResult`.
5. `PreprocessResult` exposes `wallTime`, `success`, `errorMessage`, and `fallbackNotes`.
6. `ProcessingReportFactory` maps result timing and fallback data into report DTOs.
7. `WorkerJobService` reports failed pipeline results as `PIPELINE_EXECUTION_FAILED`.

## Runtime Behavior

```text
PreprocessPipelineRunner.run
  -> resolve preset
  -> build pipeline
  -> for each step
       -> capture startedAt and nano start
       -> execute step
       -> consume step note
       -> record success execution
       -> on RuntimeException, record failed execution and stop
  -> return PreprocessResult
```

## Failure Contract

If a step throws an exception:

1. The failed step is recorded with `success=false`.
2. `PreprocessResult.success=false`.
3. `PreprocessResult.errorMessage` contains the exception message.
4. `WorkerJobService` reports `PIPELINE_EXECUTION_FAILED` to backend-api.
5. The failure is retryable at the Worker result level.

## Out Of Scope

1. Actual OpenCV implementation
2. Real image decode
3. Object upload
4. Debug artifact image files
5. OCR text extraction
