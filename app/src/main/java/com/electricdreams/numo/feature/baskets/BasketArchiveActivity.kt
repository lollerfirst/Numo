package com.electricdreams.numo.feature.baskets

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.electricdreams.numo.R
import com.electricdreams.numo.core.model.SavedBasket
import com.electricdreams.numo.core.util.CurrencyManager
import com.electricdreams.numo.core.util.SavedBasketManager
import com.electricdreams.numo.feature.history.PaymentsHistoryActivity
import com.electricdreams.numo.feature.history.TransactionDetailActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Activity for viewing archived (paid) baskets.
 * Shows a beautiful list of completed orders with expandable details
 * and links to payment information.
 */
class BasketArchiveActivity : AppCompatActivity() {

    private lateinit var savedBasketManager: SavedBasketManager
    private lateinit var currencyManager: CurrencyManager

    private lateinit var archiveRecyclerView: RecyclerView
    private lateinit var emptyView: LinearLayout
    private lateinit var adapter: BasketArchiveAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basket_archive)

        initializeManagers()
        initializeViews()
        setupRecyclerView()
        loadArchivedBaskets()
    }

    override fun onResume() {
        super.onResume()
        loadArchivedBaskets()
    }

    private fun initializeManagers() {
        savedBasketManager = SavedBasketManager.getInstance(this)
        currencyManager = CurrencyManager.getInstance(this)
    }

    private fun initializeViews() {
        findViewById<View>(R.id.back_button).setOnClickListener { finish() }

        archiveRecyclerView = findViewById(R.id.archive_recycler_view)
        emptyView = findViewById(R.id.empty_view)
    }

    private fun setupRecyclerView() {
        adapter = BasketArchiveAdapter(
            currencyManager = currencyManager,
            onBasketClick = { basket -> showBasketDetails(basket) },
            onPaymentClick = { basket -> openPaymentDetails(basket) },
            onDeleteClick = { basket -> showDeleteConfirmation(basket) }
        )

        archiveRecyclerView.layoutManager = LinearLayoutManager(this)
        archiveRecyclerView.adapter = adapter
    }

    private fun loadArchivedBaskets() {
        val baskets = savedBasketManager.getArchivedBaskets()
        adapter.updateBaskets(baskets)

        if (baskets.isEmpty()) {
            archiveRecyclerView.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            archiveRecyclerView.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }

    private fun showBasketDetails(basket: SavedBasket) {
        // Show expandable details in a bottom sheet or dialog
        val dialog = BasketDetailDialog(this, basket, currencyManager) {
            // On payment click
            openPaymentDetails(basket)
        }
        dialog.show()
    }

    private fun openPaymentDetails(basket: SavedBasket) {
        val paymentId = basket.paymentId ?: return

        // Find payment in history
        val payments = PaymentsHistoryActivity.getPaymentHistory(this)
        val payment = payments.find { it.id == paymentId }

        if (payment != null) {
            val intent = Intent(this, TransactionDetailActivity::class.java).apply {
                putExtra(TransactionDetailActivity.EXTRA_TRANSACTION_TOKEN, payment.token)
                putExtra(TransactionDetailActivity.EXTRA_TRANSACTION_AMOUNT, payment.amount)
                putExtra(TransactionDetailActivity.EXTRA_TRANSACTION_DATE, payment.date.time)
                putExtra(TransactionDetailActivity.EXTRA_TRANSACTION_UNIT, payment.getUnit())
                putExtra(TransactionDetailActivity.EXTRA_TRANSACTION_ENTRY_UNIT, payment.getEntryUnit())
                putExtra(TransactionDetailActivity.EXTRA_TRANSACTION_ENTERED_AMOUNT, payment.enteredAmount)
                payment.bitcoinPrice?.let { 
                    putExtra(TransactionDetailActivity.EXTRA_TRANSACTION_BITCOIN_PRICE, it) 
                }
                putExtra(TransactionDetailActivity.EXTRA_TRANSACTION_MINT_URL, payment.mintUrl)
                putExtra(TransactionDetailActivity.EXTRA_TRANSACTION_PAYMENT_TYPE, payment.paymentType)
                putExtra(TransactionDetailActivity.EXTRA_TRANSACTION_LIGHTNING_INVOICE, payment.lightningInvoice)
                putExtra(TransactionDetailActivity.EXTRA_CHECKOUT_BASKET_JSON, payment.checkoutBasketJson)
                putExtra(TransactionDetailActivity.EXTRA_TRANSACTION_TIP_AMOUNT, payment.tipAmountSats)
                putExtra(TransactionDetailActivity.EXTRA_TRANSACTION_TIP_PERCENTAGE, payment.tipPercentage)
            }
            startActivity(intent)
        }
    }

    private fun showDeleteConfirmation(basket: SavedBasket) {
        val index = savedBasketManager.getArchivedBasketIndex(basket.id)
        val displayName = basket.getDisplayName(index)

        AlertDialog.Builder(this)
            .setTitle(R.string.basket_archive_delete_title)
            .setMessage(getString(R.string.basket_archive_delete_message, displayName))
            .setPositiveButton(R.string.common_delete) { _, _ ->
                savedBasketManager.deleteArchivedBasket(basket.id)
                loadArchivedBaskets()
            }
            .setNegativeButton(R.string.common_cancel, null)
            .show()
    }
}

/**
 * Simple dialog to show basket details with expandable items.
 */
class BasketDetailDialog(
    context: android.content.Context,
    private val basket: SavedBasket,
    private val currencyManager: CurrencyManager,
    private val onPaymentClick: () -> Unit
) : AlertDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_basket_detail)

        // Get index for display name
        val savedBasketManager = SavedBasketManager.getInstance(context)
        val index = savedBasketManager.getArchivedBasketIndex(basket.id)

        // Header
        findViewById<TextView>(R.id.basket_name)?.text = basket.getDisplayName(index)
        
        // Date
        val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        findViewById<TextView>(R.id.basket_date)?.text = dateFormat.format(Date(basket.paidAt ?: basket.updatedAt))

        // Items list
        val itemsContainer = findViewById<LinearLayout>(R.id.items_container)
        val inflater = LayoutInflater.from(context)
        
        basket.items.forEach { item ->
            val itemView = inflater.inflate(R.layout.item_basket_detail_line, itemsContainer, false)
            
            itemView.findViewById<TextView>(R.id.item_quantity)?.text = item.quantity.toString()
            itemView.findViewById<TextView>(R.id.item_name)?.text = item.item.name ?: "Item"
            
            val priceText = if (item.isSatsPrice()) {
                "₿${item.getTotalSats()}"
            } else {
                currencyManager.formatCurrencyAmount(item.getTotalPrice())
            }
            itemView.findViewById<TextView>(R.id.item_price)?.text = priceText
            
            itemsContainer?.addView(itemView)
        }

        // Total
        val totalText = if (basket.hasMixedPriceTypes()) {
            // Show both
            val fiat = currencyManager.formatCurrencyAmount(basket.getTotalFiatPrice())
            val sats = "₿${basket.getTotalSatsPrice()}"
            "$fiat + $sats"
        } else if (basket.getTotalSatsPrice() > 0) {
            "₿${basket.getTotalSatsPrice()}"
        } else {
            currencyManager.formatCurrencyAmount(basket.getTotalFiatPrice())
        }
        findViewById<TextView>(R.id.basket_total)?.text = totalText

        // View Payment button
        findViewById<View>(R.id.view_payment_button)?.setOnClickListener {
            dismiss()
            onPaymentClick()
        }

        // Close button
        findViewById<ImageButton>(R.id.close_button)?.setOnClickListener {
            dismiss()
        }
    }
}
