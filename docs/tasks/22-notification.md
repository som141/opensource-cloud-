# 22. Notification

## 목표

작업 완료, 실패, 관리자 경고 알림을 관리하는 기능을 구현한다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/12-job.md`
4. `docs/api/job-api.md`

## 작업 범위

1. Notification entity
2. 알림 생성
3. 알림 목록 조회
4. 읽음 처리
5. 삭제

## 작업 순서

1. `Notification` entity를 만든다.
2. `NotificationType` enum을 만든다.
3. `NotificationStatus` enum을 만든다.
4. repository를 만든다.
5. 작업 완료 알림 생성을 구현한다.
6. 작업 실패 알림 생성을 구현한다.
7. 관리자 경고 알림 생성을 구현한다.
8. 알림 목록 API를 구현한다.
9. 읽음 처리 API를 구현한다.
10. 전체 읽음 API를 구현한다.
11. 삭제 API를 구현한다.
12. 프론트 알림 표시와 연결한다.

## 산출물

1. Notification domain 클래스
2. Notification API
3. Job event 기반 알림 생성
4. Frontend 표시 기준

## 완료 기준

1. 사용자는 자기 알림만 조회할 수 있다.
2. 작업 완료와 실패 알림이 구분된다.
3. 읽음 상태를 관리할 수 있다.

## 금지 사항

1. 이메일 발송부터 먼저 구현하지 않는다.
2. 관리자 경고를 일반 사용자에게 노출하지 않는다.
3. 알림 생성 실패가 Job 상태 변경을 망치게 하지 않는다.
