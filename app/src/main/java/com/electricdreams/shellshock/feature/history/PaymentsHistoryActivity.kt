package com.electricdreams.shellshock.feature.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.mutableStateOf
import com.electricdreams.shellshock.core.data.model.PaymentHistoryEntry
import com.electricdreams.shellshock.ui.screens.HistoryScreen
import com.electricdreams.shellshock.ui.theme.CashAppTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Collections
import java.util.Date

class PaymentsHistoryActivity : AppCompatActivity() {

    private val historyState = mutableStateOf<List<PaymentHistoryEntry>>(emptyList())
    private var isNightMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Load theme preference
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        isNightMode = prefs.getBoolean(KEY_NIGHT_MODE, false)
        AppCompatDelegate.setDefaultNightMode(if (isNightMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)

        setContent {
            CashAppTheme(darkTheme = isNightMode) {
                HistoryScreen(
                    history = historyState.value,
                    onBackClick = { finish() },
                    onClearHistoryClick = { showClearHistoryConfirmation() },
                    onCopyClick = { token -> copyToClipboard(token) },
                    onOpenClick = { token -> openPaymentWithApp(token) },
                    onDeleteClick = { entry -> showDeleteConfirmation(entry) }
                )
            }
        }

        loadHistory()
    }

    private fun loadHistory() {
        val history = getPaymentHistory(this)
        history.reverse() // Show newest first
        historyState.value = history
    }

    private fun copyToClipboard(token: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Payment", token)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Payment copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun openPaymentWithApp(token: String) {
        val cashuUri = "cashu:$token"
        
        // Create intent for viewing the URI
        val uriIntent = Intent(Intent.ACTION_VIEW, Uri.parse(cashuUri))
        uriIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        // Create a fallback intent for sharing as text
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, cashuUri)
        
        // Combine both intents into a chooser
        val chooserIntent = Intent.createChooser(uriIntent, "Open payment with...")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(shareIntent))
        
        try {
            startActivity(chooserIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "No apps available to handle this payment", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmation(entry: PaymentHistoryEntry) {
        AlertDialog.Builder(this)
            .setTitle("Delete Payment")
            .setMessage("Are you sure you want to delete this payment from history?")
            .setPositiveButton("Delete") { _, _ -> deletePayment(entry) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePayment(entryToDelete: PaymentHistoryEntry) {
        val history = getPaymentHistory(this)
        // Find and remove the entry (comparing by token and date ideally, but object reference might fail if reloaded)
        // Let's find by token and amount for now
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
            .setMessage("Are you sure you want to clear all payment history? This action cannot be undone.")
            .setPositiveButton("Clear All") { _, _ -> clearAllHistory() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearAllHistory() {
        saveHistory(ArrayList())
        loadHistory()
    }

    private fun saveHistory(history: List<PaymentHistoryEntry>) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString(KEY_HISTORY, Gson().toJson(history))
        editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "PaymentHistory"
        private const val KEY_HISTORY = "history"
        private const val KEY_NIGHT_MODE = "night_mode"

        fun getPaymentHistory(context: Context): ArrayList<PaymentHistoryEntry> {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val json = prefs.getString(KEY_HISTORY, "[]")
            val type = object : TypeToken<ArrayList<PaymentHistoryEntry>>() {}.type
            return Gson().fromJson(json, type)
        }

        fun addToHistory(context: Context, token: String, amount: Long) {
            val history = getPaymentHistory(context)
            history.add(PaymentHistoryEntry(token, amount, Date()))
            
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putString(KEY_HISTORY, Gson().toJson(history))
            editor.apply()
        }
    }
}
