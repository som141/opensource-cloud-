# 이슈 27. Swagger/OpenAPI

## 목적

API 테스트와 문서화를 위해 Swagger UI와 OpenAPI 설정을 추가합니다.

## 작업 범위

1. Swagger dependency 추가
2. JWT Bearer 인증 scheme 설정
3. 공통 응답 예시 정리
4. 주요 controller tag 분리
5. 로컬 접속 주소 문서화

## 완료 기준

1. `http://localhost:8080/swagger-ui/index.html`에서 문서를 확인할 수 있습니다.
2. NGINX 경유 `http://localhost/swagger-ui/index.html`도 동작합니다.
3. 인증이 필요한 API는 Swagger에서 Bearer token 입력 후 테스트할 수 있습니다.
