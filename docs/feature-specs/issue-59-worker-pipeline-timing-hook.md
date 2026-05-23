# 이슈 59. Pipeline timing hook

## 목적

전처리 pipeline의 각 step 실행 시간과 실패 정보를 기록합니다.

## 작업 범위

1. `PreprocessStepExecution`
2. 시작/종료 시간 기록
3. wall time 계산
4. 성공 여부와 오류 메시지 기록
5. fallback note 수집
6. report DTO 연결

## 완료 기준

1. 실패한 step에서 pipeline이 중단됩니다.
2. 결과와 report에 실패 정보가 남습니다.
3. Worker 실패 callback이 `PIPELINE_EXECUTION_FAILED`를 사용할 수 있습니다.
