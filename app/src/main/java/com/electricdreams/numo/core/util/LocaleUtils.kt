package com.electricdreams.numo.core.util

import android.content.Context
import android.os.Build
import java.util.Locale

/**
 * Locale / language helper utilities.
 *
 * Any unavoidable usage of deprecated configuration APIs for pre-24 devices
 * is contained here, so UI code can remain clean.
 */
object LocaleUtils {

    /**
     * Returns the primary system [Locale] for this device.
     */
    fun getSystemPrimaryLocale(context: Context): Locale {
        val config = context.resources.configuration
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.locales[0]
        } else {
            @Suppress("DEPRECATION")
            config.locale
        }
    }

    /**
     * Returns a 2-letter language code (e.g., "en", "es") for the primary system locale.
     */
    fun getSystemLanguageCode(context: Context): String {
        return getSystemPrimaryLocale(context).language
    }
}
