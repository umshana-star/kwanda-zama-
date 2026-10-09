package com.example.security

import android.content.Context
import com.example.BuildConfig
import com.example.service.whatsapp.proxy.GeminiServerProxyClient

/**
 * Unified runtime security provider that resolves the Gemini API credential
 * or delegates to a server-side proxy.
 *
 * Implements a defense-in-depth hierarchy:
 * 1. Server-side proxy (zero client-side keys stored or exposed).
 * 2. Hardware-backed Android KeyStore encrypted storage (AES-256-GCM).
 * 3. Dynamic runtime environment secret injection (e.g. from AI Studio Secrets Panel or System Environment).
 * 4. Graceful autonomous fallback if no external API key is configured.
 */
class SecureApiKeyProvider(
    private val context: Context,
    val keyStoreManager: EncryptedKeyStoreManager = EncryptedKeyStoreManager(context),
    val proxyClient: GeminiServerProxyClient = GeminiServerProxyClient()
) {

    enum class CredentialSource {
        SERVER_SIDE_PROXY,
        ENCRYPTED_KEYSTORE,
        ENVIRONMENT_INJECTION,
        LOCAL_FALLBACK
    }

    /**
     * Determines which security strategy is currently active.
     */
    fun getActiveCredentialSource(): CredentialSource {
        return when {
            proxyClient.isProxyEnabled -> CredentialSource.SERVER_SIDE_PROXY
            keyStoreManager.hasStoredKey() -> CredentialSource.ENCRYPTED_KEYSTORE
            getEnvironmentKey().isNotBlank() -> CredentialSource.ENVIRONMENT_INJECTION
            else -> CredentialSource.LOCAL_FALLBACK
        }
    }

    /**
     * Resolves the active API key at runtime in-memory.
     * Never writes plaintext keys to disk or logs.
     */
    fun resolveApiKey(): String? {
        // Priority 1: Hardware-backed keystore
        val keystoreKey = keyStoreManager.retrieveApiKey()
        if (!keystoreKey.isNullOrBlank()) {
            return keystoreKey
        }

        // Priority 2: Injected environment key (via AI Studio Secrets / BuildConfig)
        val envKey = getEnvironmentKey()
        if (envKey.isNotBlank()) {
            return envKey
        }

        return null
    }

    /**
     * Securely stores an API key in the hardware-backed keystore.
     */
    fun storeEncryptedKey(apiKey: String) {
        keyStoreManager.storeApiKey(apiKey.trim())
    }

    /**
     * Clears any encrypted key from local storage.
     */
    fun clearStoredKey() {
        keyStoreManager.clearApiKey()
    }

    /**
     * Enables or disables server-side proxy mode.
     */
    fun setServerProxyEnabled(enabled: Boolean, endpointUrl: String? = null) {
        proxyClient.isProxyEnabled = enabled
        if (!endpointUrl.isNullOrBlank()) {
            proxyClient.proxyEndpointUrl = endpointUrl
        }
    }

    private fun getEnvironmentKey(): String {
        return try {
            val configKey = BuildConfig.GEMINI_API_KEY
            if (configKey.isNotBlank() && configKey != "MY_GEMINI_API_KEY") {
                configKey.trim()
            } else {
                ""
            }
        } catch (_: Exception) {
            ""
        }
    }
}
