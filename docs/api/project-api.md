# 프로젝트 API

## 목적

프로젝트 API는 업로드된 문서 이미지와 전처리 Job을 묶는 작업 단위를 관리합니다.
이미지, 업로드, Job API는 프로젝트 멤버십을 기준으로 접근 권한을 확인합니다.

## 역할

| 역할 | 조회 | 프로젝트 수정 | 멤버 관리 | 프로젝트 삭제 |
| --- | --- | --- | --- | --- |
| `OWNER` | 가능 | 가능 | 가능 | 가능 |
| `EDITOR` | 가능 | 가능 | 불가 | 불가 |
| `VIEWER` | 가능 | 불가 | 불가 | 불가 |

## Endpoint

모든 endpoint는 `Authorization: Bearer <access-token>`이 필요합니다.

| Method | Path | 설명 |
| --- | --- | --- |
| `POST` | `/api/v1/projects` | 프로젝트 생성 |
| `GET` | `/api/v1/projects` | 현재 사용자의 프로젝트 목록 조회 |
| `GET` | `/api/v1/projects/{projectId}` | 프로젝트 상세 조회 |
| `PATCH` | `/api/v1/projects/{projectId}` | 프로젝트 메타데이터 수정 |
| `DELETE` | `/api/v1/projects/{projectId}` | 프로젝트 soft delete |
| `GET` | `/api/v1/projects/{projectId}/summary` | 프로젝트 요약 조회 |
| `POST` | `/api/v1/projects/{projectId}/members` | 프로젝트 멤버 초대 |
| `GET` | `/api/v1/projects/{projectId}/members` | 프로젝트 멤버 목록 조회 |
| `DELETE` | `/api/v1/projects/{projectId}/members/{userId}` | 프로젝트 멤버 제거 |

## 프로젝트 생성

요청:

```json
{
  "name": "Library Scan Batch",
  "description": "2026 first library document preprocessing batch",
  "defaultPreset": "LOW_CONTRAST_SCAN"
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
    "name": "Library Scan Batch",
    "description": "2026 first library document preprocessing batch",
    "defaultPreset": "LOW_CONTRAST_SCAN",
    "status": "ACTIVE",
    "ownerId": 1,
    "ownerEmail": "owner@example.com",
    "myRole": "OWNER",
    "createdAt": "2026-05-03T12:00:00",
    "updatedAt": "2026-05-03T12:00:00"
  }
}
```

## 프로젝트 목록

Spring pageable query를 사용합니다.

```text
GET /api/v1/projects?page=0&size=20
```

응답의 `result`는 `PageResponse<ProjectResponse>`입니다.

## 프로젝트 수정

`OWNER`와 `EDITOR`가 수정할 수 있습니다.

요청:

```json
{
  "name": "Updated Batch",
  "description": "Updated description",
  "defaultPreset": "A4_SCAN_300DPI"
}
```

## 프로젝트 삭제

`OWNER`만 삭제할 수 있습니다. 실제 row를 바로 삭제하지 않고 `ProjectStatus.DELETED`로 soft delete합니다.

## 멤버 초대

`OWNER`만 멤버를 초대할 수 있습니다. `OWNER` 역할은 이 API로 초대하지 않습니다.

요청:

```json
{
  "userId": 2,
  "role": "EDITOR"
}
```

## 프로젝트 요약

응답:

```json
{
  "isSuccess": true,
  "code": "common200",
  "message": "요청에 성공했습니다.",
  "result": {
    "projectId": 1,
    "name": "Library Scan Batch",
    "memberCount": 2,
    "imageCount": 15,
    "jobCount": 0
  }
}
```

`imageCount`는 삭제되지 않은 `Image` row 수입니다. `jobCount`는 프로젝트에 연결된 전처리 Job 수입니다.
