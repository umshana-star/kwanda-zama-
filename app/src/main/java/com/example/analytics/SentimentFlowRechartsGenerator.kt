package com.example.analytics

import java.util.Locale

/**
 * Generator that creates an interactive Recharts data visualization web canvas
 * for the Sentiment Flow Dashboard Overlay.
 */
object SentimentFlowRechartsGenerator {

    fun buildSentimentFlowHtml(summary: SentimentFlowSummary): String {
        // Prepare JSON for Stream Timeline
        val streamJsonBuilder = StringBuilder("[")
        summary.flowTimeline.forEachIndexed { i, pt ->
            streamJsonBuilder.append(
                String.format(
                    Locale.ROOT,
                    """{"time":"%s","negative":%d,"triaging":%d,"resolved":%d,"netSentiment":%.2f}""",
                    pt.timeLabel,
                    pt.negativeVolume,
                    pt.triagingVolume,
                    pt.resolvedVolume,
                    pt.netSentimentScore
                )
            )
            if (i < summary.flowTimeline.size - 1) streamJsonBuilder.append(",")
        }
        streamJsonBuilder.append("]")
        val streamJson = streamJsonBuilder.toString()

        // Prepare JSON for Trigger Shift Categories
        val triggerJsonBuilder = StringBuilder("[")
        summary.triggerMetrics.forEachIndexed { i, cat ->
            triggerJsonBuilder.append(
                String.format(
                    Locale.ROOT,
                    """{"name":"%s","negativeScore":%.2f,"resolvedScore":%.2f,"rate":%d,"delta":%.2f,"count":%d}""",
                    cat.categoryName,
                    cat.avgInitialSentiment,
                    cat.avgResolvedSentiment,
                    cat.resolutionRatePct,
                    cat.avgDelta,
                    cat.totalCount
                )
            )
            if (i < summary.triggerMetrics.size - 1) triggerJsonBuilder.append(",")
        }
        triggerJsonBuilder.append("]")
        val triggerJson = triggerJsonBuilder.toString()

        // Prepare JSON for Turn-by-Turn De-escalation Velocity
        val turnVelocityJson = """
            [
              {"turn":"Turn 1","label":"Initial Influx","sentiment":-0.85,"stage":"Negative Influx","color":"#FF1744","note":"Customer distress / complaint received"},
              {"turn":"Turn 2","label":"AI Triage","sentiment":0.20,"stage":"Empathy & Diagnosis","color":"#FF9100","note":"AI acknowledges distress, applies empathy protocol"},
              {"turn":"Turn 3","label":"Resolution","sentiment":0.72,"stage":"Solution Offered","color":"#00E5FF","note":"Slot reserved / price clarified / voucher dispatched"},
              {"turn":"Turn 4","label":"Resolved","sentiment":0.93,"stage":"Delight & Confirmed","color":"#00E676","note":"Customer confirms booking & thanks the salon"}
            ]
        """.trimIndent()

        val shiftRate = summary.shiftSuccessRatePct
        val avgShift = String.format(Locale.ROOT, "+%.2f", summary.averageNetShiftDelta)
        val avgTurns = String.format(Locale.ROOT, "%.1f", summary.averageTurnsToResolve)
        val totalNeg = summary.totalNegativeIngested
        val totalRes = summary.totalResolved

        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"/>
<title>Sentiment Flow Analytics</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    background-color: #06090E;
    color: #F5F7FA;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    padding: 10px;
    user-select: none;
    -webkit-user-select: none;
  }
  .card {
    background: #0B111A;
    border: 1px solid #1C2636;
    border-radius: 12px;
    padding: 14px;
    margin-bottom: 12px;
  }
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }
  .card-title {
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 1.2px;
    text-transform: uppercase;
    color: #00E5FF;
    display: flex;
    align-items: center;
    gap: 6px;
  }
  .badge {
    background: #0A1C29;
    border: 1px solid #00E5FF55;
    color: #00E5FF;
    font-size: 9.5px;
    font-weight: 700;
    padding: 3px 8px;
    border-radius: 6px;
    font-family: monospace;
  }
  .badge-success {
    background: #082618;
    border-color: #00E67655;
    color: #00E676;
  }
  .badge-warning {
    background: #2E1508;
    border-color: #FF910055;
    color: #FF9100;
  }
  .chart-box {
    width: 100%;
    position: relative;
  }
  svg {
    width: 100%;
    height: auto;
    display: block;
  }
  .recharts-legend {
    display: flex;
    gap: 12px;
    font-size: 9.5px;
    color: #94A3B8;
    margin-top: 10px;
    justify-content: center;
    font-weight: 500;
    flex-wrap: wrap;
  }
  .legend-item {
    display: flex;
    align-items: center;
    gap: 5px;
  }
  .legend-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }
  .metric-kpi-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;
    margin-bottom: 12px;
  }
  .kpi-tile {
    background: #0D1624;
    border: 1px solid #1E2D42;
    border-radius: 8px;
    padding: 10px;
  }
  .kpi-label {
    font-size: 8.5px;
    text-transform: uppercase;
    color: #64748B;
    font-weight: 700;
    letter-spacing: 0.5px;
    margin-bottom: 2px;
  }
  .kpi-value {
    font-size: 18px;
    font-weight: 800;
    color: #FFFFFF;
    font-family: monospace;
  }
  #recharts-tooltip {
    position: fixed;
    display: none;
    background: #0D1420;
    border: 1px solid #00E5FF;
    border-radius: 8px;
    padding: 8px 12px;
    font-size: 10.5px;
    color: #FFFFFF;
    pointer-events: none;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.85);
    z-index: 1000;
  }
</style>
</head>
<body>

<div id="recharts-tooltip"></div>

<!-- Metric Highlights -->
<div class="metric-kpi-row">
  <div class="kpi-tile">
    <div class="kpi-label">Negative to Resolved Rate</div>
    <div class="kpi-value" style="color: #00E676;">${shiftRate}%</div>
    <div style="font-size: 8.5px; color: #00E676; margin-top: 2px;">⚡ ${totalRes} of ${totalNeg} Converted</div>
  </div>
  <div class="kpi-tile">
    <div class="kpi-label">Net Sentiment Velocity</div>
    <div class="kpi-value" style="color: #00E5FF;">${avgShift} Δ</div>
    <div style="font-size: 8.5px; color: #94A3B8; margin-top: 2px;">Avg ${avgTurns} Turns to De-escalate</div>
  </div>
</div>

<!-- Chart 1: Recharts Sentiment Flow Stream (Negative vs Resolved AreaChart) -->
<div class="card">
  <div class="card-header">
    <div class="card-title">
      <span style="color:#00E676;">🌊</span> SENTIMENT FLOW STREAM (RECHARTS)
    </div>
    <span class="badge badge-success">SHIFT: +${shiftRate}%</span>
  </div>
  <div class="chart-box" id="sentiment-stream-chart"></div>
  <div class="recharts-legend">
    <div class="legend-item"><div class="legend-dot" style="background:#FF1744;"></div> Negative Influx</div>
    <div class="legend-item"><div class="legend-dot" style="background:#FF9100;"></div> AI Triage Active</div>
    <div class="legend-item"><div class="legend-dot" style="background:#00E676;"></div> Resolved & Satisfied</div>
    <div class="legend-item"><div class="legend-dot" style="background:#00E5FF; border-radius: 0; width: 10px; height: 2px;"></div> Net Velocity Curve</div>
  </div>
</div>

<!-- Chart 2: Recharts Negative-to-Resolved State Transition by Category -->
<div class="card">
  <div class="card-header">
    <div class="card-title" style="color:#00E5FF;">
      <span style="color:#00E5FF;">📊</span> TRIAGE RESOLUTION BY TRIGGER CATEGORY
    </div>
    <span class="badge">${summary.triggerMetrics.size} TRIGGERS</span>
  </div>
  <div class="chart-box" id="trigger-shift-chart"></div>
  <div class="recharts-legend">
    <div class="legend-item"><div class="legend-dot" style="background:#FF1744;"></div> Initial Negative (Discontent)</div>
    <div class="legend-item"><div class="legend-dot" style="background:#00E676;"></div> Final Post-Triage Resolved</div>
  </div>
</div>

<!-- Chart 3: Recharts Turn-by-Turn De-escalation Velocity -->
<div class="card">
  <div class="card-header">
    <div class="card-title" style="color:#FF9100;">
      <span style="color:#FF9100;">⚡</span> TURN-BY-TURN DE-ESCALATION VELOCITY
    </div>
    <span class="badge badge-warning">AVG: ${avgTurns} TURNS</span>
  </div>
  <div class="chart-box" id="turn-velocity-chart"></div>
  <div class="recharts-legend">
    <div class="legend-item"><div class="legend-dot" style="background:#FF1744;"></div> T1: Complaint</div>
    <div class="legend-item"><div class="legend-dot" style="background:#FF9100;"></div> T2: Empathy Triage</div>
    <div class="legend-item"><div class="legend-dot" style="background:#00E5FF;"></div> T3: Resolution Offer</div>
    <div class="legend-item"><div class="legend-dot" style="background:#00E676;"></div> T4: Delighted & Booked</div>
  </div>
</div>

<script>
const streamData = $streamJson;
const triggerData = $triggerJson;
const velocityData = $turnVelocityJson;
const tooltip = document.getElementById('recharts-tooltip');

function showTooltip(e, html) {
  tooltip.innerHTML = html;
  tooltip.style.display = 'block';
  const x = Math.min(window.innerWidth - 170, Math.max(10, e.clientX - 60));
  const y = Math.max(10, e.clientY - 60);
  tooltip.style.left = x + 'px';
  tooltip.style.top = y + 'px';
}
function hideTooltip() { tooltip.style.display = 'none'; }

// 1. RENDER SENTIMENT FLOW STREAM (RECHARTS AREA CHART)
(function renderStream() {
  const container = document.getElementById('sentiment-stream-chart');
  const width = 340;
  const height = 160;
  const margin = { top: 16, right: 14, bottom: 22, left: 28 };
  const innerW = width - margin.left - margin.right;
  const innerH = height - margin.top - margin.bottom;

  let maxVol = 1;
  streamData.forEach(d => {
    const total = d.negative + d.triaging + d.resolved;
    if (total > maxVol) maxVol = total;
  });
  maxVol = Math.max(maxVol, 20);

  const step = innerW / Math.max(1, streamData.length - 1);

  let svg = '<svg viewBox="0 0 ' + width + ' ' + height + '">';
  svg += '<defs>';
  svg += '<linearGradient id="negGrad" x1="0" y1="0" x2="0" y2="1">';
  svg += '<stop offset="0%" stop-color="#FF1744" stop-opacity="0.55"/>';
  svg += '<stop offset="100%" stop-color="#FF1744" stop-opacity="0.05"/>';
  svg += '</linearGradient>';
  svg += '<linearGradient id="resGrad" x1="0" y1="0" x2="0" y2="1">';
  svg += '<stop offset="0%" stop-color="#00E676" stop-opacity="0.55"/>';
  svg += '<stop offset="100%" stop-color="#00E676" stop-opacity="0.05"/>';
  svg += '</linearGradient>';
  svg += '</defs>';

  // Grid
  for (let g = 0; g <= 3; g++) {
    const gy = margin.top + (innerH / 3) * g;
    svg += '<line x1="' + margin.left + '" y1="' + gy + '" x2="' + (width - margin.right) + '" y2="' + gy + '" stroke="#141E2D" stroke-dasharray="2,2"/>';
  }

  // Points calculation
  const negPoints = [];
  const resPoints = [];
  const velPoints = [];

  streamData.forEach((d, i) => {
    const x = margin.left + i * step;
    const ny = margin.top + innerH - (d.negative / maxVol) * innerH;
    const ry = margin.top + innerH - (d.resolved / maxVol) * innerH;
    // Net velocity line: -1.0 -> 0 -> +1.0 mapped to innerH
    const vy = margin.top + innerH * (0.5 - d.netSentiment * 0.45);
    negPoints.push({ x, y: ny, data: d });
    resPoints.push({ x, y: ry, data: d });
    velPoints.push({ x, y: vy, data: d });
  });

  // Render Negative Area (Decaying)
  if (negPoints.length > 0) {
    let aPath = 'M ' + negPoints[0].x + ' ' + (margin.top + innerH);
    let lPath = 'M ' + negPoints[0].x + ' ' + negPoints[0].y;
    negPoints.forEach(p => {
      aPath += ' L ' + p.x + ' ' + p.y;
      lPath += ' L ' + p.x + ' ' + p.y;
    });
    aPath += ' L ' + negPoints[negPoints.length - 1].x + ' ' + (margin.top + innerH) + ' Z';
    svg += '<path d="' + aPath + '" fill="url(#negGrad)"/>';
    svg += '<path d="' + lPath + '" fill="none" stroke="#FF1744" stroke-width="1.8"/>';
  }

  // Render Resolved Area (Climbing)
  if (resPoints.length > 0) {
    let aPath = 'M ' + resPoints[0].x + ' ' + (margin.top + innerH);
    let lPath = 'M ' + resPoints[0].x + ' ' + resPoints[0].y;
    resPoints.forEach(p => {
      aPath += ' L ' + p.x + ' ' + p.y;
      lPath += ' L ' + p.x + ' ' + p.y;
    });
    aPath += ' L ' + resPoints[resPoints.length - 1].x + ' ' + (margin.top + innerH) + ' Z';
    svg += '<path d="' + aPath + '" fill="url(#resGrad)"/>';
    svg += '<path d="' + lPath + '" fill="none" stroke="#00E676" stroke-width="2.2"/>';
  }

  // Render Net Sentiment Velocity Curve
  if (velPoints.length > 0) {
    let vPath = 'M ' + velPoints[0].x + ' ' + velPoints[0].y;
    velPoints.forEach(p => { vPath += ' L ' + p.x + ' ' + p.y; });
    svg += '<path d="' + vPath + '" fill="none" stroke="#00E5FF" stroke-width="2" stroke-dasharray="3,2"/>';
  }

  // Interactive dots & X-axis labels
  streamData.forEach((d, i) => {
    const x = margin.left + i * step;
    svg += '<text x="' + x + '" y="' + (height - 6) + '" fill="#64748B" font-size="7.5" text-anchor="middle" font-family="monospace">' + d.time + '</text>';

    const tipHtml = '<strong>⏰ ' + d.time + ' Stream Shift</strong><br/>' +
                    '• Negative Influx: <span style=\"color:#FF1744;font-weight:bold;\">' + d.negative + ' msgs</span><br/>' +
                    '• AI Triage Active: <span style=\"color:#FF9100;font-weight:bold;\">' + d.triaging + ' msgs</span><br/>' +
                    '• Resolved & Booked: <span style=\"color:#00E676;font-weight:bold;\">' + d.resolved + ' msgs</span><br/>' +
                    '• Net Sentiment: <span style=\"color:#00E5FF;font-weight:bold;\">' + (d.netSentiment > 0 ? '+' : '') + d.netSentiment.toFixed(2) + '</span>';

    const pRes = resPoints[i];
    svg += '<circle cx="' + pRes.x + '" cy="' + pRes.y + '" r="4" fill="#00E676" stroke="#06090E" stroke-width="1.5" style="cursor:pointer;" onmousemove="showTooltip(event, \'' + tipHtml + '\')" onmouseleave="hideTooltip()"/>';
  });

  svg += '</svg>';
  container.innerHTML = svg;
})();

// 2. RENDER TRIAGE RESOLUTION BY TRIGGER CATEGORY (RECHARTS BAR)
(function renderTriggers() {
  const container = document.getElementById('trigger-shift-chart');
  const width = 340;
  const height = 160;
  const margin = { top: 12, right: 12, bottom: 20, left: 105 };
  const innerW = width - margin.left - margin.right;
  const innerH = height - margin.top - margin.bottom;

  let svg = '<svg viewBox="0 0 ' + width + ' ' + height + '">';
  const barH = Math.min(16, (innerH / triggerData.length) - 6);

  triggerData.forEach((d, i) => {
    const y = margin.top + i * (innerH / triggerData.length) + 2;

    const shortName = d.name.length > 18 ? d.name.substring(0, 17) + '…' : d.name;
    svg += '<text x="' + (margin.left - 6) + '" y="' + (y + barH * 0.72) + '" fill="#94A3B8" font-size="8" text-anchor="end" font-family="sans-serif">' + shortName + '</text>';

    // Track Background
    svg += '<rect x="' + margin.left + '" y="' + y + '" width="' + innerW + '" height="' + barH + '" fill="#111B28" rx="3"/>';

    // Negative initial bar (Crimson)
    const negW = Math.abs(d.negativeScore) * (innerW * 0.45);
    // Resolved final bar (Neon Green)
    const resW = d.resolvedScore * innerW;

    svg += '<rect x="' + margin.left + '" y="' + y + '" width="' + resW + '" height="' + barH + '" fill="#00E676" rx="3" opacity="0.9"/>';
    svg += '<rect x="' + margin.left + '" y="' + (y + barH * 0.6) + '" width="' + negW + '" height="' + (barH * 0.4) + '" fill="#FF1744" rx="2" opacity="0.8"/>';

    // Label on bar
    svg += '<text x="' + (margin.left + resW - 4) + '" y="' + (y + barH * 0.7) + '" fill="#06090E" font-size="7.5" font-weight="bold" text-anchor="end" font-family="monospace">' + d.rate + '%</text>';

    const tipHtml = '<strong>' + d.name + '</strong><br/>' +
                    'Initial Negative: <span style=\"color:#FF1744;font-weight:bold;\">' + d.negativeScore.toFixed(2) + '</span><br/>' +
                    'Post-Triage Resolved: <span style=\"color:#00E676;font-weight:bold;\">+' + d.resolvedScore.toFixed(2) + '</span><br/>' +
                    'Net Shift Delta: <span style=\"color:#00E5FF;font-weight:bold;\">+' + d.delta.toFixed(2) + ' Δ</span><br/>' +
                    'Resolution Success: <span style=\"color:#00E676;font-weight:bold;\">' + d.rate + '%</span> (' + d.count + ' threads)';

    svg += '<rect x="' + margin.left + '" y="' + y + '" width="' + innerW + '" height="' + barH + '" fill="transparent" style="cursor:pointer;" onmousemove="showTooltip(event, \'' + tipHtml + '\')" onmouseleave="hideTooltip()"/>';
  });

  svg += '</svg>';
  container.innerHTML = svg;
})();

// 3. RENDER TURN-BY-TURN DE-ESCALATION VELOCITY
(function renderVelocity() {
  const container = document.getElementById('turn-velocity-chart');
  const width = 340;
  const height = 130;
  const margin = { top: 16, right: 18, bottom: 22, left: 30 };
  const innerW = width - margin.left - margin.right;
  const innerH = height - margin.top - margin.bottom;

  let svg = '<svg viewBox="0 0 ' + width + ' ' + height + '">';

  // Zero-line axis (neutral sentiment = 0.0)
  const zeroY = margin.top + innerH * 0.5;
  svg += '<line x1="' + margin.left + '" y1="' + zeroY + '" x2="' + (width - margin.right) + '" y2="' + zeroY + '" stroke="#334155" stroke-dasharray="2,2"/>';
  svg += '<text x="' + (margin.left - 4) + '" y="' + (zeroY + 3) + '" fill="#64748B" font-size="7.5" text-anchor="end" font-family="monospace">0.0</text>';

  const step = innerW / (velocityData.length - 1);
  const points = [];

  velocityData.forEach((d, i) => {
    const x = margin.left + i * step;
    // -1.0 maps to bottom, +1.0 maps to top
    const y = margin.top + innerH * (0.5 - d.sentiment * 0.45);
    points.push({ x, y, data: d });
  });

  if (points.length > 0) {
    let pStr = 'M ' + points[0].x + ' ' + points[0].y;
    points.forEach(p => { pStr += ' L ' + p.x + ' ' + p.y; });
    svg += '<path d="' + pStr + '" fill="none" stroke="#00E676" stroke-width="2.5" stroke-linejoin="round"/>';

    points.forEach((p, idx) => {
      svg += '<circle cx="' + p.x + '" cy="' + p.y + '" r="5" fill="' + p.data.color + '" stroke="#06090E" stroke-width="2" style="cursor:pointer;" onmousemove="showTooltip(event, \'<strong>' + p.data.turn + ': ' + p.data.label + '</strong><br/>Sentiment: <strong>' + (p.data.sentiment > 0 ? '+' : '') + p.data.sentiment.toFixed(2) + '</strong><br/>Stage: ' + p.data.stage + '<br/>Note: ' + p.data.note + '\')" onmouseleave="hideTooltip()"/>';
      svg += '<text x="' + p.x + '" y="' + (height - 6) + '" fill="#94A3B8" font-size="8" text-anchor="middle" font-family="monospace">' + p.data.turn + '</text>';
    });
  }

  svg += '</svg>';
  container.innerHTML = svg;
})();
</script>
</body>
</html>
        """.trimIndent()
    }
}
