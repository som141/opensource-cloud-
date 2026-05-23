# 업로드 API

## 목적

업로드 API는 대용량 문서 이미지 파일을 Spring API 서버가 직접 받지 않도록 준비합니다.
클라이언트는 업로드 세션을 만들고, presigned URL을 받은 뒤 Object Storage에 직접 업로드하고, 마지막에 업로드 완료를 API에 알립니다.

## 규칙

- Spring API는 큰 이미지 파일 본문을 직접 받지 않습니다.
- presigned upload URL에는 만료 시간이 있어야 합니다.
- 업로드 완료 시 `ObjectStoragePort`로 object 존재 여부를 검증합니다.
- 업로드 완료 시 저장된 object의 magic number를 파일명, content type과 비교합니다.
- 업로드 완료 시 가능한 경우 원본 이미지의 width, height, DPI를 추출합니다.
- 이미지 row는 업로드 완료 검증 후에만 확정합니다.
- 업로드 세션 접근은 생성자만이 아니라 프로젝트 멤버십으로 제어합니다.
- 지원 확장자는 `png`, `jpg`, `jpeg`, `tif`, `tiff`, `bmp`, `webp`입니다.
- checksum은 SHA-256 hex 문자열로 받고 중복 감지에 사용합니다.
- 로컬 프론트 MVP에서는 ZIP을 브라우저에서 풀고, 풀린 이미지 항목을 일반 업로드 API로 처리합니다.

## Endpoint

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/api/v1/projects/{projectId}/upload-sessions` | 업로드 세션 생성 |
| `GET` | `/api/v1/upload-sessions/{sessionId}` | 업로드 세션 조회 |
| `POST` | `/api/v1/upload-sessions/{sessionId}/files/presigned-url` | presigned upload URL 발급 |
| `POST` | `/api/v1/upload-sessions/{sessionId}/complete` | 업로드 세션 완료 |
| `DELETE` | `/api/v1/upload-sessions/{sessionId}` | 업로드 세션 취소 |

## 업로드 세션 생성

요청:

```json
{
  "expectedFileCount": 3,
  "expectedTotalSizeBytes": 12000000
}
```

응답:

```json
{
  "isSuccess": true,
  "code": "common201",
  "message": "생성되었습니다.",
  "result": {
    "id": 1,
    "projectId": 10,
    "userId": 20,
    "status": "CREATED",
    "expectedFileCount": 3,
    "expectedTotalSizeBytes": 12000000,
    "completedAt": null,
    "cancelledAt": null
  }
}
```

## presigned URL 발급

요청:

```json
{
  "files": [
    {
      "fileName": "scan_001.png",
      "contentType": "image/png",
      "sizeBytes": 4200000,
      "checksumSha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    }
  ]
}
```

응답:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "요청에 성공했습니다.",
  "result": {
    "sessionId": 1,
    "uploadTargets": [
      {
        "uploadFileId": 100,
        "objectKey": "originals/10/1/uuid/scan_001.png",
        "uploadUrl": "http://localhost:9000/local-presigned/originals/10/1/uuid/scan_001.png",
        "expiresAt": "2026-05-03T08:00:00Z",
        "requiredHeaders": {
          "Content-Type": "image/png"
        }
      }
    ]
  }
}
```

응답은 `imageId`가 아니라 `uploadFileId`를 사용합니다. 이미지 메타데이터 row는 Object Storage 검증이 끝난 뒤 확정되기 때문입니다.

같은 세션에서 presigned URL을 반복 발급해도 세션이 선언한 예상 파일 수와 총 크기를 넘을 수 없습니다.

## 업로드 세션 완료

요청:

```json
{
  "uploadFileIds": [100, 101, 102]
}
```

응답:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "요청에 성공했습니다.",
  "result": {
    "sessionId": 1,
    "status": "COMPLETED",
    "uploadedFileCount": 3
  }
}
```

완료 검증 순서:

1. 요청된 upload file ID 수가 세션의 예상 파일 수와 맞아야 합니다.
2. 각 object key가 Object Storage에 존재해야 합니다.
3. API가 `ObjectStoragePort`로 원본 object를 다운로드합니다.
4. API가 이미지 magic number를 검증합니다.
5. 감지된 이미지 타입이 원본 확장자와 content type과 맞아야 합니다.
6. 가능한 경우 width, height, DPI 메타데이터를 추출합니다.
7. 검증 후 upload file을 `UPLOADED`로 바꾸고 image row를 생성합니다.

지원 signature:

| 포맷 | 확장자 | Content type |
| --- | --- | --- |
| PNG | `.png` | `image/png` |
| JPEG | `.jpg`, `.jpeg` | `image/jpeg` |
| WEBP | `.webp` | `image/webp` |
| BMP | `.bmp` | `image/bmp`, `image/x-ms-bmp` |
| TIFF | `.tif`, `.tiff` | `image/tiff` |

## 상태

| 상태 | 의미 |
| --- | --- |
| `CREATED` | 세션은 있지만 아직 upload URL이 발급되지 않음 |
| `UPLOAD_URL_ISSUED` | 하나 이상의 presigned upload URL이 발급됨 |
| `COMPLETED` | 모든 예상 파일이 Object Storage에서 검증됨 |
| `CANCELLED` | 사용자가 업로드 세션을 취소함 |

## 후속 작업

- 로컬 presigned URL generator를 MinIO/S3 adapter로 교체합니다.
- 브라우저 ZIP 해제가 예상 batch 크기에서 느려질 경우에만 서버 측 ZIP 해제를 별도 검토합니다.
