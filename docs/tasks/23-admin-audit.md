# 23. Admin/Audit

## 목표

운영자용 상태 조회와 중요 행위 감사 로그를 구현한다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/20-observability.md`
4. `docs/api/admin-api.md`
5. `docs/operation/troubleshooting.md`

## 작업 범위

1. Admin overview
2. Queue status
3. Worker status
4. Job 강제 제어
5. Storage usage
6. Audit log

## 작업 순서

1. admin 권한 검증을 구현한다.
2. overview API를 구현한다.
3. queue status API를 구현한다.
4. worker status API를 구현한다.
5. 전체 job 조회 API를 구현한다.
6. force retry API를 구현한다.
7. force cancel API를 구현한다.
8. storage usage API를 구현한다.
9. `AuditLog` entity를 만든다.
10. `AuditAction` enum을 만든다.
11. audit repository를 만든다.
12. audit log service를 만든다.
13. 로그인 기록을 연결한다.
14. 다운로드 기록을 연결한다.
15. 이미지 삭제 기록을 연결한다.
16. 작업 생성 기록을 연결한다.
17. 작업 취소 기록을 연결한다.
18. 관리자 감사 로그 조회 API를 구현한다.

## 산출물

1. Admin API
2. Audit domain 클래스
3. 운영 상태 조회 로직
4. 감사 로그 조회 로직

## 완료 기준

1. ADMIN 권한만 관리자 API에 접근한다.
2. 중요 사용자 행위가 감사 로그로 남는다.
3. queue, worker, storage 상태를 확인할 수 있다.

## 금지 사항

1. 개인정보를 불필요하게 감사 로그에 저장하지 않는다.
2. 일반 사용자가 관리자 API에 접근하게 하지 않는다.
3. 강제 취소/재시도에서 상태 전이 검증을 생략하지 않는다.
