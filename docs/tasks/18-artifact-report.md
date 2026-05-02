# 18. Artifact/Report

## 목표

Worker 처리 결과 이미지, preview, processing-report.json, debug artifact 저장을 구현한다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/16-worker-preprocess-pipeline.md`
4. `docs/tasks/14-internal-worker-api.md`
5. `docs/worker/report-json-spec.md`

## 작업 범위

1. Object Storage 경로
2. Processed image 저장
3. Preview 저장
4. Report JSON 생성
5. Debug artifact 조건부 저장
6. Artifact 등록 API 호출

## 작업 순서

1. `ArtifactType`을 정의한다.
2. `ArtifactPath` builder를 구현한다.
3. processed image 저장을 구현한다.
4. preview image 저장을 구현한다.
5. report JSON 저장을 구현한다.
6. debug artifact 저장을 구현한다.
7. debug flag가 false이면 debug 저장을 생략한다.
8. artifact 등록 internal API 호출을 구현한다.
9. `ProcessingReport` DTO를 구현한다.
10. `ProcessingStepReport`를 구현한다.
11. timing report를 구현한다.
12. memory usage sampling을 구현한다.
13. fallback summary를 구현한다.
14. report JSON schema 문서를 작성한다.
15. artifact 저장 테스트를 작성한다.

## 산출물

1. Artifact save service
2. Report factory/writer
3. Debug artifact save service
4. Report JSON schema

## 완료 기준

1. 결과 경로가 `processed/{projectId}/{jobId}/{itemId}` 기준이다.
2. debug artifact는 옵션일 때만 저장된다.
3. report에 skew, crop, denoise, binarization, fallback, timing 정보가 있다.

## 금지 사항

1. debug 이미지를 항상 저장해 저장소를 낭비하지 않는다.
2. report를 DB 컬럼으로만 흩어 저장하지 않는다.
3. 원본 이미지를 덮어쓰지 않는다.
