package com.electricdreams.shellshock.feature.items

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.electricdreams.shellshock.ModernPOSActivity
import com.electricdreams.shellshock.core.model.Item
import com.electricdreams.shellshock.core.util.BasketManager
import com.electricdreams.shellshock.core.util.CurrencyManager
import com.electricdreams.shellshock.core.util.ItemManager
import com.electricdreams.shellshock.core.worker.BitcoinPriceWorker
import com.electricdreams.shellshock.ui.screens.CatalogScreen
import com.electricdreams.shellshock.ui.theme.CashAppTheme

class ItemSelectionActivity : AppCompatActivity() {

    private lateinit var itemManager: ItemManager
    private lateinit var basketManager: BasketManager
    private lateinit var bitcoinPriceWorker: BitcoinPriceWorker
    private lateinit var currencyManager: CurrencyManager

    private val itemsState = mutableStateOf<List<Item>>(emptyList())
    private val basketQuantitiesState = mutableStateMapOf<String, Int>()
    private val basketTotalItemsState = mutableStateOf(0)
    private val basketTotalPriceState = mutableStateOf("0.00")
    private var isNightMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Load theme preference
        val prefs = getSharedPreferences("PaymentHistory", MODE_PRIVATE)
        isNightMode = prefs.getBoolean("night_mode", false)
        AppCompatDelegate.setDefaultNightMode(if (isNightMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)

        itemManager = ItemManager.getInstance(this)
        basketManager = BasketManager.getInstance()
        bitcoinPriceWorker = BitcoinPriceWorker.getInstance(this)
        currencyManager = CurrencyManager.getInstance(this)

        // Initialize state
        itemsState.value = itemManager.allItems
        updateBasketState()

        setContent {
            CashAppTheme(darkTheme = isNightMode) {
                CatalogScreen(
                    items = itemsState.value,
                    basketQuantities = basketQuantitiesState,
                    basketTotalItems = basketTotalItemsState.value,
                    basketTotalPrice = basketTotalPriceState.value,
                    onItemAdd = { item -> addItemToBasket(item) },
                    onItemRemove = { item -> removeItemFromBasket(item) },
                    onViewBasket = {
                        startActivity(Intent(this, BasketActivity::class.java))
                    },
                    onCheckout = { proceedToCheckout() },
                    onBackClick = { finish() }
                )
            }
        }

        bitcoinPriceWorker.start()
    }

    override fun onResume() {
        super.onResume()
        updateBasketState()
        // Refresh items in case they changed
        itemsState.value = itemManager.allItems
    }

    private fun updateBasketState() {
        basketQuantitiesState.clear()
        basketManager.basketItems.forEach { basketItem ->
            basketQuantitiesState[basketItem.item.id] = basketItem.quantity
        }
        basketTotalItemsState.value = basketManager.totalItemCount
        
        val totalPrice = basketManager.totalPrice
        val symbol = currencyManager.currentSymbol
        basketTotalPriceState.value = String.format("%s%.2f", symbol, totalPrice)
    }

    private fun addItemToBasket(item: Item) {
        val currentQty = basketQuantitiesState[item.id] ?: 0
        // Check stock if needed (logic from adapter)
        if (item.quantity > currentQty || item.quantity == 0) {
            basketManager.addItem(item, 1)
            updateBasketState()
        } else {
            Toast.makeText(this, "No more stock available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeItemFromBasket(item: Item) {
        val currentQty = basketQuantitiesState[item.id] ?: 0
        if (currentQty > 0) {
            if (currentQty == 1) {
                basketManager.removeItem(item.id)
            } else {
                basketManager.updateItemQuantity(item.id, currentQty - 1)
            }
            updateBasketState()
        }
    }

    private fun proceedToCheckout() {
        if (basketManager.totalItemCount == 0) {
            Toast.makeText(this, "Your basket is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val satoshisAmount = basketManager.getTotalSatoshis(bitcoinPriceWorker.currentPrice)
        
        val intent = Intent(this, ModernPOSActivity::class.java)
        intent.putExtra("EXTRA_PAYMENT_AMOUNT", satoshisAmount)
        
        basketManager.clearBasket()
        startActivity(intent)
    }
}
