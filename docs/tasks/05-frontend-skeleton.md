# 05. 프론트엔드 골격

## 목표

React/Vite 기반 프론트엔드 구조를 만들고 NGINX에서 정적 파일로 제공할 수 있게 합니다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/architecture/nginx-routing.md`
3. `docs/operation/docker-compose-local.md`

## 작업 범위

1. `frontend` 앱 구조 생성
2. routing 구성
3. API client 공통 모듈 생성
4. 인증 상태 처리 골격 생성
5. 프로젝트, 업로드, Job, 이미지 상세 화면 진입점 생성
6. Dockerfile과 frontend `nginx.conf` 생성

## UI 원칙

1. Bootstrap, jQuery, AdminLTE를 추가하지 않습니다.
2. 화면은 프론트엔드 책임에만 집중합니다.
3. Object Storage secret을 브라우저에 노출하지 않습니다.
4. API 호출은 NGINX의 `/api` reverse proxy를 전제로 합니다.

## 완료 기준

1. `npm run build`가 통과합니다.
2. Docker Compose에서 `http://localhost`로 화면이 열립니다.
3. Access Token 값이 URL이나 화면에 직접 노출되지 않습니다.
