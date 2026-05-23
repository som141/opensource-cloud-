# 18. Artifact와 Report

## 목표

Worker가 생성한 처리 결과를 Object Storage에 저장하고, API 서버가 다운로드 가능한 artifact metadata를 관리합니다.

## 먼저 읽을 문서

1. `docs/operation/storage-operation.md`
2. `docs/worker/report-json-spec.md`
3. `docs/api/image-api.md`
4. `docs/api/job-api.md`

## 작업 범위

1. `processed.png` 저장
2. `preview.png` 저장
3. `processing-report.json` 저장
4. debug artifact 저장 옵션 연결
5. Worker success callback에 object key 포함
6. API 서버의 `ImageArtifact` metadata 저장
7. 결과 다운로드 URL 발급
8. Job 결과 ZIP 다운로드

## 사용자 노출 정책

현재 MVP 화면은 처리된 이미지와 processed-only ZIP 다운로드를 중심으로 제공합니다.
preview, report, debug artifact는 운영 분석과 문제 재현을 위한 내부 artifact로 유지합니다.

## 완료 기준

1. artifact 업로드 실패는 `ARTIFACT_UPLOAD_FAILED`로 보고합니다.
2. 처리 성공 callback은 실제 object가 저장된 뒤에만 호출합니다.
3. Object Storage key는 project, job, item 식별자를 포함합니다.
4. signed download URL로만 결과 파일에 접근합니다.
