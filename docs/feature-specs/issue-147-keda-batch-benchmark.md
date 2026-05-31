# Issue 147. KEDA 500장 배치 비교 벤치마크 기반 추가

## 1. 작업 목적

대용량 이미지 전처리 실험에서 KEDA 자동 확장이 실제로 어떤 효과를 내는지 비교할 수 있도록 운영 기반을 추가한다.

비교 대상은 다음 3가지다.

1. KEDA 비활성화, Worker 1개 고정
2. KEDA 비활성화, Worker N개 고정
3. KEDA 활성화, Worker 0~20개 자동 확장

## 2. 구현 범위

이번 작업은 전처리 알고리즘을 바꾸지 않는다.

추가하는 범위는 다음과 같다.

| 영역 | 내용 |
| --- | --- |
| Kubernetes | Prometheus, Grafana, kube-state-metrics manifest 추가 |
| NGINX | `/grafana/` 라우팅 추가 |
| Script | KEDA ON/OFF 전환 스크립트 추가 |
| Script | 500장 배치 업로드/Job 완료 측정 스크립트 추가 |
| Docs | KEDA 배치 비교 실험 운영 문서 추가 |

## 3. Kubernetes 관측성 구성

추가된 구성은 다음과 같다.

```text
Prometheus
  -> backend-api /actuator/prometheus
  -> preprocess-worker /actuator/prometheus
  -> rabbitmq /metrics
  -> kube-state-metrics /metrics
  -> keda-operator /metrics

Grafana
  -> Prometheus datasource
  -> DocPrep KEDA 500장 배치 비교 dashboard

kube-state-metrics
  -> Deployment, HPA, Pod 상태 metric 제공
```

Grafana는 NGINX를 통해 `/grafana/` 경로로 접근한다.

## 4. KEDA 전환 스크립트

파일:

```text
scripts/k8s-scale-mode.ps1
```

지원 모드:

| 모드 | 동작 |
| --- | --- |
| `keda-on` | ScaledObject와 TriggerAuthentication을 적용하고 Worker를 KEDA 제어로 전환 |
| `keda-off-fixed` | ScaledObject/HPA를 제거하고 Worker replica를 고정 |

예시:

```powershell
.\scripts\k8s-scale-mode.ps1 -Mode keda-on -KubeConfig "C:\path\to\kube.conf"
.\scripts\k8s-scale-mode.ps1 -Mode keda-off-fixed -FixedReplicas 1 -KubeConfig "C:\path\to\kube.conf"
.\scripts\k8s-scale-mode.ps1 -Mode keda-off-fixed -FixedReplicas 20 -KubeConfig "C:\path\to\kube.conf"
```

## 5. 배치 벤치마크 스크립트

파일:

```text
scripts/k8s-batch-benchmark.ps1
```

처리 순서:

1. 프로젝트 생성 또는 기존 프로젝트 조회
2. 입력 이미지 수집
3. SHA-256 계산
4. 업로드 세션 생성
5. presigned upload URL 발급
6. Object Storage에 원본 업로드
7. 업로드 완료 처리
8. 이미지 메타데이터 조회
9. 전처리 Job 생성
10. Job 완료까지 polling
11. 결과 JSON 저장

결과 저장 경로:

```text
benchmark-results/
```

이 경로는 `.gitignore`에 추가하여 실험 결과가 원격 저장소에 올라가지 않도록 했다.

## 6. Grafana dashboard

기본 dashboard 이름:

```text
DocPrep KEDA 500장 배치 비교
```

주요 패널:

| 패널 | Prometheus query 예시 |
| --- | --- |
| Worker replica 수 | `kube_deployment_status_replicas{deployment="preprocess-worker"}` |
| HPA desired replica | `kube_horizontalpodautoscaler_status_desired_replicas{horizontalpodautoscaler="keda-hpa-preprocess-worker"}` |
| RabbitMQ 대기 메시지 | `rabbitmq_queue_messages_ready{queue="image.preprocess.normal"}` |
| RabbitMQ consumer 수 | `rabbitmq_queue_consumers{queue="image.preprocess.normal"}` |
| Worker 처리량 | `sum(rate(worker_job_processed_total[1m]))` |

## 7. 완료 조건

이번 작업의 완료 조건은 다음과 같다.

- Kubernetes manifest가 `kubectl apply -k infra/k8s --dry-run=client`에서 파싱된다.
- PowerShell 스크립트가 문법 오류 없이 로드된다.
- KEDA ON/OFF 전환 절차가 문서화되어 있다.
- 500장 배치 실험 실행 절차가 문서화되어 있다.
- PR 본문에 검증 결과가 포함된다.

## 8. 후속 작업

이번 작업은 실험 기반을 추가하는 작업이다.

후속으로 필요한 작업은 다음과 같다.

1. 실제 500장 데이터셋으로 3개 시나리오 반복 측정
2. 결과 JSON을 표로 모으는 요약 스크립트 추가
3. Grafana dashboard에 CPU/Memory 패널 추가
4. 운영용 Grafana admin password를 Secret으로 분리
