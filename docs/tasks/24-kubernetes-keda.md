# 24. Kubernetes/KEDA

## 목표

Docker Compose 기반 구조를 Kubernetes 배포 구조로 확장하고, KEDA로 Worker autoscaling을 정의한다.

## 먼저 읽을 문서

1. `README.md`
2. `docs/implementation-plan.md`
3. `docs/tasks/19-nginx-docker-compose.md`
4. `docs/architecture/kubernetes-architecture.md`
5. `docs/operation/kubernetes-deployment.md`
6. `docs/worker/retry-policy.md`

## 작업 범위

1. Namespace
2. backend-api manifests
3. `preprocess-worker` manifest
4. frontend manifests
5. nginx ingress
6. KEDA ScaledObject
7. infra service placeholder

## 작업 순서

1. `namespace.yml`을 만든다.
2. backend-api deployment를 만든다.
3. backend-api service를 만든다.
4. backend-api configmap을 만든다.
5. backend-api secret example을 만든다.
6. preprocess-worker deployment를 만든다.
7. preprocess-worker service를 만든다.
8. preprocess-worker configmap을 만든다.
9. preprocess-worker secret example을 만든다.
10. preprocess-worker `ScaledObject`를 만든다.
11. frontend deployment를 만든다.
12. frontend service를 만든다.
13. nginx deployment를 만든다.
14. nginx service를 만든다.
15. ingress를 만든다.
16. RabbitMQ manifest placeholder를 만든다.
17. MinIO manifest placeholder를 만든다.
18. PostgreSQL manifest placeholder를 만든다.
19. monitoring manifest placeholder를 만든다.
20. Kubernetes 운영 문서를 작성한다.

## 산출물

1. Kubernetes manifests
2. KEDA ScaledObject
3. Secret example
4. Kubernetes architecture 문서

## 완료 기준

1. Worker scale trigger가 RabbitMQ queue length다.
2. `minReplicaCount: 0`을 설정할 수 있다.
3. queue 이름이 Docker Compose와 일치한다.
4. secret 실제 값은 커밋되지 않는다.

## 금지 사항

1. CPU autoscaling만으로 Worker 확장을 대체하지 않는다.
2. 운영 secret을 manifest에 직접 넣지 않는다.
3. Compose와 Kubernetes에서 서비스 이름과 queue 이름을 불필요하게 다르게 만들지 않는다.
