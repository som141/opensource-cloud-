# 이슈 154. Node exporter 기반 노드 리소스 관측성과 500장 재실험

## 목적

500장 이미지 전처리 실험에서 KEDA가 Worker replica를 늘리는 것뿐 아니라, 각 Kubernetes 노드의 CPU와 메모리가 어떻게 변하는지 확인할 수 있게 한다.

## 작업 범위

1. node-exporter DaemonSet과 Service를 추가한다.
2. Prometheus가 node-exporter endpoint를 scrape하도록 설정한다.
3. Grafana에 노드 리소스 대시보드를 추가한다.
4. 기존 KEDA 배치 대시보드의 실패 처리량 쿼리를 실제 metric에 맞게 수정한다.
5. 500장 벤치마크 스크립트가 노드 리소스와 Worker Pod 분산을 샘플링하도록 보강한다.
6. 벤치마크 결과 JSON에서 SVG 그래프와 Markdown/PDF 보고서를 생성하는 스크립트를 추가한다.

## 추가 대시보드

```text
DocPrep / DocPrep Node Resource Overview
```

포함 패널:

- 노드별 CPU 사용률
- 노드별 메모리 사용률
- 노드 load1
- 노드 filesystem 사용률
- Worker Pod 노드 분산
- 노드별 Pod CPU request
- 노드별 Pod memory request

## 테스트 계획

1. `kubectl apply -k infra/k8s`로 node-exporter와 dashboard를 배포한다.
2. Prometheus target에 `node-exporter`가 뜨는지 확인한다.
3. Grafana에서 `DocPrep Node Resource Overview` 대시보드가 뜨는지 확인한다.
4. 500장 배치 테스트를 실행한다.
5. `scripts/generate-node-resource-benchmark-report.py`로 그래프와 보고서를 생성한다.

## 2026-06-03 실측 결과

KEDA ON, `minReplicaCount=0`, `maxReplicaCount=20` 상태에서 500장 배치 테스트를 다시 실행했다.

| 항목 | 값 |
| --- | --- |
| 입력 이미지 | 500장 |
| Job ID | `11` |
| Project ID | `11` |
| 성공 | 500장 |
| 실패 | 0장 |
| 전체 처리 시간 | 294.168초 |
| 평균 처리량 | 약 1.7장/초 |
| 최대 Worker replica | 20 |
| 최대 ready Worker replica | 4 |
| 최대 HPA desired replica | 20 |

노드별 최대 사용률은 다음과 같이 관측됐다.

| 노드 | 최대 CPU% | 최대 Memory% |
| --- | ---: | ---: |
| `som-control-19e78e103d2` | 8.0 | 32.0 |
| `som-node-19e78e15b54` | 44.0 | 45.0 |
| `som-node-19e78e1acde` | 73.0 | 69.0 |
| `som-node-19e78e1e65a` | 87.0 | 50.0 |

로컬 산출물은 `benchmark-results/20260603-keda-node-resource-500-*` 경로에 생성했다. 이 디렉터리는 `.gitignore` 대상이므로 원격 저장소에는 올리지 않는다.

## 2026-06-04 4가지 스케일링 케이스 재실험

같은 500장 입력을 사용해 4가지 스케일링 워크로드를 다시 측정했다. 이번 재실험은 `scripts/k8s-batch-benchmark.ps1`의 노드 리소스 샘플링을 사용했기 때문에 모든 케이스에서 시간별 노드 CPU/메모리, Worker replica, HPA desired replica, 처리량 그래프를 생성할 수 있다.

| 케이스 | Job | 성공/전체 | 전체 시간(초) | 평균 처리량(장/초) | 첫 완료 지연(초) | p50 완료(초) | p95 완료(초) | 최대 Worker | 최대 Ready | 최대 Desired | 최대 노드 CPU% | 최대 노드 Memory% |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Fixed 1 Worker | 12 | 500/500 | 308.674 | 1.62 | 0.0 | 46.372 | 85.014 | 1 | 1 | 0 | 55.0 | 59.0 |
| HPA CPU 60% | 14 | 500/500 | 303.517 | 1.647 | 0.0 | 33.065 | 78.371 | 3 | 3 | 3 | 49.0 | 62.0 |
| KEDA min 1 | 15 | 500/500 | 283.749 | 1.762 | 0.0 | 28.575 | 58.162 | 8 | 4 | 8 | 67.0 | 67.0 |
| KEDA min 0 | 16 | 500/500 | 276.798 | 1.806 | 11.356 | 33.129 | 54.597 | 20 | 4 | 20 | 57.0 | 64.0 |

해석:

- 전체 처리 시간과 평균 처리량 기준으로는 `KEDA min 0`이 가장 빠르다.
- `KEDA min 0`은 scale-from-zero 때문에 첫 완료 관측 지연이 11.356초로 나타났지만, 이후 큐 기반 확장으로 p95 완료 시점은 가장 빠르다.
- `KEDA min 1`은 최소 1개 Worker를 유지하므로 첫 완료 지연이 첫 샘플 이전으로 짧고, p50/p95 완료 시점도 HPA CPU보다 빠르다.
- HPA CPU는 최대 3개까지 확장했지만 CPU 사용률 기반이라 RabbitMQ backlog를 직접 보지 못해 KEDA보다 반응이 제한적이다.
- Fixed 1 Worker는 가장 단순하지만 전체 처리 시간과 p95 완료 시점이 가장 느리다.
- `첫 완료 지연 0.0초`는 실제 latency가 0초라는 뜻이 아니라, 첫 번째 5초 polling 샘플 전에 이미 일부 이미지가 완료됐다는 뜻이다.

로컬 산출물은 `benchmark-results/20260604-autoscaling-4case-*` 경로에 생성했다.

## 완료 조건

- node-exporter Pod가 노드마다 1개씩 실행된다.
- Prometheus가 node-exporter metric을 scrape한다.
- Grafana에서 노드별 CPU/메모리 그래프를 볼 수 있다.
- 500장 테스트 결과와 보고서가 `benchmark-results/`에 저장된다.
