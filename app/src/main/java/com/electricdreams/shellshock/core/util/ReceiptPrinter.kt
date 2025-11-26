package com.electricdreams.shellshock.core.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.print.PrintAttributes
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.electricdreams.shellshock.core.model.Amount
import com.electricdreams.shellshock.core.model.CheckoutBasket
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for generating and printing thermal receipt-style receipts.
 * Produces a narrow-format, fixed-width font receipt suitable for thermal printers.
 */
class ReceiptPrinter(private val context: Context) {

    companion object {
        // Standard thermal receipt width (58mm or 80mm paper)
        private const val RECEIPT_WIDTH_CHARS = 42 // Characters per line for 58mm paper
        private const val RECEIPT_WIDTH_CHARS_WIDE = 48 // Characters per line for 80mm paper
        
        private const val LINE_SEPARATOR = "──────────────────────────────────────────"
        private const val DOUBLE_LINE = "══════════════════════════════════════════"
    }

    /**
     * Data class containing all information needed to print a receipt.
     */
    data class ReceiptData(
        val basket: CheckoutBasket,
        val paymentType: String?, // "cashu" or "lightning"
        val paymentDate: Date,
        val transactionId: String?,
        val mintUrl: String?,
        val bitcoinPrice: Double?,
        val merchantName: String = "SHELLSHOCK POS",
        val merchantAddress: String? = null,
        val merchantVatNumber: String? = null,
    )

    /**
     * Generate a plain-text receipt suitable for thermal printers.
     */
    fun generateTextReceipt(data: ReceiptData): String {
        val sb = StringBuilder()
        val w = RECEIPT_WIDTH_CHARS
        
        // Helper functions
        fun center(text: String): String {
            val padding = (w - text.length) / 2
            return " ".repeat(maxOf(0, padding)) + text
        }
        
        fun leftRight(left: String, right: String): String {
            val spaces = w - left.length - right.length
            return left + " ".repeat(maxOf(1, spaces)) + right
        }
        
        fun line() = LINE_SEPARATOR.take(w)
        fun doubleLine() = DOUBLE_LINE.take(w)

        // Currency helper
        val currency = Amount.Currency.fromCode(data.basket.currency)
        fun formatFiat(cents: Long): String = Amount(cents, currency).toString()
        fun formatSats(sats: Long): String = Amount(sats, Amount.Currency.BTC).toString()

        // ═══════════════════════════════════════════
        // HEADER
        // ═══════════════════════════════════════════
        sb.appendLine()
        sb.appendLine(center(data.merchantName))
        data.merchantAddress?.let { sb.appendLine(center(it)) }
        data.merchantVatNumber?.let { sb.appendLine(center("VAT: $it")) }
        sb.appendLine()
        sb.appendLine(doubleLine())
        sb.appendLine(center("RECEIPT"))
        sb.appendLine(doubleLine())
        sb.appendLine()

        // ───────────────────────────────────────────
        // DATE & TRANSACTION INFO
        // ───────────────────────────────────────────
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        sb.appendLine(leftRight("Date:", dateFormat.format(data.paymentDate)))
        
        data.transactionId?.let { txId ->
            val shortTxId = if (txId.length > 16) txId.take(8) + "..." + txId.takeLast(4) else txId
            sb.appendLine(leftRight("Tx ID:", shortTxId))
        }
        
        sb.appendLine()
        sb.appendLine(line())

        // ───────────────────────────────────────────
        // ITEMS
        // ───────────────────────────────────────────
        sb.appendLine(center("ITEMS"))
        sb.appendLine(line())
        sb.appendLine()

        data.basket.items.forEach { item ->
            // Item name (may wrap if too long)
            val itemName = item.displayName
            val truncatedName = if (itemName.length > w - 12) {
                itemName.take(w - 15) + "..."
            } else {
                itemName
            }
            
            // Quantity x Unit Price
            val qtyPrice = if (item.isFiatPrice()) {
                val unitPrice = formatFiat(item.getGrossPricePerUnitCents())
                "${item.quantity} x $unitPrice"
            } else {
                val unitPrice = formatSats(item.priceSats)
                "${item.quantity} x $unitPrice"
            }
            
            // Line total
            val lineTotal = if (item.isFiatPrice()) {
                formatFiat(item.getGrossTotalCents())
            } else {
                formatSats(item.getNetTotalSats())
            }
            
            sb.appendLine(truncatedName)
            sb.appendLine(leftRight("  $qtyPrice", lineTotal))
            
            // VAT detail if applicable
            if (item.isFiatPrice() && item.vatEnabled && item.vatRate > 0) {
                val vatAmount = formatFiat(item.getTotalVatCents())
                sb.appendLine("  (incl. ${item.vatRate}% VAT: $vatAmount)")
            }
            
            sb.appendLine()
        }

        sb.appendLine(line())

        // ───────────────────────────────────────────
        // TOTALS
        // ───────────────────────────────────────────
        val hasVat = data.basket.hasVat()
        val hasFiatItems = data.basket.getFiatItems().isNotEmpty()

        if (hasVat && hasFiatItems) {
            // Net subtotal
            val netTotal = formatFiat(data.basket.getFiatNetTotalCents())
            sb.appendLine(leftRight("Subtotal (excl. VAT):", netTotal))

            // VAT breakdown
            data.basket.getVatBreakdown().forEach { (rate, amountCents) ->
                val vatAmount = formatFiat(amountCents)
                sb.appendLine(leftRight("VAT ($rate%):", vatAmount))
            }
            
            sb.appendLine(line())
        }

        // GRAND TOTAL
        val grossTotal = data.basket.getFiatGrossTotalCents()
        val totalDisplay = if (grossTotal > 0) {
            formatFiat(grossTotal)
        } else {
            formatSats(data.basket.totalSatoshis)
        }
        
        sb.appendLine()
        sb.appendLine(leftRight("TOTAL:", totalDisplay))
        sb.appendLine()

        // Bitcoin equivalent
        if (hasFiatItems && data.basket.totalSatoshis > 0) {
            val satsTotal = formatSats(data.basket.totalSatoshis)
            sb.appendLine(leftRight("Bitcoin:", satsTotal))
        }

        // Bitcoin price at time of transaction
        data.bitcoinPrice?.let { price ->
            val formattedPrice = String.format(Locale.US, "$%,.2f", price)
            sb.appendLine(leftRight("BTC/USD Rate:", formattedPrice))
        }

        sb.appendLine()
        sb.appendLine(doubleLine())

        // ───────────────────────────────────────────
        // PAYMENT INFO
        // ───────────────────────────────────────────
        sb.appendLine()
        
        val paymentMethod = when (data.paymentType) {
            "lightning" -> "⚡ Lightning Network"
            "cashu" -> "🥜 Cashu (ecash)"
            else -> "Bitcoin"
        }
        sb.appendLine(leftRight("Payment:", paymentMethod))
        
        val paidAmount = formatSats(data.basket.totalSatoshis)
        sb.appendLine(leftRight("Paid:", paidAmount))
        sb.appendLine(leftRight("Status:", "✓ PAID"))

        data.mintUrl?.let { url ->
            sb.appendLine()
            val shortUrl = if (url.length > w - 6) {
                url.take(w - 9) + "..."
            } else {
                url
            }
            sb.appendLine(leftRight("Mint:", shortUrl))
        }

        sb.appendLine()
        sb.appendLine(doubleLine())

        // ───────────────────────────────────────────
        // FOOTER
        // ───────────────────────────────────────────
        sb.appendLine()
        sb.appendLine(center("Thank you for your purchase!"))
        sb.appendLine()
        sb.appendLine(center("Powered by Bitcoin"))
        sb.appendLine(center("⚡"))
        sb.appendLine()

        return sb.toString()
    }

    /**
     * Generate an HTML receipt for web view or PDF printing.
     */
    fun generateHtmlReceipt(data: ReceiptData): String {
        val currency = Amount.Currency.fromCode(data.basket.currency)
        fun formatFiat(cents: Long): String = Amount(cents, currency).toString()
        fun formatSats(sats: Long): String = Amount(sats, Amount.Currency.BTC).toString()
        
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val hasVat = data.basket.hasVat()
        val hasFiatItems = data.basket.getFiatItems().isNotEmpty()
        
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=80mm">
    <style>
        @page {
            size: 80mm auto;
            margin: 0;
        }
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        body {
            font-family: 'Courier New', Courier, monospace;
            font-size: 12px;
            line-height: 1.4;
            width: 80mm;
            max-width: 80mm;
            padding: 8mm 4mm;
            background: white;
            color: black;
        }
        .center { text-align: center; }
        .right { text-align: right; }
        .bold { font-weight: bold; }
        .large { font-size: 16px; }
        .small { font-size: 10px; }
        .header { margin-bottom: 8px; }
        .merchant-name { font-size: 18px; font-weight: bold; }
        .divider { 
            border-top: 1px dashed #000; 
            margin: 8px 0; 
        }
        .double-divider { 
            border-top: 2px solid #000; 
            margin: 8px 0; 
        }
        .row {
            display: flex;
            justify-content: space-between;
            margin: 2px 0;
        }
        .row-label { flex: 1; }
        .row-value { text-align: right; }
        .item { margin: 8px 0; }
        .item-name { font-weight: bold; }
        .item-detail { padding-left: 8px; color: #333; }
        .vat-detail { font-size: 10px; color: #666; padding-left: 8px; }
        .total-row { font-size: 14px; font-weight: bold; margin: 4px 0; }
        .paid-badge {
            display: inline-block;
            background: #000;
            color: #fff;
            padding: 2px 8px;
            border-radius: 2px;
            font-size: 10px;
        }
        .footer { margin-top: 16px; }
        .bitcoin-symbol { font-size: 24px; }
    </style>
</head>
<body>
    <!-- Header -->
    <div class="header center">
        <div class="merchant-name">${data.merchantName}</div>
        ${data.merchantAddress?.let { "<div class=\"small\">$it</div>" } ?: ""}
        ${data.merchantVatNumber?.let { "<div class=\"small\">VAT: $it</div>" } ?: ""}
    </div>
    
    <div class="double-divider"></div>
    <div class="center bold large">RECEIPT</div>
    <div class="double-divider"></div>
    
    <!-- Transaction Info -->
    <div class="row">
        <span>Date:</span>
        <span>${dateFormat.format(data.paymentDate)}</span>
    </div>
    ${data.transactionId?.let { 
        val shortId = if (it.length > 16) it.take(8) + "..." + it.takeLast(4) else it
        "<div class=\"row\"><span>Tx ID:</span><span class=\"small\">$shortId</span></div>" 
    } ?: ""}
    
    <div class="divider"></div>
    <div class="center bold">ITEMS</div>
    <div class="divider"></div>
    
    <!-- Items -->
    ${data.basket.items.joinToString("") { item ->
        val lineTotal = if (item.isFiatPrice()) formatFiat(item.getGrossTotalCents()) else formatSats(item.getNetTotalSats())
        val unitPrice = if (item.isFiatPrice()) formatFiat(item.getGrossPricePerUnitCents()) else formatSats(item.priceSats)
        val vatInfo = if (item.isFiatPrice() && item.vatEnabled && item.vatRate > 0) {
            "<div class=\"vat-detail\">(incl. ${item.vatRate}% VAT: ${formatFiat(item.getTotalVatCents())})</div>"
        } else ""
        """
        <div class="item">
            <div class="row">
                <span class="item-name">${item.displayName}</span>
                <span class="bold">$lineTotal</span>
            </div>
            <div class="item-detail">${item.quantity} × $unitPrice</div>
            $vatInfo
        </div>
        """
    }}
    
    <div class="divider"></div>
    
    <!-- Totals -->
    ${if (hasVat && hasFiatItems) """
    <div class="row">
        <span>Subtotal (excl. VAT):</span>
        <span>${formatFiat(data.basket.getFiatNetTotalCents())}</span>
    </div>
    ${data.basket.getVatBreakdown().entries.joinToString("") { (rate, amount) ->
        "<div class=\"row\"><span>VAT ($rate%):</span><span>${formatFiat(amount)}</span></div>"
    }}
    <div class="divider"></div>
    """ else ""}
    
    <div class="row total-row">
        <span>TOTAL:</span>
        <span>${if (data.basket.getFiatGrossTotalCents() > 0) formatFiat(data.basket.getFiatGrossTotalCents()) else formatSats(data.basket.totalSatoshis)}</span>
    </div>
    
    ${if (hasFiatItems && data.basket.totalSatoshis > 0) """
    <div class="row small">
        <span>Bitcoin:</span>
        <span>${formatSats(data.basket.totalSatoshis)}</span>
    </div>
    """ else ""}
    
    ${data.bitcoinPrice?.let { """
    <div class="row small">
        <span>BTC/USD Rate:</span>
        <span>${String.format(Locale.US, "$%,.2f", it)}</span>
    </div>
    """ } ?: ""}
    
    <div class="double-divider"></div>
    
    <!-- Payment Info -->
    <div class="row">
        <span>Payment:</span>
        <span>${when (data.paymentType) {
            "lightning" -> "⚡ Lightning"
            "cashu" -> "🥜 Cashu"
            else -> "Bitcoin"
        }}</span>
    </div>
    <div class="row">
        <span>Paid:</span>
        <span>${formatSats(data.basket.totalSatoshis)}</span>
    </div>
    <div class="row">
        <span>Status:</span>
        <span class="paid-badge">✓ PAID</span>
    </div>
    
    ${data.mintUrl?.let { """
    <div class="row small" style="margin-top: 4px;">
        <span>Mint:</span>
        <span style="word-break: break-all; max-width: 50mm;">${if (it.length > 30) it.take(27) + "..." else it}</span>
    </div>
    """ } ?: ""}
    
    <div class="double-divider"></div>
    
    <!-- Footer -->
    <div class="footer center">
        <div>Thank you for your purchase!</div>
        <div class="small" style="margin-top: 8px;">Powered by Bitcoin</div>
        <div class="bitcoin-symbol">⚡</div>
    </div>
</body>
</html>
        """.trimIndent()
    }

    /**
     * Print the receipt using Android's print framework.
     */
    fun printReceipt(data: ReceiptData) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val html = generateHtmlReceipt(data)
        
        val webView = WebView(context)
        webView.settings.apply {
            javaScriptEnabled = false
            allowFileAccess = false
        }
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val jobName = "${data.merchantName} Receipt"
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                
                val attributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A7) // Small receipt-like size
                    .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                
                printManager.print(jobName, printAdapter, attributes)
            }
        }
        
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    /**
     * Share the receipt as text (for messaging apps, email, etc.)
     */
    fun shareReceipt(data: ReceiptData) {
        val receiptText = generateTextReceipt(data)
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${data.merchantName} Receipt")
            putExtra(Intent.EXTRA_TEXT, receiptText)
        }
        
        val chooser = Intent.createChooser(shareIntent, "Share Receipt")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Save receipt as text file and share.
     */
    fun shareReceiptAsFile(data: ReceiptData) {
        val receiptText = generateTextReceipt(data)
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "receipt_${dateFormat.format(data.paymentDate)}.txt"
        
        try {
            val cacheDir = File(context.cacheDir, "receipts")
            cacheDir.mkdirs()
            val file = File(cacheDir, fileName)
            
            FileOutputStream(file).use { fos ->
                fos.write(receiptText.toByteArray(Charsets.UTF_8))
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "${data.merchantName} Receipt")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(shareIntent, "Share Receipt")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Fallback to plain text share
            shareReceipt(data)
        }
    }
}
