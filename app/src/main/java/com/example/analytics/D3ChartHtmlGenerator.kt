package com.example.analytics

import java.util.Locale

/**
 * Generator that transforms Room chat analytics data into an interactive D3.js
 * SVG visualization web canvas rendered in Android WebView.
 */
object D3ChartHtmlGenerator {

    /**
     * Generates a self-contained HTML/CSS/JavaScript document with D3 interactive SVG charts.
     */
    fun buildD3Html(summary: ChatAnalyticsSummary): String {
        // Prepare JSON data for Frequency
        val activeHours = summary.hourlyFrequency.filter { it.totalCount > 0 }.ifEmpty {
            summary.hourlyFrequency.filterIndexed { index, _ -> index in 8..19 }
        }

        val frequencyJsonBuilder = StringBuilder("[")
        activeHours.forEachIndexed { i, pt ->
            frequencyJsonBuilder.append(
                String.format(
                    Locale.ROOT,
                    """{"hour":"%s","customer":%d,"ai":%d,"total":%d,"sent":%.2f}""",
                    pt.hourLabel,
                    pt.customerCount,
                    pt.aiCount,
                    pt.totalCount,
                    pt.averageSentiment
                )
            )
            if (i < activeHours.size - 1) frequencyJsonBuilder.append(",")
        }
        frequencyJsonBuilder.append("]")
        val frequencyJson = frequencyJsonBuilder.toString()

        // Prepare JSON data for Sentiment Trend
        val trendJsonBuilder = StringBuilder("[")
        summary.sentimentTrend.forEachIndexed { i, pt ->
            val cleanText = pt.textSnippet
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("&", "&amp;")
                .replace("'", "&#39;")
                .replace("\r", " ")
                .replace("\n", " ")
            trendJsonBuilder.append(
                String.format(
                    Locale.ROOT,
                    """{"i":%d,"t":"%s","score":%.2f,"cat":"%s","tag":"%s","cust":%b,"txt":"%s"}""",
                    pt.index,
                    pt.timeFormatted,
                    pt.sentimentScore,
                    pt.category.name,
                    pt.intentTag,
                    pt.isCustomer,
                    cleanText
                )
            )
            if (i < summary.sentimentTrend.size - 1) trendJsonBuilder.append(",")
        }
        trendJsonBuilder.append("]")
        val trendJson = trendJsonBuilder.toString()

        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"/>
<title>Zama D3.js Chat Analytics Canvas</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    background-color: #050505;
    color: #F5F7FA;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, monospace;
    padding: 12px;
    user-select: none;
    -webkit-user-select: none;
  }
  .card {
    background: #0A0C10;
    border: 1px solid #1E232E;
    border-radius: 12px;
    padding: 14px;
    margin-bottom: 12px;
  }
  .chart-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
  }
  .chart-title {
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 1.5px;
    text-transform: uppercase;
    color: #00E5FF;
    display: flex;
    align-items: center;
    gap: 6px;
  }
  .badge {
    background: #111B24;
    border: 1px solid #00E5FF44;
    color: #00E5FF;
    font-size: 9px;
    font-weight: 700;
    padding: 2px 6px;
    border-radius: 4px;
  }
  .chart-container {
    width: 100%;
    position: relative;
  }
  svg {
    width: 100%;
    height: auto;
    display: block;
  }
  .axis text {
    fill: #6B7280;
    font-size: 9px;
    font-family: monospace;
  }
  .axis line, .axis path {
    stroke: #1E232E;
    stroke-width: 1px;
  }
  .grid line {
    stroke: #12161F;
    stroke-dasharray: 2,2;
  }
  .legend {
    display: flex;
    gap: 12px;
    font-size: 10px;
    color: #9CA3AF;
    margin-top: 8px;
    justify-content: center;
  }
  .legend-item {
    display: flex;
    align-items: center;
    gap: 4px;
  }
  .legend-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }
  #tooltip {
    position: fixed;
    display: none;
    background: #0D1117;
    border: 1px solid #00E5FF;
    padding: 6px 10px;
    border-radius: 6px;
    font-size: 10px;
    color: #FFFFFF;
    pointer-events: none;
    box-shadow: 0 4px 12px rgba(0,0,0,0.8);
    z-index: 100;
  }
</style>
</head>
<body>

<div id="tooltip"></div>

<!-- Chart 1: Activity Frequency -->
<div class="card">
  <div class="chart-header">
    <div class="chart-title">
      <span>●</span> CHAT ACTIVITY FREQUENCY (D3.js)
    </div>
    <span class="badge">${summary.totalMessages} MSGS</span>
  </div>
  <div class="chart-container" id="freq-chart"></div>
  <div class="legend">
    <div class="legend-item"><div class="legend-dot" style="background:#00E5FF;"></div> Customer</div>
    <div class="legend-item"><div class="legend-dot" style="background:#00E676;"></div> AI Agent</div>
  </div>
</div>

<!-- Chart 2: Sentiment Trajectory -->
<div class="card">
  <div class="chart-header">
    <div class="chart-title" style="color: #00E676;">
      <span>▲</span> SENTIMENT TRAJECTORY (D3.js)
    </div>
    <span class="badge" style="border-color:#00E67644; color:#00E676;">
      AVG: ${String.format(Locale.ROOT, "%+.2f", summary.averageSentiment)}
    </span>
  </div>
  <div class="chart-container" id="sentiment-chart"></div>
  <div class="legend">
    <div class="legend-item"><div class="legend-dot" style="background:#00E676;"></div> Positive</div>
    <div class="legend-item"><div class="legend-dot" style="background:#00E5FF;"></div> Inquisitive</div>
    <div class="legend-item"><div class="legend-dot" style="background:#FF5252;"></div> Critical</div>
  </div>
</div>

<script>
const freqData = $frequencyJson;
const trendData = $trendJson;
const tooltip = document.getElementById('tooltip');

function showTooltip(e, html) {
  tooltip.innerHTML = html;
  tooltip.style.display = 'block';
  tooltip.style.left = Math.min(window.innerWidth - 130, Math.max(10, e.clientX - 50)) + 'px';
  tooltip.style.top = Math.max(10, e.clientY - 45) + 'px';
}
function hideTooltip() { tooltip.style.display = 'none'; }

// 1. RENDER ACTIVITY FREQUENCY (D3/SVG)
(function renderFrequency() {
  const container = document.getElementById('freq-chart');
  const width = 340;
  const height = 140;
  const margin = { top: 15, right: 15, bottom: 25, left: 25 };
  const innerW = width - margin.left - margin.right;
  const innerH = height - margin.top - margin.bottom;

  let maxVal = 1;
  freqData.forEach(d => { if (d.total > maxVal) maxVal = d.total; });
  maxVal = Math.max(maxVal, 4);

  let svg = '<svg viewBox="0 0 ' + width + ' ' + height + '">';
  svg += '<defs>';
  svg += '<linearGradient id="custGrad" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#00E5FF" stop-opacity="0.9"/><stop offset="100%" stop-color="#00E5FF" stop-opacity="0.1"/></linearGradient>';
  svg += '<linearGradient id="aiGrad" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#00E676" stop-opacity="0.9"/><stop offset="100%" stop-color="#00E676" stop-opacity="0.1"/></linearGradient>';
  svg += '</defs>';
  svg += '<g transform="translate(' + margin.left + ',' + margin.top + ')">';

  // Gridlines
  for (let step = 0; step <= 3; step++) {
    const y = innerH - (innerH * (step / 3));
    svg += '<line x1="0" y1="' + y + '" x2="' + innerW + '" y2="' + y + '" stroke="#1B202B" stroke-dasharray="2,2"/>';
  }

  // Bars
  const n = Math.max(1, freqData.length);
  const slotW = innerW / n;
  const barW = Math.max(4, Math.min(18, slotW * 0.4));

  freqData.forEach((d, idx) => {
    const xCenter = (idx + 0.5) * slotW;
    const custH = (d.customer / maxVal) * innerH;
    const aiH = (d.ai / maxVal) * innerH;

    const custX = xCenter - barW - 1;
    const aiX = xCenter + 1;

    // Customer bar
    svg += '<rect x="' + custX + '" y="' + (innerH - custH) + '" width="' + barW + '" height="' + custH + '" fill="url(#custGrad)" rx="2" class="bar" onclick="showBarTip(event, \'' + d.hour + '\', ' + d.customer + ', ' + d.ai + ')"/>';
    // AI bar
    svg += '<rect x="' + aiX + '" y="' + (innerH - aiH) + '" width="' + barW + '" height="' + aiH + '" fill="url(#aiGrad)" rx="2" class="bar" onclick="showBarTip(event, \'' + d.hour + '\', ' + d.customer + ', ' + d.ai + ')"/>';

    // X Axis label
    if (n <= 8 || idx % 2 === 0) {
      svg += '<text x="' + xCenter + '" y="' + (innerH + 16) + '" fill="#6B7280" font-size="8.5" text-anchor="middle" font-family="monospace">' + d.hour + '</text>';
    }
  });

  // Bottom Axis line
  svg += '<line x1="0" y1="' + innerH + '" x2="' + innerW + '" y2="' + innerH + '" stroke="#262C38"/>';
  svg += '</g></svg>';
  container.innerHTML = svg;
})();

window.showBarTip = function(e, hour, cust, ai) {
  showTooltip(e, '<b>' + hour + '</b><br/>Cust: ' + cust + ' | AI: ' + ai);
};

// 2. RENDER SENTIMENT TRAJECTORY (D3/SVG Area & Line)
(function renderSentiment() {
  const container = document.getElementById('sentiment-chart');
  const width = 340;
  const height = 140;
  const margin = { top: 15, right: 15, bottom: 25, left: 25 };
  const innerW = width - margin.left - margin.right;
  const innerH = height - margin.top - margin.bottom;

  if (trendData.length === 0) {
    container.innerHTML = '<div style="text-align:center;padding:30px;color:#6B7280;font-size:11px;">No sentiment data yet</div>';
    return;
  }

  let svg = '<svg viewBox="0 0 ' + width + ' ' + height + '">';
  svg += '<defs>';
  svg += '<linearGradient id="sentGrad" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#00E676" stop-opacity="0.3"/><stop offset="100%" stop-color="#00E676" stop-opacity="0.0"/></linearGradient>';
  svg += '</defs>';
  svg += '<g transform="translate(' + margin.left + ',' + margin.top + ')">';

  // Zero baseline (y = 0.0) -> sentiment -1.0 .. +1.0 mapped to innerH .. 0
  const zeroY = innerH * 0.5;
  svg += '<line x1="0" y1="' + zeroY + '" x2="' + innerW + '" y2="' + zeroY + '" stroke="#2A3546" stroke-dasharray="3,3"/>';
  svg += '<text x="' + (innerW - 4) + '" y="' + (zeroY - 4) + '" fill="#6B7280" font-size="8" text-anchor="end">0.0</text>';

  const n = trendData.length;
  const getX = (i) => n === 1 ? innerW / 2 : (i / (n - 1)) * innerW;
  const getY = (score) => {
    // map score -1.0..+1.0 to innerH..0
    const normalized = (score + 1.0) / 2.0; // 0..1
    return innerH - (normalized * innerH);
  };

  // Build line & area path
  let pathD = '';
  trendData.forEach((d, i) => {
    const x = getX(i);
    const y = getY(d.score);
    pathD += (i === 0 ? 'M' : 'L') + x.toFixed(1) + ',' + y.toFixed(1) + ' ';
  });

  const areaD = pathD + 'L' + innerW + ',' + zeroY + ' L0,' + zeroY + ' Z';
  svg += '<path d="' + areaD + '" fill="url(#sentGrad)"/>';
  svg += '<path d="' + pathD + '" fill="none" stroke="#00E676" stroke-width="2" stroke-linecap="round"/>';

  // Nodes
  trendData.forEach((d, i) => {
    const x = getX(i);
    const y = getY(d.score);
    const col = d.score > 0.4 ? '#00E676' : (d.score < -0.1 ? '#FF5252' : '#00E5FF');
    svg += '<circle cx="' + x.toFixed(1) + '" cy="' + y.toFixed(1) + '" r="4" fill="' + col + '" stroke="#050505" stroke-width="1.5" onclick="showNodeTip(event, \'' + d.t + '\', ' + d.score.toFixed(2) + ', \'' + d.tag + '\')"/>';
  });

  svg += '<line x1="0" y1="' + innerH + '" x2="' + innerW + '" y2="' + innerH + '" stroke="#262C38"/>';
  svg += '</g></svg>';
  container.innerHTML = svg;
})();

window.showNodeTip = function(e, time, score, tag) {
  showTooltip(e, '<b>' + tag + '</b> (' + time + ')<br/>Score: ' + (score >= 0 ? '+' : '') + score);
};

document.body.addEventListener('click', function(e) {
  if (!e.target.closest('circle') && !e.target.closest('.bar')) {
    hideTooltip();
  }
});
</script>
</body>
</html>
        """.trimIndent()
    }
}
