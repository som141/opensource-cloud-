# Issue 63. Worker OpenCV Loader And Image Codec Adapter

## Feature Summary

This task adds the Worker runtime boundary required before replacing preprocessing skeleton steps with real OpenCV
document-image operations.

The scope is limited to native OpenCV loading, image byte decoding, Mat holder metadata, and Mat cleanup. It does not
replace `DecodeStep` yet and does not add OCR text extraction.

## Implemented Units

1. Worker-only OpenCV dependency: `org.openpnp:opencv:4.9.0-0`
2. `OpenCvLoader` idempotent native loader
3. `ImageCodecAdapter.decode`
4. `ImageMatHolder` with real OpenCV `Mat`, dimensions, color space, and release state
5. `MatResourceCleaner` Mat release hook
6. Decode failure exception for empty or unsupported image bytes
7. Unit tests for valid PNG decode, empty bytes, unsupported bytes, cleanup, and loader idempotency

## Runtime Boundary

The adapter accepts source object key plus image bytes and returns an `ImageMatHolder`:

```java
ImageMatHolder holder = imageCodecAdapter.decode(objectKey, imageBytes);
```

The holder owns an OpenCV `Mat`. The caller must release it through one of these paths:

```java
holder.release();
holder.close();
matResourceCleaner.release(holder);
```

## Out Of Scope

1. Object Storage download
2. Replacing `DecodeStep`
3. Color normalization
4. Deskew/crop/denoise/binarization/morphology/DPI/sharpen implementations
5. Processed image upload
6. OCR text extraction
