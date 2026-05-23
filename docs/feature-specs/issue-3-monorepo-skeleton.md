# 이슈 3. 모노레포 기본 구조

## 목적

대규모 문서 이미지 전처리 플랫폼을 `backend-api`, `preprocess-worker`, `frontend`, `infra`, `docs`, `scripts`로 분리합니다.

## 작업 범위

1. 최상위 디렉터리 생성
2. 각 컴포넌트 책임 문서화
3. Docker, NGINX, DB, queue 설정 위치 분리
4. 사용자 문서와 작업 문서 위치 정리

## 완료 기준

1. API 서버와 Worker가 물리적으로 분리됩니다.
2. 인프라 설정은 `infra/`에 모입니다.
3. 문서는 `docs/` 하위에서 탐색 가능합니다.
