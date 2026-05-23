# 처리 리포트 JSON 명세

## 목적

`processing-report.json`은 Worker가 OCR 전에 문서 이미지를 어떻게 변환했는지 기록합니다.
이 파일은 운영/디버깅용 artifact이며 OCR 텍스트 결과가 아닙니다.

## Object key

```text
processed/{projectId}/{jobId}/{itemId}/processing-report.json
```

## 전체 구조

```json
{
  "schemaVersion": "1.0",
  "fileName": "processing-report.json",
  "report": {
    "jobId": 1,
    "itemId": 2,
    "presetName": "LOW_CONTRAST_SCAN",
    "steps": [],
    "timing": {},
    "memoryUsage": {},
    "fallbackSummary": {},
    "debugArtifacts": [],
    "success": true,
    "errorMessage": null
  }
}
```

## 단계별 리포트

각 step entry는 아래 필드를 가집니다.

| 필드 | 의미 |
| --- | --- |
| `stepName` | `DESKEW`, `BINARIZATION` 같은 pipeline step enum |
| `note` | 사람이 읽을 수 있는 실행 설명 |
| `timing.wallTime` | step wall-clock duration |
| `timing.cpuTime` | CPU duration 예약 필드. 현재 sampling하지 않으면 `0` |

## Fallback summary

step이 전체 item을 실패시키지 않고 안전한 대체 전략을 선택하면 fallback note를 남깁니다.

예시:

- source DPI가 없어 DPI normalization을 건너뜀
- 지원하지 않는 binarization mode를 Otsu로 대체
- foreground point가 부족해 deskew 또는 crop을 건너뜀

## 현재 제한

1. memory usage는 아직 sampling하지 않으므로 `0`입니다.
2. debug artifact entry는 job 요청이 `debug=true`일 때만 업로드된 PNG snapshot을 가리킵니다.
3. OCR text, confidence, CER, WER, 인식 텍스트는 범위 밖입니다.
