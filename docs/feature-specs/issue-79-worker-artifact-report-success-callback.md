# 이슈 79. Artifact 저장과 success callback

## 목적

완성된 pipeline 결과를 Object Storage에 저장하고 JobItem 성공 상태를 API 서버에 보고합니다.

## 작업 범위

1. final output `ImageMatHolder` 전달
2. `processed.png` 업로드
3. `preview.png` 업로드
4. `processing-report.json` 업로드
5. 성공 callback에 object key 포함
6. artifact upload 실패 처리

## 완료 기준

1. artifact가 저장된 뒤에만 성공 callback을 보냅니다.
2. 업로드 실패는 `ARTIFACT_UPLOAD_FAILED`로 보고합니다.
3. 실제 debug artifact image 생성은 별도 작업으로 남깁니다.
