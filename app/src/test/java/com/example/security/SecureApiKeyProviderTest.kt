package com.example.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.service.whatsapp.intelligence.GeminiAgentIntelligence
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecureApiKeyProviderTest {

    private lateinit var context: Context
    private lateinit var keyStoreManager: EncryptedKeyStoreManager
    private lateinit var secureKeyProvider: SecureApiKeyProvider

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        keyStoreManager = EncryptedKeyStoreManager(context)
        keyStoreManager.clearApiKey()
        secureKeyProvider = SecureApiKeyProvider(context, keyStoreManager)
    }

    @Test
    fun testKeyStoreManager_encryptStoreAndDecryptApiKey() {
        val sampleApiKey = "AIzaSyTestDynamicKey123456789_SecretVault"

        assertFalse(keyStoreManager.hasStoredKey())
        assertNull(keyStoreManager.retrieveApiKey())

        // Store encrypted key
        keyStoreManager.storeApiKey(sampleApiKey)
        assertTrue(keyStoreManager.hasStoredKey())

        // Retrieve and decrypt
        val decrypted = keyStoreManager.retrieveApiKey()
        assertEquals(sampleApiKey, decrypted)

        // Clear and verify removal
        keyStoreManager.clearApiKey()
        assertFalse(keyStoreManager.hasStoredKey())
        assertNull(keyStoreManager.retrieveApiKey())
    }

    @Test
    fun testSecureApiKeyProvider_prefersHardwareKeyStoreOverFallback() {
        val enrolledKey = "AIzaSyEnrolledInKeystore98765"
        secureKeyProvider.storeEncryptedKey(enrolledKey)

        assertEquals(
            SecureApiKeyProvider.CredentialSource.ENCRYPTED_KEYSTORE,
            secureKeyProvider.getActiveCredentialSource()
        )
        assertEquals(enrolledKey, secureKeyProvider.resolveApiKey())

        secureKeyProvider.clearStoredKey()
        assertFalse(secureKeyProvider.keyStoreManager.hasStoredKey())
    }

    @Test
    fun testSecureApiKeyProvider_serverProxyModeTakesPrecedence() {
        secureKeyProvider.setServerProxyEnabled(true, "https://proxy.example.com/api")

        assertEquals(
            SecureApiKeyProvider.CredentialSource.SERVER_SIDE_PROXY,
            secureKeyProvider.getActiveCredentialSource()
        )
        assertTrue(secureKeyProvider.proxyClient.isProxyEnabled)
        assertEquals("https://proxy.example.com/api", secureKeyProvider.proxyClient.proxyEndpointUrl)
    }

    @Test
    fun testGeminiAgentIntelligence_gracefullyFallsBackWhenNoKeyConfigured() = runBlocking {
        // Given an intelligence instance with no API key and proxy disabled
        secureKeyProvider.clearStoredKey()
        val intelligence = GeminiAgentIntelligence(secureKeyProvider) { "" }

        val decision = intelligence.analyzeAndFormulateReply(
            customerMessage = "Can I book Knotless Braids on Saturday?",
            senderName = "Nandi"
        )

        assertNotNull(decision)
        assertTrue(decision.replyText.contains("Knotless Braids"))
        assertTrue(decision.replyText.contains("Saturday"))
        assertEquals(com.example.service.whatsapp.intelligence.AgentIntent.BOOKING_INQUIRY, decision.intent)
    }
}
