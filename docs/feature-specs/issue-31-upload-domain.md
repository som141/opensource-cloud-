# 이슈 31. 업로드 도메인

## 목적

대용량 이미지 업로드를 API 서버가 직접 받지 않고 presigned URL 기반으로 처리합니다.

## 작업 범위

1. 업로드 세션 생성
2. 파일 metadata 등록
3. checksum 중복 검증
4. presigned upload URL 발급
5. 업로드 완료 처리
6. Object Storage 존재 여부 확인
7. Image metadata 생성 연계

## 완료 기준

1. 원본 파일 body는 Spring API 메모리를 거치지 않습니다.
2. 지원하지 않는 파일 형식은 거절합니다.
3. 같은 프로젝트 내 중복 checksum은 정책에 따라 거절합니다.
