# 이미지 API

## 목적

이미지 API는 업로드 완료 후 확정된 이미지 메타데이터를 관리합니다.
프로젝트 이미지 목록, 이미지 상세, soft delete, 원본/처리 결과/preview 다운로드 URL, 처리 리포트, debug artifact 조회를 제공합니다.

## 규칙

- `Image` row는 업로드 완료 검증 이후에만 생성합니다.
- Spring API는 공개 Object Storage URL을 직접 노출하지 않습니다.
- 다운로드 endpoint는 `PresignedDownloadUrlGenerator`를 통해 임시 signed URL을 반환합니다.
- 원본 파일과 전처리 결과 artifact는 `ImageArtifact`로 분리해 저장합니다.
- 접근 권한은 `ProjectPermissionService`의 프로젝트 멤버십 기준으로 확인합니다.
- 이미지 삭제는 soft delete이며 Object Storage object를 즉시 삭제하지 않습니다.
- 업로드 완료 시 원본 width, height, DPI를 가능한 범위에서 추출합니다.

## Endpoint

| Method | Path | 설명 |
| --- | --- | --- |
| `GET` | `/api/v1/projects/{projectId}/images` | 프로젝트 이미지 목록 |
| `GET` | `/api/v1/images/{imageId}` | 이미지 상세 |
| `DELETE` | `/api/v1/images/{imageId}` | 이미지 soft delete |
| `GET` | `/api/v1/images/{imageId}/download?type=original` | 원본 다운로드 URL |
| `GET` | `/api/v1/images/{imageId}/download?type=processed` | 처리 결과 다운로드 URL |
| `GET` | `/api/v1/images/{imageId}/download?type=preview` | preview 다운로드 URL |
| `GET` | `/api/v1/images/{imageId}/report` | 처리 리포트 다운로드 URL |
| `GET` | `/api/v1/images/{imageId}/debug-artifacts` | debug artifact 다운로드 URL 목록 |

## 이미지 목록

응답:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "요청에 성공했습니다.",
  "result": {
    "content": [
      {
        "id": 200,
        "projectId": 10,
        "originalFileName": "scan_001.png",
        "contentType": "image/png",
        "sizeBytes": 1024,
        "format": "PNG",
        "status": "UPLOADED",
        "createdAt": "2026-05-09T21:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false
  }
}
```

## 이미지 상세

응답:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "요청에 성공했습니다.",
  "result": {
    "id": 200,
    "projectId": 10,
    "uploadSessionId": 1,
    "uploadSessionFileId": 100,
    "uploaderId": 20,
    "originalFileName": "scan_001.png",
    "originalObjectKey": "originals/10/1/file/scan_001.png",
    "contentType": "image/png",
    "sizeBytes": 1024,
    "checksumSha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    "format": "PNG",
    "status": "UPLOADED",
    "width": 1240,
    "height": 1754,
    "dpiX": 300,
    "dpiY": 300,
    "createdAt": "2026-05-09T21:00:00"
  }
}
```

업로드 파일이 신뢰할 수 있는 크기나 DPI 정보를 header에 제공하지 않으면 메타데이터 값은 `null`일 수 있습니다. magic number 검증이 성공했다면 메타데이터 추출 실패는 치명 오류로 보지 않습니다.

## 다운로드 URL

요청:

```text
GET /api/v1/images/200/download?type=original
```

지원 `type`:

- `original`
- `processed`
- `preview`

응답:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "요청에 성공했습니다.",
  "result": {
    "imageId": 200,
    "type": "ORIGINAL",
    "objectKey": "originals/10/1/file/scan_001.png",
    "downloadUrl": "http://localhost:9000/local-download/originals/10/1/file/scan_001.png",
    "expiresAt": "2026-05-09T12:10:00Z",
    "requiredHeaders": {}
  }
}
```

## 리포트와 debug artifact

- `/report`는 최신 `PROCESSING_REPORT` artifact 다운로드 URL을 반환합니다.
- `/debug-artifacts`는 이미지의 모든 `DEBUG` artifact 다운로드 URL을 반환합니다.
- debug artifact는 Worker/report 통합 이후 생성되며, 생성 전에는 빈 결과일 수 있습니다.

## 업로드 완료 연동

`POST /api/v1/upload-sessions/{sessionId}/complete`가 성공하면 다음 순서로 image row가 생성됩니다.

1. API가 원본 object 존재 여부를 확인합니다.
2. API가 각 원본 object를 다운로드하고 magic number를 검증합니다.
3. byte signature가 선언된 확장자나 content type과 맞지 않으면 거부합니다.
4. PNG, JPEG, WEBP, BMP, TIFF header에서 width, height, DPI를 추출합니다.
5. upload file을 `UPLOADED`로 변경합니다.
6. upload session을 `COMPLETED`로 변경합니다.
7. 업로드 파일마다 `Image` row를 생성합니다.
8. 이미지마다 `ORIGINAL` `ImageArtifact` row를 생성합니다.

`Image` row는 `uploadSessionFileId` 기준으로 멱등 처리하므로 내부 재시도 시 중복 생성되지 않습니다.

## 후속 작업

- 로컬 download URL generator를 MinIO/S3 adapter로 교체합니다.
- Worker internal artifact 등록으로 processed, preview, report, debug 파일을 연결합니다.
