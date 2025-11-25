package com.electricdreams.shellshock.core.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.net.URI
import java.util.Locale

/**
 * Manages allowed mints for Cashu tokens.
 */
class MintManager private constructor(context: Context) {

    interface MintChangeListener {
        fun onMintsChanged(newMints: List<String>)
    }

    companion object {
        private const val TAG = "MintManager"
        private const val PREFS_NAME = "MintPreferences"
        private const val KEY_MINTS = "allowedMints"
        private const val KEY_PREFERRED_LIGHTNING_MINT = "preferredLightningMint"

        // Default mints
        private val DEFAULT_MINTS: Set<String> = setOf(
            "https://mint.minibits.cash/Bitcoin",
            "https://mint.chorus.community",
            "https://mint.cubabitcoin.org",
            "https://mint.coinos.io",
        )
        
        // Default Lightning mint (first of the default mints)
        private const val DEFAULT_LIGHTNING_MINT = "https://mint.minibits.cash/Bitcoin"

        @Volatile
        private var instance: MintManager? = null

        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): MintManager {
            if (instance == null) {
                instance = MintManager(context.applicationContext)
            }
            return instance as MintManager
        }
    }

    private val context: Context = context.applicationContext
    private val preferences: SharedPreferences =
        this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var allowedMints: MutableSet<String> =
        HashSet(preferences.getStringSet(KEY_MINTS, DEFAULT_MINTS) ?: DEFAULT_MINTS)

    private var preferredLightningMint: String? =
        preferences.getString(KEY_PREFERRED_LIGHTNING_MINT, null)

    private var listener: MintChangeListener? = null

    init {
        Log.d(TAG, "Initialized with ${allowedMints.size} allowed mints")
        // Ensure preferred Lightning mint is valid (exists in allowed mints)
        val preferred = preferredLightningMint
        if (preferred == null || !allowedMints.contains(preferred)) {
            // Set to first allowed mint or default
            preferredLightningMint = allowedMints.firstOrNull() ?: DEFAULT_LIGHTNING_MINT
            savePreferredLightningMint()
        }
    }

    /** Set a listener to be notified when allowed mints change. */
    fun setMintChangeListener(listener: MintChangeListener?) {
        this.listener = listener
    }

    /** Get the list of allowed mints. */
    fun getAllowedMints(): List<String> = ArrayList(allowedMints)

    /**
     * Get the preferred mint for Lightning payments.
     * Falls back to the first allowed mint if not set.
     */
    fun getPreferredLightningMint(): String? {
        val preferred = preferredLightningMint
        // Return preferred if it's still in the allowed list
        if (preferred != null && allowedMints.contains(preferred)) {
            return preferred
        }
        // Otherwise return the first allowed mint
        return allowedMints.firstOrNull()
    }

    /**
     * Set the preferred mint for Lightning payments.
     * @param mintUrl The mint URL to set as preferred. Must be in the allowed list.
     * @return true if the preference was set, false if the mint is not in the allowed list.
     */
    fun setPreferredLightningMint(mintUrl: String?): Boolean {
        var url = mintUrl?.trim()
        if (url.isNullOrEmpty()) {
            Log.e(TAG, "Cannot set empty mint URL as preferred Lightning mint")
            return false
        }

        url = normalizeMintUrl(url)
        if (!allowedMints.contains(url)) {
            Log.e(TAG, "Cannot set preferred Lightning mint: $url is not in the allowed list")
            return false
        }

        preferredLightningMint = url
        savePreferredLightningMint()
        Log.d(TAG, "Set preferred Lightning mint to: $url")
        return true
    }

    /** Save preferred Lightning mint to preferences. */
    private fun savePreferredLightningMint() {
        preferences.edit().putString(KEY_PREFERRED_LIGHTNING_MINT, preferredLightningMint).apply()
    }

    /**
     * Add a mint to the allowed list.
     * @param mintUrl The mint URL to add.
     * @return true if the mint was added, false if it was already in the list.
     */
    fun addMint(mintUrl: String?): Boolean {
        var url = mintUrl?.trim()
        if (url.isNullOrEmpty()) {
            Log.e(TAG, "Cannot add empty mint URL")
            return false
        }

        url = normalizeMintUrl(url)
        val changed = allowedMints.add(url)

        if (changed) {
            saveChanges()
            Log.d(TAG, "Added mint to allowed list: $url")
            listener?.onMintsChanged(getAllowedMints())
        }

        return changed
    }

    /**
     * Remove a mint from the allowed list.
     * @param mintUrl The mint URL to remove.
     * @return true if the mint was removed, false if it wasn't in the list.
     */
    fun removeMint(mintUrl: String?): Boolean {
        var url = mintUrl?.trim()
        if (url.isNullOrEmpty()) {
            Log.e(TAG, "Cannot remove empty mint URL")
            return false
        }

        url = normalizeMintUrl(url)
        val changed = allowedMints.remove(url)

        if (changed) {
            // If the removed mint was the preferred Lightning mint, reset to first available
            if (preferredLightningMint == url) {
                preferredLightningMint = allowedMints.firstOrNull()
                savePreferredLightningMint()
                Log.d(TAG, "Preferred Lightning mint was removed, now set to: $preferredLightningMint")
            }
            saveChanges()
            Log.d(TAG, "Removed mint from allowed list: $url")
            listener?.onMintsChanged(getAllowedMints())
        }

        return changed
    }

    /** Reset allowed mints to the default list. */
    fun resetToDefaults() {
        allowedMints = HashSet(DEFAULT_MINTS)
        preferredLightningMint = DEFAULT_LIGHTNING_MINT
        saveChanges()
        savePreferredLightningMint()
        Log.d(TAG, "Reset mints to default list, preferred Lightning mint: $preferredLightningMint")
        listener?.onMintsChanged(getAllowedMints())
    }

    /**
     * Check if a mint is allowed.
     * @param mintUrl The mint URL to check.
     * @return true if the mint is in the allowed list, false otherwise.
     */
    fun isMintAllowed(mintUrl: String?): Boolean {
        var url = mintUrl?.trim()
        if (url.isNullOrEmpty()) return false

        url = normalizeMintUrl(url)
        val allowed = allowedMints.contains(url)
        if (!allowed) {
            Log.d(TAG, "Mint not allowed: $url")
        }
        return allowed
    }

    /** Save current mints to preferences. */
    private fun saveChanges() {
        preferences.edit().putStringSet(KEY_MINTS, allowedMints).apply()
    }

    /**
     * Normalize mint URL to ensure consistent format:
     * - Trim whitespace
     * - Ensure https:// when protocol missing
     * - Lowercase host while leaving path/query untouched
     * - Remove trailing slash for stable comparisons
     */
    private fun normalizeMintUrl(url: String): String {
        var normalized = url.trim()
        if (!normalized.contains("://")) {
            normalized = "https://$normalized"
        }

        val sanitized = try {
            val uri = URI(normalized)
            val scheme = uri.scheme ?: "https"
            val host = uri.host ?: return normalized.removeSuffix("/")
            val userInfo = uri.userInfo?.let { "$it@" } ?: ""
            val portSegment = if (uri.port != -1) ":${uri.port}" else ""
            val path = uri.rawPath ?: ""
            val query = uri.rawQuery?.let { "?$it" } ?: ""
            val fragment = uri.rawFragment?.let { "#$it" } ?: ""

            buildString {
                append(scheme)
                append("://")
                append(userInfo)
                append(host.lowercase(Locale.ROOT))
                append(portSegment)
                append(path)
                append(query)
                append(fragment)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fully normalize mint URL: $url", e)
            normalized
        }

        return sanitized.removeSuffix("/")
    }
}
