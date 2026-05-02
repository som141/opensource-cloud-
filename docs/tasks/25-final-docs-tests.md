# 25. 최종 문서/테스트 정리

## 목표

구현 후 신규 작업자가 문서만 읽고 로컬 실행, API 검증, Worker 동작 확인, 운영 확인을 할 수 있게 정리한다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/README.md`
4. 전체 `docs/tasks/*.md`

## 작업 범위

1. Architecture 문서
2. API 문서
3. Database 문서
4. Worker 문서
5. Operation 문서
6. Test/CI 정리

## 작업 순서

1. `docs/architecture/system-overview.md`를 작성한다.
2. `docs/architecture/docker-compose-architecture.md`를 작성한다.
3. `docs/architecture/kubernetes-architecture.md`를 작성한다.
4. `docs/architecture/sequence-diagrams.md`를 작성한다.
5. Auth API 문서를 작성한다.
6. Project API 문서를 작성한다.
7. Upload API 문서를 작성한다.
8. Image API 문서를 작성한다.
9. Job API 문서를 작성한다.
10. Benchmark API 문서를 작성한다.
11. Admin API 문서를 작성한다.
12. DB ERD 문서를 작성한다.
13. DB table spec을 작성한다.
14. Migration policy를 작성한다.
15. Worker pipeline 문서를 작성한다.
16. Preset spec을 작성한다.
17. Report JSON spec을 작성한다.
18. Retry policy를 작성한다.
19. Local run guide를 작성한다.
20. Observability guide를 작성한다.
21. Queue operation 문서를 작성한다.
22. Storage operation 문서를 작성한다.
23. Troubleshooting 문서를 작성한다.
24. README 실행 가이드를 갱신한다.
25. backend-api CI workflow를 작성한다.
26. preprocess-worker CI workflow를 작성한다.
27. frontend CI workflow를 작성한다.

## 산출물

1. 전체 문서 세트
2. 로컬 실행 가이드
3. 테스트 실행 가이드
4. CI workflow

## 완료 기준

1. 신규 작업자가 문서만 보고 로컬 환경을 띄울 수 있다.
2. API와 Worker 책임 경계가 문서에 명확하다.
3. MVP 범위와 이후 확장 범위가 구분되어 있다.
4. 테스트와 빌드 명령이 README에 정리되어 있다.

## 금지 사항

1. 구현과 맞지 않는 문서를 남겨두지 않는다.
2. 실행 불가능한 명령을 검증 없이 README에 넣지 않는다.
3. 문서에 secret 값을 포함하지 않는다.
