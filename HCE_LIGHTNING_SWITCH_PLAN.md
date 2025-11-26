# HCE Payload Switching: Cashu ↔ Lightning in `PaymentRequestActivity`

This document describes a concrete implementation plan for switching the NFC HCE payload
between a Cashu payment request and a Lightning BOLT11 invoice in `PaymentRequestActivity`.

The goal: when the user selects the **Lightning** tab, the emulated NFC tag should expose a
text record `"lightning:<bolt11_request>"`. When they switch back to **Cashu**, the tag
should again expose the Cashu payment request.

---

## 1. High-level behavior

### 1.1 Default behavior (Cashu)

- When `PaymentRequestActivity` starts:
  - It creates a Cashu payment request string (`hcePaymentRequest`) using `CashuPaymentHelper`.
  - It starts `NdefHostCardEmulationService`.
  - It configures the HCE service to emulate the **Cashu payment request**.
- The Cashu request is also shown as a QR code via the existing Nostr flow.

### 1.2 Lightning behavior

- When the user switches to the **Lightning** tab:
  - If a Lightning quote has not yet been requested, start the Lightning mint flow to obtain a BOLT11 invoice.
  - When the Lightning invoice is ready, update the UI QR code and store the BOLT11 string for history.
  - If the Lightning tab is currently selected when the invoice becomes available, update the HCE service
    payload to a text record of the form `"lightning:<bolt11>"`.
- When the user switches back to the **Cashu** tab, the HCE payload should revert to the Cashu payment request.

### 1.3 Single source of truth for NFC payload

- The **active tab** (Cashu vs Lightning) and the **available payment data** (Cashu request / BOLT11) determine
  what the HCE service should emulate.
- All update calls go through two helper methods on `PaymentRequestActivity`:
  - `setHceToCashu()`
  - `setHceToLightning()`

---

## 2. Code Changes in `PaymentRequestActivity`

### 2.1 Enumerate HCE mode and track current state

**File:** `app/src/main/java/com/electricdreams/shellshock/PaymentRequestActivity.kt`

1. Add an enum and a state field near the top of the class.

   ```kotlin
   class PaymentRequestActivity : AppCompatActivity() {

       private lateinit var cashuQrImageView: ImageView
       private lateinit var lightningQrImageView: ImageView
       private lateinit var cashuQrContainer: View
       private lateinit var lightningQrContainer: View

       // HCE mode for deciding which payload to emulate (Cashu vs Lightning)
       private enum class HceMode { CASHU, LIGHTNING }

       private lateinit var cashuTab: TextView
       private lateinit var lightningTab: TextView
       private lateinit var largeAmountDisplay: TextView
       private lateinit var convertedAmountDisplay: TextView
       private lateinit var statusText: TextView
       private lateinit var closeButton: View
       private lateinit var shareButton: View
       private lateinit var nfcReadingOverlay: View
       private lateinit var lightningLoadingSpinner: View
       private lateinit var lightningLogoCard: View

       private var paymentAmount: Long = 0
       private var bitcoinPriceWorker: BitcoinPriceWorker? = null
       private var hcePaymentRequest: String? = null
       private var formattedAmountString: String = ""

       // Tab manager for Cashu/Lightning tab switching
       private lateinit var tabManager: PaymentTabManager

       // Payment handlers
       private var nostrHandler: NostrPaymentHandler? = null
       private var lightningHandler: LightningMintHandler? = null
       private var lightningStarted = false

       // Current HCE mode (defaults to Cashu)
       private var currentHceMode: HceMode = HceMode.CASHU

       // Lightning quote info for history
       private var lightningInvoice: String? = null
       private var lightningQuoteId: String? = null
       private var lightningMintUrl: String? = null

       // ... existing fields continue ...
   }
   ```

### 2.2 HCE switching helpers

Add two private methods on `PaymentRequestActivity` (somewhere near `initializePaymentRequest` / `startNostrPaymentFlow`):

```kotlin
private fun setHceToCashu() {
    val request = hcePaymentRequest ?: run {
        Log.w(TAG, "setHceToCashu() called but hcePaymentRequest is null")
        return
    }

    try {
        val hceService = NdefHostCardEmulationService.getInstance()
        if (hceService != null) {
            Log.d(TAG, "setHceToCashu(): Switching HCE payload to Cashu request")
            hceService.setPaymentRequest(request, paymentAmount)
            currentHceMode = HceMode.CASHU
        } else {
            Log.w(TAG, "setHceToCashu(): HCE service not available")
        }
    } catch (e: Exception) {
        Log.e(TAG, "setHceToCashu(): Error while setting HCE Cashu payload: ${e.message}", e)
    }
}

private fun setHceToLightning() {
    val invoice = lightningInvoice ?: run {
        Log.w(TAG, "setHceToLightning() called but lightningInvoice is null")
        return
    }
    val payload = "lightning:$invoice"

    try {
        val hceService = NdefHostCardEmulationService.getInstance()
        if (hceService != null) {
            Log.d(TAG, "setHceToLightning(): Switching HCE payload to Lightning invoice. payload=$payload")
            // Lightning mode is just a text payload; amount check is not used here
            hceService.setPaymentRequest(payload, 0L)
            currentHceMode = HceMode.LIGHTNING
        } else {
            Log.w(TAG, "setHceToLightning(): HCE service not available")
        }
    } catch (e: Exception) {
        Log.e(TAG, "setHceToLightning(): Error while setting HCE Lightning payload: ${e.message}", e)
    }
}
```

Notes:

- `hcePaymentRequest` is the existing Cashu payment request string created by `CashuPaymentHelper`.
- `lightningInvoice` is already being set in `onInvoiceReady`.
- Logging is intentionally verbose for debugging via `adb logcat`.

### 2.3 Wire HCE switching into tab selection

Inside `onCreate`, replace the existing tab setup:

```kotlin
// Set up tabs with listener
tabManager.setup(object : PaymentTabManager.TabSelectionListener {
    override fun onLightningTabSelected() {
        // Start lightning quote flow once when tab first selected
        if (!lightningStarted) {
            startLightningMintFlow()
        }
    }

    override fun onCashuTabSelected() {
        // Status text mainly controlled by Nostr / HCE flow
    }
})
```

with:

```kotlin
// Set up tabs with listener
tabManager.setup(object : PaymentTabManager.TabSelectionListener {
    override fun onLightningTabSelected() {
        Log.d(TAG, "onLightningTabSelected() called. lightningStarted=$lightningStarted, lightningInvoice=$lightningInvoice")

        // Start lightning quote flow once when tab first selected
        if (!lightningStarted) {
            startLightningMintFlow()
        } else if (lightningInvoice != null) {
            // If invoice is already known, try to switch HCE now
            setHceToLightning()
        }
    }

    override fun onCashuTabSelected() {
        Log.d(TAG, "onCashuTabSelected() called. currentHceMode=$currentHceMode")
        // When user returns to Cashu tab, restore Cashu HCE payload
        setHceToCashu()
    }
})
```

Behavior:

- First time Lightning is selected (`lightningStarted == false`): starts the Lightning flow.
- Subsequent Lightning selections (with `lightningInvoice != null`): immediately switch tag to Lightning payload.
- Cashu selections: always restore the Cashu payload.

### 2.4 Update HCE when Lightning invoice becomes ready

In `createLightningCallback()`, augment `onInvoiceReady` so that it also triggers HCE switching when the Lightning tab is active.

Original structure:

```kotlin
override fun onInvoiceReady(bolt11: String, quoteId: String, mintUrl: String) {
    // Store for history
    lightningInvoice = bolt11
    lightningQuoteId = quoteId
    lightningMintUrl = mintUrl

    // Update pending payment with Lightning info
    pendingPaymentId?.let { paymentId ->
        PaymentsHistoryActivity.updatePendingWithLightningInfo(
            context = this@PaymentRequestActivity,
            paymentId = paymentId,
            lightningInvoice = bolt11,
            lightningQuoteId = quoteId,
            lightningMintUrl = mintUrl,
        )
    }

    try {
        val qrBitmap = QrCodeGenerator.generate(bolt11, 512)
        lightningQrImageView.setImageBitmap(qrBitmap)
        // Hide loading spinner and show the bolt icon
        lightningLoadingSpinner.visibility = View.GONE
        lightningLogoCard.visibility = View.VISIBLE
    } catch (e: Exception) {
        Log.e(TAG, "Error generating Lightning QR bitmap: ${e.message}", e)
        // Still hide spinner on error
        lightningLoadingSpinner.visibility = View.GONE
    }
}
```

Add a bit at the end:

```kotlin
    // If Lightning tab is currently visible, switch HCE payload to Lightning
    if (tabManager.isLightningTabSelected()) {
        Log.d(TAG, "onInvoiceReady(): Lightning tab is selected, calling setHceToLightning()")
        setHceToLightning()
    }
}
```

This ensures that when a quote arrives while the Lightning tab is visible (fresh flow or resume), the HCE payload is updated immediately.

### 2.5 Keep Cashu as the default HCE payload

In `initializePaymentRequest()` you already create the Cashu request and call `setupNdefPayment()`.

In `setupNdefPayment()`, instead of directly calling `hceService.setPaymentRequest(request, paymentAmount)`, you can reuse `setHceToCashu()`:

Current:

```kotlin
private fun setupNdefPayment() {
    val request = hcePaymentRequest ?: return

    // Match original behavior: slight delay before configuring service
    Handler(Looper.getMainLooper()).postDelayed({
        val hceService = NdefHostCardEmulationService.getInstance()
        if (hceService != null) {
            Log.d(TAG, "Setting up NDEF payment with HCE service")

            // Set the payment request to the HCE service with expected amount
            hceService.setPaymentRequest(request, paymentAmount)

            // Set up callback for when a token is received or an error occurs
            hceService.setPaymentCallback(...)
        }
    }, 1000)
}
```

Change the part where the request is set:

```kotlin
            Log.d(TAG, "Setting up NDEF payment with HCE service")

            // Set the payment request to the HCE service with expected amount (Cashu by default)
            setHceToCashu()

            // Set up callback for when a token is received or an error occurs
            hceService.setPaymentCallback(...)
```

This keeps the old behavior (Cashu default) but routes it through the unified helper.

---

## 3. Logcat verification strategy

To verify the behavior without instrumenting UI further, use `adb logcat`.

### 3.1 Filter logs

```bash
adb logcat | grep -E "PaymentRequestActivity|NdefHostCardEmulationService"
```

### 3.2 Expected log patterns

1. **Initial Cashu setup** (opening payment request screen):

   ```text
   PaymentRequestActivity  D  setHceToCashu(): Switching HCE payload to Cashu request
   NdefHostCardEmulationService  I  Setting payment request: <cashu_request_string> for amount: <sats>
   ```

2. **Switch to Lightning tab (after invoice ready)**:

   ```text
   PaymentRequestActivity  D  onLightningTabSelected() called. lightningStarted=true, lightningInvoice=lnbc1...
   PaymentRequestActivity  D  setHceToLightning(): Switching HCE payload to Lightning invoice. payload=lightning:lnbc1...
   NdefHostCardEmulationService  I  Setting payment request: lightning:lnbc1... for amount: 0
   ```

3. **Switch back to Cashu tab**:

   ```text
   PaymentRequestActivity  D  onCashuTabSelected() called. currentHceMode=LIGHTNING
   PaymentRequestActivity  D  setHceToCashu(): Switching HCE payload to Cashu request
   NdefHostCardEmulationService  I  Setting payment request: <cashu_request_string> for amount: <sats>
   ```

---

## 4. Implementation checklist

You can use this as a step-by-step checklist while applying patches manually:

- [ ] Add `HceMode` enum and `currentHceMode` field to `PaymentRequestActivity`.
- [ ] Implement `setHceToCashu()` and `setHceToLightning()` helpers.
- [ ] Replace `tabManager.setup(...)` callbacks to invoke these helpers on tab changes.
- [ ] Extend `onInvoiceReady` to call `setHceToLightning()` when the Lightning tab is active.
- [ ] Update `setupNdefPayment()` to call `setHceToCashu()` instead of setting the request directly.
- [ ] Rebuild the app (`./gradlew :app:assembleDebug`).
- [ ] Verify NFC payload switching via `adb logcat`.
