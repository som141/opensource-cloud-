# 작업 단위 문서 인덱스

## 목적

이 디렉터리는 전체 구현 계획을 실제 실행 가능한 작은 작업 단위로 나눈 문서 모음이다. 작업자는 구현 전에 반드시 관련 작업 단위 문서를 읽고, 문서의 순서대로 진행한다.

## 공통 작업 규칙

1. `README.md`를 먼저 읽는다.
2. `docs/implementation-plan.md`를 읽는다.
3. `.md`를 읽어 이슈, 브랜치, PR 규칙을 확인한다.
4. `docs/conventions/github-workflow.md`를 읽는다.
5. `docs/conventions/code-convention.md`를 읽는다.
6. `docs/conventions/api-response-convention.md`를 읽는다.
7. 현재 작업에 해당하는 `docs/tasks/*.md` 파일을 읽는다.
8. 작업 대상 기능의 상세 문서가 있으면 함께 읽는다.
9. 문서와 코드가 충돌하면 문서 기준으로 변경 방향을 먼저 정리한다.
10. 구현 전에는 산출물과 완료 기준을 확인한다.
11. 구현 후에는 해당 작업 문서의 체크리스트를 갱신하거나 결과를 요약한다.

## 권장 실행 순서

1. `00-repository-baseline.md`
2. `01-monorepo-skeleton.md`
3. `02-backend-api-skeleton.md`
4. `03-worker-skeleton.md`
5. `04-infra-directory-skeleton.md`
6. `05-frontend-skeleton.md`
7. `06-global-error-response.md`
8. `07-auth-user.md`
9. `08-project.md`
10. `09-upload.md`
11. `10-image.md`
12. `11-preprocess-preset.md`
13. `12-job.md`
14. `13-sse-progress.md`
15. `14-internal-worker-api.md`
16. `15-worker-message-consume.md`
17. `16-worker-preprocess-pipeline.md`
18. `17-worker-preset.md`
19. `18-artifact-report.md`
20. `19-nginx-docker-compose.md`
21. `20-observability.md`
22. `21-ocr-benchmark.md`
23. `22-notification.md`
24. `23-admin-audit.md`
25. `24-kubernetes-keda.md`
26. `25-final-docs-tests.md`

## 작업 단위 작성 형식

각 문서는 아래 항목을 가진다.

1. 목표
2. 먼저 읽을 문서
3. 작업 범위
4. 작업 순서
5. 산출물
6. 완료 기준
7. 금지 사항

## 핵심 원칙

1. API 서버와 Worker를 분리한다.
2. API 서버는 OpenCV 전처리를 하지 않는다.
3. Worker는 OAuth 로그인 로직을 가지지 않는다.
4. Worker는 단순 resize 서비스가 아니다.
5. Spring 패키지는 도메인형 구조로 만든다.
6. 외부 시스템 접근 코드는 `infra`에 둔다.
7. NGINX는 단일 진입점으로 사용한다.
8. 프론트엔드에는 Bootstrap, jQuery, AdminLTE 같은 임의 템플릿을 추가하지 않는다.
