# 이슈 67. Object Storage 다운로드 연결

## 목적

Worker가 Object Storage에서 원본 이미지를 다운로드해 pipeline에 전달합니다.

## 작업 범위

1. `ObjectStoragePort.downloadBytes`
2. `MinioObjectStorageClient` 다운로드 구현
3. `WorkerJobService`에서 source bytes 주입
4. storage 실패 코드 분리
5. decode 이후 실패 코드 분리

## 완료 기준

1. 다운로드 실패는 `STORAGE_DOWNLOAD_FAILED`로 보고합니다.
2. decode 또는 이후 step 실패는 `PIPELINE_EXECUTION_FAILED`로 보고합니다.
3. artifact upload는 이 작업 범위에 포함하지 않습니다.
