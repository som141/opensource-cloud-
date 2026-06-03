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

## 완료 조건

- node-exporter Pod가 노드마다 1개씩 실행된다.
- Prometheus가 node-exporter metric을 scrape한다.
- Grafana에서 노드별 CPU/메모리 그래프를 볼 수 있다.
- 500장 테스트 결과와 보고서가 `benchmark-results/`에 저장된다.
