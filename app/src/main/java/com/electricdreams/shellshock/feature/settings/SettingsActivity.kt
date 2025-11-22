package com.electricdreams.shellshock.feature.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.mutableStateOf
import com.electricdreams.shellshock.core.util.CurrencyManager
import com.electricdreams.shellshock.core.util.ItemManager
import com.electricdreams.shellshock.core.util.MintManager
import com.electricdreams.shellshock.feature.items.ItemListActivity
import com.electricdreams.shellshock.ui.screens.SettingsScreen
import com.electricdreams.shellshock.ui.theme.CashAppTheme
import java.io.File
import java.io.FileOutputStream

class SettingsActivity : AppCompatActivity() {

    private lateinit var currencyManager: CurrencyManager
    private lateinit var mintManager: MintManager
    private lateinit var itemManager: ItemManager

    private val currentCurrencyState = mutableStateOf("USD")
    private val mintsState = mutableStateOf<List<String>>(emptyList())
    private val itemsCountState = mutableStateOf(0)
    private var isNightMode = false

    private val csvPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            importCsvFile(uri)
        }
    }

    private val itemListLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            updateItemsStatus()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Load theme preference (assuming shared prefs logic is consistent)
        val prefs = getSharedPreferences("PaymentHistory", MODE_PRIVATE) // Using same prefs file for simplicity
        isNightMode = prefs.getBoolean("night_mode", false)
        AppCompatDelegate.setDefaultNightMode(if (isNightMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)

        currencyManager = CurrencyManager.getInstance(this)
        mintManager = MintManager.getInstance(this)
        itemManager = ItemManager.getInstance(this)

        // Initial state
        currentCurrencyState.value = currencyManager.currentCurrency
        mintsState.value = mintManager.allowedMints
        updateItemsStatus()

        setContent {
            CashAppTheme(darkTheme = isNightMode) {
                SettingsScreen(
                    currentCurrency = currentCurrencyState.value,
                    onCurrencySelected = { currency ->
                        currentCurrencyState.value = currency
                        currencyManager.setPreferredCurrency(currency)
                    },
                    mints = mintsState.value,
                    onAddMint = { url -> addNewMint(url) },
                    onRemoveMint = { url -> removeMint(url) },
                    onResetMints = { resetMints() },
                    itemsCount = itemsCountState.value,
                    onImportItems = { csvPickerLauncher.launch("text/csv") },
                    onManageItems = {
                        val intent = Intent(this, ItemListActivity::class.java)
                        itemListLauncher.launch(intent)
                    },
                    onClearItems = { showClearItemsConfirmation() },
                    onBackClick = { finish() },
                    onSaveClick = {
                        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateItemsStatus()
    }

    private fun addNewMint(url: String) {
        if (mintManager.addMint(url)) {
            mintsState.value = mintManager.allowedMints
            Toast.makeText(this, "Mint added", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Mint already exists", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeMint(url: String) {
        if (mintManager.removeMint(url)) {
            mintsState.value = mintManager.allowedMints
            Toast.makeText(this, "Mint removed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetMints() {
        mintManager.resetToDefaults()
        mintsState.value = mintManager.allowedMints
        Toast.makeText(this, "Mints reset to defaults", Toast.LENGTH_SHORT).show()
    }

    private fun updateItemsStatus() {
        itemsCountState.value = itemManager.allItems.size
    }

    private fun showClearItemsConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Clear All Items")
            .setMessage("Are you sure you want to delete ALL items from your catalog? This cannot be undone.")
            .setPositiveButton("Delete All Items") { _, _ ->
                itemManager.clearItems()
                updateItemsStatus()
                Toast.makeText(this, "All items cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun importCsvFile(uri: Uri) {
        try {
            val tempFile = File(cacheDir, "import_catalog.csv")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            val importedCount = itemManager.importItemsFromCsv(tempFile.absolutePath, true)
            if (importedCount > 0) {
                Toast.makeText(this, "Imported $importedCount items", Toast.LENGTH_SHORT).show()
                updateItemsStatus()
            } else {
                Toast.makeText(this, "No items imported", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error importing CSV: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
