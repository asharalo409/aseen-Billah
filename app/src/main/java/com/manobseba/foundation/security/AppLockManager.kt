package com.manobseba.foundation.security

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

class AppLockManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("manobseba_applock", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ENABLED = "lock_enabled"
        private const val KEY_HASH = "pin_hash"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_BIOMETRIC = "biometric_enabled"
    }

    fun isLockEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false) && hasPin()

    fun hasPin(): Boolean = !prefs.getString(KEY_HASH, null).isNullOrBlank()

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC, false)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val saltB64 = Base64.getEncoder().encodeToString(salt)
        val hash = hashPin(pin, saltB64)
        prefs.edit()
            .putString(KEY_SALT, saltB64)
            .putString(KEY_HASH, hash)
            .putBoolean(KEY_ENABLED, true)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_SALT, null) ?: return false
        val stored = prefs.getString(KEY_HASH, null) ?: return false
        return hashPin(pin, salt) == stored
    }

    fun disableLock() {
        prefs.edit()
            .putBoolean(KEY_ENABLED, false)
            .putBoolean(KEY_BIOMETRIC, false)
            .remove(KEY_HASH)
            .remove(KEY_SALT)
            .apply()
    }

    private fun hashPin(pin: String, saltB64: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "$saltB64:$pin".toByteArray(Charsets.UTF_8)
        return Base64.getEncoder().encodeToString(digest.digest(input))
    }
}
