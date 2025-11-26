package com.electricdreams.shellshock.feature.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.electricdreams.shellshock.R
import com.electricdreams.shellshock.core.model.Amount
import com.electricdreams.shellshock.core.model.CheckoutBasket
import com.electricdreams.shellshock.core.model.CheckoutBasketItem
import com.electricdreams.shellshock.core.util.ReceiptPrinter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Beautiful receipt view displaying all items purchased in a checkout.
 * Follows Apple-like design principles with clean typography,
 * generous spacing, and professional layout suitable for baristas
 * to verify customer orders.
 */
class BasketReceiptActivity : AppCompatActivity() {

    private lateinit var totalAmountText: TextView
    private lateinit var totalSubtitleText: TextView
    private lateinit var checkoutDateText: TextView
    private lateinit var itemsHeaderText: TextView
    private lateinit var itemsContainer: LinearLayout
    private lateinit var totalsContainer: LinearLayout
    private lateinit var subtotalRow: LinearLayout
    private lateinit var subtotalValue: TextView
    private lateinit var vatBreakdownContainer: LinearLayout
    private lateinit var finalTotalValue: TextView
    private lateinit var satsEquivalentText: TextView
    private lateinit var paidAmountText: TextView
    private lateinit var printButton: ImageButton

    private var basket: CheckoutBasket? = null
    
    // Additional payment data for printing
    private var paymentType: String? = null
    private var paymentDate: Date = Date()
    private var transactionId: String? = null
    private var mintUrl: String? = null
    private var bitcoinPrice: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basket_receipt)

        initializeViews()
        loadBasketData()
        displayReceipt()
    }

    private fun initializeViews() {
        // Back button
        findViewById<ImageButton>(R.id.back_button).setOnClickListener { finish() }
        
        // Print button
        printButton = findViewById(R.id.print_button)
        printButton.setOnClickListener { printReceipt() }

        // Hero section
        totalAmountText = findViewById(R.id.total_amount)
        totalSubtitleText = findViewById(R.id.total_subtitle)
        checkoutDateText = findViewById(R.id.checkout_date)

        // Items section
        itemsHeaderText = findViewById(R.id.items_header)
        itemsContainer = findViewById(R.id.items_container)

        // Totals section
        totalsContainer = findViewById(R.id.totals_container)
        subtotalRow = findViewById(R.id.subtotal_row)
        subtotalValue = findViewById(R.id.subtotal_value)
        vatBreakdownContainer = findViewById(R.id.vat_breakdown_container)
        finalTotalValue = findViewById(R.id.final_total_value)
        satsEquivalentText = findViewById(R.id.sats_equivalent)

        // Payment info
        paidAmountText = findViewById(R.id.paid_amount)
    }

    private fun loadBasketData() {
        val basketJson = intent.getStringExtra(EXTRA_CHECKOUT_BASKET_JSON)
        basket = CheckoutBasket.fromJson(basketJson)
        
        // Load additional payment data
        paymentType = intent.getStringExtra(EXTRA_PAYMENT_TYPE)
        val dateMillis = intent.getLongExtra(EXTRA_PAYMENT_DATE, System.currentTimeMillis())
        paymentDate = Date(dateMillis)
        transactionId = intent.getStringExtra(EXTRA_TRANSACTION_ID)
        mintUrl = intent.getStringExtra(EXTRA_MINT_URL)
        val btcPrice = intent.getDoubleExtra(EXTRA_BITCOIN_PRICE, -1.0)
        bitcoinPrice = if (btcPrice > 0) btcPrice else null
        
        android.util.Log.d("BasketReceiptActivity", "Received basket JSON: ${basketJson?.length ?: 0} chars")
        android.util.Log.d("BasketReceiptActivity", "Parsed basket: ${basket?.items?.size ?: 0} items")
    }
    
    private fun printReceipt() {
        val currentBasket = basket ?: return
        
        val receiptPrinter = ReceiptPrinter(this)
        val receiptData = ReceiptPrinter.ReceiptData(
            basket = currentBasket,
            paymentType = paymentType,
            paymentDate = paymentDate,
            transactionId = transactionId,
            mintUrl = mintUrl,
            bitcoinPrice = bitcoinPrice,
        )
        
        // Print directly - one click printing
        receiptPrinter.printReceipt(receiptData)
    }

    private fun displayReceipt() {
        val basket = this.basket ?: run {
            // No basket data - show empty state and finish
            finish()
            return
        }

        // Display hero section
        displayHeroSection(basket)

        // Display items
        displayItems(basket)

        // Display totals
        displayTotals(basket)

        // Display payment info
        displayPaymentInfo(basket)
    }

    private fun displayHeroSection(basket: CheckoutBasket) {
        // Format and display total amount
        val currency = Amount.Currency.fromCode(basket.currency)
        val grossTotalCents = basket.getFiatGrossTotalCents()
        
        if (grossTotalCents > 0) {
            val totalAmount = Amount(grossTotalCents, currency)
            totalAmountText.text = totalAmount.toString()
        } else {
            // All sats pricing
            val satsAmount = Amount(basket.totalSatoshis, Amount.Currency.BTC)
            totalAmountText.text = satsAmount.toString()
        }

        // VAT subtitle
        val totalVat = basket.getFiatVatTotalCents()
        if (totalVat > 0) {
            val vatAmount = Amount(totalVat, currency)
            totalSubtitleText.text = "incl. $vatAmount VAT"
            totalSubtitleText.visibility = View.VISIBLE
        } else {
            totalSubtitleText.visibility = View.GONE
        }

        // Checkout date
        val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        checkoutDateText.text = dateFormat.format(basket.getCheckoutDate())
    }

    private fun displayItems(basket: CheckoutBasket) {
        // Items header with count
        val itemCount = basket.getTotalItemCount()
        itemsHeaderText.text = if (itemCount == 1) "1 ITEM" else "$itemCount ITEMS"

        // Clear container
        itemsContainer.removeAllViews()

        // Add each item
        val inflater = LayoutInflater.from(this)
        val currency = Amount.Currency.fromCode(basket.currency)

        basket.items.forEachIndexed { index, item ->
            val itemView = inflater.inflate(R.layout.item_receipt_line, itemsContainer, false)
            bindItemView(itemView, item, currency)
            itemsContainer.addView(itemView)

            // Add divider between items (not after last)
            if (index < basket.items.size - 1) {
                addDivider(itemsContainer)
            }
        }
    }

    private fun bindItemView(view: View, item: CheckoutBasketItem, currency: Amount.Currency) {
        // Quantity badge
        val quantityText = view.findViewById<TextView>(R.id.item_quantity)
        quantityText.text = item.quantity.toString()

        // Item name (with variation if present)
        val nameText = view.findViewById<TextView>(R.id.item_name)
        nameText.text = item.displayName

        // Unit price text
        val unitPriceText = view.findViewById<TextView>(R.id.item_unit_price)
        
        if (item.isFiatPrice()) {
            val unitPrice = Amount(item.getGrossPricePerUnitCents(), currency)
            unitPriceText.text = if (item.quantity > 1) "$unitPrice each" else "$unitPrice"
        } else {
            val unitPriceSats = Amount(item.priceSats, Amount.Currency.BTC)
            unitPriceText.text = if (item.quantity > 1) "$unitPriceSats each" else "$unitPriceSats"
        }

        // Line total
        val totalText = view.findViewById<TextView>(R.id.item_total)
        if (item.isFiatPrice()) {
            val lineTotal = Amount(item.getGrossTotalCents(), currency)
            totalText.text = lineTotal.toString()
        } else {
            val lineTotalSats = Amount(item.getNetTotalSats(), Amount.Currency.BTC)
            totalText.text = lineTotalSats.toString()
        }

        // VAT detail row (only for fiat items with VAT)
        val vatDetailRow = view.findViewById<LinearLayout>(R.id.vat_detail_row)
        if (item.isFiatPrice() && item.vatEnabled && item.vatRate > 0) {
            val vatLabel = view.findViewById<TextView>(R.id.vat_label)
            val vatAmountText = view.findViewById<TextView>(R.id.vat_amount)

            vatLabel.text = "incl. ${item.vatRate}% VAT"
            val itemVat = Amount(item.getTotalVatCents(), currency)
            vatAmountText.text = itemVat.toString()
            vatDetailRow.visibility = View.VISIBLE
        } else {
            vatDetailRow.visibility = View.GONE
        }
    }

    private fun displayTotals(basket: CheckoutBasket) {
        val currency = Amount.Currency.fromCode(basket.currency)
        val hasVat = basket.hasVat()
        val hasFiatItems = basket.getFiatItems().isNotEmpty()

        // Subtotal row (net, only if there's VAT to show)
        if (hasVat && hasFiatItems) {
            val netTotal = basket.getFiatNetTotalCents()
            subtotalValue.text = Amount(netTotal, currency).toString()
            subtotalRow.visibility = View.VISIBLE
        } else {
            subtotalRow.visibility = View.GONE
        }

        // VAT breakdown by rate
        vatBreakdownContainer.removeAllViews()
        if (hasVat && hasFiatItems) {
            val vatBreakdown = basket.getVatBreakdown()
            vatBreakdown.forEach { (rate, amount) ->
                addVatRow(rate, amount, currency)
            }
        }

        // Final total
        val grossTotal = basket.getFiatGrossTotalCents()
        if (grossTotal > 0) {
            finalTotalValue.text = Amount(grossTotal, currency).toString()
        } else {
            finalTotalValue.text = Amount(basket.totalSatoshis, Amount.Currency.BTC).toString()
        }

        // Sats equivalent
        if (hasFiatItems && basket.totalSatoshis > 0) {
            satsEquivalentText.text = "≈ ${Amount(basket.totalSatoshis, Amount.Currency.BTC)}"
            satsEquivalentText.visibility = View.VISIBLE
        } else {
            satsEquivalentText.visibility = View.GONE
        }
    }

    private fun addVatRow(rate: Int, amountCents: Long, currency: Amount.Currency) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (8 * resources.displayMetrics.density).toInt()
            }
        }

        val label = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "VAT ($rate%)"
            textSize = 15f
            setTextColor(resources.getColor(R.color.color_text_secondary, theme))
        }

        val value = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = Amount(amountCents, currency).toString()
            textSize = 15f
            setTextColor(resources.getColor(R.color.color_text_secondary, theme))
        }

        row.addView(label)
        row.addView(value)
        vatBreakdownContainer.addView(row)
    }

    private fun displayPaymentInfo(basket: CheckoutBasket) {
        // Show how much was actually paid (in sats)
        paidAmountText.text = Amount(basket.totalSatoshis, Amount.Currency.BTC).toString()
    }

    private fun addDivider(container: LinearLayout) {
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (0.5f * resources.displayMetrics.density).toInt()
            ).apply {
                marginStart = (56 * resources.displayMetrics.density).toInt() // Align with item text
                marginEnd = (16 * resources.displayMetrics.density).toInt()
            }
            setBackgroundColor(resources.getColor(R.color.color_divider, theme))
        }
        container.addView(divider)
    }

    companion object {
        const val EXTRA_CHECKOUT_BASKET_JSON = "checkout_basket_json"
        const val EXTRA_PAYMENT_TYPE = "payment_type"
        const val EXTRA_PAYMENT_DATE = "payment_date"
        const val EXTRA_TRANSACTION_ID = "transaction_id"
        const val EXTRA_MINT_URL = "mint_url"
        const val EXTRA_BITCOIN_PRICE = "bitcoin_price"
    }
}
