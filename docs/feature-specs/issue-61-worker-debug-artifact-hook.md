# 이슈 61. Debug artifact hook

## 목적

전처리 단계별 debug artifact를 저장할 수 있는 계약을 추가합니다.

## 작업 범위

1. `DebugArtifactDescriptor`
2. `PreprocessContext.recordDebugArtifact`
3. `debug=false`일 때 기록 무시
4. `debug=true`일 때 deterministic object key 생성
5. report DTO에 debug metadata 포함

## 완료 기준

1. 실제 이미지 생성과 업로드는 후속 작업으로 남깁니다.
2. debug object key는 `processed/{projectId}/{jobId}/{itemId}/debug/` 하위에 생성됩니다.
3. debug 옵션이 꺼진 작업은 불필요한 metadata를 남기지 않습니다.
