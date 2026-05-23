# Object Storage 운영

## 목적

Object Storage는 원본 문서 이미지와 전처리 결과물을 저장합니다.
API 서버는 임시 접근 URL을 발급하고, 실제 파일 전송은 브라우저, Worker, storage adapter가 수행합니다.

## Bucket과 공개 범위

- Bucket은 기본적으로 private이어야 합니다.
- 원본 이미지는 공개 읽기를 허용하지 않습니다.
- 처리된 이미지, preview, debug artifact, report는 signed download URL을 통해 접근합니다.
- Object Storage credential은 backend/worker 환경변수 또는 Kubernetes secret에만 둡니다.

## 원본 업로드 경로

```text
originals/{projectId}/{uploadSessionId}/{uploadFileToken}/{originalFileName}
```

예시:

```text
originals/10/1/550e8400-e29b-41d4-a716-446655440000/scan_001.png
```

`uploadFileToken`은 최종 image ID가 아닙니다.
이미지 메타데이터가 확정되기 전 업로드 단계에서 사용하는 임시 token입니다.

## 처리 결과 경로

```text
processed/{projectId}/{jobId}/{itemId}/processed.png
processed/{projectId}/{jobId}/{itemId}/preview.png
processed/{projectId}/{jobId}/{itemId}/processing-report.json
processed/{projectId}/{jobId}/{itemId}/debug/{step}.png
```

사용자 화면과 다운로드 API는 기본적으로 `processed.png`와 processed-only ZIP을 중심으로 제공합니다.
preview/report/debug는 운영 분석과 디버깅용 artifact로 유지합니다.

## Presigned upload 흐름

1. Client가 Spring API로 upload session을 생성합니다.
2. Client가 파일 metadata와 checksum을 Spring API에 보냅니다.
3. Spring API가 metadata를 검증하고 object key를 생성합니다.
4. Spring API가 만료 시간이 있는 presigned upload URL을 반환합니다.
5. Client가 파일 body를 Object Storage로 직접 업로드합니다.
6. Client가 upload complete API를 호출합니다.
7. Spring API가 `ObjectStoragePort`로 object 존재 여부를 확인합니다.
8. Image 도메인이 완료된 업로드 파일에서 `Image`와 `ORIGINAL` `ImageArtifact` row를 생성합니다.

## 로컬 MinIO adapter

로컬 backend와 Worker는 같은 MinIO bucket을 사용합니다.

- `MinioObjectStorageAdapter`
- `StorageProperties`

Adapter는 업로드/다운로드 signed URL에는 `storage.public-endpoint`를 사용합니다.
브라우저에서 접근 가능한 endpoint를 반환하기 위해서입니다.
반면 backend-api 내부의 object existence check는 `storage.endpoint`를 사용해 Docker network 안의 MinIO에 접근합니다.

Docker Compose local 모드에서는 `MINIO_PUBLIC_ENDPOINT`를 `http://localhost`로 두고 NGINX가 bucket path를 MinIO로 proxy합니다.

```text
Browser
  -> http://localhost/image-preprocess-local/{objectKey}
  -> NGINX
  -> minio:9000
```

이 구조는 로컬 수동 테스트에서 CORS와 blocked-port 문제를 줄입니다.
MinIO console/API debugging을 위해 `http://localhost:9000`도 함께 노출합니다.

브라우저 업로드를 위해 MinIO CORS origin도 허용해야 합니다.

```text
MINIO_API_CORS_ALLOW_ORIGIN=http://localhost,http://localhost:5173,http://127.0.0.1,http://127.0.0.1:5173
```

로컬 smoke flow는 이미지 body를 올리기 전에 presigned PUT CORS `OPTIONS` preflight를 검증합니다.

## 실제 adapter에 필요한 환경변수

```text
MINIO_ENDPOINT
MINIO_PUBLIC_ENDPOINT
MINIO_ACCESS_KEY
MINIO_SECRET_KEY
MINIO_BUCKET
MINIO_REGION
```

S3 호환 운영 storage를 사용할 때도 endpoint, region, access key, secret key, bucket 값이 필요합니다.

## Worker 다운로드 흐름

Worker는 전처리 파이프라인 실행 전에 `ObjectStoragePort.downloadBytes`로 원본 object를 읽습니다.

```text
originalObjectKey
  -> ObjectStoragePort.downloadBytes
  -> PreprocessContext.withSourceImageBytes
  -> DecodeStep
```

다운로드 실패는 `STORAGE_DOWNLOAD_FAILED`로 보고하며 retry 가능한 오류로 취급합니다.
다운로드 이후 decode 실패는 `PIPELINE_EXECUTION_FAILED`로 보고합니다.

## 운영 점검 항목

- Presigned URL 만료 시간은 짧게 유지합니다. 일반적으로 10분에서 30분을 사용합니다.
- Presigned upload는 Spring API를 우회하므로 `client_max_body_size`만으로 업로드 보호가 끝나지 않습니다.
- Object key에는 project와 session 식별자를 포함해 정리와 감사 추적이 가능해야 합니다.
- 취소되었거나 만료된 upload session object를 정리하는 cleanup job이 필요합니다.
- Debug artifact는 저장 용량을 크게 늘리므로 기본 비활성화가 안전합니다.
