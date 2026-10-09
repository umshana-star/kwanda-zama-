package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.ChatThemePalette

@Composable
fun AgentSettingsDialog(
    palette: ChatThemePalette,
    isAwayModeActive: Boolean,
    awayMessage: String,
    isAutoReplyActive: Boolean,
    onToggleAwayMode: (Boolean) -> Unit,
    onUpdateAwayMessage: (String) -> Unit,
    onResetAwayMessage: () -> Unit,
    onToggleAutoReply: (Boolean) -> Unit,
    onSimulateAwayTest: () -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = palette.headerBackground,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp)
                .testTag("agent_settings_dialog")
        ) {
            AgentSettingsPanel(
                palette = palette,
                isAwayModeActive = isAwayModeActive,
                awayMessage = awayMessage,
                isAutoReplyActive = isAutoReplyActive,
                onToggleAwayMode = onToggleAwayMode,
                onUpdateAwayMessage = onUpdateAwayMessage,
                onResetAwayMessage = onResetAwayMessage,
                onToggleAutoReply = onToggleAutoReply,
                onSimulateAwayTest = {
                    onSimulateAwayTest()
                    onDismissRequest()
                }
            )
        }
    }
}
