package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.ZamaBorder
import com.example.ui.theme.ZamaChromeLight
import com.example.ui.theme.ZamaChromeMid
import com.example.ui.theme.ZamaElectricCyan
import com.example.ui.theme.ZamaGraphite
import com.example.ui.theme.ZamaVoid

@Composable
fun ZamaEditorialHero(
    onExplore3D: () -> Unit,
    onLaunchChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D111A),
                        ZamaVoid
                    )
                )
            )
            .border(1.dp, ZamaBorder, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        // Monospace Tagline
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0x3300E5FF),
                shape = RoundedCornerShape(100.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6600E5FF))
            ) {
                Text(
                    text = "ZAMA AI / 2026 EDITION",
                    color = ZamaElectricCyan,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Massive Editorial Headline with Glitch Typography
        FuturisticGlitchText(
            text = "INTELLIGENCE\nIN MOTION.",
            effectType = TypographyEffectType.CYBER_GLITCH,
            intensity = 0.85f,
            color = ZamaChromeLight,
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            lineHeight = 42.sp,
            letterSpacing = (-1).sp,
            modifier = Modifier.testTag("hero_glitch_headline")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Manifest Line
        Text(
            text = "WE ARE BUILDING INTELLIGENCE DIFFERENTLY.",
            color = ZamaElectricCyan,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Explanatory Paragraph
        Text(
            text = "The key is: you don't want to build another WhatsApp. WhatsApp is the communication channel. Zama sits behind WhatsApp and acts as an autonomous AI employee that books appointments, quotes prices, and runs client workflows around the clock.",
            color = ZamaChromeMid,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Action CTA Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ElevatedButton(
                onClick = onLaunchChat,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = ZamaElectricCyan,
                    contentColor = ZamaVoid
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("hero_launch_chat_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TEST AI AGENT",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onExplore3D,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ZamaChromeLight
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("hero_explore_3d_btn")
            ) {
                Text(
                    text = "3D SCULPTURE",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
