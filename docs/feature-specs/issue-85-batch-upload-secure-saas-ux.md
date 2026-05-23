# 이슈 85. 배치 업로드와 보안형 SaaS UX

## 목적

여러 이미지를 한 번에 업로드하고, Access Token 값이 URL이나 화면에 직접 노출되지 않는 SaaS형 화면 흐름으로 개선합니다.

## 작업 범위

1. 다중 파일 선택
2. 파일별 상태 표시
3. batch upload 실행
4. OAuth 성공 후 token 노출 제거
5. 왼쪽 하단 계정 상태 표시
6. 작업 진행률과 결과 다운로드 표시

## 완료 기준

1. Access Token이 URL query parameter로 노출되지 않습니다.
2. Refresh Token은 `HttpOnly` cookie로 유지됩니다.
3. 사용자는 로그인 상태를 계정 표시로 확인할 수 있습니다.
