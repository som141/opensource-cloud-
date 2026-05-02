# Issue 1. Docs conventions

## 이슈

- Issue: `#1`
- Title: `📝 [Docs] GitHub workflow, code convention, API response convention 문서 추가`
- Branch: `docs/som/1`

## 목표

프로젝트 초기 작업 전에 모든 기여자가 따라야 할 GitHub 작업 흐름, 코드 컨벤션, API 응답 컨벤션을 문서로 고정한다.

## 작업 범위

1. GitHub workflow 문서 추가
2. Code convention 문서 추가
3. API response convention 문서 추가
4. README에 프로젝트 정체성과 문서 진입점 반영
5. GitHub issue/PR template 추가

## 변경 파일

```text
README.md
docs/conventions/github-workflow.md
docs/conventions/code-convention.md
docs/conventions/api-response-convention.md
docs/feature-specs/issue-1-docs-conventions.md
.github/ISSUE_TEMPLATE/feature.md
.github/ISSUE_TEMPLATE/bug.md
.github/ISSUE_TEMPLATE/refactor.md
.github/PULL_REQUEST_TEMPLATE.md
```

## 완료 기준

1. 이슈 기반 브랜치/PR 작업 흐름이 문서화되어 있다.
2. Spring 도메인형 구조와 컴포넌트 책임 분리가 문서화되어 있다.
3. 공통 API 응답 형식이 문서화되어 있다.
4. 다음 작업인 모노레포 skeleton 생성 전에 참고할 기준이 존재한다.

## 범위 제외

1. Spring Boot 프로젝트 생성
2. Worker pipeline 클래스 생성
3. Docker Compose 구현
4. NGINX 설정 구현
