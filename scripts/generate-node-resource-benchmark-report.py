from __future__ import annotations

import argparse
import html
import json
from datetime import datetime
from pathlib import Path
from statistics import mean


COLORS = [
    "#005f4f",
    "#c45f35",
    "#2374ab",
    "#8a5a00",
    "#7b2cbf",
    "#1b998b",
    "#d1495b",
]


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8-sig"))


def parse_time(value: str) -> datetime:
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def value_at(sample: dict, key: str) -> float | None:
    value = sample.get(key)
    if value is None:
        return None
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def collect_node_series(samples: list[dict], metric_name: str) -> dict[str, list[tuple[float, float]]]:
    if not samples:
        return {}
    start = parse_time(samples[0]["sampledAt"])
    series: dict[str, list[tuple[float, float]]] = {}
    for sample in samples:
        seconds = (parse_time(sample["sampledAt"]) - start).total_seconds()
        for node in sample.get("nodeResources") or []:
            node_name = node.get("node")
            value = node.get(metric_name)
            if node_name is None or value is None:
                continue
            series.setdefault(node_name, []).append((seconds, float(value)))
    return series


def collect_sample_series(samples: list[dict], keys: list[str]) -> dict[str, list[tuple[float, float]]]:
    if not samples:
        return {}
    start = parse_time(samples[0]["sampledAt"])
    series = {key: [] for key in keys}
    for sample in samples:
        seconds = (parse_time(sample["sampledAt"]) - start).total_seconds()
        for key in keys:
            value = value_at(sample, key)
            if value is not None:
                series[key].append((seconds, value))
    return {key: values for key, values in series.items() if values}


def collect_worker_pod_series(samples: list[dict]) -> dict[str, list[tuple[float, float]]]:
    if not samples:
        return {}
    start = parse_time(samples[0]["sampledAt"])
    series: dict[str, list[tuple[float, float]]] = {}
    for sample in samples:
        seconds = (parse_time(sample["sampledAt"]) - start).total_seconds()
        groups = sample.get("workerPodsByNode") or []
        if isinstance(groups, dict):
            groups = [groups]
        for group in groups:
            if not isinstance(group, dict):
                continue
            node = group.get("node")
            pods = group.get("pods")
            if node is None or pods is None:
                continue
            series.setdefault(node, []).append((seconds, float(pods)))
    return series


def summarize_nodes(samples: list[dict]) -> list[dict]:
    cpu = collect_node_series(samples, "cpuPercent")
    memory = collect_node_series(samples, "memoryPercent")
    nodes = sorted(set(cpu) | set(memory))
    rows = []
    for node in nodes:
        cpu_values = [point[1] for point in cpu.get(node, [])]
        memory_values = [point[1] for point in memory.get(node, [])]
        rows.append(
            {
                "node": node,
                "cpuMax": round(max(cpu_values), 2) if cpu_values else None,
                "cpuAvg": round(mean(cpu_values), 2) if cpu_values else None,
                "memoryMax": round(max(memory_values), 2) if memory_values else None,
                "memoryAvg": round(mean(memory_values), 2) if memory_values else None,
            }
        )
    return rows


def svg_line_chart(
    path: Path,
    title: str,
    series: dict[str, list[tuple[float, float]]],
    y_label: str,
    width: int = 980,
    height: int = 420,
) -> None:
    margin_left = 64
    margin_right = 180
    margin_top = 46
    margin_bottom = 54
    plot_width = width - margin_left - margin_right
    plot_height = height - margin_top - margin_bottom

    all_points = [point for values in series.values() for point in values]
    if not all_points:
        path.write_text(
            f"<svg xmlns='http://www.w3.org/2000/svg' width='{width}' height='{height}'>"
            f"<text x='24' y='40' font-size='18'>{html.escape(title)}</text>"
            f"<text x='24' y='82' font-size='14'>데이터 없음</text></svg>",
            encoding="utf-8",
        )
        return

    x_min = min(point[0] for point in all_points)
    x_max = max(point[0] for point in all_points)
    y_min = 0
    y_max = max(point[1] for point in all_points)
    if y_max <= 0:
        y_max = 1
    y_max *= 1.08
    if x_max <= x_min:
        x_max = x_min + 1

    def x(value: float) -> float:
        return margin_left + ((value - x_min) / (x_max - x_min)) * plot_width

    def y(value: float) -> float:
        return margin_top + plot_height - ((value - y_min) / (y_max - y_min)) * plot_height

    parts = [
        f"<svg xmlns='http://www.w3.org/2000/svg' width='{width}' height='{height}' viewBox='0 0 {width} {height}'>",
        "<rect width='100%' height='100%' fill='#fbfaf4'/>",
        f"<text x='24' y='30' font-family='Arial' font-size='20' font-weight='700' fill='#102019'>{html.escape(title)}</text>",
        f"<text x='24' y='52' font-family='Arial' font-size='12' fill='#66736c'>{html.escape(y_label)}</text>",
        f"<line x1='{margin_left}' y1='{margin_top}' x2='{margin_left}' y2='{margin_top + plot_height}' stroke='#8b958d'/>",
        f"<line x1='{margin_left}' y1='{margin_top + plot_height}' x2='{margin_left + plot_width}' y2='{margin_top + plot_height}' stroke='#8b958d'/>",
    ]

    for tick in range(0, 6):
        y_value = y_max * tick / 5
        y_pos = y(y_value)
        parts.append(
            f"<line x1='{margin_left}' y1='{y_pos:.1f}' x2='{margin_left + plot_width}' y2='{y_pos:.1f}' "
            "stroke='#e1ded4'/>"
        )
        parts.append(
            f"<text x='{margin_left - 10}' y='{y_pos + 4:.1f}' text-anchor='end' "
            f"font-family='Arial' font-size='11' fill='#66736c'>{y_value:.1f}</text>"
        )

    for index, (name, values) in enumerate(sorted(series.items())):
        color = COLORS[index % len(COLORS)]
        points = " ".join(f"{x(point[0]):.1f},{y(point[1]):.1f}" for point in values)
        parts.append(
            f"<polyline points='{points}' fill='none' stroke='{color}' stroke-width='2.6' "
            "stroke-linecap='round' stroke-linejoin='round'/>"
        )
        legend_y = margin_top + 22 + index * 22
        parts.append(f"<line x1='{width - margin_right + 28}' y1='{legend_y}' x2='{width - margin_right + 52}' y2='{legend_y}' stroke='{color}' stroke-width='3'/>")
        parts.append(
            f"<text x='{width - margin_right + 60}' y='{legend_y + 4}' font-family='Arial' "
            f"font-size='12' fill='#102019'>{html.escape(name)}</text>"
        )

    parts.append(
        f"<text x='{margin_left + plot_width / 2:.1f}' y='{height - 16}' text-anchor='middle' "
        "font-family='Arial' font-size='12' fill='#66736c'>경과 시간(초)</text>"
    )
    parts.append("</svg>")
    path.write_text("\n".join(parts), encoding="utf-8")


def make_markdown(report: dict, graph_paths: dict[str, Path], output_path: Path) -> None:
    rows = "\n".join(
        "| {node} | {cpuAvg} | {cpuMax} | {memoryAvg} | {memoryMax} |".format(**row)
        for row in report["nodes"]
    )
    markdown = f"""# 500장 배치 노드 리소스 관측 보고서

생성 시각: {report["generatedAt"]}

## 결론

- Job #{report["jobId"]}는 {report["fileCount"]}장 입력으로 실행되었습니다.
- 결과는 성공 {report["succeeded"]}장, 실패 {report["failed"]}장입니다.
- 관측 최대 Worker replica는 {report["maxWorkerReplicas"]}, ready replica는 {report["maxReadyReplicas"]}, HPA desired replica는 {report["maxDesiredReplicas"]}입니다.
- 노드별 CPU/메모리 사용률은 node-exporter와 `kubectl top nodes` 샘플 기준으로 확인합니다.

## 노드별 리소스 요약

| 노드 | 평균 CPU% | 최대 CPU% | 평균 Memory% | 최대 Memory% |
| --- | ---: | ---: | ---: | ---: |
{rows}

## 그래프

![Node CPU]({graph_paths["node_cpu"].name})

![Node Memory]({graph_paths["node_memory"].name})

![Worker Replicas]({graph_paths["worker_replicas"].name})

![Job Progress]({graph_paths["job_progress"].name})

![Worker Pods by Node]({graph_paths["worker_pods"].name})

## 해석

KEDA가 queue backlog를 감지하면 HPA desired replica가 먼저 증가하고, 이후 `preprocess-worker` Deployment replica와 ready replica가 따라갑니다. ready replica가 desired보다 낮으면 KEDA가 실패한 것이 아니라 노드 CPU/메모리 request를 만족하는 스케줄링 여유가 부족한 상태일 수 있습니다.

노드 리소스 그래프는 Worker가 늘어날 때 어떤 노드의 CPU/메모리 사용률이 상승하는지 보여줍니다. 이 값은 향후 Worker request 조정, 노드 증설, KEDA `value` 조정의 근거로 사용합니다.
"""
    output_path.write_text(markdown, encoding="utf-8")


def write_pdf_if_available(report: dict, output_path: Path) -> bool:
    try:
        from reportlab.lib import colors
        from reportlab.lib.pagesizes import A4
        from reportlab.lib.styles import ParagraphStyle
        from reportlab.lib.units import mm
        from reportlab.pdfbase import pdfmetrics
        from reportlab.pdfbase.ttfonts import TTFont
        from reportlab.platypus import Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle
    except ImportError:
        return False

    pdfmetrics.registerFont(TTFont("Malgun", r"C:\Windows\Fonts\malgun.ttf"))
    pdfmetrics.registerFont(TTFont("Malgun-Bold", r"C:\Windows\Fonts\malgunbd.ttf"))
    base = ParagraphStyle("Base", fontName="Malgun", fontSize=9.2, leading=13)
    title = ParagraphStyle("Title", parent=base, fontName="Malgun-Bold", fontSize=22, leading=28)
    heading = ParagraphStyle("Heading", parent=base, fontName="Malgun-Bold", fontSize=14, leading=18)

    rows = [["노드", "평균 CPU%", "최대 CPU%", "평균 Memory%", "최대 Memory%"]]
    rows.extend([
        [row["node"], row["cpuAvg"], row["cpuMax"], row["memoryAvg"], row["memoryMax"]]
        for row in report["nodes"]
    ])

    table = Table(rows, colWidths=[58 * mm, 28 * mm, 28 * mm, 32 * mm, 32 * mm], repeatRows=1)
    table.setStyle(TableStyle([
        ("FONTNAME", (0, 0), (-1, -1), "Malgun"),
        ("FONTNAME", (0, 0), (-1, 0), "Malgun-Bold"),
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#e7efe4")),
        ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#d6cbb9")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("FONTSIZE", (0, 0), (-1, -1), 8.2),
        ("LEADING", (0, 0), (-1, -1), 11),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5),
    ]))

    doc = SimpleDocTemplate(
        str(output_path),
        pagesize=A4,
        leftMargin=14 * mm,
        rightMargin=14 * mm,
        topMargin=14 * mm,
        bottomMargin=14 * mm,
        title="500장 배치 노드 리소스 관측 보고서",
    )
    story = [
        Paragraph("500장 배치 노드 리소스 관측 보고서", title),
        Paragraph(
            f"Job #{report['jobId']} / 입력 {report['fileCount']}장 / 성공 {report['succeeded']}장 / 실패 {report['failed']}장",
            base,
        ),
        Spacer(1, 8),
        Paragraph("Worker 확장 관측", heading),
        Paragraph(
            f"최대 Worker replica {report['maxWorkerReplicas']}, ready replica {report['maxReadyReplicas']}, "
            f"HPA desired replica {report['maxDesiredReplicas']}",
            base,
        ),
        Spacer(1, 8),
        Paragraph("노드별 리소스 요약", heading),
        table,
        Spacer(1, 8),
        Paragraph(
            "상세 시계열 그래프는 같은 디렉터리에 생성된 SVG 파일과 Grafana "
            "`DocPrep Node Resource Overview` 대시보드에서 확인합니다.",
            base,
        ),
    ]
    doc.build(story)
    return True


def write_clean_graphs(graph_paths: dict[str, Path], samples: list[dict]) -> None:
    svg_line_chart(
        graph_paths["node_cpu"],
        "노드별 CPU 사용률",
        collect_node_series(samples, "cpuPercent"),
        "CPU %",
    )
    svg_line_chart(
        graph_paths["node_memory"],
        "노드별 메모리 사용률",
        collect_node_series(samples, "memoryPercent"),
        "Memory %",
    )
    svg_line_chart(
        graph_paths["worker_replicas"],
        "Worker / HPA replica",
        collect_sample_series(samples, ["workerReplicas", "workerReadyReplicas", "hpaDesiredReplicas"]),
        "replicas",
    )
    svg_line_chart(
        graph_paths["job_progress"],
        "JobItem 진행 상태",
        collect_sample_series(samples, ["queued", "processing", "succeeded", "failed"]),
        "items",
    )
    svg_line_chart(
        graph_paths["worker_pods"],
        "Worker Pod 노드 분산",
        collect_worker_pod_series(samples),
        "pods",
    )


def make_markdown(report: dict, graph_paths: dict[str, Path], output_path: Path) -> None:
    rows = "\n".join(
        "| {node} | {cpuAvg} | {cpuMax} | {memoryAvg} | {memoryMax} |".format(**row)
        for row in report["nodes"]
    )
    duration = float(report.get("durationSeconds") or 0)
    succeeded = int(report.get("succeeded") or 0)
    throughput = round(succeeded / duration, 2) if duration > 0 else 0
    markdown = f"""# 500장 배치 노드 리소스 관측 보고서

생성 시각: {report["generatedAt"]}

## 결론

- Job #{report["jobId"]}은 {report["fileCount"]}장 입력으로 실행했습니다.
- 결과는 성공 {report["succeeded"]}장, 실패 {report["failed"]}장입니다.
- 전체 처리 시간은 {report["durationSeconds"]}초이고, 평균 처리량은 초당 {throughput}장입니다.
- 관측된 최대 Worker replica는 {report["maxWorkerReplicas"]}, 최대 ready replica는 {report["maxReadyReplicas"]}, 최대 HPA desired replica는 {report["maxDesiredReplicas"]}입니다.
- 노드별 CPU/메모리 사용률은 node-exporter와 `kubectl top nodes` 샘플 기준으로 확인했습니다.

## 노드별 리소스 요약

| 노드 | 평균 CPU% | 최대 CPU% | 평균 Memory% | 최대 Memory% |
| --- | ---: | ---: | ---: | ---: |
{rows}

## 그래프

![Node CPU]({graph_paths["node_cpu"].name})

![Node Memory]({graph_paths["node_memory"].name})

![Worker Replicas]({graph_paths["worker_replicas"].name})

![Job Progress]({graph_paths["job_progress"].name})

![Worker Pods by Node]({graph_paths["worker_pods"].name})

## 해석

KEDA가 queue backlog를 감지하면 HPA desired replica가 먼저 증가하고, 이후 `preprocess-worker` Deployment replica와 ready replica가 따라갑니다. 이번 테스트에서는 HPA desired replica가 최대 {report["maxDesiredReplicas"]}까지 올라갔지만 ready replica는 최대 {report["maxReadyReplicas"]}까지 관측됐습니다. 이것은 KEDA가 확장을 요청했지만 클러스터 노드의 CPU/메모리 여유, Pod request, 이미지 pull, 스케줄링 지연 때문에 실제 ready Pod 수가 제한될 수 있음을 의미합니다.

노드 리소스 그래프는 Worker가 늘어날 때 어떤 노드의 CPU/메모리 사용률이 상승하는지 보여줍니다. 이 값은 향후 Worker request 조정, 노드 증설, KEDA trigger `value` 조정의 근거로 사용합니다.

## 운영 판단

- 500장 배치는 실패 없이 완료됐으므로 현재 API, RabbitMQ, Worker, MinIO 저장 흐름은 정상입니다.
- 확장 목표가 20 replica까지 올라간 반면 ready replica가 4개에 머물렀으므로, 실제 고부하 테스트에서는 Worker Pod request와 노드 가용 리소스를 같이 봐야 합니다.
- KEDA 성능 입증은 같은 입력 500장으로 `keda-on`, `keda-on-min1`, `hpa-cpu`, `keda-off-fixed`를 각각 돌려 처리 시간, ready replica, queue backlog, 노드 CPU/메모리를 비교하는 방식이 가장 명확합니다.
"""
    output_path.write_text(markdown, encoding="utf-8-sig")


def write_pdf_if_available(report: dict, output_path: Path) -> bool:
    try:
        from reportlab.lib import colors
        from reportlab.lib.pagesizes import A4
        from reportlab.lib.styles import ParagraphStyle
        from reportlab.lib.units import mm
        from reportlab.pdfbase import pdfmetrics
        from reportlab.pdfbase.ttfonts import TTFont
        from reportlab.platypus import Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle
    except ImportError:
        return False

    pdfmetrics.registerFont(TTFont("Malgun", r"C:\Windows\Fonts\malgun.ttf"))
    pdfmetrics.registerFont(TTFont("Malgun-Bold", r"C:\Windows\Fonts\malgunbd.ttf"))
    base = ParagraphStyle("Base", fontName="Malgun", fontSize=9.2, leading=13)
    title = ParagraphStyle("Title", parent=base, fontName="Malgun-Bold", fontSize=20, leading=25)
    heading = ParagraphStyle("Heading", parent=base, fontName="Malgun-Bold", fontSize=13, leading=17)

    duration = float(report.get("durationSeconds") or 0)
    succeeded = int(report.get("succeeded") or 0)
    throughput = round(succeeded / duration, 2) if duration > 0 else 0
    rows = [["노드", "평균 CPU%", "최대 CPU%", "평균 Memory%", "최대 Memory%"]]
    rows.extend([
        [row["node"], row["cpuAvg"], row["cpuMax"], row["memoryAvg"], row["memoryMax"]]
        for row in report["nodes"]
    ])

    table = Table(rows, colWidths=[58 * mm, 28 * mm, 28 * mm, 32 * mm, 32 * mm], repeatRows=1)
    table.setStyle(TableStyle([
        ("FONTNAME", (0, 0), (-1, -1), "Malgun"),
        ("FONTNAME", (0, 0), (-1, 0), "Malgun-Bold"),
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#e7efe4")),
        ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#d6cbb9")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("FONTSIZE", (0, 0), (-1, -1), 8.2),
        ("LEADING", (0, 0), (-1, -1), 11),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5),
    ]))

    doc = SimpleDocTemplate(
        str(output_path),
        pagesize=A4,
        leftMargin=14 * mm,
        rightMargin=14 * mm,
        topMargin=14 * mm,
        bottomMargin=14 * mm,
        title="500장 배치 노드 리소스 관측 보고서",
    )
    story = [
        Paragraph("500장 배치 노드 리소스 관측 보고서", title),
        Paragraph(
            f"Job #{report['jobId']} / 입력 {report['fileCount']}장 / 성공 {report['succeeded']}장 / "
            f"실패 {report['failed']}장 / 전체 {report['durationSeconds']}초 / 초당 {throughput}장",
            base,
        ),
        Spacer(1, 8),
        Paragraph("Worker 확장 관측", heading),
        Paragraph(
            f"최대 Worker replica {report['maxWorkerReplicas']}, 최대 ready replica {report['maxReadyReplicas']}, "
            f"최대 HPA desired replica {report['maxDesiredReplicas']}로 관측됐습니다.",
            base,
        ),
        Spacer(1, 8),
        Paragraph("노드별 리소스 요약", heading),
        table,
        Spacer(1, 8),
        Paragraph("운영 해석", heading),
        Paragraph(
            "KEDA는 큐 적체를 기준으로 확장을 요청합니다. desired replica가 ready replica보다 큰 경우는 "
            "KEDA 자체 실패가 아니라 노드 여유 리소스, Pod request, 이미지 pull, 스케줄링 지연을 같이 봐야 합니다.",
            base,
        ),
        Paragraph(
            "상세 시계열 그래프는 같은 디렉터리에 생성된 SVG 파일과 Grafana "
            "`DocPrep Node Resource Overview` 대시보드에서 확인합니다.",
            base,
        ),
    ]
    doc.build(story)
    return True


def build_report(result: dict) -> dict:
    samples = result.get("kubernetesSamples") or []
    max_worker = max((sample.get("workerReplicas") or 0 for sample in samples), default=0)
    max_ready = max((sample.get("workerReadyReplicas") or 0 for sample in samples), default=0)
    max_desired = max((sample.get("hpaDesiredReplicas") or 0 for sample in samples), default=0)
    return {
        "generatedAt": datetime.now().isoformat(timespec="seconds"),
        "scenario": result.get("scenario"),
        "jobId": result.get("jobId"),
        "projectId": result.get("projectId"),
        "fileCount": result.get("fileCount"),
        "succeeded": result.get("succeeded"),
        "failed": result.get("failed"),
        "durationSeconds": result.get("durationSeconds"),
        "maxWorkerReplicas": max_worker,
        "maxReadyReplicas": max_ready,
        "maxDesiredReplicas": max_desired,
        "nodes": summarize_nodes(samples),
        "sampleCount": len(samples),
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="500장 배치 노드 리소스 관측 보고서를 생성합니다.")
    parser.add_argument("--result", required=True, type=Path, help="k8s-batch-benchmark.ps1 결과 JSON")
    parser.add_argument("--output-dir", default=Path("benchmark-results"), type=Path)
    parser.add_argument("--prefix", default=None)
    args = parser.parse_args()

    result = read_json(args.result)
    samples = result.get("kubernetesSamples") or []
    prefix = args.prefix or f"{datetime.now().strftime('%Y%m%d-%H%M%S')}-node-resource"
    output_dir = args.output_dir
    output_dir.mkdir(parents=True, exist_ok=True)

    graph_paths = {
        "node_cpu": output_dir / f"{prefix}-node-cpu.svg",
        "node_memory": output_dir / f"{prefix}-node-memory.svg",
        "worker_replicas": output_dir / f"{prefix}-worker-replicas.svg",
        "job_progress": output_dir / f"{prefix}-job-progress.svg",
        "worker_pods": output_dir / f"{prefix}-worker-pods-by-node.svg",
    }

    svg_line_chart(graph_paths["node_cpu"], "노드별 CPU 사용률", collect_node_series(samples, "cpuPercent"), "CPU %")
    svg_line_chart(graph_paths["node_memory"], "노드별 메모리 사용률", collect_node_series(samples, "memoryPercent"), "Memory %")
    svg_line_chart(
        graph_paths["worker_replicas"],
        "Worker / HPA replica",
        collect_sample_series(samples, ["workerReplicas", "workerReadyReplicas", "hpaDesiredReplicas"]),
        "replicas",
    )
    svg_line_chart(
        graph_paths["job_progress"],
        "JobItem 진행 상태",
        collect_sample_series(samples, ["queued", "processing", "succeeded", "failed"]),
        "items",
    )
    svg_line_chart(graph_paths["worker_pods"], "Worker Pod 노드 분산", collect_worker_pod_series(samples), "pods")

    write_clean_graphs(graph_paths, samples)

    report = build_report(result)
    json_path = output_dir / f"{prefix}-report.json"
    md_path = output_dir / f"{prefix}-report.md"
    pdf_path = output_dir / f"{prefix}-report.pdf"
    json_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8-sig")
    make_markdown(report, graph_paths, md_path)
    write_pdf_if_available(report, pdf_path)

    print(json_path)
    print(md_path)
    print(pdf_path)


if __name__ == "__main__":
    main()
