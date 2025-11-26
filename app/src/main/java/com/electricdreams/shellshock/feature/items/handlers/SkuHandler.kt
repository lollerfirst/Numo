package com.electricdreams.shellshock.feature.items.handlers

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.electricdreams.shellshock.R
import com.electricdreams.shellshock.core.util.ItemManager
import com.electricdreams.shellshock.feature.items.BarcodeScannerActivity

/**
 * Handles SKU input, validation, and barcode scanning functionality.
 */
class SkuHandler(
    private val activity: AppCompatActivity,
    private val skuInput: EditText,
    private val skuContainer: View,
    private val skuErrorText: TextView,
    private val scanBarcodeButton: ImageButton,
    private val itemManager: ItemManager,
    private val barcodeScanLauncher: ActivityResultLauncher<Intent>
) {
    private var editItemId: String? = null
    private var isSkuValid: Boolean = true

    /**
     * Initializes SKU input validation and barcode button.
     */
    fun initialize() {
        setupSkuValidation()
        scanBarcodeButton.setOnClickListener { launchBarcodeScanner() }
    }

    /**
     * Sets the item ID for edit mode (used for duplicate validation).
     */
    fun setEditItemId(itemId: String?) {
        editItemId = itemId
    }

    /**
     * Returns whether the current SKU is valid.
     */
    fun isValid(): Boolean = isSkuValid

    /**
     * Gets the current SKU value.
     */
    fun getSku(): String = skuInput.text.toString().trim()

    /**
     * Sets the SKU value (used when loading existing item data).
     */
    fun setSku(sku: String?) {
        skuInput.setText(sku ?: "")
    }

    /**
     * Gets the SKU EditText for focus handling.
     */
    fun getSkuInput(): EditText = skuInput

    /**
     * Handles barcode scan result.
     */
    fun handleBarcodeScanResult(barcodeValue: String?) {
        if (!barcodeValue.isNullOrEmpty()) {
            // Check if SKU already exists before setting
            if (itemManager.isSkuDuplicate(barcodeValue, editItemId)) {
                // Show error but don't enter the SKU
                Toast.makeText(activity, "This barcode is already used by another item", Toast.LENGTH_LONG).show()
            } else {
                skuInput.setText(barcodeValue)
                Toast.makeText(activity, "Barcode scanned successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSkuValidation() {
        skuInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val sku = s.toString().trim()
                validateSku(sku)
            }
        })
    }

    private fun validateSku(sku: String) {
        if (sku.isEmpty()) {
            // Empty SKU is valid (optional field)
            setSkuError(false)
            return
        }

        val isDuplicate = itemManager.isSkuDuplicate(sku, editItemId)
        setSkuError(isDuplicate)
    }

    private fun setSkuError(hasError: Boolean) {
        isSkuValid = !hasError

        if (hasError) {
            skuContainer.setBackgroundResource(R.drawable.bg_input_field_error)
            skuErrorText.visibility = View.VISIBLE
        } else {
            skuContainer.setBackgroundResource(0) // Remove background (handled by card)
            skuErrorText.visibility = View.GONE
        }
    }

    private fun launchBarcodeScanner() {
        val intent = Intent(activity, BarcodeScannerActivity::class.java)
        barcodeScanLauncher.launch(intent)
    }
}
