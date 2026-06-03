# 노드 리소스 관측 운영 문서

이 문서는 Kubernetes 노드별 CPU, 메모리, 디스크 사용량을 Prometheus와 Grafana로 확인하는 방법을 정리한다.

## 목적

KEDA가 Worker replica를 늘릴 때 실제 병목은 KEDA 자체가 아니라 노드 CPU/메모리 부족일 수 있다. 따라서 500장 이상 배치 테스트에서는 다음 값을 함께 봐야 한다.

| 관측 대상 | 의미 |
| --- | --- |
| RabbitMQ queue length | 처리해야 할 이미지 메시지 backlog |
| KEDA/HPA desired replica | KEDA가 요청한 Worker 수 |
| Worker ready replica | 실제 실행 가능한 Worker 수 |
| 노드 CPU 사용률 | Worker 확장 시 노드 계산 자원이 얼마나 차는지 |
| 노드 메모리 사용률 | OpenCV 처리 중 메모리 압박이 생기는지 |
| Worker Pod 분산 | Worker가 특정 노드에 몰리는지 |

## 추가된 컴포넌트

| 컴포넌트 | 위치 | 역할 |
| --- | --- | --- |
| `node-exporter` DaemonSet | `infra/k8s/monitoring/node-exporter-daemonset.yml` | 각 노드의 CPU, 메모리, 파일시스템 metric 노출 |
| `node-exporter` Headless Service | `infra/k8s/monitoring/node-exporter-service.yml` | Prometheus endpoint discovery 대상 |
| Prometheus scrape config | `infra/k8s/monitoring/prometheus-configmap.yml` | node-exporter endpoint scrape |
| Grafana dashboard | `DocPrep Node Resource Overview` | 노드별 리소스 그래프 |

## Grafana에서 보는 대시보드

Grafana 접속 후 아래 위치를 연다.

```text
DocPrep / DocPrep Node Resource Overview
```

대시보드 패널은 다음과 같다.

| 패널 | PromQL |
| --- | --- |
| Node CPU 사용률 | `100 * (1 - avg by (instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])))` |
| Node 메모리 사용률 | `100 * (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes))` |
| Node load1 | `node_load1` |
| Node filesystem 사용률 | `100 * (1 - (node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"}))` |
| Worker Pod 분산 | `count by (node) (kube_pod_info{namespace="docprep-cloud",pod=~"preprocess-worker-.*"})` |
| Node별 Pod CPU request | `sum by (node) (kube_pod_container_resource_requests{namespace="docprep-cloud",resource="cpu"})` |
| Node별 Pod memory request | `sum by (node) (kube_pod_container_resource_requests{namespace="docprep-cloud",resource="memory"})` |

## 500장 테스트와 보고서 생성

벤치마크 스크립트는 Kubernetes 샘플을 남길 때 `kubectl top nodes`와 Worker Pod 분산도 함께 기록한다.

```powershell
.\scripts\k8s-batch-benchmark.ps1 `
  -ApiBaseUrl "https://<ngrok-domain>/api" `
  -AccessToken "<access-token>" `
  -InputPath "$env:USERPROFILE\Downloads\<500-images-folder>" `
  -MaxFiles 500 `
  -Scenario "keda-node-resource-500" `
  -KubeConfig "$env:USERPROFILE\Downloads\kube (1).conf" `
  -NgrokSkipBrowserWarning
```

결과 JSON이 생성되면 노드 리소스 보고서를 만든다.

```powershell
python scripts/generate-node-resource-benchmark-report.py `
  --result benchmark-results\<benchmark-result>.json `
  --output-dir benchmark-results `
  --prefix 20260603-keda-node-resource-500
```

생성물은 다음과 같다.

| 파일 | 설명 |
| --- | --- |
| `*-node-cpu.svg` | 노드별 CPU 사용률 그래프 |
| `*-node-memory.svg` | 노드별 메모리 사용률 그래프 |
| `*-worker-replicas.svg` | Worker/HPA replica 변화 |
| `*-job-progress.svg` | JobItem 진행 상태 변화 |
| `*-worker-pods-by-node.svg` | Worker Pod 노드 분산 |
| `*-report.md` | Markdown 보고서 |
| `*-report.pdf` | PDF 요약 보고서 |
| `*-report.json` | 보고서 원본 데이터 |

## 2026-06-03 500장 KEDA ON 재실험 결과

이번 재실험은 다음 조건으로 수행했다.

| 항목 | 값 |
| --- | --- |
| Scale mode | KEDA ON |
| KEDA min/max | `0 / 20` |
| 입력 이미지 | 500장 |
| Job ID | `11` |
| 성공/실패 | `500 / 0` |
| 전체 처리 시간 | 294.168초 |
| 평균 처리량 | 약 1.7장/초 |
| 최대 Worker replica | 20 |
| 최대 ready Worker replica | 4 |
| 최대 HPA desired replica | 20 |

실험 후 KEDA는 queue backlog가 사라지면서 Worker를 다시 0개로 줄였다. 즉, scale-to-zero 동작은 정상이다.

다만 최대 desired replica가 20까지 올라간 반면 ready Worker는 4개까지 관측됐다. 이것은 KEDA가 확장을 요청하지 못했다는 뜻이 아니라, 현재 클러스터에서 Worker Pod의 CPU/메모리 request를 만족하며 즉시 스케줄링할 수 있는 여유가 제한적이었다는 뜻으로 해석해야 한다.

로컬 보고서 산출물은 다음 경로에 있다.

```text
benchmark-results/20260603-keda-node-resource-500-report.md
benchmark-results/20260603-keda-node-resource-500-report.pdf
benchmark-results/20260603-keda-node-resource-500-node-cpu.svg
benchmark-results/20260603-keda-node-resource-500-node-memory.svg
benchmark-results/20260603-keda-node-resource-500-worker-replicas.svg
benchmark-results/20260603-keda-node-resource-500-job-progress.svg
benchmark-results/20260603-keda-node-resource-500-worker-pods-by-node.svg
```

`benchmark-results/`는 로컬 실험 산출물 보관 위치이며 원격 저장소에는 올리지 않는다.

## 4가지 스케일링 케이스 비교 보고서 생성

KEDA의 효과를 입증하려면 단일 KEDA ON 결과만 보면 부족하다. 같은 입력 500장을 다음 4가지 케이스로 실행해 비교한다.

| 케이스 | 의미 |
| --- | --- |
| Fixed 1 Worker | KEDA/HPA 없이 Worker 1개 고정 |
| HPA CPU 60% | Kubernetes HPA가 CPU 평균 사용률 60%를 기준으로 확장 |
| KEDA min 1 | RabbitMQ queue length 기반 확장, 최소 Worker 1개 유지 |
| KEDA min 0 | RabbitMQ queue length 기반 확장, scale-to-zero에서 시작 |

4개 결과 JSON을 만든 뒤 아래 명령으로 통합 보고서를 생성한다.

```powershell
python scripts/generate-autoscaling-comparison-report.py `
  --output-dir benchmark-results `
  --prefix 20260604-autoscaling-4case `
  --label "Fixed 1 Worker" `
  --result benchmark-results\20260603-235028-fixed-1-500-20260603-500-images.json `
  --label "HPA CPU 60%" `
  --result benchmark-results\20260604-000053-hpa-cpu-60-min1-500-20260603-500-images.json `
  --label "KEDA min 1" `
  --result benchmark-results\20260604-000615-keda-min1-500-20260604-500-images.json `
  --label "KEDA min 0" `
  --result benchmark-results\20260604-001125-keda-min0-500-20260604-500-images.json
```

생성되는 산출물은 다음과 같다.

| 파일 | 설명 |
| --- | --- |
| `20260604-autoscaling-4case-report.md` | 한글 Markdown 보고서 |
| `20260604-autoscaling-4case-report.html` | 그래프가 바로 보이는 HTML 보고서 |
| `20260604-autoscaling-4case-report.pdf` | 그래프 PNG가 삽입된 PDF 보고서 |
| `20260604-autoscaling-4case-report.json` | 통계 원본 JSON |
| `20260604-autoscaling-4case-duration-throughput.png` | 전체 처리 시간과 평균 처리량 |
| `20260604-autoscaling-4case-latency.png` | 첫 완료, p50, p95 완료 지연 |
| `20260604-autoscaling-4case-replicas.png` | Worker/current/ready/desired 증가량 |
| `20260604-autoscaling-4case-time-node-cpu.png` | 시간별 노드 CPU 사용률 |
| `20260604-autoscaling-4case-time-node-memory.png` | 시간별 노드 메모리 사용률 |
| `20260604-autoscaling-4case-time-ready-replicas.png` | 시간별 Ready Worker replica |
| `20260604-autoscaling-4case-time-desired-replicas.png` | 시간별 HPA desired replica |
| `20260604-autoscaling-4case-time-throughput.png` | 시간별 처리량 |
| `20260604-autoscaling-4case-time-progress.png` | 시간별 완료 이미지 수 |

2026-06-04 재실험 결과는 다음과 같다.

| 케이스 | 전체 시간(초) | 평균 처리량(장/초) | p95 완료(초) | 최대 Ready | 최대 Desired | 최대 노드 CPU% | 최대 노드 Memory% |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Fixed 1 Worker | 308.674 | 1.62 | 85.014 | 1 | 0 | 55.0 | 59.0 |
| HPA CPU 60% | 303.517 | 1.647 | 78.371 | 3 | 3 | 49.0 | 62.0 |
| KEDA min 1 | 283.749 | 1.762 | 58.162 | 4 | 8 | 67.0 | 67.0 |
| KEDA min 0 | 276.798 | 1.806 | 54.597 | 4 | 20 | 57.0 | 64.0 |

이 결과는 KEDA가 CPU 사용률이 아니라 RabbitMQ backlog를 기준으로 빠르게 확장 목표를 세우며, HPA CPU보다 배치 완료 시점이 빠르다는 것을 보여준다. 다만 최대 desired replica가 20이어도 실제 ready replica는 4개까지였으므로, 다음 성능 개선은 KEDA 설정만이 아니라 Worker resource request, 노드 여유 자원, 스케줄링 정책을 함께 봐야 한다.

## 해석 기준

| 상황 | 해석 |
| --- | --- |
| HPA desired는 높지만 ready Worker가 낮음 | 노드 리소스 부족 또는 taint 때문에 스케줄링이 막힌 상태 |
| queue ready가 높고 Worker replica가 낮음 | KEDA trigger, RabbitMQ metric, ScaledObject 상태를 확인 |
| Node CPU가 80% 이상으로 오래 유지 | Worker CPU request/limit, 노드 증설, KEDA threshold 조정 필요 |
| Node Memory가 80% 이상으로 오래 유지 | Worker concurrency는 늘리지 말고 Pod 수/노드 수 기준으로 조정 |
| 특정 노드에 Worker가 몰림 | node affinity, topology spread constraints 검토 필요 |

## 운영 주의사항

- `node-exporter`는 host filesystem을 read-only로 mount한다.
- 공개 인터넷에 `node-exporter:9100`을 직접 노출하지 않는다.
- Prometheus 내부 scrape만 허용한다.
- Worker 리소스 부족을 판단할 때는 실제 사용률과 `requests/limits`를 함께 본다.
- MVP 클러스터는 리소스가 작기 때문에 KEDA desired replica와 ready replica가 다를 수 있다.
