# 05. Frontend 골격

## 목표

NGINX로 정적 파일을 제공할 수 있는 프론트엔드 골격을 만든다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/04-infra-directory-skeleton.md`
4. `docs/api/auth-api.md`
5. `docs/api/job-api.md`

## 작업 범위

1. Frontend 프로젝트 구조
2. 라우팅 구조
3. 기능별 feature 구조
4. API client skeleton
5. SSE client skeleton
6. 정적 배포용 Dockerfile과 NGINX config

## 작업 순서

1. 선택된 프레임워크가 없으면 React + Vite 기준으로 생성한다.
2. `frontend/src/app`을 생성한다.
3. `frontend/src/pages`를 생성한다.
4. `frontend/src/features`를 생성한다.
5. `frontend/src/entities`를 생성한다.
6. `frontend/src/shared`를 생성한다.
7. `frontend/src/styles`를 생성한다.
8. 로그인 페이지 placeholder를 만든다.
9. 대시보드 페이지 placeholder를 만든다.
10. 프로젝트 목록/상세 페이지 placeholder를 만든다.
11. 업로드 페이지 placeholder를 만든다.
12. Job 상세 페이지 placeholder를 만든다.
13. 이미지 상세 페이지 placeholder를 만든다.
14. 벤치마크 페이지 placeholder를 만든다.
15. 관리자 페이지 placeholder를 만든다.
16. `frontend/Dockerfile`과 `frontend/nginx.conf`를 만든다.

## 산출물

1. 프론트엔드 디렉터리 구조
2. 라우팅 placeholder
3. API/SSE client placeholder
4. 정적 파일 배포 skeleton

## 완료 기준

1. 프론트엔드가 API 서버 내부에 섞여 있지 않다.
2. `/api` 호출은 NGINX reverse proxy를 전제로 한다.
3. UI 라이브러리를 임의로 추가하지 않았다.

## 금지 사항

1. Bootstrap, jQuery, AdminLTE를 추가하지 않는다.
2. 프론트에서 이미지 전처리 비즈니스 로직을 수행하지 않는다.
3. 프론트에서 Object Storage secret을 직접 사용하지 않는다.
