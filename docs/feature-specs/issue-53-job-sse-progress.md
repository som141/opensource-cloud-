# 이슈 53. Job SSE 진행률

## 목적

Job 진행률을 프론트엔드에 실시간으로 전달합니다.

## 작업 범위

1. SSE endpoint 추가
2. Job progress 계산
3. 진행률 이벤트 DTO
4. NGINX SSE 설정
5. 프론트엔드 수신 처리

## 완료 기준

1. 작업 진행률이 새로고침 없이 갱신됩니다.
2. SSE 연결 실패 시 화면이 명확한 상태를 보여줍니다.
3. NGINX가 이벤트를 buffering하지 않습니다.
