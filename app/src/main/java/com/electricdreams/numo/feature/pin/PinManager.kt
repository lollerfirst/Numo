package com.electricdreams.numo.feature.pin

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages PIN storage, validation, and lockout logic.
 * 
 * Security features:
 * - PIN is encrypted using Android Keystore
 * - Maximum 5 failed attempts before 1-hour lockout
 * - Minimum 3-second delay between attempts
 * - PIN reset requires mnemonic verification
 */
class PinManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "pin_prefs"
        private const val KEY_ENCRYPTED_PIN = "encrypted_pin"
        private const val KEY_PIN_IV = "pin_iv"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"
        private const val KEY_LAST_ATTEMPT_TIME = "last_attempt_time"
        private const val KEY_PIN_ENABLED = "pin_enabled"

        private const val KEYSTORE_ALIAS = "numo_pin_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"

        const val MIN_PIN_LENGTH = 4
        const val MAX_PIN_LENGTH = 16
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 60 * 60 * 1000L // 1 hour
        const val MIN_ATTEMPT_INTERVAL_MS = 3000L // 3 seconds

        @Volatile
        private var instance: PinManager? = null

        fun getInstance(context: Context): PinManager {
            return instance ?: synchronized(this) {
                instance ?: PinManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Check if PIN protection is enabled.
     */
    fun isPinEnabled(): Boolean {
        return prefs.getBoolean(KEY_PIN_ENABLED, false) && 
               prefs.getString(KEY_ENCRYPTED_PIN, null) != null
    }

    /**
     * Set a new PIN.
     * @param pin The PIN to set (4-16 digits)
     * @return true if PIN was set successfully
     */
    fun setPin(pin: String): Boolean {
        if (!isValidPin(pin)) return false

        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val encryptedBytes = cipher.doFinal(pin.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv

            prefs.edit()
                .putString(KEY_ENCRYPTED_PIN, Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
                .putString(KEY_PIN_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .putBoolean(KEY_PIN_ENABLED, true)
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, 0)
                .apply()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Validate a PIN attempt.
     * @return ValidationResult with success status and any error message
     */
    fun validatePin(enteredPin: String): ValidationResult {
        // Check lockout first
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0)
        if (System.currentTimeMillis() < lockoutUntil) {
            val remainingMs = lockoutUntil - System.currentTimeMillis()
            val remainingMinutes = (remainingMs / 60000).toInt() + 1
            return ValidationResult(
                success = false,
                error = "Too many attempts. Try again in $remainingMinutes minute${if (remainingMinutes > 1) "s" else ""}.",
                isLockedOut = true,
                remainingLockoutMs = remainingMs
            )
        }

        // Check minimum interval between attempts
        val lastAttemptTime = prefs.getLong(KEY_LAST_ATTEMPT_TIME, 0)
        val timeSinceLastAttempt = System.currentTimeMillis() - lastAttemptTime
        if (timeSinceLastAttempt < MIN_ATTEMPT_INTERVAL_MS && lastAttemptTime > 0) {
            return ValidationResult(
                success = false,
                error = "Please wait a moment before trying again.",
                needsDelay = true,
                remainingDelayMs = MIN_ATTEMPT_INTERVAL_MS - timeSinceLastAttempt
            )
        }

        // Update last attempt time
        prefs.edit().putLong(KEY_LAST_ATTEMPT_TIME, System.currentTimeMillis()).apply()

        // Decrypt and compare PIN
        val storedPin = decryptPin()
        if (storedPin == null) {
            return ValidationResult(success = false, error = "PIN not configured properly.")
        }

        return if (enteredPin == storedPin) {
            // Success - reset failed attempts
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, 0)
                .apply()
            ValidationResult(success = true)
        } else {
            // Failed attempt
            val failedAttempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            val attemptsRemaining = MAX_FAILED_ATTEMPTS - failedAttempts

            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                // Lock out for 1 hour
                prefs.edit()
                    .putInt(KEY_FAILED_ATTEMPTS, failedAttempts)
                    .putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + LOCKOUT_DURATION_MS)
                    .apply()
                ValidationResult(
                    success = false,
                    error = "Too many failed attempts. Locked for 1 hour.",
                    isLockedOut = true,
                    remainingLockoutMs = LOCKOUT_DURATION_MS
                )
            } else {
                prefs.edit().putInt(KEY_FAILED_ATTEMPTS, failedAttempts).apply()
                ValidationResult(
                    success = false,
                    error = "Incorrect PIN. $attemptsRemaining attempt${if (attemptsRemaining > 1) "s" else ""} remaining.",
                    attemptsRemaining = attemptsRemaining
                )
            }
        }
    }

    /**
     * Remove the PIN (disable PIN protection).
     */
    fun removePin() {
        prefs.edit()
            .remove(KEY_ENCRYPTED_PIN)
            .remove(KEY_PIN_IV)
            .putBoolean(KEY_PIN_ENABLED, false)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0)
            .apply()

        // Remove key from keystore
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            keyStore.deleteEntry(KEYSTORE_ALIAS)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Reset failed attempts (used after successful mnemonic verification).
     */
    fun resetLockout() {
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0)
            .putLong(KEY_LAST_ATTEMPT_TIME, 0)
            .apply()
    }

    /**
     * Get remaining lockout time in milliseconds.
     */
    fun getRemainingLockoutMs(): Long {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0)
        val remaining = lockoutUntil - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0
    }

    /**
     * Check if currently locked out.
     */
    fun isLockedOut(): Boolean {
        return getRemainingLockoutMs() > 0
    }

    /**
     * Get number of failed attempts.
     */
    fun getFailedAttempts(): Int {
        return prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
    }

    /**
     * Check if a PIN string is valid format.
     */
    fun isValidPin(pin: String): Boolean {
        return pin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH && pin.all { it.isDigit() }
    }

    private fun decryptPin(): String? {
        return try {
            val encryptedBase64 = prefs.getString(KEY_ENCRYPTED_PIN, null) ?: return null
            val ivBase64 = prefs.getString(KEY_PIN_IV, null) ?: return null

            val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)

            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            val secretKey = keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey ?: return null

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))

            String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        keyStore.getKey(KEYSTORE_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    data class ValidationResult(
        val success: Boolean,
        val error: String? = null,
        val isLockedOut: Boolean = false,
        val remainingLockoutMs: Long = 0,
        val needsDelay: Boolean = false,
        val remainingDelayMs: Long = 0,
        val attemptsRemaining: Int = MAX_FAILED_ATTEMPTS
    )
}
