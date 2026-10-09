package com.example.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages secure storage of sensitive credentials using the Android Keystore system.
 * The cryptographic master key is generated and stored inside the Android hardware-backed
 * KeyStore, ensuring it cannot be extracted from app storage or decompiled APKs.
 * Data is encrypted using AES-256 in Galois/Counter Mode (GCM) with random IVs.
 */
class EncryptedKeyStoreManager(private val context: Context) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "zama_gemini_api_key_master"
        private const val PREFS_NAME = "zama_secure_credentials_vault"
        private const val KEY_CIPHERTEXT = "encrypted_gemini_key"
        private const val KEY_IV = "gemini_key_iv"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }

    private val isAndroidKeyStoreAvailable: Boolean = try {
        java.security.Security.getProvider(ANDROID_KEYSTORE) != null
    } catch (_: Exception) {
        false
    }

    private val keyStore: KeyStore? = if (isAndroidKeyStoreAvailable) {
        try {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        } catch (_: Exception) {
            null
        }
    } else {
        null
    }

    @Volatile
    private var jvmFallbackSecretKey: SecretKey? = null

    @Synchronized
    private fun getOrCreateMasterKey(): SecretKey {
        if (keyStore != null) {
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build()

                keyGenerator.init(spec)
                return keyGenerator.generateKey()
            }

            val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        } else {
            // Environment fallback for JVM testing/Robolectric where AndroidKeyStore provider is omitted
            return jvmFallbackSecretKey ?: synchronized(this) {
                jvmFallbackSecretKey ?: KeyGenerator.getInstance("AES").apply {
                    init(256)
                }.generateKey().also { jvmFallbackSecretKey = it }
            }
        }
    }

    /**
     * Encrypts the provided API key with the hardware-backed master key
     * and persists the ciphertext along with the initialization vector (IV).
     */
    fun storeApiKey(apiKey: String) {
        val masterKey = getOrCreateMasterKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CIPHERTEXT, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()
    }

    /**
     * Decrypts and retrieves the API key in-memory using the hardware-backed KeyStore.
     * Returns null if no key is stored or decryption fails.
     */
    fun retrieveApiKey(): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ciphertextBase64 = prefs.getString(KEY_CIPHERTEXT, null) ?: return null
        val ivBase64 = prefs.getString(KEY_IV, null) ?: return null

        return try {
            val ciphertext = Base64.decode(ciphertextBase64, Base64.NO_WRAP)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)

            val masterKey = getOrCreateMasterKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)

            val decryptedBytes = cipher.doFinal(ciphertext)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Checks whether an encrypted key exists in local secure storage.
     */
    fun hasStoredKey(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_CIPHERTEXT) && prefs.contains(KEY_IV)
    }

    /**
     * Clears stored ciphertext and IV from secure preferences.
     */
    fun clearApiKey() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_CIPHERTEXT)
            .remove(KEY_IV)
            .commit()
    }
}
