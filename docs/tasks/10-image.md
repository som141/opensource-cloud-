# 10. Image

## 목표

원본 이미지와 처리 결과 artifact의 메타데이터와 조회 API를 구현한다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/09-upload.md`
4. `docs/api/image-api.md`
5. `docs/operation/storage-operation.md`

## 작업 범위

1. Image entity
2. Image artifact entity
3. 이미지 목록/상세 조회
4. 다운로드 URL 발급
5. 처리 리포트 조회

## 작업 순서

1. `Image` entity를 만든다.
2. `ImageArtifact` entity를 만든다.
3. `ImageStatus` enum을 만든다.
4. `ImageFormat` enum을 만든다.
5. repository를 만든다.
6. 이미지 목록 API를 구현한다.
7. 이미지 상세 API를 구현한다.
8. 원본 다운로드 URL API를 구현한다.
9. processed 다운로드 URL API를 구현한다.
10. preview 다운로드 URL API를 구현한다.
11. report 조회 API를 구현한다.
12. debug artifact 목록 API를 구현한다.
13. 이미지 soft delete를 구현한다.
14. 이미지 접근 권한 검증을 구현한다.

## 산출물

1. Image domain 클래스
2. Image artifact 관리 로직
3. Image API
4. 다운로드 URL 발급 로직

## 완료 기준

1. private bucket을 전제로 동작한다.
2. 원본과 결과 artifact가 분리되어 저장된다.
3. 프로젝트 권한이 이미지 접근에 적용된다.

## 금지 사항

1. 파일을 public URL로 직접 공개하지 않는다.
2. 이미지 삭제 시 Object Storage와 DB 상태를 불일치시키지 않는다.
3. Worker 전처리 결과를 API 서버에서 직접 생성하지 않는다.
