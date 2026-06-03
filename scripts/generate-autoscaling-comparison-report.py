from __future__ import annotations

import json
import zipfile
from datetime import datetime
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import PageBreak, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle


ROOT = Path("benchmark-results")
INPUT_SUMMARY_PATH = Path(r"C:\tmp\docprep-500-input\input-summary.json")


SCENARIOS = [
    {
        "key": "fixed_1",
        "label": "Fixed 1",
        "job_id": 8,
        "description": "오토스케일링 없음, Worker 1개 고정",
        "kind": "baseline",
    },
    {
        "key": "hpa_cpu",
        "label": "HPA CPU",
        "job_id": 10,
        "description": "Kubernetes 기본 HPA, CPU 60%, min 1, max 20",
        "kind": "hpa",
    },
    {
        "key": "keda_min1",
        "label": "KEDA min 1",
        "job_id": 9,
        "description": "RabbitMQ queue length 기반 KEDA, min 1, max 20",
        "kind": "keda",
    },
    {
        "key": "keda_min0",
        "label": "KEDA min 0",
        "job_id": 7,
        "description": "RabbitMQ queue length 기반 KEDA, scale-to-zero, max 20",
        "kind": "keda",
    },
]


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8-sig"))


def parse_datetime(value: str) -> datetime:
    return datetime.fromisoformat(value)


def job_timing(job: dict) -> dict:
    created = parse_datetime(job["createdAt"])
    started = parse_datetime(job["startedAt"])
    completed = parse_datetime(job["completedAt"])
    return {
        "queue_wait_seconds": round((started - created).total_seconds(), 3),
        "processing_seconds": round((completed - started).total_seconds(), 3),
        "total_seconds": round((completed - created).total_seconds(), 3),
    }


def zip_entry_count(path: Path) -> int:
    with zipfile.ZipFile(path) as zip_file:
        return len(zip_file.infolist())


def megabytes(value: int) -> str:
    return f"{value / 1024 / 1024:.2f} MB"


def load_scenario_result(scenario: dict) -> dict:
    job_id = scenario["job_id"]
    detail = read_json(ROOT / f"job-{job_id}-detail.json")["result"]
    summary = read_json(ROOT / f"job-{job_id}-summary.json")["result"]
    zip_path = ROOT / f"job-{job_id}-processed-results.zip"
    result = {
        **scenario,
        "status": detail["status"],
        "total": summary["total"],
        "succeeded": summary["succeeded"],
        "failed": summary["failed"],
        "progress_percent": summary["progressPercent"],
        "zip_bytes": zip_path.stat().st_size,
        "zip_entries": zip_entry_count(zip_path),
        **job_timing(detail),
    }

    result_path_candidates = {
        "keda_min1": "20260601-211625-keda-min1-500-20260601-500-images.json",
        "hpa_cpu": "20260601-212337-hpa-cpu-60-min1-500-20260601-500-images.json",
        "keda_min0": "20260601-keda-on-500-result.json",
    }
    result_file = result_path_candidates.get(scenario["key"])
    if result_file and (ROOT / result_file).exists():
        raw = read_json(ROOT / result_file)
        samples = raw.get("kubernetesSamples", [])
        result["sample_count"] = len(samples)
        result["observed_max_worker_replicas"] = max(
            [sample.get("workerReplicas") or 0 for sample in samples],
            default=None,
        )
        result["observed_max_ready_replicas"] = max(
            [sample.get("workerReadyReplicas") or 0 for sample in samples],
            default=None,
        )
        result["observed_max_hpa_desired_replicas"] = max(
            [sample.get("hpaDesiredReplicas") or 0 for sample in samples],
            default=None,
        )

    if scenario["key"] == "hpa_cpu" and (ROOT / "worker-hpa-cpu-after-job-10.json").exists():
        hpa = read_json(ROOT / "worker-hpa-cpu-after-job-10.json")
        status = hpa.get("status", {})
        result["observed_max_worker_replicas"] = max(result.get("observed_max_worker_replicas") or 0, status.get("currentReplicas") or 0)
        result["observed_max_hpa_desired_replicas"] = max(result.get("observed_max_hpa_desired_replicas") or 0, status.get("desiredReplicas") or 0)

    if scenario["key"] == "keda_min0" and (ROOT / "20260601-keda-on-500-result.json").exists():
        raw = read_json(ROOT / "20260601-keda-on-500-result.json")
        keda = raw.get("keda", {})
        result["observed_max_worker_replicas"] = keda.get("observedMaxDeploymentReplicas")
        result["observed_max_ready_replicas"] = keda.get("observedMaxReadyReplicas")
        result["observed_max_hpa_desired_replicas"] = keda.get("observedMaxDesiredReplicas")

    if scenario["key"] == "fixed_1":
        result["observed_max_worker_replicas"] = 1
        result["observed_max_ready_replicas"] = 1
        result["observed_max_hpa_desired_replicas"] = None

    return result


def build_report_data() -> dict:
    input_summary = json.loads(INPUT_SUMMARY_PATH.read_text(encoding="utf-8"))
    scenarios = [load_scenario_result(scenario) for scenario in SCENARIOS]
    fixed = next(item for item in scenarios if item["key"] == "fixed_1")
    keda_min1 = next(item for item in scenarios if item["key"] == "keda_min1")
    hpa_cpu = next(item for item in scenarios if item["key"] == "hpa_cpu")

    return {
        "generated_at": datetime.now().isoformat(timespec="seconds"),
        "input": input_summary,
        "scenarios": scenarios,
        "analysis": {
            "keda_min1_vs_fixed_processing_speedup": round(fixed["processing_seconds"] / keda_min1["processing_seconds"], 2),
            "keda_min1_vs_hpa_processing_speedup": round(hpa_cpu["processing_seconds"] / keda_min1["processing_seconds"], 2),
            "keda_min1_vs_hpa_total_delta_seconds": round(hpa_cpu["total_seconds"] - keda_min1["total_seconds"], 3),
            "keda_min1_vs_fixed_total_delta_seconds": round(fixed["total_seconds"] - keda_min1["total_seconds"], 3),
        },
        "conclusion": (
            "KEDA min 1은 HPA CPU와 Fixed 1보다 빠르게 500장 배치를 완료했다. "
            "HPA CPU도 1개에서 3개까지 늘었지만 CPU가 올라간 뒤에 반응하므로 큐 기반 KEDA보다 늦게 확장됐다. "
            "KEDA min 0은 유휴 비용 절감을 보여주지만 scale-from-zero 대기 시간이 포함된다."
        ),
    }


def paragraph(text: str, style: ParagraphStyle) -> Paragraph:
    return Paragraph(str(text).replace("\n", "<br/>"), style)


def make_table(data: list[list[object]], widths: list[float], style: ParagraphStyle) -> Table:
    rows = [[cell if hasattr(cell, "wrap") else paragraph(cell, style) for cell in row] for row in data]
    table = Table(rows, colWidths=widths, repeatRows=1)
    table.setStyle(
        TableStyle(
            [
                ("FONTNAME", (0, 0), (-1, -1), "Malgun"),
                ("FONTSIZE", (0, 0), (-1, -1), 8.3),
                ("LEADING", (0, 0), (-1, -1), 11.5),
                ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#d6cbb9")),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 5),
                ("RIGHTPADDING", (0, 0), (-1, -1), 5),
                ("TOPPADDING", (0, 0), (-1, -1), 5),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#e7efe4")),
                ("FONTNAME", (0, 0), (-1, 0), "Malgun-Bold"),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.HexColor("#00614d")),
            ]
        )
    )
    return table


def write_markdown(data: dict) -> None:
    rows = "\n".join(
        [
            "| 방식 | 큐 대기 | 처리 시간 | 전체 시간 | 성공/전체 | 관측 replica |",
            "| --- | ---: | ---: | ---: | ---: | ---: |",
            *[
                (
                    f"| {item['label']} | {item['queue_wait_seconds']}초 | "
                    f"{item['processing_seconds']}초 | {item['total_seconds']}초 | "
                    f"{item['succeeded']}/{item['total']} | "
                    f"{item.get('observed_max_worker_replicas', '-')} |"
                )
                for item in data["scenarios"]
            ],
        ]
    )
    markdown = f"""# Kubernetes 오토스케일링 500장 배치 비교 리포트

생성 시각: {data["generated_at"]}

## 결론

{data["conclusion"]}

## 핵심 수치

{rows}

## 분석

- KEDA min 1은 Fixed 1보다 처리 구간 기준 {data["analysis"]["keda_min1_vs_fixed_processing_speedup"]}배 빨랐다.
- KEDA min 1은 HPA CPU보다 처리 구간 기준 {data["analysis"]["keda_min1_vs_hpa_processing_speedup"]}배 빨랐다.
- HPA CPU는 최종적으로 replica 3개까지 늘었지만, 큐 길이가 아니라 CPU 사용률을 보고 반응하므로 배치 큐 적체를 직접 알지 못한다.
- KEDA min 0은 scale-to-zero 비용 절감 실험이고, KEDA min 1은 HPA와 같은 출발선에서의 성능 비교 실험이다.

## 산출물

- PDF: `benchmark-results/20260601-autoscaling-comparison-report.pdf`
- JSON: `benchmark-results/20260601-autoscaling-comparison-report.json`
"""
    (ROOT / "20260601-autoscaling-comparison-report.md").write_text(markdown, encoding="utf-8")


def write_pdf(data: dict) -> None:
    pdfmetrics.registerFont(TTFont("Malgun", r"C:\Windows\Fonts\malgun.ttf"))
    pdfmetrics.registerFont(TTFont("Malgun-Bold", r"C:\Windows\Fonts\malgunbd.ttf"))

    base = ParagraphStyle("BaseKo", fontName="Malgun", fontSize=9.4, leading=13.5, textColor=colors.HexColor("#24352e"))
    small = ParagraphStyle("SmallKo", parent=base, fontSize=8, leading=11, textColor=colors.HexColor("#64736c"))
    title = ParagraphStyle("TitleKo", parent=base, fontName="Malgun-Bold", fontSize=24, leading=30, textColor=colors.HexColor("#102019"))
    h1 = ParagraphStyle("H1Ko", parent=base, fontName="Malgun-Bold", fontSize=15.5, leading=20, spaceBefore=12, spaceAfter=7)
    accent = ParagraphStyle("AccentKo", parent=base, fontName="Malgun-Bold", fontSize=11, leading=16, textColor=colors.HexColor("#00614d"))

    doc = SimpleDocTemplate(
        str(ROOT / "20260601-autoscaling-comparison-report.pdf"),
        pagesize=A4,
        leftMargin=14 * mm,
        rightMargin=14 * mm,
        topMargin=14 * mm,
        bottomMargin=14 * mm,
        title="Kubernetes 오토스케일링 500장 배치 비교 리포트",
    )

    scenario_rows = [["방식", "큐 대기", "처리 시간", "전체 시간", "성공/전체", "관측 replica"]]
    for item in data["scenarios"]:
        scenario_rows.append(
            [
                item["label"],
                f"{item['queue_wait_seconds']}초",
                f"{item['processing_seconds']}초",
                f"{item['total_seconds']}초",
                f"{item['succeeded']}/{item['total']}",
                item.get("observed_max_worker_replicas", "-"),
            ]
        )

    detail_rows = [["방식", "설명", "해석"]]
    detail_rows.extend(
        [
            ["Fixed 1", "Worker 1개 고정", "오토스케일링 없는 기준선이다. 처리량이 Worker 1개에 묶인다."],
            ["HPA CPU", "CPU 60%, min 1, max 20", "CPU가 오른 뒤 반응한다. 큐 적체를 직접 보지 못해 KEDA보다 늦다."],
            ["KEDA min 1", "RabbitMQ queue length, min 1, max 20", "HPA와 같은 min 1 출발선에서 가장 빠른 처리 시간을 보였다."],
            ["KEDA min 0", "RabbitMQ queue length, scale-to-zero", "유휴 비용 절감 모드다. 시작 지연이 포함되지만 Fixed 1보다 전체 시간이 짧았다."],
        ]
    )

    story = [
        paragraph("대규모 문서 이미지 전처리 · Autoscaling 비교", small),
        paragraph("500장 배치 처리 Fixed / HPA / KEDA 비교 리포트", title),
        paragraph(data["conclusion"], base),
        Spacer(1, 8),
        make_table(scenario_rows, [34 * mm, 25 * mm, 28 * mm, 28 * mm, 27 * mm, 26 * mm], base),
        paragraph("1. 결론", h1),
        paragraph(
            f"KEDA min 1은 Fixed 1보다 처리 구간 기준 {data['analysis']['keda_min1_vs_fixed_processing_speedup']}배 빨랐고, "
            f"HPA CPU보다 {data['analysis']['keda_min1_vs_hpa_processing_speedup']}배 빨랐다.",
            accent,
        ),
        paragraph(
            "HPA CPU도 최종적으로 replica 3개까지 증가했지만, 큐 길이가 아니라 CPU 사용률을 보고 반응한다. "
            "이미지 전처리 Worker는 MinIO 다운로드/업로드, RabbitMQ ack, 이미지 처리 CPU 부하가 섞여 있으므로 "
            "CPU만으로는 대기 메시지 수를 직접 알 수 없다.",
            base,
        ),
        paragraph(
            "KEDA는 RabbitMQ queue length를 직접 스케일 기준으로 사용한다. 따라서 배치 작업이 큐에 쌓이는 순간 "
            "처리해야 할 backlog를 바로 스케일링 신호로 바꿀 수 있다.",
            base,
        ),
        paragraph("2. 실험 조건", h1),
        make_table(
            [
                ["항목", "값"],
                ["입력셋", f"{data['input']['createdCount']}개 JPEG, {megabytes(data['input']['totalBytes'])}"],
                ["원본 후보 / 사용 가능 원본", f"{data['input']['candidateCount']}개 / {data['input']['validSourceCount']}개"],
                ["API 경로", "ngrok 공개 도메인 → NGINX → Spring API"],
                ["처리 경로", "MinIO 원본 업로드 → RabbitMQ 메시지 → Worker 전처리 → 결과 ZIP"],
            ],
            [48 * mm, 120 * mm],
            base,
        ),
        paragraph("3. 방식별 해석", h1),
        make_table(detail_rows, [32 * mm, 48 * mm, 88 * mm], base),
        PageBreak(),
        paragraph("4. 왜 KEDA가 필요한가", h1),
        paragraph(
            "Kubernetes 기본 HPA는 CPU/메모리처럼 Pod 내부 리소스 사용량을 기준으로 동작한다. "
            "반면 큐 기반 배치 Worker에서 중요한 신호는 현재 CPU보다 '대기 중인 메시지가 몇 개인가'이다. "
            "큐가 500개 쌓였는데 CPU가 아직 충분히 높지 않으면 HPA는 늦게 움직일 수 있다.",
            base,
        ),
        paragraph(
            "이번 HPA CPU 실험도 최종적으로 3개까지 늘었지만, KEDA min 1보다 전체 완료 시간이 길었다. "
            "KEDA min 1은 queue length를 기준으로 더 직접적인 확장 판단을 하므로 배치 큐 처리에 더 적합하다.",
            base,
        ),
        paragraph(
            "KEDA min 0은 성능만 보면 min 1보다 cold start 비용이 있다. 대신 작업이 없을 때 Worker를 0개로 줄여 비용을 절감한다. "
            "따라서 운영 기본값은 트래픽 패턴에 따라 선택해야 한다. 지연 시간이 중요하면 min 1, 비용 절감이 중요하면 min 0이 맞다.",
            base,
        ),
        paragraph("5. 다음 실험", h1),
        make_table(
            [
                ["실험", "목적"],
                ["HPA target 30/40/50%", "CPU 임계값을 낮췄을 때 KEDA와의 차이가 줄어드는지 확인"],
                ["KEDA value 10/25/50", "Worker당 queue length 기준 변경에 따른 반응 속도 확인"],
                ["Worker request 조정", "현재 Pending 병목을 줄이고 KEDA max 20이 실제 ready까지 가는지 확인"],
                ["1000장 이상", "큐 길이가 커질수록 KEDA와 HPA의 격차가 커지는지 확인"],
            ],
            [52 * mm, 116 * mm],
            base,
        ),
    ]

    def page_number(canvas, current_doc):
        canvas.saveState()
        canvas.setFont("Malgun", 8)
        canvas.setFillColor(colors.HexColor("#6c776f"))
        canvas.drawRightString(A4[0] - 14 * mm, 9 * mm, str(current_doc.page))
        canvas.restoreState()

    doc.build(story, onFirstPage=page_number, onLaterPages=page_number)


def main() -> None:
    ROOT.mkdir(exist_ok=True)
    data = build_report_data()
    (ROOT / "20260601-autoscaling-comparison-report.json").write_text(
        json.dumps(data, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    write_markdown(data)
    write_pdf(data)
    print(ROOT / "20260601-autoscaling-comparison-report.pdf")


if __name__ == "__main__":
    main()
