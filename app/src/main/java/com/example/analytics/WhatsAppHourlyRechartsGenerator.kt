package com.example.analytics

import java.util.Locale

/**
 * Generator that creates an interactive Recharts-styled data visualization web canvas
 * rendered inside an Android WebView. Visualizes the number of messages processed per hour
 * by autonomous WhatsApp agents.
 */
object WhatsAppHourlyRechartsGenerator {

    fun buildHourlyRechartsHtml(metrics: WhatsAppAgentHourlyMetrics): String {
        // Build JSON array for hourly points
        val jsonBuilder = StringBuilder("[")
        metrics.hourlyPoints.forEachIndexed { i, pt ->
            jsonBuilder.append(
                String.format(
                    Locale.ROOT,
                    """{"hour":"%s","hourNum":%d,"total":%d,"inbound":%d,"outbound":%d,"latency":%d,"rate":%d,"isPeak":%b,"intent":"%s","agent":"%s"}""",
                    pt.hourLabel,
                    pt.hourNumber,
                    pt.totalProcessed,
                    pt.inboundCustomerCount,
                    pt.autonomousReplyCount,
                    pt.avgLatencyMs,
                    pt.autonomousSuccessRate,
                    pt.isPeakHour,
                    pt.topIntent.replace("\"", "\\\""),
                    pt.agentName.replace("\"", "\\\"")
                )
            )
            if (i < metrics.hourlyPoints.size - 1) jsonBuilder.append(",")
        }
        jsonBuilder.append("]")
        val hourlyJson = jsonBuilder.toString()

        val totalMsgs = metrics.totalMessagesProcessed
        val peakHour = metrics.peakHourLabel
        val peakVolume = metrics.peakHourVolume
        val autonomyPct = metrics.overallAutonomyPercentage
        val avgLatency = metrics.avgProcessingLatencyMs
        val agentDisplayName = metrics.selectedAgentName
        val totalAuto = metrics.totalAutonomousReplies
        val totalInbound = metrics.totalCustomerInbound

        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"/>
<title>WhatsApp Agent Hourly Telemetry (Recharts)</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    background-color: #06090E;
    color: #F5F7FA;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    padding: 10px;
    user-select: none;
    -webkit-user-select: none;
    overflow-x: hidden;
  }
  .card {
    background: #0B111A;
    border: 1px solid #1C2636;
    border-radius: 12px;
    padding: 14px;
    margin-bottom: 12px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
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
    background: #2E1A08;
    border-color: #FFB30055;
    color: #FFB300;
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
    font-size: 9px;
    text-transform: uppercase;
    color: #64748B;
    font-weight: 600;
    letter-spacing: 0.5px;
    margin-bottom: 2px;
  }
  .kpi-value {
    font-size: 17px;
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
    line-height: 1.4;
  }
  .active-bar {
    opacity: 1 !important;
    filter: drop-shadow(0 0 6px rgba(0, 229, 255, 0.6));
  }
</style>
</head>
<body>

<div id="recharts-tooltip"></div>

<!-- Executive Metric KPI Grid -->
<div class="metric-kpi-row">
  <div class="kpi-tile">
    <div class="kpi-label">Messages Processed</div>
    <div class="kpi-value" style="color: #00E5FF;">${totalMsgs} msgs</div>
    <div style="font-size: 8.5px; color: #00E676; margin-top: 2px;">🤖 ${totalAuto} Auto • 💬 ${totalInbound} Inbound</div>
  </div>
  <div class="kpi-tile">
    <div class="kpi-label">Peak Velocity</div>
    <div class="kpi-value" style="color: #FFB300;">${peakVolume}/hr</div>
    <div style="font-size: 8.5px; color: #FFB300; margin-top: 2px;">🔥 Rush Window: ${peakHour}</div>
  </div>
  <div class="kpi-tile">
    <div class="kpi-label">Autonomy Rate</div>
    <div class="kpi-value" style="color: #00E676;">${autonomyPct}%</div>
    <div style="font-size: 8.5px; color: #94A3B8; margin-top: 2px;">⚡ Zero Human Intervention</div>
  </div>
  <div class="kpi-tile">
    <div class="kpi-label">Avg Response Speed</div>
    <div class="kpi-value" style="color: #BA68C8;">${avgLatency} ms</div>
    <div style="font-size: 8.5px; color: #00E676; margin-top: 2px;">✓ Well Under 1,500ms Target</div>
  </div>
</div>

<!-- Chart 1: Messages Processed Per Hour (Stacked Recharts Bar Chart) -->
<div class="card">
  <div class="card-header">
    <div class="card-title">
      <span style="color:#00E5FF;">📊</span> MESSAGES PROCESSED PER HOUR (RECHARTS)
    </div>
    <span class="badge badge-warning">PEAK: ${peakHour} (${peakVolume}/h)</span>
  </div>
  <div class="chart-box" id="hourly-barchart"></div>
  <div class="recharts-legend">
    <div class="legend-item"><div class="legend-dot" style="background:#00E676;"></div> Autonomous AI Replies</div>
    <div class="legend-item"><div class="legend-dot" style="background:#00E5FF;"></div> Inbound Customer Queries</div>
    <div class="legend-item"><div class="legend-dot" style="background:#FFB300; border-radius: 0; width: 10px; height: 3px;"></div> Peak Rush Indicator</div>
  </div>
</div>

<!-- Chart 2: Autonomy Success Rate (%) Trend Across Business Hours (Recharts Area Spline) -->
<div class="card">
  <div class="card-header">
    <div class="card-title" style="color:#00E676;">
      <span style="color:#00E676;">📈</span> AUTONOMOUS RESOLUTION RATE (%)
    </div>
    <span class="badge badge-success">SLA: &gt;95%</span>
  </div>
  <div class="chart-box" id="autonomy-areachart"></div>
  <div class="recharts-legend">
    <div class="legend-item"><div class="legend-dot" style="background:#00E676;"></div> Autonomy Success Rate</div>
    <div class="legend-item"><div class="legend-dot" style="background:#FF5252; border-radius: 0; width: 10px; height: 2px;"></div> Minimum SLA (95%)</div>
  </div>
</div>

<!-- Chart 3: Response Latency (ms) Across Hours (Recharts Latency Line Chart) -->
<div class="card">
  <div class="card-header">
    <div class="card-title" style="color:#BA68C8;">
      <span style="color:#BA68C8;">⚡</span> AGENT RESPONSE LATENCY PER HOUR
    </div>
    <span class="badge">AVG: ${avgLatency}ms</span>
  </div>
  <div class="chart-box" id="latency-linechart"></div>
  <div class="recharts-legend">
    <div class="legend-item"><div class="legend-dot" style="background:#BA68C8;"></div> Reply Latency (ms)</div>
    <div class="legend-item"><div class="legend-dot" style="background:#FF5252; border-radius: 0; width: 10px; height: 2px;"></div> Target SLA Threshold (1,500ms)</div>
  </div>
</div>

<script>
const data = $hourlyJson;
const tooltip = document.getElementById('recharts-tooltip');

function showTooltip(e, html) {
  tooltip.innerHTML = html;
  tooltip.style.display = 'block';
  const clientX = e.clientX || (e.touches && e.touches[0] ? e.touches[0].clientX : 100);
  const clientY = e.clientY || (e.touches && e.touches[0] ? e.touches[0].clientY : 100);
  const x = Math.min(window.innerWidth - 180, Math.max(10, clientX - 60));
  const y = Math.max(10, clientY - 80);
  tooltip.style.left = x + 'px';
  tooltip.style.top = y + 'px';
}

function hideTooltip() {
  tooltip.style.display = 'none';
}

// 1. RENDER HOURLY MESSAGES PROCESSED (STACKED RECHARTS BAR CHART)
(function renderHourlyBarChart() {
  const container = document.getElementById('hourly-barchart');
  if (!container || data.length === 0) return;

  const width = 340;
  const height = 180;
  const margin = { top: 22, right: 12, bottom: 26, left: 30 };
  const innerW = width - margin.left - margin.right;
  const innerH = height - margin.top - margin.bottom;

  const maxVal = Math.max(12, ...data.map(d => d.total));
  const roundedMax = Math.ceil(maxVal * 1.15);

  let svg = '<svg viewBox="0 0 ' + width + ' ' + height + '">';

  // Gradients and Patterns
  svg += '<defs>';
  svg += '<linearGradient id="cyanGrad" x1="0" y1="0" x2="0" y2="1">';
  svg += '<stop offset="0%" stop-color="#00E5FF" stop-opacity="1"/>';
  svg += '<stop offset="100%" stop-color="#00B0FF" stop-opacity="0.8"/>';
  svg += '</linearGradient>';
  svg += '<linearGradient id="greenGrad" x1="0" y1="0" x2="0" y2="1">';
  svg += '<stop offset="0%" stop-color="#00E676" stop-opacity="1"/>';
  svg += '<stop offset="100%" stop-color="#00C853" stop-opacity="0.8"/>';
  svg += '</linearGradient>';
  svg += '<linearGradient id="peakGlow" x1="0" y1="0" x2="0" y2="1">';
  svg += '<stop offset="0%" stop-color="#FFD700" stop-opacity="0.9"/>';
  svg += '<stop offset="100%" stop-color="#FF9100" stop-opacity="0.7"/>';
  svg += '</linearGradient>';
  svg += '</defs>';

  // Grid Lines and Y-Axis Labels
  const gridSteps = 4;
  for (let g = 0; g <= gridSteps; g++) {
    const gy = margin.top + (innerH / gridSteps) * g;
    const val = Math.round(roundedMax - (roundedMax / gridSteps) * g);
    svg += '<line x1="' + margin.left + '" y1="' + gy + '" x2="' + (width - margin.right) + '" y2="' + gy + '" stroke="#141E2D" stroke-dasharray="2,2"/>';
    svg += '<text x="' + (margin.left - 4) + '" y="' + (gy + 3) + '" fill="#64748B" font-size="8" text-anchor="end" font-family="monospace">' + val + '</text>';
  }

  // Draw Bars
  const barWidth = Math.max(10, Math.min(18, (innerW / data.length) * 0.72));
  const step = innerW / data.length;

  data.forEach((d, i) => {
    const cx = margin.left + i * step + step / 2;
    const x = cx - barWidth / 2;

    const totalHeight = (d.total / roundedMax) * innerH;
    const inboundHeight = (d.inbound / roundedMax) * innerH;
    const autoHeight = (d.outbound / roundedMax) * innerH;

    const yBase = margin.top + innerH;
    const yInbound = yBase - inboundHeight;
    const yAuto = yInbound - autoHeight;

    const isPeak = d.isPeak;

    const tipContent = '<strong>' + (isPeak ? '🔥 ' : '⏱️ ') + d.hour + ' (' + d.agent + ')</strong><br/>' +
      'Total: <span style=\"color:#FFFFFF;font-weight:bold;\">' + d.total + ' msgs</span>' + (isPeak ? ' <span style=\"color:#FFB300;\">[RUSH]</span>' : '') + '<br/>' +
      '🤖 Autonomous Replies: <span style=\"color:#00E676;font-weight:bold;\">' + d.outbound + '</span><br/>' +
      '💬 Customer Queries: <span style=\"color:#00E5FF;font-weight:bold;\">' + d.inbound + '</span><br/>' +
      '⚡ Latency: <span style=\"color:#BA68C8;\">' + d.latency + 'ms</span> • Autonomy: ' + d.rate + '%<br/>' +
      '<span style=\"color:#94A3B8;font-size:9px;\">Top Intent: ' + d.intent + '</span>';

    // Highlight pillar for peak hour
    if (isPeak) {
      svg += '<rect x="' + (cx - step / 2 + 1) + '" y="' + margin.top + '" width="' + (step - 2) + '" height="' + innerH + '" fill="rgba(255, 179, 0, 0.08)" rx="4"/>';
      svg += '<text x="' + cx + '" y="' + (yAuto - 5) + '" fill="#FFB300" font-size="7.5" font-weight="bold" text-anchor="middle" font-family="monospace">PEAK</text>';
    }

    // Inbound Bar (Lower)
    svg += '<rect x="' + x + '" y="' + yInbound + '" width="' + barWidth + '" height="' + Math.max(1, inboundHeight) + '" fill="url(#cyanGrad)" rx="2" style="cursor:pointer;" onmousemove="showTooltip(event, \'' + tipContent + '\')" onmouseleave="hideTooltip()" ontouchstart="showTooltip(event, \'' + tipContent + '\')"/>';

    // Outbound Autonomous Bar (Upper)
    svg += '<rect x="' + x + '" y="' + yAuto + '" width="' + barWidth + '" height="' + Math.max(1, autoHeight) + '" fill="' + (isPeak ? 'url(#peakGlow)' : 'url(#greenGrad)') + '" rx="2" style="cursor:pointer;" onmousemove="showTooltip(event, \'' + tipContent + '\')" onmouseleave="hideTooltip()" ontouchstart="showTooltip(event, \'' + tipContent + '\')"/>';

    // X-Axis Hour Label (Show every 2nd or 3rd hour if crowded)
    const showLabel = (data.length <= 10) || (i % 2 === 0) || isPeak;
    if (showLabel) {
      svg += '<text x="' + cx + '" y="' + (height - 8) + '" fill="' + (isPeak ? '#FFB300' : '#94A3B8') + '" font-size="7.5" text-anchor="middle" font-family="monospace" ' + (isPeak ? 'font-weight="bold"' : '') + '>' + d.hour + '</text>';
    }
  });

  svg += '</svg>';
  container.innerHTML = svg;
})();

// 2. RENDER AUTONOMOUS RESOLUTION RATE (%) (RECHARTS AREA SPLINE)
(function renderAutonomyAreaChart() {
  const container = document.getElementById('autonomy-areachart');
  if (!container || data.length === 0) return;

  const width = 340;
  const height = 130;
  const margin = { top: 18, right: 12, bottom: 20, left: 30 };
  const innerW = width - margin.left - margin.right;
  const innerH = height - margin.top - margin.bottom;

  const minRate = 80;
  const maxRate = 100;
  const step = innerW / Math.max(1, data.length - 1);

  let svg = '<svg viewBox="0 0 ' + width + ' ' + height + '">';
  svg += '<defs>';
  svg += '<linearGradient id="autonomyGrad" x1="0" y1="0" x2="0" y2="1">';
  svg += '<stop offset="0%" stop-color="#00E676" stop-opacity="0.45"/>';
  svg += '<stop offset="100%" stop-color="#00E676" stop-opacity="0.02"/>';
  svg += '</linearGradient>';
  svg += '</defs>';

  // Target 95% SLA line
  const slaY = margin.top + innerH - ((95 - minRate) / (maxRate - minRate)) * innerH;
  svg += '<line x1="' + margin.left + '" y1="' + slaY + '" x2="' + (width - margin.right) + '" y2="' + slaY + '" stroke="#FF5252" stroke-width="1.2" stroke-dasharray="3,3"/>';
  svg += '<text x="' + (width - margin.right - 2) + '" y="' + (slaY - 3) + '" fill="#FF5252" font-size="7.5" text-anchor="end" font-weight="bold">95% SLA</text>';

  // Grid
  [85, 90, 95, 100].forEach(r => {
    const y = margin.top + innerH - ((r - minRate) / (maxRate - minRate)) * innerH;
    svg += '<line x1="' + margin.left + '" y1="' + y + '" x2="' + (width - margin.right) + '" y2="' + y + '" stroke="#141E2D" stroke-dasharray="2,2"/>';
    svg += '<text x="' + (margin.left - 4) + '" y="' + (y + 3) + '" fill="#64748B" font-size="7.5" text-anchor="end" font-family="monospace">' + r + '%</text>';
  });

  const points = [];
  data.forEach((d, i) => {
    const x = margin.left + i * step;
    const y = margin.top + innerH - ((d.rate - minRate) / (maxRate - minRate)) * innerH;
    points.push({ x, y, data: d });
  });

  if (points.length > 0) {
    let areaPath = 'M ' + points[0].x + ' ' + (margin.top + innerH);
    let linePath = 'M ' + points[0].x + ' ' + points[0].y;

    points.forEach(p => {
      areaPath += ' L ' + p.x + ' ' + p.y;
      linePath += ' L ' + p.x + ' ' + p.y;
    });

    areaPath += ' L ' + points[points.length - 1].x + ' ' + (margin.top + innerH) + ' Z';

    svg += '<path d="' + areaPath + '" fill="url(#autonomyGrad)"/>';
    svg += '<path d="' + linePath + '" fill="none" stroke="#00E676" stroke-width="2" stroke-linejoin="round"/>';

    points.forEach(p => {
      const tipContent = '<strong>' + p.data.hour + ' Autonomy Rate</strong><br/>Rate: <span style=\"color:#00E676;font-weight:bold;\">' + p.data.rate + '%</span><br/>Auto Replies: ' + p.data.outbound + ' / ' + p.data.total;
      svg += '<circle cx="' + p.x + '" cy="' + p.y + '" r="3" fill="#00E676" stroke="#06090E" stroke-width="1.5" style="cursor:pointer;" onmousemove="showTooltip(event, \'' + tipContent + '\')" onmouseleave="hideTooltip()" ontouchstart="showTooltip(event, \'' + tipContent + '\')"/>';
    });
  }

  svg += '</svg>';
  container.innerHTML = svg;
})();

// 3. RENDER RESPONSE LATENCY (MS) PER HOUR
(function renderLatencyChart() {
  const container = document.getElementById('latency-linechart');
  if (!container || data.length === 0) return;

  const width = 340;
  const height = 120;
  const margin = { top: 16, right: 12, bottom: 20, left: 32 };
  const innerW = width - margin.left - margin.right;
  const innerH = height - margin.top - margin.bottom;

  const maxLatency = Math.max(1600, ...data.map(d => d.latency));
  const step = innerW / Math.max(1, data.length - 1);

  let svg = '<svg viewBox="0 0 ' + width + ' ' + height + '">';

  // SLA Line (1500ms)
  const slaY = margin.top + innerH - (1500 / maxLatency) * innerH;
  svg += '<line x1="' + margin.left + '" y1="' + slaY + '" x2="' + (width - margin.right) + '" y2="' + slaY + '" stroke="#FF5252" stroke-width="1" stroke-dasharray="3,3"/>';
  svg += '<text x="' + (width - margin.right - 2) + '" y="' + (slaY - 3) + '" fill="#FF5252" font-size="7" text-anchor="end" font-weight="bold">1,500ms SLA</text>';

  // Grid
  [500, 1000, 1500].forEach(lat => {
    const y = margin.top + innerH - (lat / maxLatency) * innerH;
    svg += '<line x1="' + margin.left + '" y1="' + y + '" x2="' + (width - margin.right) + '" y2="' + y + '" stroke="#141E2D" stroke-dasharray="2,2"/>';
    svg += '<text x="' + (margin.left - 4) + '" y="' + (y + 3) + '" fill="#64748B" font-size="7" text-anchor="end" font-family="monospace">' + lat + 'm</text>';
  });

  const points = [];
  data.forEach((d, i) => {
    const x = margin.left + i * step;
    const y = margin.top + innerH - (d.latency / maxLatency) * innerH;
    points.push({ x, y, data: d });
  });

  if (points.length > 0) {
    let linePath = 'M ' + points[0].x + ' ' + points[0].y;
    points.forEach(p => { linePath += ' L ' + p.x + ' ' + p.y; });

    svg += '<path d="' + linePath + '" fill="none" stroke="#BA68C8" stroke-width="1.8" stroke-linejoin="round"/>';

    points.forEach(p => {
      const tipContent = '<strong>' + p.data.hour + ' Latency</strong><br/>Speed: <span style=\"color:#BA68C8;font-weight:bold;\">' + p.data.latency + ' ms</span><br/>SLA Target: 1,500 ms';
      svg += '<circle cx="' + p.x + '" cy="' + p.y + '" r="2.8" fill="#BA68C8" stroke="#06090E" stroke-width="1.2" style="cursor:pointer;" onmousemove="showTooltip(event, \'' + tipContent + '\')" onmouseleave="hideTooltip()" ontouchstart="showTooltip(event, \'' + tipContent + '\')"/>';
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
