# 08. Project

## 목표

대량 이미지와 작업을 묶는 프로젝트 도메인을 구현한다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/07-auth-user.md`
4. `docs/api/project-api.md`

## 작업 범위

1. 프로젝트 entity
2. 프로젝트 멤버 entity
3. 프로젝트 권한
4. 프로젝트 CRUD
5. 프로젝트 summary

## 작업 순서

1. `Project` entity를 만든다.
2. `ProjectMember` entity를 만든다.
3. `ProjectRole` enum을 만든다.
4. `ProjectStatus` enum을 만든다.
5. repository를 만든다.
6. 프로젝트 생성 DTO를 만든다.
7. 프로젝트 응답 DTO를 만든다.
8. 프로젝트 생성 API를 구현한다.
9. 프로젝트 목록 API를 구현한다.
10. 프로젝트 상세 API를 구현한다.
11. 프로젝트 수정 API를 구현한다.
12. 프로젝트 soft delete를 구현한다.
13. 멤버 초대 API를 구현한다.
14. 멤버 목록 API를 구현한다.
15. 멤버 제거 API를 구현한다.
16. `ProjectPermissionService`를 구현한다.
17. 프로젝트 summary API를 구현한다.

## 산출물

1. Project domain 클래스
2. Project API
3. Project permission service
4. Project API 테스트

## 완료 기준

1. owner, editor, viewer 권한이 구분된다.
2. 삭제는 soft delete를 기본으로 한다.
3. 다른 사용자의 프로젝트에 접근할 수 없다.

## 금지 사항

1. 이미지나 Job 권한을 프로젝트 권한과 분리해서 임의 처리하지 않는다.
2. repository를 controller에서 직접 호출하지 않는다.
3. owner 없이 프로젝트를 만들지 않는다.
