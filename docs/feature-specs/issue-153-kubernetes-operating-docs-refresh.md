# 이슈 153: Kubernetes 운영 아키텍처와 자원 정책 최신화

## 목적

Kubernetes 배포와 KEDA/HPA 비교 실험 이후 실제 운영 기준이 문서에 충분히 반영되지 않은 부분을 정리한다.
특히 컨테이너별 자원 설정, Worker autoscaling 정책, 관측성 확인 방법, 운영 원칙을 한글 문서로 명확히 남긴다.

## 작업 범위

1. 시스템 전체 아키텍처 문서 최신화
2. Kubernetes/KEDA 아키텍처 문서 최신화
3. 컨테이너 requests/limits와 replica 정책 문서 추가
4. 운영 원칙 문서 추가
5. Kubernetes 배포 가이드 최신화
6. Grafana/Prometheus/KEDA 확인 방법 문서화
7. README와 docs index 링크 정리

## 반영 기준

| 항목 | 반영 내용 |
| --- | --- |
| Namespace | `docprep-cloud`, `keda`, `ingress-nginx`, `github-actions`, `ngrok`, `kubernetes-dashboard` |
| Worker scaling | KEDA `min=0`, `max=20`, normal queue threshold 25, high queue threshold 10 |
| HPA 비교 | Fixed 1, HPA CPU, KEDA min 1, KEDA min 0 실험 결과 |
| 자원 정책 | backend-api, frontend, nginx, worker, postgres, rabbitmq, minio, prometheus, grafana, kube-state-metrics, otel-collector |
| 운영 위험 | `emptyDir`, Grafana `admin/admin`, OAuth redirect, object route, Worker Pending |
| 배포 방식 | GHCR 이미지 빌드 후 self-hosted runner 기반 Kubernetes 자동 배포 |

## 완료 조건

- [x] 새 운영 원칙 문서를 추가한다.
- [x] 새 런타임 자원 정책 문서를 추가한다.
- [x] 시스템/Kubernetes 아키텍처 문서를 최신화한다.
- [x] Kubernetes 배포 가이드를 현재 클러스터 구조 기준으로 갱신한다.
- [x] README와 docs 인덱스에서 새 문서를 연결한다.
- [x] 실제 secret 값은 문서에 기록하지 않는다.

## 관련 문서

- `docs/architecture/system-overview.md`
- `docs/architecture/kubernetes-architecture.md`
- `docs/architecture/runtime-resource-policy.md`
- `docs/operation/operating-principles.md`
- `docs/operation/kubernetes-deployment.md`
- `docs/operation/observability.md`
- `docs/operation/keda-batch-benchmark.md`
