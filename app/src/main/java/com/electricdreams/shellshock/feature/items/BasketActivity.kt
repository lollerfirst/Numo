package com.electricdreams.shellshock.feature.items

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.mutableStateOf
import com.electricdreams.shellshock.ModernPOSActivity
import com.electricdreams.shellshock.core.model.BasketItem
import com.electricdreams.shellshock.core.util.BasketManager
import com.electricdreams.shellshock.core.util.CurrencyManager
import com.electricdreams.shellshock.core.worker.BitcoinPriceWorker
import com.electricdreams.shellshock.ui.screens.BasketScreen
import com.electricdreams.shellshock.ui.theme.CashAppTheme

class BasketActivity : AppCompatActivity() {

    private lateinit var basketManager: BasketManager
    private lateinit var bitcoinPriceWorker: BitcoinPriceWorker
    private lateinit var currencyManager: CurrencyManager

    private val basketItemsState = mutableStateOf<List<BasketItem>>(emptyList())
    private val totalPriceState = mutableStateOf("0.00")
    private var isNightMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Load theme preference
        val prefs = getSharedPreferences("PaymentHistory", MODE_PRIVATE)
        isNightMode = prefs.getBoolean("night_mode", false)
        AppCompatDelegate.setDefaultNightMode(if (isNightMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)

        basketManager = BasketManager.getInstance()
        bitcoinPriceWorker = BitcoinPriceWorker.getInstance(this)
        currencyManager = CurrencyManager.getInstance(this)

        updateBasketState()

        setContent {
            CashAppTheme(darkTheme = isNightMode) {
                BasketScreen(
                    basketItems = basketItemsState.value,
                    totalPrice = totalPriceState.value,
                    onRemoveItem = { item -> removeItem(item) },
                    onClearBasket = { showClearBasketConfirmation() },
                    onContinueShopping = { finish() },
                    onCheckout = { proceedToCheckout() },
                    onBackClick = { finish() }
                )
            }
        }

        bitcoinPriceWorker.start()
    }

    private fun updateBasketState() {
        basketItemsState.value = ArrayList(basketManager.basketItems) // Copy list to trigger recomposition
        val totalPrice = basketManager.totalPrice
        val symbol = currencyManager.currentSymbol
        totalPriceState.value = String.format("%s%.2f", symbol, totalPrice)
    }

    private fun removeItem(item: BasketItem) {
        basketManager.removeItem(item.item.id)
        updateBasketState()
    }

    private fun showClearBasketConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Clear Basket")
            .setMessage("Are you sure you want to clear your basket?")
            .setPositiveButton("Clear") { _, _ ->
                basketManager.clearBasket()
                updateBasketState()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun proceedToCheckout() {
        if (basketManager.totalItemCount == 0) {
            Toast.makeText(this, "Your basket is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val satoshisAmount = basketManager.getTotalSatoshis(bitcoinPriceWorker.currentPrice)
        
        val intent = Intent(this, ModernPOSActivity::class.java)
        intent.putExtra("EXTRA_PAYMENT_AMOUNT", satoshisAmount)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        
        basketManager.clearBasket()
        startActivity(intent)
        finish()
    }
}
