package com.example.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.BuildConfig
import com.example.model.LocalPreferences
import kotlinx.serialization.json.Json
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the API Bearer token encrypted via Android Keystore (AES-256-GCM)
 * alongside non-sensitive client preferences (guest cart session id, theme,
 * pending payment order id for Razorpay recovery, and configurable API base URL).
 */
class SecureSessionStore(context: Context) {

    private val securePrefs = context.getSharedPreferences(PREFS_SECURE, Context.MODE_PRIVATE)
    private val appPrefs = context.getSharedPreferences(PREFS_APP, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun getOrCreateGuestId(): String {
        val existing = appPrefs.getString(KEY_GUEST_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        val created = "mlv_" + bytes.joinToString("") { "%02x".format(it) }
        appPrefs.edit().putString(KEY_GUEST_ID, created).apply()
        return created
    }

    fun getApiBaseUrl(): String {
        val saved = appPrefs.getString(KEY_API_BASE_URL, null)?.trim()
        return if (!saved.isNullOrBlank() && saved.startsWith("http")) {
            saved.trimEnd('/') + "/"
        } else {
            BuildConfig.MELVORE_API_BASE.trimEnd('/') + "/"
        }
    }

    fun setApiBaseUrl(url: String): String {
        val normalized = url.trim().trimEnd('/')
        val valid = if (normalized.startsWith("https://") || normalized.startsWith("http://")) {
            normalized
        } else {
            BuildConfig.MELVORE_API_BASE.trimEnd('/')
        }
        appPrefs.edit().putString(KEY_API_BASE_URL, valid).apply()
        return valid
    }

    fun getPreferences(): LocalPreferences {
        val raw = appPrefs.getString(KEY_UI_PREFS, null) ?: return LocalPreferences()
        return runCatching { json.decodeFromString<LocalPreferences>(raw) }
            .getOrElse { LocalPreferences() }
    }

    fun savePreferences(prefs: LocalPreferences) {
        appPrefs.edit()
            .putString(KEY_UI_PREFS, json.encodeToString(LocalPreferences.serializer(), prefs))
            .apply()
    }

    fun getPendingPaymentOrderId(): Int? {
        val id = appPrefs.getInt(KEY_PENDING_ORDER_ID, -1)
        return if (id > 0) id else null
    }

    fun setPendingPaymentOrderId(orderId: Int?) {
        if (orderId == null || orderId <= 0) {
            appPrefs.edit().remove(KEY_PENDING_ORDER_ID).apply()
        } else {
            appPrefs.edit().putInt(KEY_PENDING_ORDER_ID, orderId).apply()
        }
    }

    fun saveAuthToken(token: String) {
        if (token.isBlank()) {
            clearAuthToken()
            return
        }
        val encrypted = encryptWithKeystore(token)
        if (encrypted != null) {
            securePrefs.edit()
                .putString(KEY_TOKEN_CIPHER, encrypted.first)
                .putString(KEY_TOKEN_IV, encrypted.second)
                .remove(KEY_TOKEN_FALLBACK)
                .apply()
        } else {
            // Only reached when AndroidKeyStore provider is absent (e.g., Robolectric JVM unit tests)
            val encoded = Base64.encodeToString(token.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            securePrefs.edit()
                .putString(KEY_TOKEN_FALLBACK, encoded)
                .remove(KEY_TOKEN_CIPHER)
                .remove(KEY_TOKEN_IV)
                .apply()
        }
    }

    fun getAuthToken(): String? {
        val cipherB64 = securePrefs.getString(KEY_TOKEN_CIPHER, null)
        val ivB64 = securePrefs.getString(KEY_TOKEN_IV, null)
        if (!cipherB64.isNullOrBlank() && !ivB64.isNullOrBlank()) {
            return decryptWithKeystore(cipherB64, ivB64)
        }
        val fallback = securePrefs.getString(KEY_TOKEN_FALLBACK, null) ?: return null
        return runCatching {
            String(Base64.decode(fallback, Base64.NO_WRAP), Charsets.UTF_8)
        }.getOrNull()
    }

    fun clearAuthToken() {
        securePrefs.edit()
            .remove(KEY_TOKEN_CIPHER)
            .remove(KEY_TOKEN_IV)
            .remove(KEY_TOKEN_FALLBACK)
            .apply()
    }

    private fun getOrCreateKeystoreSecretKey(): SecretKey? {
        return runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            if (existing != null) return@runCatching existing

            val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGen.init(spec)
            keyGen.generateKey()
        }.getOrNull()
    }

    private fun encryptWithKeystore(plaintext: String): Pair<String, String>? {
        val key = getOrCreateKeystoreSecretKey() ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORM)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val ivBytes = cipher.iv
            Base64.encodeToString(cipherBytes, Base64.NO_WRAP) to
                Base64.encodeToString(ivBytes, Base64.NO_WRAP)
        }.getOrNull()
    }

    private fun decryptWithKeystore(cipherB64: String, ivB64: String): String? {
        val key = getOrCreateKeystoreSecretKey() ?: return null
        return runCatching {
            val cipherBytes = Base64.decode(cipherB64, Base64.NO_WRAP)
            val ivBytes = Base64.decode(ivB64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORM)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, ivBytes))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        }.getOrNull()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "melvore_auth_token_aes_key"
        private const val AES_GCM_TRANSFORM = "AES/GCM/NoPadding"

        private const val PREFS_SECURE = "melvore_secure_session"
        private const val PREFS_APP = "melvore_app_prefs"
        private const val KEY_TOKEN_CIPHER = "token_cipher"
        private const val KEY_TOKEN_IV = "token_iv"
        private const val KEY_TOKEN_FALLBACK = "token_fallback"
        private const val KEY_GUEST_ID = "guest_session_id"
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_UI_PREFS = "ui_preferences"
        private const val KEY_PENDING_ORDER_ID = "pending_payment_order_id"
    }
}
