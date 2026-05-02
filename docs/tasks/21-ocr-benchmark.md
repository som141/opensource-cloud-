# 21. OCR Benchmark

## 목표

RAW OCR과 PREPROCESSED OCR을 비교해 전처리 품질 개선을 수치로 확인하는 기능을 구현한다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/16-worker-preprocess-pipeline.md`
4. `docs/api/benchmark-api.md`
5. `docs/worker/image-test-integration.md`

## 작업 범위

1. Benchmark entity
2. Benchmark queue
3. Raw OCR
4. Preprocessed OCR
5. Metric 비교
6. JSON/CSV/Markdown export

## 작업 순서

1. `Benchmark` entity를 만든다.
2. `BenchmarkItem` entity를 만든다.
3. `OcrMetric` 모델을 만든다.
4. benchmark repository를 만든다.
5. benchmark 생성 API를 구현한다.
6. benchmark message DTO를 만든다.
7. benchmark queue publisher를 구현한다.
8. Worker benchmark listener를 구현한다.
9. Raw OCR 실행을 구현한다.
10. Preprocessed OCR 실행을 구현한다.
11. confidence 계산을 구현한다.
12. garbageRatio 계산을 구현한다.
13. CER 계산을 구현한다.
14. WER 계산을 구현한다.
15. wallMillis/cpuMillis/peakMemory 비교를 구현한다.
16. 개선/동일/악화 판정을 구현한다.
17. JSON summary를 생성한다.
18. CSV summary를 생성한다.
19. Markdown summary를 생성한다.
20. benchmark 결과 조회 API를 구현한다.

## 산출물

1. Benchmark API
2. Benchmark Worker 흐름
3. OCR metric calculator
4. Export writer

## 완료 기준

1. 원본 OCR과 전처리 후 OCR을 비교할 수 있다.
2. confidence, garbageRatio, CER, WER가 저장된다.
3. JSON, CSV, Markdown 결과를 받을 수 있다.

## 금지 사항

1. OCR benchmark를 MVP 1차 필수 기능으로 끼워 넣어 일정 리스크를 키우지 않는다.
2. 전처리 품질을 단순 처리 시간만으로 판단하지 않는다.
3. OCR 언어와 PSM 설정을 하드코딩만 하지 않는다.
