from __future__ import annotations

import argparse
import json
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from statistics import mean
from typing import Any

import matplotlib.pyplot as plt
from matplotlib import font_manager


DEFAULT_FONT = r"C:\Windows\Fonts\malgun.ttf"
DEFAULT_BOLD_FONT = r"C:\Windows\Fonts\malgunbd.ttf"


@dataclass
class Scenario:
    label: str
    path: Path
    raw: dict[str, Any]
    samples: list[dict[str, Any]]
    metrics: dict[str, Any]


def read_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8-sig"))


def parse_time(value: str) -> datetime:
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def elapsed_seconds(base: datetime, value: str) -> float:
    return max(0.0, (parse_time(value) - base).total_seconds())


def setup_korean_font() -> None:
    font_path = Path(DEFAULT_FONT)
    if font_path.exists():
        font_manager.fontManager.addfont(str(font_path))
        plt.rcParams["font.family"] = "Malgun Gothic"
    plt.rcParams["axes.unicode_minus"] = False


def sample_elapsed_series(samples: list[dict[str, Any]]) -> list[float]:
    if not samples:
        return []
    base = parse_time(samples[0]["sampledAt"])
    return [elapsed_seconds(base, sample["sampledAt"]) for sample in samples]


def sample_number(sample: dict[str, Any], key: str) -> float:
    value = sample.get(key)
    if value is None:
        return 0.0
    try:
        return float(value)
    except (TypeError, ValueError):
        return 0.0


def max_node_metric(sample: dict[str, Any], key: str) -> float:
    values = []
    for node in sample.get("nodeResources") or []:
        value = node.get(key)
        if value is not None:
            values.append(float(value))
    return max(values) if values else 0.0


def avg_node_metric(sample: dict[str, Any], key: str) -> float:
    values = []
    for node in sample.get("nodeResources") or []:
        value = node.get(key)
        if value is not None:
            values.append(float(value))
    return mean(values) if values else 0.0


def interpolate_completion_time(samples: list[dict[str, Any]], target_count: float) -> float | None:
    if not samples:
        return None
    elapsed = sample_elapsed_series(samples)
    previous_count = sample_number(samples[0], "succeeded")
    previous_time = elapsed[0]
    if previous_count >= target_count:
        return previous_time
    for index in range(1, len(samples)):
        current_count = sample_number(samples[index], "succeeded")
        current_time = elapsed[index]
        if current_count >= target_count:
            delta_count = current_count - previous_count
            if delta_count <= 0:
                return current_time
            ratio = (target_count - previous_count) / delta_count
            return previous_time + ((current_time - previous_time) * ratio)
        previous_count = current_count
        previous_time = current_time
    return elapsed[-1]


def throughput_series(samples: list[dict[str, Any]]) -> tuple[list[float], list[float]]:
    if len(samples) < 2:
        return [], []
    elapsed = sample_elapsed_series(samples)
    x_values: list[float] = []
    y_values: list[float] = []
    for index in range(1, len(samples)):
        dt = elapsed[index] - elapsed[index - 1]
        if dt <= 0:
            continue
        completed_delta = sample_number(samples[index], "succeeded") - sample_number(samples[index - 1], "succeeded")
        x_values.append(elapsed[index])
        y_values.append(max(0.0, completed_delta / dt))
    return x_values, y_values


def time_to_max(samples: list[dict[str, Any]], key: str) -> float | None:
    if not samples:
        return None
    elapsed = sample_elapsed_series(samples)
    values = [sample_number(sample, key) for sample in samples]
    if not values:
        return None
    max_value = max(values)
    for index, value in enumerate(values):
        if value == max_value:
            return elapsed[index]
    return None


def build_metrics(raw: dict[str, Any]) -> dict[str, Any]:
    samples = raw.get("kubernetesSamples") or []
    file_count = int(raw.get("fileCount") or 0)
    succeeded = int(raw.get("succeeded") or 0)
    failed = int(raw.get("failed") or 0)
    duration = float(raw.get("durationSeconds") or 0.0)
    elapsed = sample_elapsed_series(samples)
    observed_duration = elapsed[-1] if elapsed else 0.0
    first_completion = next(
        (elapsed[index] for index, sample in enumerate(samples) if sample_number(sample, "succeeded") > 0),
        None,
    )
    p50 = interpolate_completion_time(samples, max(1.0, file_count * 0.50)) if file_count else None
    p95 = interpolate_completion_time(samples, max(1.0, file_count * 0.95)) if file_count else None
    processing_window = None
    if first_completion is not None and observed_duration > first_completion:
        processing_window = observed_duration - first_completion

    cpu_values = [max_node_metric(sample, "cpuPercent") for sample in samples]
    memory_values = [max_node_metric(sample, "memoryPercent") for sample in samples]
    avg_cpu_values = [avg_node_metric(sample, "cpuPercent") for sample in samples]
    avg_memory_values = [avg_node_metric(sample, "memoryPercent") for sample in samples]
    ready_values = [sample_number(sample, "workerReadyReplicas") for sample in samples]

    return {
        "scenario": raw.get("scenario"),
        "jobId": raw.get("jobId"),
        "projectId": raw.get("projectId"),
        "fileCount": file_count,
        "succeeded": succeeded,
        "failed": failed,
        "durationSeconds": round(duration, 3),
        "observedProcessingSeconds": round(observed_duration, 3),
        "firstCompletionLatencySeconds": round(first_completion, 3) if first_completion is not None else None,
        "p50CompletionLatencySeconds": round(p50, 3) if p50 is not None else None,
        "p95CompletionLatencySeconds": round(p95, 3) if p95 is not None else None,
        "processingWindowSeconds": round(processing_window, 3) if processing_window is not None else None,
        "avgThroughputPerSecond": round(succeeded / duration, 3) if duration > 0 else 0.0,
        "observedThroughputPerSecond": round(succeeded / observed_duration, 3) if observed_duration > 0 else 0.0,
        "activeThroughputPerSecond": round(succeeded / processing_window, 3) if processing_window and processing_window > 0 else 0.0,
        "maxWorkerReplicas": int(max((sample_number(sample, "workerReplicas") for sample in samples), default=0)),
        "maxReadyReplicas": int(max(ready_values, default=0)),
        "avgReadyReplicas": round(mean(ready_values), 3) if ready_values else 0.0,
        "maxHpaDesiredReplicas": int(max((sample_number(sample, "hpaDesiredReplicas") for sample in samples), default=0)),
        "timeToMaxReadyReplicasSeconds": round(time_to_max(samples, "workerReadyReplicas") or 0.0, 3),
        "timeToMaxDesiredReplicasSeconds": round(time_to_max(samples, "hpaDesiredReplicas") or 0.0, 3),
        "maxQueuedItems": int(max((sample_number(sample, "queued") for sample in samples), default=0)),
        "maxProcessingItems": int(max((sample_number(sample, "processing") for sample in samples), default=0)),
        "maxNodeCpuPercent": round(max(cpu_values), 3) if cpu_values else 0.0,
        "avgMaxNodeCpuPercent": round(mean(cpu_values), 3) if cpu_values else 0.0,
        "maxNodeMemoryPercent": round(max(memory_values), 3) if memory_values else 0.0,
        "avgMaxNodeMemoryPercent": round(mean(memory_values), 3) if memory_values else 0.0,
        "avgNodeCpuPercent": round(mean(avg_cpu_values), 3) if avg_cpu_values else 0.0,
        "avgNodeMemoryPercent": round(mean(avg_memory_values), 3) if avg_memory_values else 0.0,
        "sampleCount": len(samples),
    }


def load_scenarios(labels: list[str], paths: list[Path]) -> list[Scenario]:
    if len(labels) != len(paths):
        raise ValueError("--label 개수와 --result 개수는 같아야 합니다.")
    scenarios = []
    for label, path in zip(labels, paths):
        raw = read_json(path)
        samples = raw.get("kubernetesSamples") or []
        scenarios.append(Scenario(label=label, path=path, raw=raw, samples=samples, metrics=build_metrics(raw)))
    return scenarios


def save_bar_chart(path: Path, title: str, scenarios: list[Scenario], metrics: list[tuple[str, str]], ylabel: str) -> None:
    fig, ax = plt.subplots(figsize=(11, 6))
    labels = [scenario.label for scenario in scenarios]
    width = 0.8 / max(1, len(metrics))
    x_positions = range(len(labels))
    for metric_index, (metric_key, metric_label) in enumerate(metrics):
        offset = (metric_index - (len(metrics) - 1) / 2) * width
        values = [scenario.metrics.get(metric_key) or 0 for scenario in scenarios]
        ax.bar([x + offset for x in x_positions], values, width=width, label=metric_label)
    ax.set_title(title, fontsize=15, fontweight="bold")
    ax.set_ylabel(ylabel)
    ax.set_xticks(list(x_positions))
    ax.set_xticklabels(labels, rotation=12, ha="right")
    ax.grid(axis="y", alpha=0.25)
    ax.legend()
    fig.tight_layout()
    fig.savefig(path, dpi=160)
    plt.close(fig)


def save_line_chart(
    path: Path,
    title: str,
    scenarios: list[Scenario],
    y_getter,
    ylabel: str,
) -> None:
    fig, ax = plt.subplots(figsize=(12, 6))
    for scenario in scenarios:
        x_values = sample_elapsed_series(scenario.samples)
        y_values = [y_getter(sample) for sample in scenario.samples]
        ax.plot(x_values, y_values, marker="o", linewidth=2, markersize=3, label=scenario.label)
    ax.set_title(title, fontsize=15, fontweight="bold")
    ax.set_xlabel("Job 관측 시작 이후 경과 시간(초)")
    ax.set_ylabel(ylabel)
    ax.grid(alpha=0.25)
    ax.legend()
    fig.tight_layout()
    fig.savefig(path, dpi=160)
    plt.close(fig)


def save_throughput_line_chart(path: Path, scenarios: list[Scenario]) -> None:
    fig, ax = plt.subplots(figsize=(12, 6))
    for scenario in scenarios:
        x_values, y_values = throughput_series(scenario.samples)
        ax.plot(x_values, y_values, marker="o", linewidth=2, markersize=3, label=scenario.label)
    ax.set_title("시간별 처리량", fontsize=15, fontweight="bold")
    ax.set_xlabel("Job 관측 시작 이후 경과 시간(초)")
    ax.set_ylabel("초당 완료 이미지 수")
    ax.grid(alpha=0.25)
    ax.legend()
    fig.tight_layout()
    fig.savefig(path, dpi=160)
    plt.close(fig)


def save_all_graphs(output_dir: Path, prefix: str, scenarios: list[Scenario]) -> dict[str, Path]:
    graphs = {
        "duration_throughput": output_dir / f"{prefix}-duration-throughput.png",
        "latency": output_dir / f"{prefix}-latency.png",
        "replicas": output_dir / f"{prefix}-replicas.png",
        "node_cpu": output_dir / f"{prefix}-time-node-cpu.png",
        "node_memory": output_dir / f"{prefix}-time-node-memory.png",
        "ready_replicas": output_dir / f"{prefix}-time-ready-replicas.png",
        "desired_replicas": output_dir / f"{prefix}-time-desired-replicas.png",
        "throughput": output_dir / f"{prefix}-time-throughput.png",
        "progress": output_dir / f"{prefix}-time-progress.png",
    }
    save_bar_chart(
        graphs["duration_throughput"],
        "전체 처리 시간과 평균 처리량",
        scenarios,
        [("durationSeconds", "전체 처리 시간(초)"), ("avgThroughputPerSecond", "평균 처리량(장/초)")],
        "시간(초) / 처리량(장/초)",
    )
    save_bar_chart(
        graphs["latency"],
        "완료 지연 시간",
        scenarios,
        [
            ("firstCompletionLatencySeconds", "첫 완료"),
            ("p50CompletionLatencySeconds", "p50 완료"),
            ("p95CompletionLatencySeconds", "p95 완료"),
        ],
        "초",
    )
    save_bar_chart(
        graphs["replicas"],
        "Pod 증가량과 확장 목표",
        scenarios,
        [("maxWorkerReplicas", "최대 Worker"), ("maxReadyReplicas", "최대 Ready"), ("maxHpaDesiredReplicas", "최대 Desired")],
        "replica",
    )
    save_line_chart(
        graphs["node_cpu"],
        "시간별 노드 CPU 사용률(각 시점 최대 노드)",
        scenarios,
        lambda sample: max_node_metric(sample, "cpuPercent"),
        "CPU %",
    )
    save_line_chart(
        graphs["node_memory"],
        "시간별 노드 메모리 사용률(각 시점 최대 노드)",
        scenarios,
        lambda sample: max_node_metric(sample, "memoryPercent"),
        "Memory %",
    )
    save_line_chart(
        graphs["ready_replicas"],
        "시간별 Ready Worker replica",
        scenarios,
        lambda sample: sample_number(sample, "workerReadyReplicas"),
        "ready replicas",
    )
    save_line_chart(
        graphs["desired_replicas"],
        "시간별 HPA desired replica",
        scenarios,
        lambda sample: sample_number(sample, "hpaDesiredReplicas"),
        "desired replicas",
    )
    save_throughput_line_chart(graphs["throughput"], scenarios)
    save_line_chart(
        graphs["progress"],
        "시간별 완료 이미지 수",
        scenarios,
        lambda sample: sample_number(sample, "succeeded"),
        "완료 이미지 수",
    )
    return graphs


def write_json(path: Path, scenarios: list[Scenario], generated_at: str, graphs: dict[str, Path]) -> None:
    payload = {
        "generatedAt": generated_at,
        "input": {
            "fileCount": scenarios[0].metrics["fileCount"] if scenarios else 0,
            "note": "동일한 C:\\tmp\\docprep-500-input 입력 500장으로 재실험",
        },
        "scenarios": [
            {
                "label": scenario.label,
                "resultPath": str(scenario.path),
                **scenario.metrics,
            }
            for scenario in scenarios
        ],
        "graphs": {key: str(value) for key, value in graphs.items()},
    }
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8-sig")


def table_rows(scenarios: list[Scenario]) -> str:
    lines = []
    for scenario in scenarios:
        m = scenario.metrics
        lines.append(
            "| {label} | {jobId} | {succeeded}/{fileCount} | {failed} | {durationSeconds} | "
            "{avgThroughputPerSecond} | {firstCompletionLatencySeconds} | {p50CompletionLatencySeconds} | "
            "{p95CompletionLatencySeconds} | {maxWorkerReplicas} | {maxReadyReplicas} | "
            "{maxHpaDesiredReplicas} | {maxNodeCpuPercent} | {maxNodeMemoryPercent} |".format(
                label=scenario.label,
                **m,
            )
        )
    return "\n".join(lines)


def analysis_text(scenarios: list[Scenario]) -> list[str]:
    fastest = min(scenarios, key=lambda scenario: scenario.metrics["durationSeconds"])
    slowest = max(scenarios, key=lambda scenario: scenario.metrics["durationSeconds"])
    highest_throughput = max(scenarios, key=lambda scenario: scenario.metrics["avgThroughputPerSecond"])
    highest_ready = max(scenarios, key=lambda scenario: scenario.metrics["maxReadyReplicas"])
    highest_desired = max(scenarios, key=lambda scenario: scenario.metrics["maxHpaDesiredReplicas"])
    return [
        f"가장 빠른 전체 처리 시간은 `{fastest.label}`의 {fastest.metrics['durationSeconds']}초입니다.",
        f"가장 느린 케이스는 `{slowest.label}`의 {slowest.metrics['durationSeconds']}초입니다.",
        f"평균 처리량은 `{highest_throughput.label}`가 {highest_throughput.metrics['avgThroughputPerSecond']}장/초로 가장 높습니다.",
        f"실제 Ready Worker는 `{highest_ready.label}`가 최대 {highest_ready.metrics['maxReadyReplicas']}개로 가장 높았습니다.",
        f"KEDA/HPA 확장 목표는 `{highest_desired.label}`가 최대 desired {highest_desired.metrics['maxHpaDesiredReplicas']}개로 가장 높았습니다.",
        "desired replica가 높아도 ready replica가 낮으면 큐 기반 확장 요청은 발생했지만 노드 리소스, Pod request, 이미지 pull, 스케줄링 여유가 병목일 수 있습니다.",
    ]


def write_markdown(path: Path, scenarios: list[Scenario], generated_at: str, graphs: dict[str, Path]) -> None:
    analysis = "\n".join(f"- {line}" for line in analysis_text(scenarios))
    graph_lines = "\n\n".join(f"![{key}]({graph.name})" for key, graph in graphs.items())
    markdown = f"""# 4가지 스케일링 워크로드 500장 배치 비교 보고서

생성 시각: {generated_at}

## 실험 범위

동일한 500장 이미지 입력을 사용해 다음 4가지 스케일링 케이스를 다시 측정했다.

- Fixed 1 Worker: KEDA/HPA 없이 Worker 1개 고정
- HPA CPU 60%: Kubernetes 기본 HPA, CPU 평균 사용률 60% 기준
- KEDA min 1: RabbitMQ queue length 기반 KEDA, 최소 Worker 1개 유지
- KEDA min 0: RabbitMQ queue length 기반 KEDA, scale-to-zero 상태에서 시작

## 핵심 통계

| 케이스 | Job | 성공/전체 | 실패 | 전체 시간(초) | 평균 처리량(장/초) | 첫 완료 지연(초) | p50 완료(초) | p95 완료(초) | 최대 Worker | 최대 Ready | 최대 Desired | 최대 노드 CPU% | 최대 노드 Memory% |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
{table_rows(scenarios)}

## 분석

{analysis}

## 그래프

{graph_lines}

## 해석

이번 보고서의 latency는 Job 관측 시작 시점부터 완료 샘플까지의 지연으로 계산했다. 개별 이미지별 p95 latency가 아니라, 배치 진행률 샘플을 기반으로 p50/p95 완료 시점을 보간한 값이다. 현재 시스템은 이미지별 세부 latency histogram을 별도 metric으로 내보내지 않으므로, 더 정밀한 latency가 필요하면 Worker가 `job_item_processing_seconds` histogram을 Prometheus에 기록해야 한다.

첫 완료 지연이 `0.0`으로 표시된 케이스는 첫 번째 샘플 시점에 이미 일부 이미지가 완료되어 있었다는 뜻이다. 실제 첫 완료 latency가 0초라는 의미가 아니라, 현재 5초 단위 polling 샘플러로는 첫 완료가 첫 샘플 이전에 발생했다는 의미다.

시간별 자원 사용량 그래프는 각 샘플 시점에서 가장 높은 노드 CPU/메모리 사용률을 사용했다. 노드별 상세 그래프는 Grafana `DocPrep Node Resource Overview`에서 확인한다.
"""
    path.write_text(markdown, encoding="utf-8-sig")


def write_html(path: Path, scenarios: list[Scenario], generated_at: str, graphs: dict[str, Path]) -> None:
    rows = "\n".join(
        "<tr>"
        f"<td>{scenario.label}</td>"
        f"<td>{scenario.metrics['jobId']}</td>"
        f"<td>{scenario.metrics['succeeded']}/{scenario.metrics['fileCount']}</td>"
        f"<td>{scenario.metrics['durationSeconds']}</td>"
        f"<td>{scenario.metrics['avgThroughputPerSecond']}</td>"
        f"<td>{scenario.metrics['firstCompletionLatencySeconds']}</td>"
        f"<td>{scenario.metrics['p50CompletionLatencySeconds']}</td>"
        f"<td>{scenario.metrics['p95CompletionLatencySeconds']}</td>"
        f"<td>{scenario.metrics['maxWorkerReplicas']}</td>"
        f"<td>{scenario.metrics['maxReadyReplicas']}</td>"
        f"<td>{scenario.metrics['maxHpaDesiredReplicas']}</td>"
        f"<td>{scenario.metrics['maxNodeCpuPercent']}</td>"
        f"<td>{scenario.metrics['maxNodeMemoryPercent']}</td>"
        "</tr>"
        for scenario in scenarios
    )
    graph_html = "\n".join(
        f"<section><h2>{key}</h2><img src='{graph.name}' alt='{key}'></section>"
        for key, graph in graphs.items()
    )
    analysis = "\n".join(f"<li>{line}</li>" for line in analysis_text(scenarios))
    html = f"""<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <title>4가지 스케일링 워크로드 500장 배치 비교 보고서</title>
  <style>
    body {{ font-family: 'Malgun Gothic', 'Apple SD Gothic Neo', sans-serif; margin: 36px; color: #102019; background: #fbfaf4; }}
    h1 {{ font-size: 34px; margin-bottom: 8px; }}
    table {{ border-collapse: collapse; width: 100%; margin: 20px 0 28px; background: white; }}
    th, td {{ border: 1px solid #d6cbb9; padding: 8px 10px; font-size: 13px; text-align: right; }}
    th:first-child, td:first-child {{ text-align: left; }}
    th {{ background: #e7efe4; }}
    img {{ width: 100%; max-width: 1180px; border: 1px solid #d6cbb9; background: white; margin-bottom: 26px; }}
    .note {{ color: #58645d; }}
  </style>
</head>
<body>
  <h1>4가지 스케일링 워크로드 500장 배치 비교 보고서</h1>
  <p class="note">생성 시각: {generated_at}</p>
  <h2>핵심 통계</h2>
  <table>
    <thead>
      <tr>
        <th>케이스</th><th>Job</th><th>성공/전체</th><th>전체 시간(초)</th><th>평균 처리량</th>
        <th>첫 완료</th><th>p50</th><th>p95</th><th>최대 Worker</th><th>최대 Ready</th>
        <th>최대 Desired</th><th>최대 CPU%</th><th>최대 Memory%</th>
      </tr>
    </thead>
    <tbody>{rows}</tbody>
  </table>
  <h2>분석</h2>
  <ul>{analysis}</ul>
  {graph_html}
</body>
</html>
"""
    path.write_text(html, encoding="utf-8-sig")


def write_pdf(path: Path, scenarios: list[Scenario], generated_at: str, graphs: dict[str, Path]) -> bool:
    try:
        from reportlab.lib import colors
        from reportlab.lib.pagesizes import A4, landscape
        from reportlab.lib.styles import ParagraphStyle
        from reportlab.lib.units import mm
        from reportlab.pdfbase import pdfmetrics
        from reportlab.pdfbase.ttfonts import TTFont
        from reportlab.platypus import Image, PageBreak, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle
    except ImportError:
        return False

    pdfmetrics.registerFont(TTFont("Malgun", DEFAULT_FONT))
    pdfmetrics.registerFont(TTFont("Malgun-Bold", DEFAULT_BOLD_FONT))
    base = ParagraphStyle("Base", fontName="Malgun", fontSize=8.8, leading=12)
    title = ParagraphStyle("Title", parent=base, fontName="Malgun-Bold", fontSize=19, leading=24)
    heading = ParagraphStyle("Heading", parent=base, fontName="Malgun-Bold", fontSize=12, leading=16)
    rows = [[
        "케이스", "Job", "성공/전체", "전체 시간", "처리량", "첫 완료", "p50", "p95",
        "Max Worker", "Max Ready", "Max Desired", "Max CPU", "Max Mem",
    ]]
    for scenario in scenarios:
        m = scenario.metrics
        rows.append([
            scenario.label,
            m["jobId"],
            f"{m['succeeded']}/{m['fileCount']}",
            m["durationSeconds"],
            m["avgThroughputPerSecond"],
            m["firstCompletionLatencySeconds"],
            m["p50CompletionLatencySeconds"],
            m["p95CompletionLatencySeconds"],
            m["maxWorkerReplicas"],
            m["maxReadyReplicas"],
            m["maxHpaDesiredReplicas"],
            m["maxNodeCpuPercent"],
            m["maxNodeMemoryPercent"],
        ])

    table = Table(rows, colWidths=[34 * mm, 13 * mm, 20 * mm, 18 * mm, 16 * mm, 16 * mm, 16 * mm, 16 * mm, 18 * mm, 17 * mm, 19 * mm, 16 * mm, 16 * mm])
    table.setStyle(TableStyle([
        ("FONTNAME", (0, 0), (-1, -1), "Malgun"),
        ("FONTNAME", (0, 0), (-1, 0), "Malgun-Bold"),
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#e7efe4")),
        ("GRID", (0, 0), (-1, -1), 0.3, colors.HexColor("#d6cbb9")),
        ("FONTSIZE", (0, 0), (-1, -1), 7.2),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
    ]))

    doc = SimpleDocTemplate(
        str(path),
        pagesize=landscape(A4),
        leftMargin=12 * mm,
        rightMargin=12 * mm,
        topMargin=12 * mm,
        bottomMargin=12 * mm,
        title="4가지 스케일링 워크로드 500장 배치 비교 보고서",
    )
    story = [
        Paragraph("4가지 스케일링 워크로드 500장 배치 비교 보고서", title),
        Paragraph(f"생성 시각: {generated_at}", base),
        Spacer(1, 5),
        Paragraph("핵심 통계", heading),
        table,
        Spacer(1, 7),
        Paragraph("분석", heading),
    ]
    for line in analysis_text(scenarios):
        story.append(Paragraph(f"- {line}", base))
    story.append(Paragraph(
        "첫 완료 지연이 0초로 표시된 케이스는 첫 샘플 시점에 이미 일부 이미지가 완료되어 있었음을 의미합니다. "
        "개별 이미지 latency가 0초라는 뜻은 아닙니다.",
        base,
    ))
    story.append(PageBreak())

    for key, graph in graphs.items():
        story.append(Paragraph(key, heading))
        story.append(Image(str(graph), width=250 * mm, height=136 * mm))
        story.append(Spacer(1, 5))
    doc.build(story)
    return True


def main() -> None:
    parser = argparse.ArgumentParser(description="4가지 스케일링 워크로드 비교 보고서를 생성합니다.")
    parser.add_argument("--result", action="append", required=True, type=Path, help="벤치마크 결과 JSON")
    parser.add_argument("--label", action="append", required=True, help="결과 JSON에 붙일 표시 이름")
    parser.add_argument("--output-dir", default=Path("benchmark-results"), type=Path)
    parser.add_argument("--prefix", default="autoscaling-4case-comparison")
    args = parser.parse_args()

    setup_korean_font()
    args.output_dir.mkdir(parents=True, exist_ok=True)
    scenarios = load_scenarios(args.label, args.result)
    generated_at = datetime.now().isoformat(timespec="seconds")
    graphs = save_all_graphs(args.output_dir, args.prefix, scenarios)

    json_path = args.output_dir / f"{args.prefix}-report.json"
    md_path = args.output_dir / f"{args.prefix}-report.md"
    html_path = args.output_dir / f"{args.prefix}-report.html"
    pdf_path = args.output_dir / f"{args.prefix}-report.pdf"
    write_json(json_path, scenarios, generated_at, graphs)
    write_markdown(md_path, scenarios, generated_at, graphs)
    write_html(html_path, scenarios, generated_at, graphs)
    write_pdf(pdf_path, scenarios, generated_at, graphs)

    print(json_path)
    print(md_path)
    print(html_path)
    print(pdf_path)
    for graph in graphs.values():
        print(graph)


if __name__ == "__main__":
    main()
