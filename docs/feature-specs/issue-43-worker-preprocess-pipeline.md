# 이슈 43. Worker 전처리 pipeline skeleton

## 목적

문서 이미지 전처리 pipeline을 step 기반 구조로 실행할 수 있는 골격을 만듭니다.

## 작업 범위

1. `PreprocessStep`
2. `PreprocessPipeline`
3. `PreprocessPipelineRunner`
4. `PreprocessContext`
5. `PreprocessResult`
6. 필수 step class skeleton

## 완료 기준

1. pipeline은 Worker 애플리케이션 안에만 존재합니다.
2. 각 step은 독립적으로 테스트할 수 있습니다.
3. resize-only 구조로 축소하지 않습니다.
