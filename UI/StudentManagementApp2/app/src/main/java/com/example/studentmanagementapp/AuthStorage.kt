package com.example.studentmanagementapp

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object AuthStorage {
    private const val PREFS = "secure_auth"
    private const val SESSION = "session"
    private const val KEY_ALIAS = "student_management_auth_key"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val combined = cipher.iv + cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String? {
        return try {
            val combined = Base64.decode(value, Base64.NO_WRAP)
            if (combined.size <= 12) return null
            val iv = combined.copyOfRange(0, 12)
            val encrypted = combined.copyOfRange(12, combined.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, iv)
            )
            String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun saveSession(
        context: Context,
        accessToken: String,
        expiresInSeconds: Int,
        username: String,
        role: String
    ) {
        val expiresAt = System.currentTimeMillis() + expiresInSeconds * 1000L
        val session = JSONObject().apply {
            put("access_token", accessToken)
            put("expires_at", expiresAt)
            put("username", username)
            put("role", role)
        }.toString()

        prefs(context).edit()
            .putString(SESSION, encrypt(session))
            .apply()
    }

    private fun readSession(context: Context): JSONObject? {
        val encrypted = prefs(context).getString(SESSION, null) ?: return null
        return decrypt(encrypted)?.let {
            runCatching { JSONObject(it) }.getOrNull()
        }
    }

    fun getToken(context: Context): String? {
        val session = readSession(context) ?: return null
        val expiresAt = session.optLong("expires_at", 0L)
        if (expiresAt <= System.currentTimeMillis()) {
            clear(context)
            return null
        }
        return session.optString("access_token").takeIf { it.isNotBlank() }
    }

    fun getUsername(context: Context): String =
        readSession(context)?.optString("username", "").orEmpty()

    fun getRole(context: Context): String =
        readSession(context)?.optString("role", "").orEmpty()

    fun hasValidSession(context: Context): Boolean = getToken(context) != null

    fun clear(context: Context) {
        prefs(context).edit().remove(SESSION).apply()
    }
}
