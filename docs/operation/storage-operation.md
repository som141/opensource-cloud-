# Storage Operation

## Purpose

Object storage keeps original document images and later preprocessing artifacts. The API server should issue
temporary access URLs, while workers and storage adapters handle actual object transfer.

## Buckets And Privacy

- Buckets must be private by default.
- Original images must not be publicly readable.
- Processed images, previews, debug artifacts, and reports must be accessed through signed download URLs.
- Object storage credentials must stay in backend/worker environment variables or Kubernetes secrets.

## Original Upload Path

```text
originals/{projectId}/{uploadSessionId}/{uploadFileToken}/{originalFileName}
```

Example:

```text
originals/10/1/550e8400-e29b-41d4-a716-446655440000/scan_001.png
```

`uploadFileToken` is not the final image ID. It is an upload-time token used before image metadata is finalized.

## Processed Artifact Path

```text
processed/{projectId}/{jobId}/{itemId}/processed.png
processed/{projectId}/{jobId}/{itemId}/preview.png
processed/{projectId}/{jobId}/{itemId}/processing-report.json
processed/{projectId}/{jobId}/{itemId}/debug/{step}.png
```

## Presigned Upload Flow

1. Client creates an upload session through Spring API.
2. Client sends file metadata and checksum to Spring API.
3. Spring API validates metadata and creates object keys.
4. Spring API returns presigned upload URLs with expiration timestamps.
5. Client uploads file bodies directly to object storage.
6. Client calls upload complete.
7. Spring API verifies object existence through `ObjectStoragePort`.
8. Image domain creates `Image` and `ORIGINAL` `ImageArtifact` rows from completed upload files.

## Local MinIO Adapter

The local backend uses the same MinIO bucket as the Worker:

- `MinioObjectStorageAdapter`
- `StorageProperties`

The adapter signs upload/download URLs with `storage.public-endpoint` so browser uploads can use `localhost`, while
object existence checks use `storage.endpoint` so backend-api can reach MinIO inside Docker.

For local browser uploads, MinIO must also allow the frontend origin:

```text
MINIO_API_CORS_ALLOW_ORIGIN=http://localhost,http://localhost:5173
```

The local smoke flow verifies presigned PUT CORS with an `OPTIONS` preflight before uploading the image body.

## Required Environment Variables For Real Adapter

```text
MINIO_ENDPOINT
MINIO_PUBLIC_ENDPOINT
MINIO_ACCESS_KEY
MINIO_SECRET_KEY
MINIO_BUCKET
MINIO_REGION
```

For S3-compatible production storage, equivalent endpoint, region, access key, secret key, and bucket values are
required.

## Worker Download Flow

The Worker uses `ObjectStoragePort.downloadBytes` to read the original object before running the preprocessing pipeline:

```text
originalObjectKey
  -> ObjectStoragePort.downloadBytes
  -> PreprocessContext.withSourceImageBytes
  -> DecodeStep
```

Download failures are reported as `STORAGE_DOWNLOAD_FAILED` and remain retryable. Decode failures after a successful
download are reported as `PIPELINE_EXECUTION_FAILED`.

## Operational Checks

- Presigned URL expiration should be short, usually 10 to 30 minutes.
- `client_max_body_size` in NGINX is not the main protection for presigned upload because the file body bypasses
  Spring API.
- Object keys should include project and session identifiers for cleanup and audit.
- Cleanup jobs should remove cancelled or expired upload session objects.
- Debug artifacts should be disabled by default because they increase storage usage.
