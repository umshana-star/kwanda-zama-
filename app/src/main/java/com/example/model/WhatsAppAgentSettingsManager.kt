package com.example.model

import android.content.Context
import android.content.SharedPreferences
import com.example.service.whatsapp.WhatsAppAgentService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages persistent preferences for the WhatsApp Autonomous Agent,
 * including Away mode, custom away notices, and master auto-reply toggles.
 */
class WhatsAppAgentSettingsManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "zama_whatsapp_agent_settings"
        private const val KEY_AWAY_MODE_ACTIVE = "key_away_mode_active"
        private const val KEY_AWAY_MESSAGE = "key_away_message"
        private const val KEY_AUTO_REPLY_ACTIVE = "key_auto_reply_active"
        private const val KEY_BUSINESS_HOURS_ENABLED = "key_business_hours_enabled"
        private const val KEY_OPENING_TIME = "key_opening_time"
        private const val KEY_CLOSING_TIME = "key_closing_time"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isAwayModeActive = MutableStateFlow(
        prefs.getBoolean(KEY_AWAY_MODE_ACTIVE, false)
    )
    val isAwayModeActive: StateFlow<Boolean> = _isAwayModeActive.asStateFlow()

    private val _awayMessage = MutableStateFlow(
        prefs.getString(KEY_AWAY_MESSAGE, WhatsAppAgentService.DEFAULT_AWAY_MESSAGE)
            ?: WhatsAppAgentService.DEFAULT_AWAY_MESSAGE
    )
    val awayMessage: StateFlow<String> = _awayMessage.asStateFlow()

    private val _isAutoReplyActive = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_REPLY_ACTIVE, true)
    )
    val isAutoReplyActive: StateFlow<Boolean> = _isAutoReplyActive.asStateFlow()

    fun setAwayModeActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_AWAY_MODE_ACTIVE, active).apply()
        _isAwayModeActive.value = active
    }

    fun setAwayMessage(message: String) {
        val trimmed = message.trim()
        val textToSave = if (trimmed.isNotBlank()) trimmed else WhatsAppAgentService.DEFAULT_AWAY_MESSAGE
        prefs.edit().putString(KEY_AWAY_MESSAGE, textToSave).apply()
        _awayMessage.value = textToSave
    }

    fun resetAwayMessage() {
        setAwayMessage(WhatsAppAgentService.DEFAULT_AWAY_MESSAGE)
    }

    fun setAutoReplyActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_REPLY_ACTIVE, active).apply()
        _isAutoReplyActive.value = active
    }

    fun clearAllSettings() {
        prefs.edit().clear().commit()
        _isAwayModeActive.value = false
        _awayMessage.value = WhatsAppAgentService.DEFAULT_AWAY_MESSAGE
        _isAutoReplyActive.value = true
    }
}
