# 이슈 151: 자동 Kubernetes 배포와 500장 배치 검증

## 목적

`main`에 코드가 머지된 뒤 GHCR 이미지 빌드가 성공하면 Kubernetes 배포가 자동 실행되도록 GitHub Actions 흐름을 보강한다.
또한 다운로드 폴더의 이미지 파일을 기준으로 500장 배치 전처리 테스트를 수행하고, 과정과 결과를 PDF로 남긴다.

## 작업 범위

1. `Deploy Kubernetes` workflow에 `workflow_run` trigger를 추가한다.
2. 자동 배포 입력값은 `production` environment variables에서 읽도록 정리한다.
3. `Build GHCR Images`가 만든 commit SHA 기반 이미지 태그를 `Deploy Kubernetes`가 그대로 사용한다.
4. 다운로드 폴더 이미지로 500장 테스트 입력셋을 만든다.
5. 실제 배포 환경에서 업로드, Job 생성, Worker 처리, KEDA scale 상태를 검증한다.
6. 검증 결과를 `benchmark-results/` 하위 JSON/HTML/PDF 산출물로 저장한다.

## 자동 배포 흐름

```text
main push 또는 PR merge
-> Build GHCR Images 성공
-> Deploy Kubernetes 자동 실행
-> self-hosted runner에서 kubectl apply
-> rollout 상태 확인
```

자동 배포는 `workflow_run` 이벤트에서 실행되므로 PR 브랜치에서는 최종 자동 실행까지 검증할 수 없다.
PR 단계에서는 수동 `workflow_dispatch` 실행으로 같은 배포 job이 정상 동작하는지 확인한다.

## 500장 검증 방식

다운로드 폴더에 있는 이미지 파일을 재귀 탐색한다.
실제 이미지 수가 500장보다 적으면 같은 이미지를 순환 복사해서 임시 입력셋을 만들고, 중복 checksum으로 거절되지 않도록 복사본마다 식별 바이트를 추가한다.

검증 대상은 다음이다.

- 프로젝트 생성 또는 기존 프로젝트 재사용
- presigned URL 발급
- MinIO 원본 업로드
- 업로드 완료 처리와 이미지 메타데이터 생성
- Job 생성
- RabbitMQ 기반 Worker 처리
- KEDA/HPA/Worker replica 변화
- 처리 결과 다운로드 가능 여부

## 완료 조건

- `Deploy Kubernetes` workflow가 수동 실행으로 성공한다.
- 자동 배포 trigger와 environment variable 사용 방식이 문서화된다.
- 500장 테스트 결과 JSON이 생성된다.
- 테스트 과정과 요약 결과 PDF가 생성된다.
- 실패 또는 제약이 있으면 리포트에 명시한다.

## 검증 결과

### 자동 배포 workflow

- 수동 검증 run: `https://github.com/som141/opensource-cloud-/actions/runs/26742366531`
- 결과: 성공
- 검증 방식: `ci/som/151` 브랜치에서 `workflow_dispatch`로 `Deploy Kubernetes`를 실행해 self-hosted runner의 `kubectl apply`와 rollout을 확인했다.
- 자동 `workflow_run` 검증: PR merge 후 `main`의 `Build GHCR Images` 성공 이벤트에서 최종 확인한다.

### 500장 배치 테스트

- 입력 원본: `Downloads` 하위 이미지 후보 147개
- 디코딩 가능 원본: 144개
- 테스트 입력셋: 500개 JPEG, 총 29.60MB
- KEDA ON Job ID: `7`
- KEDA OFF Job ID: `8`
- KEDA ON 결과: 500개 성공, 0개 실패
- KEDA OFF 결과: 500개 성공, 0개 실패
- KEDA ON Job 처리 시간: 52.572초
- KEDA OFF Job 처리 시간: 98.1초
- KEDA ON Job 생성부터 완료까지: 81.755초
- KEDA OFF Job 생성부터 완료까지: 100.815초
- KEDA ON 결과 ZIP: `benchmark-results/job-7-processed-results.zip`
- KEDA OFF 결과 ZIP: `benchmark-results/job-8-processed-results.zip`
- ZIP 엔트리: 500개
- 결과 리포트 JSON: `benchmark-results/20260601-keda-on-500-result.json`
- 비교 리포트 JSON: `benchmark-results/20260601-keda-comparison-report.json`
- 비교 리포트 PDF: `benchmark-results/20260601-keda-comparison-report.pdf`

### KEDA 관측 결과

- `minReplicaCount`: 0
- `maxReplicaCount`: 20
- normal queue target: 25
- high queue target: 10
- 관측 최대 desired/deployment replica: 20
- 관측 최대 ready Worker: 4
- 피크 시 Pending Worker: 16
- Pending 사유: 현재 4개 노드 기준 CPU/메모리 부족과 control-plane taint 때문에 추가 Worker가 스케줄링되지 못했다.

이번 결과는 KEDA trigger와 최대 스케일 요청은 정상이고, 실제 병렬 처리량은 현재 클러스터 리소스에 의해 제한된다는 의미다.

### KEDA ON/OFF 비교 분석

- KEDA ON은 scale-to-zero에서 시작했기 때문에 큐 대기 시간이 29.183초였다.
- KEDA OFF는 Worker 1개가 이미 실행 중이어서 큐 대기 시간이 2.716초였다.
- 순수 Worker 처리 구간은 KEDA ON 52.572초, KEDA OFF 98.1초로 KEDA ON이 1.87배 빨랐다.
- Job 생성부터 완료까지는 KEDA ON 81.755초, KEDA OFF 100.815초로 KEDA ON이 19.06초 빨랐다.
- 현재 클러스터에서 KEDA ON의 실제 ready Worker는 최대 4개였기 때문에, 노드 리소스를 늘리면 KEDA ON과 OFF의 차이는 더 커질 가능성이 높다.

### Kubernetes HPA CPU 비교 분석

KEDA의 필요성을 입증하기 위해 Kubernetes 기본 HPA CPU 방식도 추가로 실험했다.

| 방식 | 큐 대기 | 처리 시간 | 전체 시간 | 성공/전체 | 관측 replica |
| --- | ---: | ---: | ---: | ---: | ---: |
| Fixed 1 | 2.716초 | 98.1초 | 100.815초 | 500/500 | 1 |
| HPA CPU | 2.145초 | 91.736초 | 93.881초 | 500/500 | 3 |
| KEDA min 1 | 2.209초 | 59.728초 | 61.937초 | 500/500 | 10 |
| KEDA min 0 | 29.183초 | 52.572초 | 81.755초 | 500/500 | 20 |

- HPA CPU는 Worker 1개에서 3개까지 늘었지만 CPU 사용률 기반이라 큐 적체를 직접 보지 못했다.
- KEDA min 1은 HPA와 같은 `min=1` 출발선에서 가장 빠른 완료 시간을 보였다.
- KEDA min 1은 HPA CPU보다 처리 구간 기준 1.54배 빨랐다.
- KEDA min 0은 scale-to-zero 비용 절감 모드라 시작 지연이 포함된다.
- HPA 비교를 위해 `metrics-server`를 클러스터에 설치했고, `kubectl top nodes`가 정상 동작하는 것을 확인했다.
- 운영 상태는 실험 후 다시 KEDA ON `minReplicaCount=0`으로 복구했다.

### 리포트 생성 방식 수정

처음 생성한 PDF는 PowerShell 파이프로 Python 코드를 전달하는 과정에서 한글 문자열이 `???`로 변환되는 문제가 있었다.
이를 피하기 위해 `scripts/generate-keda-comparison-report.py`를 추가하고, UTF-8 Python 파일에서 ReportLab과 Windows `Malgun Gothic` 폰트를 사용해 PDF를 생성하도록 변경했다.
추가 비교 실험은 `scripts/generate-autoscaling-comparison-report.py`로 생성하며, 결과는 `benchmark-results/20260601-autoscaling-comparison-report.pdf`에 저장된다.
