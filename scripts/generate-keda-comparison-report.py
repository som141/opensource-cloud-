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


def read_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8-sig"))


def parse_datetime(value: str) -> datetime:
    return datetime.fromisoformat(value)


def timing(job: dict) -> dict:
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


def build_comparison() -> dict:
    input_summary = json.loads(INPUT_SUMMARY_PATH.read_text(encoding="utf-8"))
    job_on = read_json(ROOT / "job-7-detail.json")["result"]
    job_off = read_json(ROOT / "job-8-detail.json")["result"]
    summary_on = read_json(ROOT / "job-7-summary.json")["result"]
    summary_off = read_json(ROOT / "job-8-summary.json")["result"]
    keda_on_result = read_json(ROOT / "20260601-keda-on-500-result.json")
    scaled_object = read_json(ROOT / "keda-scaledobject-preprocess-worker.json")

    on_timing = timing(job_on)
    off_timing = timing(job_off)
    comparison = {
        "generated_at": datetime.now().isoformat(timespec="seconds"),
        "input": input_summary,
        "keda_on": {
            "mode": "KEDA ON: RabbitMQ 큐 길이 기반 자동 확장, min 0, max 20",
            "job_id": job_on["id"],
            "project_id": job_on["projectId"],
            "status": job_on["status"],
            "total": summary_on["total"],
            "succeeded": summary_on["succeeded"],
            "failed": summary_on["failed"],
            **on_timing,
            "zip_bytes": (ROOT / "job-7-processed-results.zip").stat().st_size,
            "zip_entries": zip_entry_count(ROOT / "job-7-processed-results.zip"),
            "observed_max_desired_replicas": 20,
            "observed_max_deployment_replicas": 20,
            "observed_max_ready_replicas": 4,
            "observed_pending_replicas_at_peak": 16,
            "scale_events": keda_on_result["keda"]["observedScaleEvents"],
        },
        "keda_off": {
            "mode": "KEDA OFF: ScaledObject/HPA 제거, Worker 1개 고정",
            "job_id": job_off["id"],
            "project_id": job_off["projectId"],
            "status": job_off["status"],
            "total": summary_off["total"],
            "succeeded": summary_off["succeeded"],
            "failed": summary_off["failed"],
            **off_timing,
            "zip_bytes": (ROOT / "job-8-processed-results.zip").stat().st_size,
            "zip_entries": zip_entry_count(ROOT / "job-8-processed-results.zip"),
            "fixed_ready_replicas": 1,
        },
        "keda_config": {
            "min_replica_count": scaled_object["spec"]["minReplicaCount"],
            "max_replica_count": scaled_object["spec"]["maxReplicaCount"],
            "polling_interval_seconds": scaled_object["spec"]["pollingInterval"],
            "cooldown_period_seconds": scaled_object["spec"]["cooldownPeriod"],
            "triggers": [
                {
                    "name": trigger.get("name"),
                    "queue_name": trigger.get("metadata", {}).get("queueName"),
                    "target_queue_length_per_replica": trigger.get("metadata", {}).get("value"),
                    "activation_value": trigger.get("metadata", {}).get("activationValue"),
                }
                for trigger in scaled_object["spec"]["triggers"]
            ],
        },
    }
    comparison["analysis"] = {
        "processing_speedup": round(
            comparison["keda_off"]["processing_seconds"] / comparison["keda_on"]["processing_seconds"],
            2,
        ),
        "total_speedup": round(
            comparison["keda_off"]["total_seconds"] / comparison["keda_on"]["total_seconds"],
            2,
        ),
        "queue_wait_delta_seconds": round(
            comparison["keda_on"]["queue_wait_seconds"] - comparison["keda_off"]["queue_wait_seconds"],
            3,
        ),
        "processing_delta_seconds": round(
            comparison["keda_off"]["processing_seconds"] - comparison["keda_on"]["processing_seconds"],
            3,
        ),
        "total_delta_seconds": round(
            comparison["keda_off"]["total_seconds"] - comparison["keda_on"]["total_seconds"],
            3,
        ),
    }
    return comparison


def write_markdown(comparison: dict) -> None:
    on = comparison["keda_on"]
    off = comparison["keda_off"]
    analysis = comparison["analysis"]
    input_data = comparison["input"]
    markdown = f"""# KEDA 500장 배치 처리 비교 리포트

생성 시각: {comparison["generated_at"]}

## 결론

- KEDA ON은 scale-to-zero에서 시작했지만 전체 완료 시간이 KEDA OFF Worker 1개 고정보다 {analysis["total_delta_seconds"]}초 빨랐다.
- 순수 Worker 처리 구간만 보면 KEDA ON이 {analysis["processing_speedup"]}배 빨랐다.
- KEDA ON은 최대 20개까지 확장을 요청했지만 현재 클러스터 리소스 부족 때문에 실제 ready Worker는 최대 4개였다.
- 따라서 이번 실험은 KEDA의 queue 기반 확장 로직은 정상이고, 실제 성능 한계는 노드 CPU/메모리 부족이라는 점을 보여준다.

## 핵심 수치

| 항목 | KEDA ON | KEDA OFF |
| --- | ---: | ---: |
| Worker 모드 | 0~20 자동 확장 | 1개 고정 |
| 성공/전체 | {on["succeeded"]}/{on["total"]} | {off["succeeded"]}/{off["total"]} |
| 실패 | {on["failed"]} | {off["failed"]} |
| 큐 대기 | {on["queue_wait_seconds"]}초 | {off["queue_wait_seconds"]}초 |
| 처리 시간 | {on["processing_seconds"]}초 | {off["processing_seconds"]}초 |
| 생성~완료 | {on["total_seconds"]}초 | {off["total_seconds"]}초 |
| 결과 ZIP 엔트리 | {on["zip_entries"]}개 | {off["zip_entries"]}개 |

## 입력 데이터

- 원본 탐색 경로: `{input_data["sourceRoot"]}`
- 이미지 후보: {input_data["candidateCount"]}개
- 디코딩 가능 원본: {input_data["validSourceCount"]}개
- 테스트 입력셋: {input_data["createdCount"]}개 JPEG, {megabytes(input_data["totalBytes"])}

## 해석

KEDA OFF는 Worker 1개가 이미 실행 중이었기 때문에 큐 대기 시간이 짧았다.
반대로 KEDA ON은 Worker가 0개인 상태에서 시작했기 때문에 약 {on["queue_wait_seconds"]}초의 scale-up 대기 시간이 있었다.
그럼에도 KEDA ON이 전체 완료 시간에서 더 빨랐다. 이유는 대기 이후 여러 Worker가 병렬로 이미지를 처리했기 때문이다.

현재 클러스터는 KEDA가 요청한 20개 Worker를 모두 실행할 자원이 없었다.
실제 ready Worker는 4개였고, 16개는 Pending이었다.
노드 자원을 늘리거나 Worker request를 낮추면 KEDA ON과 OFF의 격차는 더 커질 가능성이 높다.

## 산출물

- KEDA ON 결과 ZIP: `benchmark-results/job-7-processed-results.zip`
- KEDA OFF 결과 ZIP: `benchmark-results/job-8-processed-results.zip`
- 비교 JSON: `benchmark-results/20260601-keda-comparison-report.json`
- 비교 PDF: `benchmark-results/20260601-keda-comparison-report.pdf`
"""
    (ROOT / "20260601-keda-comparison-report.md").write_text(markdown, encoding="utf-8")


def paragraph(text: str, style: ParagraphStyle) -> Paragraph:
    return Paragraph(text.replace("\n", "<br/>"), style)


def make_table(data: list[list[object]], widths: list[float], base_style: ParagraphStyle) -> Table:
    rows = [
        [cell if hasattr(cell, "wrap") else paragraph(str(cell), base_style) for cell in row]
        for row in data
    ]
    table = Table(rows, colWidths=widths, repeatRows=1)
    table.setStyle(
        TableStyle(
            [
                ("FONTNAME", (0, 0), (-1, -1), "Malgun"),
                ("FONTSIZE", (0, 0), (-1, -1), 8.5),
                ("LEADING", (0, 0), (-1, -1), 12),
                ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#d6cbb9")),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 6),
                ("RIGHTPADDING", (0, 0), (-1, -1), 6),
                ("TOPPADDING", (0, 0), (-1, -1), 5),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
                ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#e7efe4")),
                ("FONTNAME", (0, 0), (-1, 0), "Malgun-Bold"),
                ("TEXTCOLOR", (0, 0), (-1, 0), colors.HexColor("#00614d")),
            ]
        )
    )
    return table


def write_pdf(comparison: dict) -> None:
    pdfmetrics.registerFont(TTFont("Malgun", r"C:\Windows\Fonts\malgun.ttf"))
    pdfmetrics.registerFont(TTFont("Malgun-Bold", r"C:\Windows\Fonts\malgunbd.ttf"))

    base = ParagraphStyle(
        "BaseKo",
        fontName="Malgun",
        fontSize=9.5,
        leading=14,
        textColor=colors.HexColor("#24352e"),
    )
    title = ParagraphStyle(
        "TitleKo",
        parent=base,
        fontName="Malgun-Bold",
        fontSize=25,
        leading=31,
        textColor=colors.HexColor("#102019"),
        spaceAfter=10,
    )
    h1 = ParagraphStyle(
        "H1Ko",
        parent=base,
        fontName="Malgun-Bold",
        fontSize=16,
        leading=21,
        textColor=colors.HexColor("#102019"),
        spaceBefore=12,
        spaceAfter=8,
    )
    h2 = ParagraphStyle(
        "H2Ko",
        parent=base,
        fontName="Malgun-Bold",
        fontSize=12,
        leading=16,
        textColor=colors.HexColor("#00614d"),
        spaceBefore=8,
        spaceAfter=6,
    )
    small = ParagraphStyle(
        "SmallKo",
        parent=base,
        fontSize=8,
        leading=11,
        textColor=colors.HexColor("#5b6b64"),
    )

    on = comparison["keda_on"]
    off = comparison["keda_off"]
    analysis = comparison["analysis"]
    input_data = comparison["input"]
    config = comparison["keda_config"]

    pdf_path = ROOT / "20260601-keda-comparison-report.pdf"
    doc = SimpleDocTemplate(
        str(pdf_path),
        pagesize=A4,
        leftMargin=14 * mm,
        rightMargin=14 * mm,
        topMargin=14 * mm,
        bottomMargin=14 * mm,
        title="KEDA 500장 배치 처리 비교 리포트",
    )
    story = [
        paragraph("대규모 문서 이미지 전처리 · Kubernetes/KEDA 검증", small),
        paragraph("500장 배치 처리 KEDA ON/OFF 비교 리포트", title),
        paragraph(
            "같은 500장 입력셋으로 queue 기반 KEDA 자동 확장과 Worker 1개 고정 모드를 비교했다. "
            "검증 경로는 실제 운영 배포 환경의 API, MinIO, RabbitMQ, Worker, Kubernetes 이벤트를 기준으로 했다.",
            base,
        ),
        Spacer(1, 8),
        make_table(
            [
                ["항목", "KEDA ON", "KEDA OFF", "판정"],
                ["Worker 모드", "0~20 자동 확장", "1개 고정", "ON은 burst 대응, OFF는 고정 처리량"],
                ["성공/전체", f"{on['succeeded']}/{on['total']}", f"{off['succeeded']}/{off['total']}", "둘 다 기능 성공"],
                ["큐 대기", f"{on['queue_wait_seconds']}초", f"{off['queue_wait_seconds']}초", "OFF는 pre-warmed Worker라 시작이 빠름"],
                ["Worker 처리", f"{on['processing_seconds']}초", f"{off['processing_seconds']}초", f"ON이 {analysis['processing_speedup']}배 빠름"],
                ["생성~완료", f"{on['total_seconds']}초", f"{off['total_seconds']}초", f"ON이 {analysis['total_delta_seconds']}초 빠름"],
            ],
            [32 * mm, 38 * mm, 38 * mm, 58 * mm],
            base,
        ),
    ]

    story.extend(
        [
            paragraph("1. 결론", h1),
            paragraph(
                f"KEDA ON은 scale-to-zero에서 시작했지만 전체 완료 시간이 KEDA OFF보다 "
                f"{analysis['total_delta_seconds']}초 빨랐다. 순수 Worker 처리 구간은 KEDA ON이 "
                f"{analysis['processing_speedup']}배 빨랐다.",
                base,
            ),
            paragraph(
                "KEDA OFF는 Worker 1개가 이미 실행 중이라 큐 대기 시간이 짧았다. "
                "하지만 500장을 Worker 1개가 대부분 순차 처리하기 때문에 처리 시간이 길어졌다. "
                "KEDA ON은 처음 scale-up 대기 비용이 있었지만 이후 병렬 처리로 전체 시간을 줄였다.",
                base,
            ),
            paragraph(
                "중요한 한계: KEDA는 20개까지 확장을 요청했지만 현재 클러스터 CPU/메모리 부족으로 "
                "실제 ready Worker는 최대 4개였다. 따라서 KEDA 로직은 정상이고, 병목은 노드 리소스다.",
                h2,
            ),
            paragraph("2. 실험 조건", h1),
            make_table(
                [
                    ["항목", "값"],
                    ["원본 탐색 경로", input_data["sourceRoot"]],
                    ["이미지 후보 / 디코딩 가능 원본", f"{input_data['candidateCount']}개 / {input_data['validSourceCount']}개"],
                    ["테스트 입력셋", f"{input_data['createdCount']}개 JPEG, 최대 {input_data['maxDimension']}px, 품질 {input_data['jpegQuality']}"],
                    ["입력셋 용량", megabytes(input_data["totalBytes"])],
                    ["KEDA ON 설정", f"min {config['min_replica_count']}, max {config['max_replica_count']}, polling {config['polling_interval_seconds']}초, cooldown {config['cooldown_period_seconds']}초"],
                    ["KEDA OFF 설정", "ScaledObject/HPA 삭제, Worker 1개 고정"],
                ],
                [52 * mm, 116 * mm],
                base,
            ),
            paragraph("3. 처리 결과 비교", h1),
            make_table(
                [
                    ["지표", "KEDA ON", "KEDA OFF", "차이/해석"],
                    ["Job ID", f"#{on['job_id']}", f"#{off['job_id']}", "별도 프로젝트/Job으로 실행"],
                    ["상태", on["status"], off["status"], "둘 다 SUCCEEDED"],
                    ["실패 건수", on["failed"], off["failed"], "둘 다 0건"],
                    ["큐 대기 시간", f"{on['queue_wait_seconds']}초", f"{off['queue_wait_seconds']}초", f"KEDA ON은 scale-from-zero로 {analysis['queue_wait_delta_seconds']}초 더 대기"],
                    ["Worker 처리 시간", f"{on['processing_seconds']}초", f"{off['processing_seconds']}초", f"KEDA ON이 {analysis['processing_delta_seconds']}초 단축"],
                    ["전체 완료 시간", f"{on['total_seconds']}초", f"{off['total_seconds']}초", f"KEDA ON이 {analysis['total_delta_seconds']}초 단축"],
                    ["결과 ZIP 엔트리", f"{on['zip_entries']}개", f"{off['zip_entries']}개", "처리된 이미지만 ZIP에 포함"],
                    ["결과 ZIP 크기", megabytes(on["zip_bytes"]), megabytes(off["zip_bytes"]), "동일 입력셋 기반이라 유사"],
                ],
                [34 * mm, 30 * mm, 30 * mm, 74 * mm],
                base,
            ),
        ]
    )

    story.extend(
        [
            paragraph("4. KEDA 스케일링 분석", h1),
            paragraph(
                "KEDA ON은 RabbitMQ queue length를 기준으로 Worker를 늘렸다. "
                "이벤트상 0 → 1 → 4 → 8 → 16 → 20 순서로 확장 요청이 발생했다.",
                base,
            ),
            make_table(
                [
                    ["항목", "값"],
                    ["관측 최대 desired replica", on["observed_max_desired_replicas"]],
                    ["관측 최대 deployment replica", on["observed_max_deployment_replicas"]],
                    ["관측 최대 ready Worker", on["observed_max_ready_replicas"]],
                    ["피크 시 Pending Worker", on["observed_pending_replicas_at_peak"]],
                    ["Pending 사유", "0/4 nodes available: CPU/메모리 부족 및 control-plane taint"],
                ],
                [55 * mm, 113 * mm],
                base,
            ),
            paragraph("Queue trigger 설정", h2),
            make_table(
                [["이름", "큐", "Worker당 목표 큐 길이", "활성 기준"]]
                + [
                    [
                        trigger["name"],
                        trigger["queue_name"],
                        trigger["target_queue_length_per_replica"],
                        trigger["activation_value"],
                    ]
                    for trigger in config["triggers"]
                ],
                [32 * mm, 62 * mm, 42 * mm, 32 * mm],
                base,
            ),
        ]
    )

    scale_rows = [["시간", "대상", "이벤트", "메시지"]]
    for event in on["scale_events"]:
        if event["reason"] in {"KEDAScaleTargetActivated", "KEDAScaleTargetDeactivated", "SuccessfulRescale", "ScalingReplicaSet"}:
            scale_rows.append([event["time"], event["kind"], event["reason"], event["message"]])
    story.extend([paragraph("주요 스케일 이벤트", h2), make_table(scale_rows, [34 * mm, 26 * mm, 38 * mm, 70 * mm], base)])

    story.extend(
        [
            PageBreak(),
            paragraph("5. 해석: KEDA를 쓰는 이유", h1),
            paragraph(
                "KEDA OFF는 Worker 1개가 계속 떠 있기 때문에 첫 작업 시작이 빠르다. "
                "그러나 대량 배치에서는 처리량이 Worker 1개에 묶인다. KEDA ON은 Worker가 0개에서 "
                "시작하는 비용이 있지만, 큐가 쌓이는 순간 병렬 Worker를 늘려 처리량을 확보한다.",
                base,
            ),
            paragraph(
                "현재 KEDA ON의 성능 향상은 제한적이다. 이유는 KEDA가 20개를 요청했지만 실제 ready Worker가 "
                "4개뿐이었기 때문이다. 노드 리소스를 확장하면 KEDA ON의 처리 시간은 더 줄어들 가능성이 높다. "
                "반대로 작업이 없을 때 Worker를 0개까지 줄일 수 있어 비용 면에서도 KEDA ON이 유리하다.",
                base,
            ),
            paragraph("6. 다음 실험 제안", h1),
            make_table(
                [
                    ["실험", "목적", "필요 조건"],
                    ["KEDA ON + 노드 리소스 증설", "20개 Worker가 실제 ready일 때의 최대 처리량 확인", "CPU/메모리 증설 또는 Worker request 조정"],
                    ["KEDA OFF Worker 4개 고정", "현재 KEDA ON의 실제 ready 수와 동일 조건 비교", "FixedReplicas=4"],
                    ["KEDA OFF Worker 20개 고정", "최대 고정 비용과 KEDA 자동 확장 비용 비교", "20개 Worker 수용 가능한 노드"],
                    ["1000장 이상 배치", "큐 길이에 따른 KEDA 반응 곡선 확인", "더 큰 입력셋"],
                ],
                [45 * mm, 62 * mm, 61 * mm],
                base,
            ),
            paragraph("7. 산출물", h1),
            make_table(
                [
                    ["산출물", "경로"],
                    ["KEDA ON 결과 ZIP", "benchmark-results/job-7-processed-results.zip"],
                    ["KEDA OFF 결과 ZIP", "benchmark-results/job-8-processed-results.zip"],
                    ["비교 JSON", "benchmark-results/20260601-keda-comparison-report.json"],
                    ["비교 Markdown", "benchmark-results/20260601-keda-comparison-report.md"],
                    ["비교 PDF", "benchmark-results/20260601-keda-comparison-report.pdf"],
                ],
                [46 * mm, 122 * mm],
                base,
            ),
            Spacer(1, 8),
            paragraph(
                "이 PDF는 ReportLab으로 생성했고 Windows Malgun Gothic 폰트를 직접 등록했다. "
                "PowerShell 파이프 인코딩 때문에 한글 문자열이 물음표로 변환되던 문제를 피하기 위해 "
                "UTF-8 Python 파일 기반으로 생성한다.",
                small,
            ),
        ]
    )

    def page_number(canvas, current_doc):
        canvas.saveState()
        canvas.setFont("Malgun", 8)
        canvas.setFillColor(colors.HexColor("#6c776f"))
        canvas.drawRightString(A4[0] - 14 * mm, 9 * mm, str(current_doc.page))
        canvas.restoreState()

    doc.build(story, onFirstPage=page_number, onLaterPages=page_number)


def main() -> None:
    ROOT.mkdir(exist_ok=True)
    comparison = build_comparison()
    (ROOT / "20260601-keda-comparison-report.json").write_text(
        json.dumps(comparison, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    write_markdown(comparison)
    write_pdf(comparison)
    print(ROOT / "20260601-keda-comparison-report.pdf")


if __name__ == "__main__":
    main()
