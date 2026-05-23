# 이슈 110. Docker Compose preflight

## 목적

로컬 스택 실행 후 주요 서비스와 routing이 정상인지 빠르게 점검하는 스크립트를 제공합니다.

## 작업 범위

1. Docker Compose config 확인
2. 컨테이너 상태 확인
3. NGINX health 확인
4. frontend route 확인
5. OpenAPI route 확인
6. backend health 확인
7. MinIO health 확인
8. RabbitMQ queue topology 확인

## 완료 기준

1. 실패 지점이 명확히 출력됩니다.
2. 브라우저 테스트 전에 빠르게 실행할 수 있습니다.
3. 문서에 실패 대응 위치를 정리합니다.
