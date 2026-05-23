# 이슈 108. 업로드 이미지 metadata 추출

## 목적

업로드 완료 후 이미지의 width, height, format, DPI 등 metadata를 추출해 DB에 저장합니다.

## 작업 범위

1. 이미지 header 읽기
2. width/height 추출
3. format 추출
4. DPI 정보 추출
5. metadata 저장
6. 추출 실패 처리

## 완료 기준

1. Worker 전처리 전에 API에서 기본 metadata를 조회할 수 있습니다.
2. metadata 추출 실패가 업로드 전체 실패와 동일하게 취급되는지 정책을 명확히 합니다.
3. 대용량 파일 전체를 불필요하게 메모리에 올리지 않습니다.
