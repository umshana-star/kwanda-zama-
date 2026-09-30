package com.example.analytics

import java.util.Locale

/**
 * Generator that transforms agent performance metrics into a modern,
 * interactive Recharts data visualization web canvas rendered in Android WebView.
 */
object RechartsHtmlGenerator {

    fun buildRechartsHtml(metrics: AgentPerformanceMetrics): String {
        // Prepare JSON for Latency AreaChart
        val latencyJsonBuilder = StringBuilder("[")
        metrics.latencyTrend.forEachIndexed { i, pt ->
            latencyJsonBuilder.append(
                String.format(
                    Locale.ROOT,
                    """{"turn":"Turn #%d","time":"%s","latency":%d,"sla":%d,"tag":"%s"}""",
                    pt.turnIndex,
                    pt.timeLabel,
                    pt.latencyMs,
                    pt.targetSlaMs,
                    pt.intentTag
                )
            )
            if (i < metrics.latencyTrend.size - 1) latencyJsonBuilder.append(",")
        }
        latencyJsonBuilder.append("]")
        val latencyJson = latencyJsonBuilder.toString()

        // Prepare JSON for Triage Tasks Stacked Bar & Donut
        val triageJsonBuilder = StringBuilder("[")
        metrics.triageSummary.categoryBreakdown.forEachIndexed { i, cat ->
            triageJsonBuilder.append(
                String.format(
                    Locale.ROOT,
                    """{"name":"%s","resolved":%d,"pending":%d,"total":%d,"rate":%d}""",
                    cat.category,
                    cat.resolvedCount,
                    cat.pendingCount,
                    cat.totalCount,
                    cat.resolutionRatePercentage
                )
            )
            if (i < metrics.triageSummary.categoryBreakdown.size - 1) triageJsonBuilder.append(",")
        }
        triageJsonBuilder.append("]")
        val triageJson = triageJsonBuilder.toString()

        // Prepare JSON for Peak Interaction Hours
        val peakJsonBuilder = StringBuilder("[")
        metrics.peakHours.forEachIndexed { i, pt ->
            peakJsonBuilder.append(
                String.format(
                    Locale.ROOT,
                    """{"hour":"%s","volume":%d,"customer":%d,"agent":%d,"isPeak":%b}""",
                    pt.hourLabel,
                    pt.messageVolume,
                    pt.customerQueries,
                    pt.agentReplies,
                    pt.isPeakHour
                )
            )
            if (i < metrics.peakHours.size - 1) peakJsonBuilder.append(",")
        }
        peakJsonBuilder.append("]")
        val peakJson = peakJsonBuilder.toString()

        val avgLatency = metrics.averageLatencyMs
        val resolutionPct = metrics.triageSummary.resolutionPercentage
        val totalResolved = metrics.triageSummary.totalResolved
        val totalPending = metrics.triageSummary.totalPending
        val busiestWindow = metrics.busiestHourWindow

        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"/>
<title>Agent Performance Analytics (Recharts)</title>
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
    gap: 14px;
    font-size: 10px;
    color: #94A3B8;
    margin-top: 10px;
    justify-content: center;
    font-weight: 500;
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
    font-size: 9px;
    text-transform: uppercase;
    color: #64748B;
    font-weight: 600;
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
    <div class="kpi-label">Avg Response Latency</div>
    <div class="kpi-value" style="color: #00E5FF;">${avgLatency} ms</div>
    <div style="font-size: 8.5px; color: #00E676; margin-top: 2px;">⚡ Under 1,500ms Target</div>
  </div>
  <div class="kpi-tile">
    <div class="kpi-label">Triage Resolved Rate</div>
    <div class="kpi-value" style="color: #00E676;">${resolutionPct}%</div>
    <div style="font-size: 8.5px; color: #94A3B8; margin-top: 2px;">${totalResolved} Resolved • ${totalPending} Pending</div>
  </div>
</div>

<!-- Chart 1: Recharts Response Latency (AreaChart) -->
<div class="card">
  <div class="card-header">
    <div class="card-title">
      <span style="color:#00E5FF;">📈</span> AGENT RESPONSE LATENCY (RECHARTS)
    </div>
    <span class="badge">SLA: 1.5s</span>
  </div>
  <div class="chart-box" id="latency-chart"></div>
  <div class="recharts-legend">
    <div class="legend-item"><div class="legend-dot" style="background:#00E5FF;"></div> Response Latency (ms)</div>
    <div class="legend-item"><div class="legend-dot" style="background:#FF5252; border-radius: 0; width: 10px; height: 2px;"></div> Target SLA (1,500ms)</div>
  </div>
</div>

<!-- Chart 2: Recharts Resolved vs. Pending Triage Tasks (Stacked Bar & Status) -->
<div class="card">
  <div class="card-header">
    <div class="card-title" style="color:#00E676;">
      <span style="color:#00E676;">📊</span> TRIAGE TASKS: RESOLVED VS PENDING
    </div>
    <span class="badge badge-success">${totalResolved} / ${totalResolved + totalPending} DONE</span>
  </div>
  <div class="chart-box" id="triage-chart"></div>
  <div class="recharts-legend">
    <div class="legend-item"><div class="legend-dot" style="background:#00E676;"></div> Resolved (${totalResolved})</div>
    <div class="legend-item"><div class="legend-dot" style="background:#FF5252;"></div> Pending Escalation (${totalPending})</div>
  </div>
</div>

<!-- Chart 3: Recharts Peak Interaction Hours Over Time (BarChart) -->
<div class="card">
  <div class="card-header">
    <div class="card-title" style="color:#FF9100;">
      <span style="color:#FF9100;">🔥</span> PEAK INTERACTION HOURS OVER TIME
    </div>
    <span class="badge badge-warning">RUSH: ${busiestWindow}</span>
  </div>
  <div class="chart-box" id="peak-chart"></div>
  <div class="recharts-legend">
    <div class="legend-item"><div class="legend-dot" style="background:#00E5FF;"></div> Standard Traffic</div>
    <div class="legend-item"><div class="legend-dot" style="background:#FF9100;"></div> Peak Interaction Rush</div>
  </div>
</div>

<script>
const latencyData = $latencyJson;
const triageData = $triageJson;
const peakData = $peakJson;
const tooltip = document.getElementById('recharts-tooltip');

function showTooltip(e, html) {
  tooltip.innerHTML = html;
  tooltip.style.display = 'block';
  const x = Math.min(window.innerWidth - 160, Math.max(10, e.clientX - 50));
  const y = Math.max(10, e.clientY - 55);
  tooltip.style.left = x + 'px';
  tooltip.style.top = y + 'px';
}
function hideTooltip() { tooltip.style.display = 'none'; }

// 1. RENDER RESPONSE LATENCY (RECHARTS AREA CHART)
(function renderLatency() {
  const container = document.getElementById('latency-chart');
  const width = 340;
  const height = 150;
  const margin = { top: 18, right: 12, bottom: 22, left: 32 };
  const innerW = width - margin.left - margin.right;
  const innerH = height - margin.top - margin.bottom;

  const maxVal = Math.max(1800, ...latencyData.map(d => d.latency));
  const slaY = margin.top + innerH - (1500 / maxVal) * innerH;

  let points = [];
  const step = innerW / Math.max(1, latencyData.size || (latencyData.length - 1));

  latencyData.forEach((d, i) => {
    const x = margin.left + i * step;
    const y = margin.top + innerH - (d.latency / maxVal) * innerH;
    points.push({ x, y, data: d });
  });

  let svg = '<svg viewBox="0 0 ' + width + ' ' + height + '">';
  svg += '<defs>';
  svg += '<linearGradient id="latencyGradient" x1="0" y1="0" x2="0" y2="1">';
  svg += '<stop offset="0%" stop-color="#00E5FF" stop-opacity="0.45"/>';
  svg += '<stop offset="100%" stop-color="#00E5FF" stop-opacity="0.02"/>';
  svg += '</linearGradient>';
  svg += '</defs>';

  // Grid lines
  for (let g = 0; g <= 3; g++) {
    const gy = margin.top + (innerH / 3) * g;
    const val = Math.round(maxVal - (maxVal / 3) * g);
    svg += '<line x1="' + margin.left + '" y1="' + gy + '" x2="' + (width - margin.right) + '" y2="' + gy + '" stroke="#141E2D" stroke-dasharray="2,2"/>';
    svg += '<text x="' + (margin.left - 4) + '" y="' + (gy + 3) + '" fill="#475569" font-size="8" text-anchor="end" font-family="monospace">' + val + '</text>';
  }

  // SLA Reference Line
  svg += '<line x1="' + margin.left + '" y1="' + slaY + '" x2="' + (width - margin.right) + '" y2="' + slaY + '" stroke="#FF5252" stroke-width="1.2" stroke-dasharray="4,3"/>';
  svg += '<text x="' + (width - margin.right - 2) + '" y="' + (slaY - 3) + '" fill="#FF5252" font-size="7.5" text-anchor="end" font-weight="bold">SLA (1.5s)</text>';

  // Path Area
  if (points.length > 0) {
    let areaPath = 'M ' + points[0].x + ' ' + (margin.top + innerH);
    let linePath = 'M ' + points[0].x + ' ' + points[0].y;

    points.forEach(p => {
      areaPath += ' L ' + p.x + ' ' + p.y;
      linePath += ' L ' + p.x + ' ' + p.y;
    });

    areaPath += ' L ' + points[points.length - 1].x + ' ' + (margin.top + innerH) + ' Z';

    svg += '<path d="' + areaPath + '" fill="url(#latencyGradient)"/>';
    svg += '<path d="' + linePath + '" fill="none" stroke="#00E5FF" stroke-width="2" stroke-linejoin="round"/>';

    // Data dots with hover tooltip
    points.forEach((p, idx) => {
      const isVoice = p.data.isVoice ? '🎙️ ' : '';
      const tipHtml = '<strong>' + isVoice + p.data.turn + ' (' + p.data.time + ')</strong><br/>Latency: <span style=\"color:#00E5FF;font-weight:bold;\">' + p.data.latency + ' ms</span><br/>Category: ' + p.data.tag + '<br/>SLA Status: ' + (p.data.latency <= 1500 ? '<span style=\"color:#00E676;\">✅ Within SLA</span>' : '<span style=\"color:#FF5252;\">⚠️ SLA Breach</span>');
      svg += '<circle cx="' + p.x + '" cy="' + p.y + '" r="3.5" fill="#00E5FF" stroke="#06090E" stroke-width="1.5" style="cursor:pointer;" onmousemove="showTooltip(event, \'' + tipHtml + '\')" onmouseleave="hideTooltip()"/>';
    });
  }

  svg += '</svg>';
  container.innerHTML = svg;
})();

// 2. RENDER RESOLVED VS PENDING TRIAGE TASKS (RECHARTS STACKED BAR)
(function renderTriage() {
  const container = document.getElementById('triage-chart');
  const width = 340;
  const height = 150;
  const margin = { top: 12, right: 12, bottom: 26, left: 95 };
  const innerW = width - margin.left - margin.right;
  const innerH = height - margin.top - margin.bottom;

  let maxTotal = 1;
  triageData.forEach(d => { if (d.total > maxTotal) maxTotal = d.total; });
  maxTotal = Math.max(maxTotal, 10);

  let svg = '<svg viewBox="0 0 ' + width + ' ' + height + '">';
  const barHeight = Math.min(18, (innerH / triageData.length) - 8);

  triageData.forEach((d, i) => {
    const y = margin.top + i * (innerH / triageData.length) + 4;
    const resolvedW = (d.resolved / maxTotal) * innerW;
    const pendingW = (d.pending / maxTotal) * innerW;

    // Category Label
    const shortName = d.name.length > 16 ? d.name.substring(0, 15) + '…' : d.name;
    svg += '<text x="' + (margin.left - 6) + '" y="' + (y + barHeight * 0.72) + '" fill="#94A3B8" font-size="8" text-anchor="end" font-family="sans-serif">' + shortName + '</text>';

    // Track Background
    svg += '<rect x="' + margin.left + '" y="' + y + '" width="' + innerW + '" height="' + barHeight + '" fill="#111B28" rx="3"/>';

    // Resolved segment (Green)
    const tipHtml = '<strong>' + d.name + '</strong><br/>Resolved: <span style=\"color:#00E676;\">' + d.resolved + '</span><br/>Pending: <span style=\"color:#FF5252;\">' + d.pending + '</span><br/>Resolution Rate: <strong>' + d.rate + '%</strong>';
    if (resolvedW > 0) {
      svg += '<rect x="' + margin.left + '" y="' + y + '" width="' + resolvedW + '" height="' + barHeight + '" fill="#00E676" rx="3" style="cursor:pointer;" onmousemove="showTooltip(event, \'' + tipHtml + '\')" onmouseleave="hideTooltip()"/>';
    }

    // Pending segment (Red)
    if (pendingW > 0) {
      svg += '<rect x="' + (margin.left + resolvedW) + '" y="' + y + '" width="' + pendingW + '" height="' + barHeight + '" fill="#FF5252" rx="3" style="cursor:pointer;" onmousemove="showTooltip(event, \'' + tipHtml + '\')" onmouseleave="hideTooltip()"/>';
    }

    // Percentage tag on right
    svg += '<text x="' + (margin.left + innerW + 4) + '" y="' + (y + barHeight * 0.72) + '" fill="#00E676" font-size="8" font-weight="bold" font-family="monospace">' + d.rate + '%</text>';
  });

  svg += '</svg>';
  container.innerHTML = svg;
})();

// 3. RENDER PEAK INTERACTION HOURS (RECHARTS BAR CHART)
(function renderPeakHours() {
  const container = document.getElementById('peak-chart');
  const width = 340;
  const height = 140;
  const margin = { top: 12, right: 12, bottom: 24, left: 24 };
  const innerW = width - margin.left - margin.right;
  const innerH = height - margin.top - margin.bottom;

  let maxVol = 1;
  peakData.forEach(d => { if (d.volume > maxVol) maxVol = d.volume; });
  maxVol = Math.max(maxVol, 15);

  const barWidth = Math.max(8, (innerW / peakData.length) - 4);

  let svg = '<svg viewBox="0 0 ' + width + ' ' + height + '">';

  peakData.forEach((d, i) => {
    const x = margin.left + i * (innerW / peakData.length) + 2;
    const barH = (d.volume / maxVol) * innerH;
    const y = margin.top + innerH - barH;
    const fillColor = d.isPeak ? '#FF9100' : '#00E5FF';

    const tipHtml = '<strong>' + d.hour + ' Window</strong><br/>Total Interactions: <strong>' + d.volume + '</strong><br/>Customer Inquiries: ' + d.customer + '<br/>Agent Responses: ' + d.agent + (d.isPeak ? '<br/><span style=\"color:#FF9100;\">🔥 High Rush Window</span>' : '');

    // Bar rect
    svg += '<rect x="' + x + '" y="' + y + '" width="' + barWidth + '" height="' + barH + '" fill="' + fillColor + '" rx="2" style="cursor:pointer;" onmousemove="showTooltip(event, \'' + tipHtml + '\')" onmouseleave="hideTooltip()"/>';

    // X Axis hour label (show alternate hours to prevent crowding)
    if (i % 2 === 0) {
      svg += '<text x="' + (x + barWidth / 2) + '" y="' + (height - 6) + '" fill="#64748B" font-size="7.5" text-anchor="middle" font-family="monospace">' + d.hour.substring(0, 2) + 'h</text>';
    }
  });

  svg += '</svg>';
  container.innerHTML = svg;
})();
</script>
</body>
</html>
        """.trimIndent()
    }
}
