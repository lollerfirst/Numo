package com.electricdreams.shellshock.feature.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.mutableStateOf
import com.electricdreams.shellshock.core.data.model.PaymentHistoryEntry
import com.electricdreams.shellshock.core.data.model.TokenHistoryEntry
import com.electricdreams.shellshock.ui.screens.HistoryScreen
import com.electricdreams.shellshock.ui.theme.CashAppTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class TokenHistoryActivity : AppCompatActivity() {

    private val historyState = mutableStateOf<List<PaymentHistoryEntry>>(emptyList())
    private var isNightMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        isNightMode = prefs.getBoolean("night_mode", false)
        AppCompatDelegate.setDefaultNightMode(if (isNightMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)

        setContent {
            CashAppTheme(darkTheme = isNightMode) {
                HistoryScreen(
                    history = historyState.value,
                    onBackClick = { finish() },
                    onClearHistoryClick = { showClearHistoryConfirmation() },
                    onCopyClick = { token -> copyToClipboard(token) },
                    onOpenClick = { token -> 
                        // Token history might not support opening, or same logic
                        Toast.makeText(this, "Opening tokens not supported yet", Toast.LENGTH_SHORT).show()
                    },
                    onDeleteClick = { entry -> showDeleteConfirmation(entry) }
                )
            }
        }

        loadHistory()
    }

    private fun loadHistory() {
        val tokenHistory = getTokenHistory(this)
        // Map TokenHistoryEntry to PaymentHistoryEntry for UI reuse
        val paymentHistory = tokenHistory.map { 
            PaymentHistoryEntry(it.token, it.amount, it.date) 
        }.toMutableList()
        
        paymentHistory.reverse() // Show newest first
        historyState.value = paymentHistory
    }

    private fun copyToClipboard(token: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Token", token)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Token copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteConfirmation(entry: PaymentHistoryEntry) {
        AlertDialog.Builder(this)
            .setTitle("Delete Token")
            .setMessage("Are you sure you want to delete this token from history?")
            .setPositiveButton("Delete") { _, _ -> deleteToken(entry) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteToken(entryToDelete: PaymentHistoryEntry) {
        val history = getTokenHistory(this)
        val iterator = history.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.token == entryToDelete.token && item.amount == entryToDelete.amount) {
                iterator.remove()
                break
            }
        }
        
        saveHistory(history)
        loadHistory()
    }

    private fun showClearHistoryConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Clear History")
            .setMessage("Are you sure you want to clear all token history? This action cannot be undone.")
            .setPositiveButton("Clear All") { _, _ -> clearAllHistory() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearAllHistory() {
        saveHistory(ArrayList())
        loadHistory()
    }

    private fun saveHistory(history: List<TokenHistoryEntry>) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(KEY_HISTORY, Gson().toJson(history))
        editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "TokenHistory"
        private const val KEY_HISTORY = "history"

        fun getTokenHistory(context: Context): ArrayList<TokenHistoryEntry> {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val json = prefs.getString(KEY_HISTORY, "[]")
            val type = object : TypeToken<ArrayList<TokenHistoryEntry>>() {}.type
            return Gson().fromJson(json, type)
        }

        fun addToHistory(context: Context, token: String, amount: Long) {
            val history = getTokenHistory(context)
            history.add(TokenHistoryEntry(token, amount, Date()))
            
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString(KEY_HISTORY, Gson().toJson(history))
            editor.apply()
        }
    }
}
