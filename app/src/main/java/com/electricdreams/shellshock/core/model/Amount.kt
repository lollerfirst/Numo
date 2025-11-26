package com.electricdreams.shellshock.core.model

import java.text.NumberFormat
import java.util.Locale

/**
 * Represents a monetary amount with currency.
 * For BTC: [value] is satoshis.
 * For fiat currencies: [value] is minor units (e.g. cents).
 */
data class Amount(
    val value: Long,
    val currency: Currency,
) {
    enum class Currency(val symbol: String) {
        BTC("₿"),
        USD("$"),
        EUR("€"),
        GBP("£"),
        JPY("¥");

        companion object {
            @JvmStatic
            fun fromCode(code: String): Currency = when {
                code.equals("SAT", ignoreCase = true) ||
                    code.equals("SATS", ignoreCase = true) -> BTC
                else -> runCatching { valueOf(code.uppercase(Locale.US)) }
                    .getOrElse { USD }
            }

            /** Find currency by its symbol (e.g., "$" -> USD) */
            @JvmStatic
            fun fromSymbol(symbol: String): Currency? = entries.find { it.symbol == symbol }
        }
    }

    override fun toString(): String {
        return when (currency) {
            Currency.BTC -> {
                val formatter = NumberFormat.getNumberInstance(Locale.US)
                "${currency.symbol}${formatter.format(value)}"
            }
            Currency.JPY -> {
                val major = value / 100.0
                String.format(Locale.US, "%s%.0f", currency.symbol, major)
            }
            else -> {
                val major = value / 100.0
                String.format(Locale.US, "%s%.2f", currency.symbol, major)
            }
        }
    }

    companion object {
        /**
         * Parse a formatted amount string back to an Amount.
         * Handles formats like "$0.25", "€1.50", "₿24", "¥100", etc.
         * Returns null if parsing fails.
         */
        @JvmStatic
        fun parse(formatted: String): Amount? {
            if (formatted.isEmpty()) return null

            // Find the currency by the first character (symbol)
            val symbol = formatted.take(1)
            val currency = Currency.fromSymbol(symbol) ?: return null
            
            // Extract the numeric part (remove symbol and any thousand separators)
            val numericPart = formatted.drop(1).replace(",", "")
            
            return try {
                when (currency) {
                    Currency.BTC -> {
                        // For BTC, value is in satoshis (no decimal conversion needed)
                        val sats = numericPart.toLong()
                        Amount(sats, currency)
                    }
                    Currency.JPY -> {
                        // JPY has no decimal places, but stored as cents internally
                        val yen = numericPart.toDouble()
                        Amount((yen * 100).toLong(), currency)
                    }
                    else -> {
                        // For other fiat, convert decimal to minor units (cents)
                        val majorUnits = numericPart.toDouble()
                        val minorUnits = Math.round(majorUnits * 100)
                        Amount(minorUnits, currency)
                    }
                }
            } catch (e: NumberFormatException) {
                null
            }
        }
    }
}
