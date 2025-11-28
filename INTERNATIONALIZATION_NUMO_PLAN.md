# Numo Internationalization Plan (English & Spanish)

This document describes how to systematically find and externalize all user‑visible strings in the **Numo** app into `strings.xml` (English) and `values-es/strings.xml` (Spanish).

---

## 1. Establish the i18n baseline

### 1.1. Define key naming convention (Numo-specific)

Use a consistent scheme for all string keys:

- **Pattern**: `screen_or_feature_component_purpose`
- Examples tailored to Numo:

  - Common:
    - `common_ok`, `common_cancel`, `common_retry`, `common_error_prefix`
  - Balance check:
    - `balance_check_title`, `balance_check_button_check`, `balance_check_button_tap_card`, `balance_check_instructions`
  - Tips:
    - `tips_settings_title`, `tips_settings_enable_label`
    - `tip_selection_title`, `tip_selection_question_add_tip`, `tip_selection_button_no_tip`, `tip_selection_button_confirm`
  - Items / catalog:
    - `item_entry_title_add`, `item_entry_title_edit`
    - `item_entry_label_name`, `item_entry_label_variation`, `item_entry_label_description`
    - `item_entry_section_basic_information`, `item_entry_section_pricing`
    - `items_quantity_in_stock`, `items_button_add_preset`
  - Payments:
    - `payment_request_status_preparing`, `payment_request_status_waiting`
    - `payment_request_status_success`, `payment_request_status_error_generic`
  - Onboarding:
    - `onboarding_status_creating_wallet`, `onboarding_status_generating_seed_phrase`
    - `onboarding_seed_invalid_characters`, `onboarding_seed_words_entered_count`, etc.

Document this convention (for example in `docs/INTERNATIONALIZATION_NUMO.md`) so it’s easy to follow in future work.

### 1.2. Set up base resource files

Project layout:

- Code: `app/src/main/java/com/electricdreams/numo/**`
- Layouts: `app/src/main/res/layout/**`
- Current strings: `app/src/main/res/values/strings.xml` (only a few entries)
- No `values-es/strings.xml` yet.

Steps:

1. Ensure `app/src/main/res/values/strings.xml` exists and is the **canonical English** file.
2. Create Spanish file:

   ```bash
   mkdir -p app/src/main/res/values-es
   cat > app/src/main/res/values-es/strings.xml << 'EOF'
   <resources>
       <string name="app_name">Numo</string>
       <!-- Spanish translations will be added as keys appear in values/strings.xml -->
   </resources>
   EOF
   ```

3. From this point on:
   - Every time you add/change a key in `values/strings.xml`, mirror it in `values-es/strings.xml`.

---

## 2. Build a concrete "to‑fix" inventory from the code

Instead of hunting by eye, generate **two task lists** from the repo: one for layouts, one for Kotlin/Java.

### 2.1. Layout strings to externalize

From repo root:

```bash
cd /home/lollerfirst/AndroidStudioProjects/shellshock2

rg 'android:(text|hint|title|contentDescription)="' app/src/main/res/layout -n \
  | rg -v '@string' \
  > docs/i18n_layout_todo_numo.txt
```

This will capture lines like:

- `activity_balance_check.xml`:
  - `"Check Balance"`, `"Tap Card"`,
  - `"Hold your NFC card near the back of the device to check balance"`
- `activity_tips_settings.xml`:
  - `"Tips"`, `"Enable Tips"`, `"TIP PRESETS"`, `"Add Preset"`, `"Reset to Defaults"`, etc.
- `activity_tip_selection.xml`:
  - `"Order Total"`, `"Would you like to add a tip?"`, `"Custom Tip"`, `"Enter amount"`, `"No Tip"`, `"Switch to ₿"`, `"Back"`, `"Confirm"`
- `activity_item_entry.xml`:
  - `"Add Item"`, `"Save"`, `"Add Photo"`, `"Remove Photo"`, `"BASIC INFORMATION"`, `"CATEGORY"`, `"PRICING"`,
  - `"Fiat"`, `"Bitcoin"`, `"USD"`, `"sats"`, `"Apply VAT"`, `"Price includes VAT"`

This file is your **layout checklist**.

### 2.2. Code strings to externalize

```bash
rg 'Toast\\.makeText\\(|\\.setTitle\\("|\\.setMessage\\("|text\\s*=\\s*"[^"]*[A-Za-z][^"]*"' \
   app/src/main/java/com/electricdreams/numo -n \
   > docs/i18n_code_todo_numo.txt
```

This picks up user‑visible English in code, e.g.:

- `PaymentRequestActivity.kt`:
  - Toasts: `"Invalid payment amount"`, `"Nothing to share yet"`,
    `"Failed to prepare NDEF payment data"`, `"Payment failed: $errorMessage"`
  - Status labels: `"Preparing payment request..."`, `"Waiting for payment..."`,
    `"Error generating QR code"`, `"Payment successful!"`
- `PaymentReceivedActivity.kt`:
  - `"No token to share"`, `"No apps available to share this token"`,
    `"Transaction details not available"`, `"$formattedAmount received."`
- `BalanceCheckActivity.kt`:
  - `"Balance: " + Amount(...)`, `"Error: $message"`
- `payment/PaymentResultHandler.kt`, `PaymentMethodHandler.kt`, `NfcPaymentProcessor.kt`:
  - `"Payment error: $message"`, `"Host Card Emulation is not available on this device"`,
    `"Failed to create payment request"`,
    `"Please enter an amount first"`, `"PIN-based rescan not supported in this build"`,
    `"Enter PIN"`, `"OK"`, `"Cancel"`.
- `feature/onboarding/OnboardingActivity.kt`:
  - Status texts: `"Creating your wallet..."`, `"Generating seed phrase..."`,
    `"Setting up default mints..."`, `"Initializing wallet..."`,
    `"Connecting to mints..."`, `"Fetching mint information..."`
  - Seed validation: `"Invalid characters detected"`, `"$filledCount of 12 words entered"`,
    `"Ready to continue"`
  - Backup/mints: `"Searching for backup on Nostr..."`, `"Initializing restore..."`,
    `"Backup Found"`, `"No Backup Found"`, `"Using default mints"`,
    `"Select which mints to restore"`, `"Restore Wallet"`,
    `"These mints will store your ecash tokens"`, `"Continue"`, `"Waiting..."`,
    `"Wallet Restored"`, `"Wallet Created"`, etc.
  - Toasts: `"Clipboard is empty"`,
    `"Please paste a valid 12-word seed phrase"`, `"Seed phrase pasted"`.
- `TopUpActivity.kt`, `Withdraw*Activity.kt`, items, tips, history screens, etc.

You now have **two concrete files** listing all remaining hardcoded text.

---

## 3. Layout pass: replace all hardcoded text in XML

Work through `docs/i18n_layout_todo_numo.txt` file‑by‑file.

### 3.1. General approach per layout file

For each line like:

```xml
android:text="Check Balance"
```

1. **Choose a key** based on the convention:
   - Example: `balance_check_button_check` for the main button.
2. Add to `values/strings.xml`:

   ```xml
   <!-- Balance check -->
   <string name="balance_check_title">Check Balance</string>
   <string name="balance_check_button_check">Check Balance</string>
   <string name="balance_check_button_tap_card">Tap Card</string>
   <string name="balance_check_instructions">
       Hold your NFC card near the back of the device to check balance
   </string>
   ```

3. Replace XML:

   ```xml
   android:text="@string/balance_check_button_check"
   ```

4. Add Spanish equivalents to `values-es/strings.xml`:

   ```xml
   <!-- Balance check -->
   <string name="balance_check_title">Ver saldo</string>
   <string name="balance_check_button_check">Ver saldo</string>
   <string name="balance_check_button_tap_card">Acerca la tarjeta</string>
   <string name="balance_check_instructions">
       Acerca tu tarjeta NFC a la parte trasera del dispositivo para ver el saldo
   </string>
   ```

Repeat the same process for all other layout strings (tips, items, onboarding, history, etc.).

### 3.2. Suggested layout processing order

To keep changes organized, process layouts in this order:

1. **Tips & payment flows**
   - `activity_tips_settings.xml`
   - `activity_tip_selection.xml`
   - `activity_payment_request.xml`
   - `activity_payment_received.xml`
2. **Items / catalog / POS**
   - `activity_item_entry.xml`
   - `activity_item_list.xml`
   - `activity_item_selection.xml`
   - `activity_modern_pos.xml`
   - `activity_barcode_scanner.xml`
   - `activity_barcode_scanner_checkout.xml`
3. **Onboarding & restore**
   - `activity_onboarding.xml`
   - `activity_restore_wallet.xml`
   - `activity_seed_phrase.xml`
4. **Settings**
   - `activity_settings.xml`
   - `activity_security_settings.xml`
   - `activity_currency_settings.xml`
   - `activity_theme_settings.xml`
   - `activity_developer_settings.xml`
   - `activity_mints_settings.xml`
5. **History & receipts**
   - `activity_history.xml`
   - `activity_transaction_detail.xml`
   - `activity_basket_receipt.xml`
   - `item_payment_history.xml`, `item_receipt_line.xml`, etc.
6. **NFC dialogs and helpers**
   - `dialog_nfc.xml`
   - `dialog_nfc_modern.xml`
   - `dialog_nfc_modern_simplified.xml`
   - `dialog_ndef_payment.xml`

For each file, clear all phrases so:

```bash
rg 'android:(text|hint|title|contentDescription)="' app/src/main/res/layout -n | rg -v '@string'
```

only finds **numbers, symbols, or demo placeholders**.

---

## 4. Code pass: replace hardcoded strings in Kotlin/Java

Use `docs/i18n_code_todo_numo.txt` as a checklist. Work **feature by feature**, not one string at a time.

### 4.1. Payment request / received / general payment errors

Files:

- `PaymentRequestActivity.kt`
- `PaymentReceivedActivity.kt`
- `payment/PaymentResultHandler.kt`
- `payment/PaymentMethodHandler.kt`

Actions:

1. For all Toasts and status texts like:

   ```kotlin
   Toast.makeText(this, "Invalid payment amount", Toast.LENGTH_SHORT).show()
   statusText.text = "Preparing payment request..."
   ```

2. Add to `values/strings.xml`:

   ```xml
   <!-- Payment request -->
   <string name="payment_request_error_invalid_amount">Invalid payment amount</string>
   <string name="payment_request_status_preparing">Preparing payment request...</string>
   <string name="payment_request_status_waiting">Waiting for payment...</string>
   <string name="payment_request_status_error_qr">Error generating QR code</string>
   <string name="payment_request_status_error_generic">Error: %1$s</string>
   <string name="payment_request_status_success">Payment successful!</string>
   <string name="payment_request_status_failed">Payment failed: %1$s</string>

   <!-- Payment received -->
   <string name="payment_received_amount">%1$s received.</string>
   <string name="payment_received_error_no_token">No token to share</string>
   <string name="payment_received_error_no_share_app">
       No apps available to share this token
   </string>
   <string name="payment_received_error_no_details">
       Transaction details not available
   </string>

   <!-- Generic payment -->
   <string name="payment_error_generic">Payment error: %1$s</string>
   <string name="payment_error_hce_not_available">
       Host Card Emulation is not available on this device
   </string>
   <string name="payment_error_failed_to_create_request">
       Failed to create payment request
   </string>
   <string name="payment_error_setup_ndef">
       Error setting up NDEF payment: %1$s
   </string>
   ```

3. Replace in code, for example:

   ```kotlin
   Toast.makeText(
       this,
       getString(R.string.payment_request_error_invalid_amount),
       Toast.LENGTH_SHORT
   ).show()

   statusText.text = getString(R.string.payment_request_status_preparing)

   statusText.text = getString(R.string.payment_request_status_error_generic, message)

   statusText.text = getString(R.string.payment_request_status_failed, errorMessage)
   Toast.makeText(
       this,
       getString(R.string.payment_request_status_failed, errorMessage),
       Toast.LENGTH_LONG
   ).show()
   ```

4. Mirror all keys in `values-es/strings.xml` with Spanish translations.

### 4.2. Balance check

File: `BalanceCheckActivity.kt`

1. Define in `values/strings.xml`:

   ```xml
   <!-- Balance check -->
   <string name="balance_check_status_balance">Balance: %1$s</string>
   <string name="balance_check_status_error">Error: %1$s</string>
   ```

2. Use in code:

   ```kotlin
   balanceDisplay.text = getString(
       R.string.balance_check_status_balance,
       Amount(balance, Amount.Currency.BTC).toString()
   )

   balanceDisplay.text = getString(R.string.balance_check_status_error, message)
   ```

3. Spanish equivalents:

   ```xml
   <string name="balance_check_status_balance">Saldo: %1$s</string>
   <string name="balance_check_status_error">Error: %1$s</string>
   ```

### 4.3. Onboarding flow

File: `feature/onboarding/OnboardingActivity.kt`

This has many strings; treat it as its own sub‑task.

1. Group strings into sections in `values/strings.xml`:

   ```xml
   <!-- Onboarding statuses -->
   <string name="onboarding_status_creating_wallet">Creating your wallet...</string>
   <string name="onboarding_status_generating_seed">Generating seed phrase...</string>
   <string name="onboarding_status_setting_up_mints">Setting up default mints...</string>
   <string name="onboarding_status_initializing_wallet">Initializing wallet...</string>
   <string name="onboarding_status_connecting_mints">Connecting to mints...</string>
   <string name="onboarding_status_fetching_mints">Fetching mint information...</string>

   <!-- Onboarding seed validation -->
   <string name="onboarding_seed_invalid_characters">
       Invalid characters detected
   </string>
   <string name="onboarding_seed_words_entered_count">
       %1$d of 12 words entered
   </string>
   <string name="onboarding_seed_ready_to_continue">Ready to continue</string>

   <!-- Onboarding backup/mints -->
   <string name="onboarding_fetching_searching_backup">
       Searching for backup on Nostr...
   </string>
   <string name="onboarding_restoring_initializing">
       Initializing restore...
   </string>
   <string name="onboarding_backup_found_title">Backup Found</string>
   <string name="onboarding_backup_found_subtitle">
       Last backed up %1$s
   </string>
   <string name="onboarding_backup_not_found_title">No Backup Found</string>
   <string name="onboarding_backup_not_found_subtitle">
       Using default mints
   </string>
   <string name="onboarding_mints_subtitle_restore">
       Select which mints to restore
   </string>
   <string name="onboarding_mints_subtitle_description">
       These mints will store your ecash tokens
   </string>
   <string name="onboarding_mints_button_restore_wallet">
       Restore Wallet
   </string>
   <string name="onboarding_mints_button_continue">
       Continue
   </string>
   <string name="onboarding_mints_count_selected">
       %1$d mint%2$s selected
   </string>
   <string name="onboarding_status_waiting">Waiting...</string>

   <!-- Onboarding success -->
   <string name="onboarding_success_restored_title">Wallet Restored</string>
   <string name="onboarding_success_restored_subtitle">
       Recovered %1$s sats
   </string>
   <string name="onboarding_success_created_title">Wallet Created</string>
   <string name="onboarding_success_created_subtitle">
       Your wallet is ready to use
   </string>

   <!-- Clipboard / seed paste -->
   <string name="onboarding_clipboard_empty">Clipboard is empty</string>
   <string name="onboarding_seed_paste_invalid">
       Please paste a valid 12-word seed phrase
   </string>
   <string name="onboarding_seed_paste_success">Seed phrase pasted</string>
   ```

2. Replace all instances like `generatingStatus.text = "Creating your wallet..."` and the related toasts with these `getString(...)` calls.
3. Add Spanish translations in `values-es/strings.xml`.

### 4.4. Tips / tip selection

Files:

- `activity_tips_settings.xml` (layouts – already handled in Section 3)
- `TipsSettingsActivity.kt`
- `TipSelectionActivity.kt`
- Any custom tip views (e.g. `TipSelectionView`)

Actions:

1. Extract texts like:
   - `"Enter tip amount"`, `"Would you like to add a tip?"`,
     `"Custom Tip"`, `"No Tip"`
   - `"Switch to ${entryCurrency.symbol}"`, `"Switch to ₿"`, etc.

2. Create resources:

   ```xml
   <string name="tip_selection_enter_tip_amount">Enter tip amount</string>
   <string name="tip_selection_question_add_tip">Would you like to add a tip?</string>
   <string name="tip_selection_label_custom_tip">Custom Tip</string>
   <string name="tip_selection_button_no_tip">No Tip</string>
   <string name="tip_selection_switch_to_currency">Switch to %1$s</string>
   ```

3. Use in code:

   ```kotlin
   questionText.text = getString(R.string.tip_selection_question_add_tip)
   customCurrencyToggle.text = getString(
       R.string.tip_selection_switch_to_currency,
       entryCurrency.symbol
   )
   ```

4. Add Spanish translations.

### 4.5. Top-up, NFC, withdraw

Files:

- `TopUpActivity.kt`
- `payment/NfcPaymentProcessor.kt`
- `WithdrawLightningActivity.kt`
- `WithdrawMeltQuoteActivity.kt`
- `WithdrawSuccessActivity.kt`

Actions:

- Extract strings like:
  - Dialog titles: `"Enter PIN"`, `"Scan Card Again"`, `"Processing Import"`
  - Status: `"Ready to import proofs"`, `"Importing proofs..."`,
    `"PIN accepted. Please scan your card again to complete import."`
  - Generic `"Cancel"`, `"OK"` → use `common_cancel`, `common_ok`.

- Define resources:

  ```xml
  <string name="dialog_title_enter_pin">Enter PIN</string>
  <string name="top_up_ready_to_import_proofs">Ready to import proofs</string>
  <string name="top_up_scan_card_again">Scan Card Again</string>
  <string name="top_up_processing_import">Processing Import</string>
  <string name="top_up_importing_proofs">Importing proofs...</string>
  <string name="top_up_message_pin_accepted">
      PIN accepted. Please scan your card again to complete import.
  </string>
  <string name="common_ok">OK</string>
  <string name="common_cancel">Cancel</string>
  ```

- Replace all direct `"OK"` / `"Cancel"` uses with `R.string.common_ok` / `R.string.common_cancel` and use the top‑up strings for NFC dialogues and status text.

- Add Spanish translations.

### 4.6. Items / catalog

Files:

- `ItemEntryActivity.kt`
- `ItemListActivity.kt`
- `ItemSelectionActivity.kt`
- `SelectionItemsAdapter.kt`
- `CategoryTagHandler.kt`

Typical strings:

- Titles & labels: `"Edit Item"`, `"Delete Item"`, `"+ Add New"`
- Stock text:

  ```kotlin
  stockView.text = "${item.quantity} in stock"
  ```

Create resources, e.g.:

```xml
<string name="items_edit_item_title">Edit Item</string>
<string name="items_delete_item">Delete Item</string>
<string name="category_add_new">+ Add New</string>
<string name="items_quantity_in_stock">%1$d in stock</string>
```

Then use `getString(...)` in code and add Spanish translations.

### 4.7. History & receipts

Files:

- `BasketReceiptActivity.kt`
- `TransactionDetailActivity.kt`

Strings to externalize:

- Payment types: `"Lightning Payment"`, `"Cashu Payment"`, `"Payment Received"`
- Mint details: `"From $mintName"`, `"Unknown"`
- Basket labels: `"Total"`, `"Total Paid"`, `"Fiat Subtotal (net)"`, `"VAT ($rate%)"`, etc.

Example resources:

```xml
<string name="history_payment_type_lightning">Lightning Payment</string>
<string name="history_payment_type_cashu">Cashu Payment</string>
<string name="history_payment_type_received">Payment Received</string>
<string name="history_from_mint">From %1$s</string>
<string name="history_unknown_mint">Unknown</string>
<string name="history_total_label">Total</string>
<string name="history_total_paid_label">Total Paid</string>
<string name="history_vat_label">VAT (%1$s)</string>
<string name="history_items_header_single">1 ITEM</string>
```

Wire these into the corresponding Kotlin code and add Spanish versions.

---

## 5. Add and maintain Spanish translations

As you add keys to `values/strings.xml`, **immediately** add Spanish entries to `values-es/strings.xml`.

- Keep placeholders identical: `%1$s`, `%1$d`, `%2$s`, etc.
- For plurals, ensure the same structure in both locales.

Example:

```xml
<!-- values/strings.xml -->
<plurals name="items_quantity_label">
    <item quantity="one">%1$d item</item>
    <item quantity="other">%1$d items</item>
</plurals>

<!-- values-es/strings.xml -->
<plurals name="items_quantity_label">
    <item quantity="one">%1$d artículo</item>
    <item quantity="other">%1$d artículos</item>
</plurals>
```

---

## 6. Verification

### 6.1. Lint: HardcodedText

In Android Studio:

1. `Analyze → Inspect Code…`
2. Scope: `app` module.
3. Inspection: **Android → Lint → Correctness → HardcodedText**.

Fix any remaining user‑visible hardcoded strings. For unavoidable cases, add a suppression with a comment explaining why.

### 6.2. Command-line sanity checks

Re‑run the searches:

```bash
rg 'android:(text|hint|title|contentDescription)="' app/src/main/res/layout -n | rg -v '@string'

rg 'Toast\\.makeText\\(|\\.setTitle\\("|\\.setMessage\\("|text\\s*=\\s*"[^"]*[A-Za-z][^"]*"' \
   app/src/main/java/com/electricdreams/numo -n
```

Confirm remaining hits are only:

- non‑user‑visible strings,
- debug/log messages,
- or numeric/symbolic placeholders.

### 6.3. Manual QA in both languages

1. Run the app in **English** (default):
   - Exercise all main flows: onboarding, settings, item entry, POS, tips, payment request/received, history, receipts.
2. Switch device/emulator to **Español (España/Latinoamérica)**:
   - Repeat the same flows.
   - Verify:
     - No English text remains.
     - Strings fit within UI; adjust wording if truncated.

---

## 7. Make it stick

- Add to your PR checklist:
  - **“No new hardcoded user‑visible strings in layouts or code.”**
- Optionally add a pre‑merge check (script or CI job) that runs the two `rg` commands and fails if any hits remain.
- Keep `docs/i18n_layout_todo_numo.txt` and `docs/i18n_code_todo_numo.txt` around while there are still remaining items; once they’re empty, you can delete or archive them.
