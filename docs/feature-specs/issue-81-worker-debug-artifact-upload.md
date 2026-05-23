# 이슈 81. Debug artifact 업로드

## 목적

`debug=true` 작업에서 전처리 단계별 중간 이미지를 Object Storage에 저장합니다.

## 작업 범위

1. step별 debug image 생성
2. debug object key 생성
3. Object Storage 업로드
4. report에 debug artifact metadata 포함
5. debug disabled 작업에서 저장 생략

## 완료 기준

1. debug artifact는 기본 비활성화입니다.
2. 저장 경로는 `processed/{projectId}/{jobId}/{itemId}/debug/` 하위입니다.
3. debug 저장 실패 정책은 일반 artifact 실패와 구분합니다.
