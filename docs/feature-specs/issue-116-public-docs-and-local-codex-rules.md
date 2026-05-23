# Issue 116. 사용자 문서와 Codex 로컬 규칙 문서 분리

## 목적

AI/Codex가 참고하는 작업 규칙 문서와 사용자가 읽는 공개 문서를 분리한다.  
공개 문서는 한글 기준으로 정리하고, README는 SaaS 프로젝트 소개 문서처럼 기능, 아키텍처, 빠른 시작, 운영 배포, 문서 링크를 제공한다.

## 변경 범위

- `README.md`를 한글 SaaS 프로젝트 README로 재작성
- `docs/README.md` 문서 인덱스 추가
- `docs/architecture/system-overview.md` 추가
- `docs/architecture/repository-structure.md` 추가
- `docs/operation/README.md` 추가
- `docs/operation/production-deployment-guide.md` 추가
- 운영 배포 관련 문서 한글화
- Codex 로컬 규칙 문서 Git 추적 제외

## 로컬 전용 문서 기준

아래 문서는 작업자 로컬에서만 유지한다.

```text
AGENTS.md
CODEX_DIRECTORY_SPEC.md
LOCAL_CONFIG.md
*.local.md
.codex/
```

이 파일들은 공개 사용자 문서가 아니므로 원격 repository에 올리지 않는다.

## 공개 문서 기준

공개 문서는 아래 경로에 둔다.

```text
README.md
docs/
```

문서는 기본적으로 한글로 작성하고, 운영자가 실제로 따라 할 수 있도록 명령어와 검증 기준을 포함한다.

## 완료 조건

- README에서 주요 문서로 이동할 수 있다.
- 운영 배포 문서가 서버 준비, 환경변수, GitHub Actions, E2E 검증 순서로 연결된다.
- Codex 규칙 문서는 Git 추적 대상에서 제외된다.
- Markdown 링크와 Git diff 공백 검증을 수행한다.
