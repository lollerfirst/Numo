package com.electricdreams.shellshock.feature.settings

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.gridlayout.widget.GridLayout
import androidx.lifecycle.lifecycleScope
import com.electricdreams.shellshock.R
import com.electricdreams.shellshock.core.cashu.CashuWalletManager
import com.electricdreams.shellshock.core.util.MintManager
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity for restoring wallet from a 12-word seed phrase.
 * Features:
 * - 12 input fields for entering seed words
 * - Paste from clipboard functionality
 * - Real-time validation
 * - Progress UI during restore with per-mint status
 * - Balance comparison before/after restore
 */
class RestoreWalletActivity : AppCompatActivity() {

    private lateinit var seedInputGrid: GridLayout
    private lateinit var pasteButton: MaterialButton
    private lateinit var restoreButton: MaterialButton
    private lateinit var validationStatus: LinearLayout
    private lateinit var validationIcon: ImageView
    private lateinit var validationText: TextView
    private lateinit var progressOverlay: FrameLayout
    private lateinit var progressStatus: TextView
    private lateinit var mintProgressContainer: LinearLayout
    private lateinit var successOverlay: FrameLayout
    private lateinit var balanceChangesContainer: LinearLayout
    private lateinit var successSummaryText: TextView
    private lateinit var doneButton: MaterialButton

    private val seedInputs = mutableListOf<EditText>()
    private val mintProgressViews = mutableMapOf<String, View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restore_wallet)

        initViews()
        setupSeedInputs()
        setupListeners()
    }

    private fun initViews() {
        seedInputGrid = findViewById(R.id.seed_input_grid)
        pasteButton = findViewById(R.id.paste_button)
        restoreButton = findViewById(R.id.restore_button)
        validationStatus = findViewById(R.id.validation_status)
        validationIcon = findViewById(R.id.validation_icon)
        validationText = findViewById(R.id.validation_text)
        progressOverlay = findViewById(R.id.restore_progress_overlay)
        progressStatus = findViewById(R.id.restore_progress_status)
        mintProgressContainer = findViewById(R.id.mint_progress_container)
        successOverlay = findViewById(R.id.restore_success_overlay)
        balanceChangesContainer = findViewById(R.id.balance_changes_container)
        successSummaryText = findViewById(R.id.success_summary_text)
        doneButton = findViewById(R.id.done_button)

        findViewById<View>(R.id.back_button).setOnClickListener { 
            finish() 
        }
    }

    private fun setupSeedInputs() {
        seedInputGrid.removeAllViews()
        seedInputs.clear()

        for (i in 0 until 12) {
            val inputContainer = createSeedInputView(i + 1)
            
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(i % 2, 1f)
                rowSpec = GridLayout.spec(i / 2)
                setMargins(4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 4.dpToPx())
            }
            inputContainer.layoutParams = params
            
            seedInputGrid.addView(inputContainer)
        }
    }

    private fun createSeedInputView(index: Int): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_seed_input)
            setPadding(12.dpToPx(), 10.dpToPx(), 12.dpToPx(), 10.dpToPx())
        }

        val indexText = TextView(this).apply {
            text = "$index"
            setTextColor(ContextCompat.getColor(context, R.color.color_text_tertiary))
            textSize = 13f
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            layoutParams = LinearLayout.LayoutParams(24.dpToPx(), LinearLayout.LayoutParams.WRAP_CONTENT)
            gravity = android.view.Gravity.CENTER
        }

        val input = EditText(this).apply {
            hint = ""
            setTextColor(ContextCompat.getColor(context, R.color.color_text_primary))
            setHintTextColor(ContextCompat.getColor(context, R.color.color_text_tertiary))
            textSize = 15f
            typeface = android.graphics.Typeface.MONOSPACE
            background = null
            isSingleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT or 
                       android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
                       android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            imeOptions = if (index == 12) EditorInfo.IME_ACTION_DONE else EditorInfo.IME_ACTION_NEXT
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 8.dpToPx()
            }

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    validateInputs()
                }
            })

            setOnFocusChangeListener { _, hasFocus ->
                container.background = ContextCompat.getDrawable(
                    context,
                    if (hasFocus) R.drawable.bg_seed_input_focused else R.drawable.bg_seed_input
                )
            }
        }

        seedInputs.add(input)

        container.addView(indexText)
        container.addView(input)

        return container
    }

    private fun setupListeners() {
        pasteButton.setOnClickListener {
            pasteFromClipboard()
        }

        restoreButton.setOnClickListener {
            showRestoreConfirmationDialog()
        }

        doneButton.setOnClickListener {
            finish()
        }
    }

    private fun pasteFromClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        
        if (clipData == null || clipData.itemCount == 0) {
            Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val pastedText = clipData.getItemAt(0).text?.toString()?.trim() ?: ""
        val words = pastedText.split("\\s+".toRegex()).filter { it.isNotBlank() }

        if (words.size != 12) {
            Toast.makeText(this, "Please paste a valid 12-word seed phrase", Toast.LENGTH_LONG).show()
            return
        }

        // Fill all inputs
        words.forEachIndexed { index, word ->
            if (index < seedInputs.size) {
                seedInputs[index].setText(word.lowercase())
            }
        }

        validateInputs()
        Toast.makeText(this, "Seed phrase pasted", Toast.LENGTH_SHORT).show()
    }

    private fun validateInputs(): Boolean {
        val words = seedInputs.map { it.text.toString().trim().lowercase() }
        val filledCount = words.count { it.isNotBlank() }
        val allFilled = filledCount == 12
        val allValid = words.all { it.isBlank() || it.matches(Regex("^[a-z]+$")) }

        // Update validation status
        when {
            filledCount == 0 -> {
                validationStatus.visibility = View.GONE
            }
            !allValid -> {
                validationStatus.visibility = View.VISIBLE
                validationIcon.setImageResource(R.drawable.ic_close)
                validationIcon.setColorFilter(ContextCompat.getColor(this, R.color.color_warning_red))
                validationText.text = "Invalid characters detected"
                validationText.setTextColor(ContextCompat.getColor(this, R.color.color_warning_red))
            }
            !allFilled -> {
                validationStatus.visibility = View.VISIBLE
                validationIcon.setImageResource(R.drawable.ic_warning)
                validationIcon.setColorFilter(ContextCompat.getColor(this, R.color.color_warning))
                validationText.text = "$filledCount of 12 words entered"
                validationText.setTextColor(ContextCompat.getColor(this, R.color.color_warning))
            }
            else -> {
                validationStatus.visibility = View.VISIBLE
                validationIcon.setImageResource(R.drawable.ic_check)
                validationIcon.setColorFilter(ContextCompat.getColor(this, R.color.color_success_green))
                validationText.text = "Ready to restore"
                validationText.setTextColor(ContextCompat.getColor(this, R.color.color_success_green))
            }
        }

        // Update restore button state
        val canRestore = allFilled && allValid
        restoreButton.isEnabled = canRestore
        restoreButton.alpha = if (canRestore) 1f else 0.5f

        return canRestore
    }

    private fun showRestoreConfirmationDialog() {
        val currentMnemonic = CashuWalletManager.getMnemonic()
        val mintManager = MintManager.getInstance(this)
        val mintCount = mintManager.getAllowedMints().size

        val message = buildString {
            append("This will replace your current wallet with the one from the entered seed phrase.\n\n")
            append("⚠️ IMPORTANT:\n")
            append("• Make sure you have backed up your current seed phrase\n")
            append("• This action cannot be undone\n")
            append("• Restore will be attempted for $mintCount configured mint(s)\n\n")
            if (currentMnemonic != null) {
                append("Your current seed phrase starts with: \"${currentMnemonic.split(" ").take(3).joinToString(" ")}...\"")
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm Wallet Restore")
            .setMessage(message)
            .setPositiveButton("I've Backed Up, Restore") { _, _ ->
                performRestore()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performRestore() {
        val words = seedInputs.map { it.text.toString().trim().lowercase() }
        val newMnemonic = words.joinToString(" ")

        // Show progress overlay
        progressOverlay.visibility = View.VISIBLE
        mintProgressContainer.visibility = View.VISIBLE
        mintProgressContainer.removeAllViews()
        mintProgressViews.clear()

        // Initialize mint progress items
        val mintManager = MintManager.getInstance(this)
        val mints = mintManager.getAllowedMints()

        mints.forEach { mintUrl ->
            val progressView = createMintProgressView(mintUrl)
            mintProgressContainer.addView(progressView)
            mintProgressViews[mintUrl] = progressView
        }

        lifecycleScope.launch {
            try {
                progressStatus.text = "Initializing restore..."

                val balanceChanges = CashuWalletManager.restoreFromMnemonic(newMnemonic) { mintUrl, status, balanceBefore, balanceAfter ->
                    withContext(Dispatchers.Main) {
                        updateMintProgress(mintUrl, status, balanceBefore, balanceAfter)
                    }
                }

                withContext(Dispatchers.Main) {
                    showSuccess(balanceChanges)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressOverlay.visibility = View.GONE
                    Toast.makeText(
                        this@RestoreWalletActivity, 
                        "Restore failed: ${e.message}", 
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun createMintProgressView(mintUrl: String): View {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.item_mint_restore_progress, mintProgressContainer, false)
        
        val mintName = view.findViewById<TextView>(R.id.mint_name)
        mintName.text = extractMintName(mintUrl)
        
        return view
    }

    private fun updateMintProgress(mintUrl: String, status: String, balanceBefore: Long, balanceAfter: Long) {
        val view = mintProgressViews[mintUrl] ?: return
        
        val spinner = view.findViewById<ProgressBar>(R.id.mint_spinner)
        val statusIcon = view.findViewById<ImageView>(R.id.mint_status_icon)
        val mintStatus = view.findViewById<TextView>(R.id.mint_status)
        val balanceChange = view.findViewById<TextView>(R.id.balance_change)

        mintStatus.text = status

        when {
            status == "Complete" -> {
                spinner.visibility = View.GONE
                statusIcon.visibility = View.VISIBLE
                statusIcon.setImageResource(R.drawable.ic_check)
                statusIcon.setColorFilter(ContextCompat.getColor(this, R.color.color_success_green))
                
                val diff = balanceAfter - balanceBefore
                if (diff != 0L) {
                    balanceChange.visibility = View.VISIBLE
                    balanceChange.text = if (diff >= 0) "+$diff sats" else "$diff sats"
                    balanceChange.setTextColor(
                        ContextCompat.getColor(
                            this, 
                            if (diff >= 0) R.color.color_success_green else R.color.color_warning_red
                        )
                    )
                }
            }
            status.startsWith("Failed") -> {
                spinner.visibility = View.GONE
                statusIcon.visibility = View.VISIBLE
                statusIcon.setImageResource(R.drawable.ic_close)
                statusIcon.setColorFilter(ContextCompat.getColor(this, R.color.color_warning_red))
            }
            else -> {
                spinner.visibility = View.VISIBLE
                statusIcon.visibility = View.GONE
            }
        }
    }

    private fun showSuccess(balanceChanges: Map<String, Pair<Long, Long>>) {
        progressOverlay.visibility = View.GONE
        successOverlay.visibility = View.VISIBLE

        // Calculate totals
        val totalBefore = balanceChanges.values.sumOf { it.first }
        val totalAfter = balanceChanges.values.sumOf { it.second }
        val totalDiff = totalAfter - totalBefore

        successSummaryText.text = when {
            totalDiff > 0 -> "Recovered $totalDiff sats across ${balanceChanges.size} mint(s)"
            totalDiff < 0 -> "Balance changed by $totalDiff sats"
            else -> "Wallet restored with ${totalAfter} sats total"
        }

        // Show balance changes per mint
        balanceChangesContainer.removeAllViews()
        balanceChanges.forEach { (mintUrl, balances) ->
            val (before, after) = balances
            val diff = after - before
            
            val itemView = createBalanceChangeItem(mintUrl, before, after, diff)
            balanceChangesContainer.addView(itemView)
        }
    }

    private fun createBalanceChangeItem(mintUrl: String, before: Long, after: Long, diff: Long): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(16.dpToPx(), 12.dpToPx(), 16.dpToPx(), 12.dpToPx())
            background = ContextCompat.getDrawable(context, R.drawable.bg_seed_word)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8.dpToPx()
            }
        }

        val infoContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val nameText = TextView(this).apply {
            text = extractMintName(mintUrl)
            setTextColor(ContextCompat.getColor(context, R.color.color_text_primary))
            textSize = 14f
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }

        val detailText = TextView(this).apply {
            text = "$before → $after sats"
            setTextColor(ContextCompat.getColor(context, R.color.color_text_secondary))
            textSize = 13f
        }

        infoContainer.addView(nameText)
        infoContainer.addView(detailText)

        val diffText = TextView(this).apply {
            text = if (diff >= 0) "+$diff" else "$diff"
            setTextColor(
                ContextCompat.getColor(
                    context,
                    when {
                        diff > 0 -> R.color.color_success_green
                        diff < 0 -> R.color.color_warning_red
                        else -> R.color.color_text_secondary
                    }
                )
            )
            textSize = 15f
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        }

        container.addView(infoContainer)
        container.addView(diffText)

        return container
    }

    private fun extractMintName(mintUrl: String): String {
        return try {
            val url = java.net.URL(mintUrl)
            url.host
        } catch (e: Exception) {
            mintUrl.take(30)
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
