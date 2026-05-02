# 16. Worker Preprocess Pipeline

## 목표

`image-test` 레포의 OpenCV 기반 문서 이미지 전처리 메커니즘을 Worker pipeline으로 구현한다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/15-worker-message-consume.md`
4. `docs/worker/preprocess-pipeline.md`
5. `docs/worker/image-test-integration.md`

## 작업 범위

1. PreprocessStep interface
2. Pipeline runner
3. Context/result
4. 전처리 단계 구현
5. timing/fallback/debug hook

## 작업 순서

1. `PreprocessStep` interface를 정의한다.
2. `PreprocessContext`를 정의한다.
3. `PreprocessResult`를 정의한다.
4. `PreprocessPipeline`을 정의한다.
5. `PreprocessPipelineRunner`를 구현한다.
6. `DecodeStep`을 구현한다.
7. `ColorNormalizeStep`을 구현한다.
8. `OrientationNormalizeStep`을 구현한다.
9. `DeskewStep`을 구현한다.
10. `CropStep`을 구현한다.
11. `DenoiseStep`을 구현한다.
12. `ContrastNormalizeStep`을 구현한다.
13. `BinarizationStep`을 구현한다.
14. `MorphologyCleanupStep`을 구현한다.
15. `DpiNormalizeStep`을 구현한다.
16. `SharpenStep`을 구현한다.
17. 단계별 timing 측정을 구현한다.
18. fallback note 수집을 구현한다.
19. debug artifact hook을 구현한다.
20. pipeline 테스트를 작성한다.

## 산출물

1. 전처리 pipeline 클래스
2. 단계별 step 클래스
3. Preprocess context/result
4. Pipeline 테스트

## 완료 기준

1. 단순 resize step만 존재하지 않는다.
2. DPI Normalize가 OCR 품질 정규화 단계로 표현된다.
3. 각 단계 결과가 context에 누적된다.
4. 실패한 step을 report에 남길 수 있다.

## 금지 사항

1. API 서버에서 이 pipeline을 실행하지 않는다.
2. OpenCV Mat 리소스를 무제한 방치하지 않는다.
3. 모든 프리셋이 같은 파라미터로만 동작하게 만들지 않는다.
