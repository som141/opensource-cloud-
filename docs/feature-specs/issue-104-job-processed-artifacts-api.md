# 이슈 104. Job 처리 결과 artifact API

## 목적

Job 기준으로 처리된 이미지 artifact 목록과 다운로드 URL을 제공합니다.

## 작업 범위

1. Job artifact 목록 조회
2. processed image 다운로드 URL
3. processed-only ZIP 다운로드
4. 실패 항목 제외 또는 표시 정책
5. 권한 검증

## 완료 기준

1. 사용자는 처리 완료된 이미지만 안정적으로 다운로드할 수 있습니다.
2. preview/report/debug는 기본 사용자 다운로드에 포함하지 않습니다.
3. 프로젝트 권한이 없는 사용자는 접근할 수 없습니다.
