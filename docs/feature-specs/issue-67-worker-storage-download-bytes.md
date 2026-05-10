# Issue 67. Worker Object Storage Download Bytes

## Feature Summary

This task connects Worker object storage download to the preprocessing context. The Worker can now fetch original object
bytes through `ObjectStoragePort.downloadBytes` and pass them to `PreprocessContext.withSourceImageBytes` before running
the pipeline.

This enables the `DecodeStep` path from issue 65 to decode actual downloaded bytes when a real object exists in storage.

## Implemented Units

1. Worker-only MinIO SDK dependency: `io.minio:minio:9.0.0`
2. `ObjectStoragePort.downloadBytes`
3. `MinioObjectStorageClient.downloadBytes`
4. `ObjectStorageDownloadFailedException`
5. `WorkerJobService` download bytes -> `PreprocessContext.withSourceImageBytes`
6. Storage failure still maps to `STORAGE_DOWNLOAD_FAILED`
7. Pipeline failure still maps to `PIPELINE_EXECUTION_FAILED`
8. Worker service tests for download invocation and context byte propagation

## Runtime Flow

```text
WorkerJobService
  -> ObjectStoragePort.downloadBytes(originalObjectKey)
  -> PreprocessContext.fromMessage(message).withSourceImageBytes(bytes)
  -> PreprocessPipelineRunner.run(context)
  -> DecodeStep decodes bytes through ImageDecodePort
```

## Failure Behavior

If storage download fails, the Worker reports:

```text
STORAGE_DOWNLOAD_FAILED, retryable=true
```

If storage download succeeds but decode or a later pipeline step fails, the Worker reports:

```text
PIPELINE_EXECUTION_FAILED, retryable=true
```

## Out Of Scope

1. Processed image upload
2. Preview image upload
3. Processing report upload
4. Debug artifact upload
5. Worker success callback
6. OCR text extraction
