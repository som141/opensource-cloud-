# 16. Worker 전처리 파이프라인

## 목표

`image-test` 레포의 OpenCV 문서 이미지 전처리 메커니즘을 Worker pipeline으로 구현합니다.

이 작업은 resize 서비스가 아니며 OCR 텍스트 추출도 하지 않습니다.
Worker는 OCR 전에 스캔 문서를 더 안정적인 이미지 상태로 만드는 역할만 담당합니다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/15-worker-message-consume.md`
4. `docs/worker/preprocess-pipeline.md`
5. `docs/worker/image-test-integration.md`

## 필수 단계

1. `DecodeStep`
2. `ColorNormalizeStep`
3. `OrientationNormalizeStep`
4. `DeskewStep`
5. `CropStep`
6. `DenoiseStep`
7. `ContrastNormalizeStep`
8. `BinarizationStep`
9. `MorphologyCleanupStep`
10. `DpiNormalizeStep`
11. `SharpenStep`

## 구현 순서

1. pipeline timing과 실패 report hook을 추가합니다.
2. fallback note 수집 구조를 추가합니다.
3. debug artifact hook 계약을 추가합니다.
4. OpenCV loader와 image codec adapter를 추가합니다.
5. `DecodeStep`을 실제 이미지 decode로 교체합니다.
6. `ImageMatHolder`와 Mat resource 정리 규칙을 추가합니다.
7. 문서 전처리 단계를 하나씩 구현합니다.
8. 단계별 report 생성을 연결합니다.
9. artifact 저장을 연결합니다.
10. 처리 결과가 저장된 뒤 Worker success callback을 보냅니다.

## 완료된 세부 범위

| 이슈 | 내용 |
| --- | --- |
| #59 | timing, 실패 report hook, fallback note |
| #61 | debug artifact hook 계약 |
| #63 | OpenCV loader와 codec boundary |
| #65 | 실제 `DecodeStep` 연결 |
| #67 | Object Storage download bytes 연결 |
| #69 | `ColorNormalizeStep` 구현 |
| #71 | orientation normalize와 deskew 구현 |
| #73 | crop과 DPI normalize 구현 |
| #75 | denoise, contrast, binarization 구현 |
| #77 | morphology cleanup과 sharpen 구현 |
| #79 | processed image, preview, report 저장과 success callback |
| #118 | 전처리 품질 파라미터 튜닝 |

## 완료 기준

1. 단순 resize-only step이 존재하지 않습니다.
2. `DpiNormalizeStep`은 thumbnail resize가 아니라 OCR 품질 보정 단계로 유지됩니다.
3. 각 step 실행 결과가 context/result/report에 기록됩니다.
4. 실패 step은 report와 Worker callback에 반영됩니다.
5. API 서버는 pipeline을 실행하지 않습니다.
6. step 단위 테스트와 Worker 테스트가 통과합니다.

## 금지 사항

1. `backend-api`에서 OpenCV pipeline을 실행하지 않습니다.
2. OCR 텍스트 추출 기능을 추가하지 않습니다.
3. 모든 preset을 같은 무파라미터 동작으로 합치지 않습니다.
4. timing/failure reporting을 우회하지 않습니다.
