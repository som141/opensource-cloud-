# KEDA 500장 배치 비교 실험 가이드

이 문서는 동일한 이미지 배치를 기준으로 KEDA 자동 확장 방식과 고정 Worker 방식의 처리 성능을 비교하는 절차를 정리한다.

## 1. 실험 목적

목표는 단순히 “이미지 500장 처리 성공”을 확인하는 것이 아니다.

비교해야 할 항목은 다음과 같다.

| 항목 | 설명 |
| --- | --- |
| 전체 처리 시간 | Job 생성부터 모든 JobItem 완료까지 걸린 시간 |
| 처리량 | 초당 처리 이미지 수 |
| Worker replica 변화 | KEDA가 몇 개까지 Worker를 늘렸는지 |
| RabbitMQ 적체 | `messages_ready`, `messages_unacknowledged`, consumer 수 |
| 실패율 | 실패한 JobItem 수와 실패 원인 |
| 리소스 사용량 | Worker CPU/Memory 사용량 |
| 비용 관점 | Worker를 계속 켜두는 방식과 필요할 때만 늘리는 방식의 차이 |

## 2. 비교 시나리오

최소 비교 시나리오는 아래 3개다.

| 시나리오 | 설명 | 목적 |
| --- | --- | --- |
| `keda-off-1` | KEDA 비활성화, Worker 1개 고정 | 가장 느린 기준값 |
| `keda-off-20` | KEDA 비활성화, Worker 20개 고정 | 최대 병렬 처리 기준값 |
| `keda-on` | KEDA 활성화, Worker 0~20개 자동 확장 | 실제 운영 방식 |

KEDA는 이미지 용량을 기준으로 확장하지 않는다. RabbitMQ queue에 쌓인 메시지 수를 기준으로 확장한다.

현재 기준은 다음과 같다.

| Queue | 확장 기준 |
| --- | --- |
| `image.preprocess.normal` | Worker 1개당 대기 메시지 25개 |
| `image.preprocess.high` | Worker 1개당 대기 메시지 10개 |

예를 들어 일반 큐에 500개 메시지가 쌓이면 `ceil(500 / 25) = 20`이므로 최대 Worker 20개까지 올라갈 수 있다.

## 3. 사전 준비

필요한 준비물은 다음과 같다.

1. Kubernetes cluster 접근 가능한 `kubeconfig`
2. 배포된 DocPrep Cloud API URL
3. 테스트 사용자 Access Token
4. 이미지 500장 디렉터리 또는 ZIP 파일
5. KEDA CRD 설치
6. Prometheus, Grafana, kube-state-metrics 배포

Access Token은 브라우저에서 로그인한 뒤 DevTools Console에서 확인할 수 있다.

```javascript
localStorage.getItem('doc-pipeline.access-token')
```

토큰은 실험 명령에만 사용하고 문서나 Git에 저장하지 않는다.

## 4. 관측성 리소스 배포

이번 작업에서 Kubernetes manifest에 아래 리소스를 추가했다.

| 리소스 | 역할 |
| --- | --- |
| Prometheus | API, Worker, RabbitMQ, KEDA, kube-state-metrics metric 수집 |
| Grafana | KEDA 배치 비교 dashboard 제공 |
| kube-state-metrics | Deployment, HPA, Pod replica metric 제공 |

배포는 기존 Kubernetes manifest와 동일하게 진행한다.

```powershell
kubectl --kubeconfig "C:\path\to\kube.conf" apply -k infra/k8s
```

배포 후 상태를 확인한다.

```powershell
kubectl --kubeconfig "C:\path\to\kube.conf" -n docprep-cloud get pods,svc | Select-String "grafana|prometheus|kube-state"
```

NGINX를 통해 Grafana에 접근할 수 있다.

```text
https://YOUR_DOMAIN/grafana/
```

초기 테스트 계정은 다음과 같다.

```text
ID: admin
PW: admin
```

운영 배포에서는 반드시 Grafana 비밀번호를 별도 Secret으로 교체해야 한다.

## 5. KEDA ON/OFF 전환

전환 스크립트는 `scripts/k8s-scale-mode.ps1`이다.

KEDA 활성화:

```powershell
.\scripts\k8s-scale-mode.ps1 `
  -Mode keda-on `
  -KubeConfig "C:\path\to\kube.conf"
```

KEDA 비활성화 후 Worker 1개 고정:

```powershell
.\scripts\k8s-scale-mode.ps1 `
  -Mode keda-off-fixed `
  -FixedReplicas 1 `
  -KubeConfig "C:\path\to\kube.conf"
```

KEDA 비활성화 후 Worker 20개 고정:

```powershell
.\scripts\k8s-scale-mode.ps1 `
  -Mode keda-off-fixed `
  -FixedReplicas 20 `
  -KubeConfig "C:\path\to\kube.conf"
```

## 6. 500장 벤치마크 실행

벤치마크 스크립트는 `scripts/k8s-batch-benchmark.ps1`이다.

기본 실행 예시는 다음과 같다.

```powershell
.\scripts\k8s-batch-benchmark.ps1 `
  -Scenario "keda-on-500" `
  -ApiBaseUrl "https://YOUR_DOMAIN/api" `
  -AccessToken "<ACCESS_TOKEN>" `
  -InputPath "C:\dataset\doc-images-500" `
  -MaxFiles 500 `
  -KubeConfig "C:\path\to\kube.conf" `
  -NgrokSkipBrowserWarning
```

ZIP 파일을 넣어도 된다.

```powershell
.\scripts\k8s-batch-benchmark.ps1 `
  -Scenario "keda-off-1-500" `
  -ApiBaseUrl "https://YOUR_DOMAIN/api" `
  -AccessToken "<ACCESS_TOKEN>" `
  -InputPath "C:\dataset\doc-images-500.zip" `
  -MaxFiles 500 `
  -KubeConfig "C:\path\to\kube.conf" `
  -NgrokSkipBrowserWarning
```

ngrok 무료 도메인을 API 앞단에 둘 때는 응답이 경고 페이지로 바뀌지 않도록 `-NgrokSkipBrowserWarning`을 함께 사용한다.

스크립트는 presigned URL 원본 업로드 중 일시적인 네트워크 끊김이 발생하면 파일별 최대 5회까지 재시도한다.
Kubernetes 샘플링은 Worker가 0개인 상태에서도 실패하지 않도록 누락된 replica 필드를 `null`로 기록한다.

스크립트는 아래 순서로 동작한다.

1. 프로젝트 생성 또는 기존 프로젝트 조회
2. 입력 이미지 SHA-256 계산
3. 업로드 세션 생성
4. presigned upload URL 발급
5. Object Storage에 원본 업로드
6. 업로드 세션 완료 처리
7. 이미지 메타데이터 조회
8. 전처리 Job 생성
9. Job 완료까지 polling
10. 결과 JSON 저장

결과 파일은 기본적으로 `benchmark-results/` 아래에 저장된다.

```text
benchmark-results/20260601-020000-keda-on-500-500-images.json
```

## 7. 권장 실험 순서

캐시나 이전 큐 상태 영향을 줄이기 위해 아래 순서로 실행한다.

1. RabbitMQ queue가 비어 있는지 확인
2. `keda-off-1` 실행
3. 결과 JSON 저장
4. queue와 Job 상태가 안정화될 때까지 대기
5. `keda-off-20` 실행
6. 결과 JSON 저장
7. queue와 Job 상태가 안정화될 때까지 대기
8. `keda-on` 실행
9. 결과 JSON 저장
10. Grafana에서 Worker replica와 queue length 그래프 확인

RabbitMQ queue 확인:

```powershell
kubectl --kubeconfig "C:\path\to\kube.conf" `
  -n docprep-cloud exec deploy/rabbitmq -- `
  rabbitmqctl list_queues name messages_ready messages_unacknowledged messages consumers
```

## 8. 결과 해석 기준

결과 JSON에서 먼저 봐야 할 값은 다음과 같다.

| 필드 | 의미 |
| --- | --- |
| `durationSeconds` | 전체 처리 시간 |
| `fileCount` | 입력 이미지 수 |
| `succeeded` | 성공 JobItem 수 |
| `failed` | 실패 JobItem 수 |
| `kubernetesSamples[].workerReplicas` | polling 시점의 Worker replica 수 |
| `kubernetesSamples[].hpaDesiredReplicas` | HPA가 원하는 replica 수 |

처리량은 다음처럼 계산한다.

```text
처리량 = succeeded / durationSeconds
```

최종 비교 표는 아래 형태로 정리한다.

| 시나리오 | 이미지 수 | 성공 | 실패 | 총 시간 | 처리량 | 최대 Worker |
| --- | --- | --- | --- | --- | --- | --- |
| `keda-off-1` | 500 |  |  |  |  | 1 |
| `keda-off-20` | 500 |  |  |  |  | 20 |
| `keda-on` | 500 |  |  |  |  |  |

## 9. 주의사항

1. KEDA는 CPU 사용량이 아니라 RabbitMQ queue length를 기준으로 확장한다.
2. 500장 이미지가 한 번에 JobItem 500개로 생성되어야 KEDA 확장 효과가 보인다.
3. 이미지 1장이 매우 커도 queue 메시지는 1개이므로 Worker가 크게 늘어나지 않는다.
4. Worker 최대 20개를 요청해도 cluster CPU/Memory가 부족하면 Pod가 `Pending` 상태가 될 수 있다.
5. Grafana dashboard는 Prometheus scrape가 정상이어야 의미 있는 그래프를 보여준다.
6. 실제 논문/발표용 비교는 각 시나리오를 최소 3회 반복하고 평균값을 사용한다.

## 10. 관련 파일

| 파일 | 설명 |
| --- | --- |
| `infra/k8s/monitoring/prometheus-*.yml` | Prometheus 배포 |
| `infra/k8s/monitoring/grafana-*.yml` | Grafana 배포와 dashboard |
| `infra/k8s/monitoring/kube-state-metrics-*.yml` | Kubernetes 상태 metric 수집 |
| `scripts/k8s-scale-mode.ps1` | KEDA ON/OFF 전환 |
| `scripts/k8s-batch-benchmark.ps1` | 배치 업로드/Job 완료 측정 |
| `benchmark-results/` | 로컬 벤치마크 결과 저장 경로 |
