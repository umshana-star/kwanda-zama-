package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TECH_TIMELINE_STEPS
import com.example.model.TechStep
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaGraphite
import com.example.ui.theme.ZamaVoid

@Composable
fun ZamaTechnologyTimeline(
    modifier: Modifier = Modifier
) {
    var expandedStepNumber by remember { mutableStateOf<String?>("01") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF090D15),
                        ZamaVoid
                    )
                )
            )
            .border(1.dp, ZamaBorder, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SECTION 07 // TECHNOLOGY STACK",
                    color = ZamaElectricCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "THE ZERO-KNOWLEDGE PIPELINE",
                    color = ZamaChromeLight,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Surface(
                color = Color(0x3300E5FF),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text(
                    text = "4-PHASE PROTOCOL",
                    color = ZamaElectricCyan,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Vertical Timeline
        TECH_TIMELINE_STEPS.forEachIndexed { index, step ->
            val isExpanded = expandedStepNumber == step.number
            TimelineStepItem(
                step = step,
                isExpanded = isExpanded,
                isLast = index == TECH_TIMELINE_STEPS.size - 1,
                onToggle = {
                    expandedStepNumber = if (isExpanded) null else step.number
                }
            )
        }
    }
}

@Composable
private fun TimelineStepItem(
    step: TechStep,
    isExpanded: Boolean,
    isLast: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tech_step_${step.number}")
    ) {
        // Timeline Axis with Indicator Node
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isExpanded) ZamaElectricCyan else Color(0xFF161C26))
                    .border(
                        1.dp,
                        if (isExpanded) Color.White else ZamaBorder,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            if (isExpanded) ZamaVoid else ZamaElectricCyan,
                            CircleShape
                        )
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(if (isExpanded) 140.dp else 56.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    if (isExpanded) ZamaElectricCyan else Color(0xFF263040),
                                    Color(0xFF161C26)
                                )
                            )
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Content Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (isExpanded) Color(0xFF10141D) else Color(0x660B0E14))
                .border(
                    1.dp,
                    if (isExpanded) Color(0x6600E5FF) else Color(0x1FFFFFFF),
                    RoundedCornerShape(14.dp)
                )
                .clickable { onToggle() }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = step.number,
                        color = if (isExpanded) ZamaElectricCyan else ZamaGraphite,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = step.title,
                        color = ZamaChromeLight,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0x22FFFFFF),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = step.status,
                            color = ZamaElectricCyan,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = "Expand details",
                        tint = ZamaChromeMid,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = step.subtitle,
                color = ZamaChromeMid,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = step.description,
                        color = Color(0xFFC4CAD6),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = Color(0xFF07090D),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x3300E5FF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = "Cryptographic Spec",
                                tint = ZamaElectricCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = step.formulaOrSpec,
                                color = ZamaElectricCyan,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}
