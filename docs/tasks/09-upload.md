# 09. Upload

## 목표

대용량 이미지 업로드를 위한 upload session과 presigned URL 방식을 구현한다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/08-project.md`
4. `docs/api/upload-api.md`
5. `docs/operation/storage-operation.md`

## 작업 범위

1. Upload session entity
2. Presigned upload URL
3. 업로드 완료 검증
4. 파일 형식 검증
5. checksum 기반 중복 감지

## 작업 순서

1. `UploadSession` entity를 만든다.
2. `UploadSessionFile` entity를 만든다.
3. `UploadSessionStatus` enum을 만든다.
4. repository를 만든다.
5. `ObjectStoragePort`를 정의한다.
6. `PresignedUrlGenerator`를 정의한다.
7. 업로드 세션 생성 API를 구현한다.
8. presigned upload URL 발급 API를 구현한다.
9. 파일 확장자 검증을 구현한다.
10. content type 검증을 구현한다.
11. size limit 검증을 구현한다.
12. checksum 수집 구조를 구현한다.
13. 업로드 완료 API를 구현한다.
14. object 존재 여부 검증을 구현한다.
15. 이미지 메타데이터 생성 흐름에 연결한다.
16. 중복 파일 hash 확인을 구현한다.
17. 업로드 취소 API를 구현한다.

## 산출물

1. Upload domain 클래스
2. Storage port
3. Presigned upload API
4. Upload 완료 검증 로직

## 완료 기준

1. API 서버가 대용량 파일 본문을 직접 받지 않는다.
2. presigned URL에 만료 시간이 있다.
3. 위장 확장자를 기본 검증한다.

## 금지 사항

1. Object Storage secret을 프론트에 노출하지 않는다.
2. 파일 확장자만 믿고 이미지로 판단하지 않는다.
3. 업로드 완료 검증 없이 Image row를 확정하지 않는다.
