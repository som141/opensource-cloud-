# 이슈 33. 이미지 도메인

## 목적

원본 이미지와 처리 결과 artifact의 metadata를 관리합니다.

## 작업 범위

1. `Image` entity
2. `ImageArtifact` entity
3. 이미지 목록 조회
4. 이미지 상세 조회
5. 원본/결과 다운로드 URL 발급
6. 처리 report 조회

## 완료 기준

1. Object Storage key는 DB metadata로 추적됩니다.
2. 다운로드는 signed URL을 통해 제공됩니다.
3. 이미지 접근은 프로젝트 권한을 확인합니다.
