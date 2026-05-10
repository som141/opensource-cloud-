# Issue 65. Worker DecodeStep Image Decode

## Feature Summary

This task connects the Worker `DecodeStep` to the OpenCV image codec boundary added in issue 63.

The Worker still does not run the full document preprocessing pipeline end to end. This issue only adds the source image
bytes boundary, decoded `ImageMatHolder` ownership in `PreprocessContext`, and release-on-pipeline-finish behavior.

## Implemented Units

1. `ImageDecodePort` domain port
2. `ImageCodecAdapter implements ImageDecodePort`
3. `PreprocessContext.withSourceImageBytes`
4. `PreprocessContext.storeDecodedImage`
5. `PreprocessContext.releaseDecodedImage`
6. `DecodeStep` calls `ImageDecodePort.decode` when source bytes are attached
7. `PreprocessPipelineRunner` releases decoded holder after pipeline completion or failure
8. Unit tests for decode success, missing bytes deferral, unsupported bytes failure, and runner cleanup

## Decode Behavior

When source image bytes are attached:

```java
PreprocessContext context = PreprocessContext.fromMessage(message)
    .withSourceImageBytes(imageBytes);
```

`DecodeStep` decodes the bytes through `ImageDecodePort`, stores the returned `ImageMatHolder` in context, and records
the decoded width, height, and color space in the step note.

When source image bytes are not attached, `DecodeStep` records that decode is waiting for storage download and does not
fail the skeleton pipeline. This keeps the current Worker runtime compatible until real Object Storage download is
implemented.

When invalid bytes are attached, OpenCV decode failure is propagated as a pipeline step failure.

## Resource Ownership

`PreprocessContext` owns the decoded holder during pipeline execution. `PreprocessPipelineRunner` releases the holder in
`finally` so future steps can use the Mat during the run without leaking native memory afterward.

## Out Of Scope

1. Real Object Storage download
2. Processed image upload
3. Preview image upload
4. Debug image upload
5. Color normalization and downstream OpenCV step mutation
6. OCR text extraction
