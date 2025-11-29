package com.electricdreams.numo.feature.settings

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.electricdreams.numo.R
import com.electricdreams.numo.core.cashu.CashuWalletManager
import com.electricdreams.numo.core.model.Amount
import com.electricdreams.numo.core.util.MintIconCache
import com.electricdreams.numo.core.util.MintManager
import com.electricdreams.numo.feature.scanner.QRScannerActivity
import com.electricdreams.numo.ui.components.AddMintInputCard
import com.electricdreams.numo.ui.components.MintListItem
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Clean, Material Design mint management screen.
 * Features a prominent "Lightning Mint" hero card showing the primary mint for receiving payments,
 * with a clean list of all mints below.
 */
class MintsSettingsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MintsSettings"
        private const val PREF_LIGHTNING_MINT = "lightning_mint_url"
    }

    // Views
    private lateinit var backButton: ImageButton
    private lateinit var resetButton: ImageButton
    private lateinit var lightningMintSection: View
    private lateinit var lightningMintCard: MaterialCardView
    private lateinit var lightningMintIcon: ImageView
    private lateinit var lightningMintName: TextView
    private lateinit var lightningMintUrlText: TextView
    private lateinit var lightningMintBalance: TextView
    private lateinit var allMintsHeader: TextView
    private lateinit var mintsContainer: LinearLayout
    private lateinit var addMintCard: AddMintInputCard
    private lateinit var emptyState: View

    // State
    private lateinit var mintManager: MintManager
    private var mintBalances = mutableMapOf<String, Long>()
    private var selectedLightningMint: String? = null
    private val mintItems = mutableMapOf<String, MintListItem>()

    // QR Scanner launcher
    private val qrScannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val qrValue = result.data?.getStringExtra(QRScannerActivity.EXTRA_QR_VALUE)
            qrValue?.let { url ->
                addMintCard.setMintUrl(normalizeUrl(url))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mints_settings)

        MintIconCache.initialize(this)
        mintManager = MintManager.getInstance(this)

        // Load saved lightning mint preference
        selectedLightningMint = getPreferences(MODE_PRIVATE).getString(PREF_LIGHTNING_MINT, null)

        initViews()
        setupListeners()
        loadMintsAndBalances()
        startEntranceAnimations()
    }

    override fun onResume() {
        super.onResume()
        refreshBalances()
    }

    private fun initViews() {
        backButton = findViewById(R.id.back_button)
        resetButton = findViewById(R.id.reset_button)
        lightningMintSection = findViewById(R.id.lightning_mint_section)
        lightningMintCard = findViewById(R.id.lightning_mint_card)
        lightningMintIcon = findViewById(R.id.lightning_mint_icon)
        lightningMintName = findViewById(R.id.lightning_mint_name)
        lightningMintUrlText = findViewById(R.id.lightning_mint_url)
        lightningMintBalance = findViewById(R.id.lightning_mint_balance)
        allMintsHeader = findViewById(R.id.all_mints_header)
        mintsContainer = findViewById(R.id.mints_container)
        addMintCard = findViewById(R.id.add_mint_card)
        emptyState = findViewById(R.id.empty_state)
    }

    private fun setupListeners() {
        backButton.setOnClickListener { 
            finish() 
        }

        resetButton.setOnClickListener {
            showResetConfirmation()
        }

        addMintCard.setOnAddMintListener(object : AddMintInputCard.OnAddMintListener {
            override fun onAddMint(mintUrl: String) {
                addNewMint(mintUrl)
            }

            override fun onScanQR() {
                openQRScanner()
            }
        })
    }

    private fun openQRScanner() {
        val intent = Intent(this, QRScannerActivity::class.java).apply {
            putExtra(QRScannerActivity.EXTRA_TITLE, getString(R.string.mints_scan_mint_qr))
            putExtra(QRScannerActivity.EXTRA_INSTRUCTION, getString(R.string.mints_scan_instruction))
        }
        qrScannerLauncher.launch(intent)
    }

    private fun loadMintsAndBalances() {
        val mints = mintManager.getAllowedMints()
        
        if (mints.isEmpty()) {
            showEmptyState()
            return
        }

        lifecycleScope.launch {
            // Load balances
            val balances = withContext(Dispatchers.IO) {
                CashuWalletManager.getAllMintBalances()
            }
            mintBalances.clear()
            mintBalances.putAll(balances)
            
            // Auto-select lightning mint if none selected
            if (selectedLightningMint == null || !mints.contains(selectedLightningMint)) {
                // Select mint with highest balance
                val highestBalanceMint = mints.maxByOrNull { mintBalances[it] ?: 0L }
                highestBalanceMint?.let { setLightningMint(it, animate = false) }
            }
            
            // Build UI
            buildMintList(mints)
            updateLightningMintCard()
            
            // Refresh stale mint info
            refreshStaleMintInfo()
        }
    }

    private fun refreshBalances() {
        lifecycleScope.launch {
            val balances = withContext(Dispatchers.IO) {
                CashuWalletManager.getAllMintBalances()
            }
            mintBalances.clear()
            mintBalances.putAll(balances)
            
            // Update UI
            val mints = mintManager.getAllowedMints()
            buildMintList(mints)
            updateLightningMintCard()
        }
    }

    private fun buildMintList(mints: List<String>) {
        mintsContainer.removeAllViews()
        mintItems.clear()

        if (mints.isEmpty()) {
            showEmptyState()
            return
        }

        hideEmptyState()
        
        // Sort by balance (highest first)
        val sortedMints = mints.sortedByDescending { mintBalances[it] ?: 0L }
        
        sortedMints.forEachIndexed { index, mintUrl ->
            val item = MintListItem(this)
            val balance = mintBalances[mintUrl] ?: 0L
            val isPrimary = mintUrl == selectedLightningMint

            item.bind(mintUrl, balance, isPrimary)
            
            item.setOnMintItemListener(object : MintListItem.OnMintItemListener {
                override fun onMintTapped(url: String) {
                    setLightningMint(url, animate = true)
                }

                override fun onMintLongPressed(url: String): Boolean {
                    showRemoveConfirmation(url)
                    return true
                }
            })

            mintsContainer.addView(item)
            mintItems[mintUrl] = item

            // Staggered entrance animation
            item.animateEntrance(index * 60L)
        }
    }

    private fun setLightningMint(mintUrl: String, animate: Boolean) {
        val previousMint = selectedLightningMint
        selectedLightningMint = mintUrl
        
        // Save preference
        getPreferences(MODE_PRIVATE).edit()
            .putString(PREF_LIGHTNING_MINT, mintUrl)
            .apply()
        
        // Update list items
        previousMint?.let { prev ->
            mintItems[prev]?.updatePrimaryState(false, animate)
        }
        mintItems[mintUrl]?.updatePrimaryState(true, animate)
        
        // Update hero card
        updateLightningMintCard()
        
        if (animate) {
            Toast.makeText(this, R.string.mints_lightning_changed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLightningMintCard() {
        val url = selectedLightningMint
        if (url == null) {
            lightningMintSection.visibility = View.GONE
            return
        }
        
        lightningMintSection.visibility = View.VISIBLE
        
        val displayName = mintManager.getMintDisplayName(url)
        val shortUrl = url.removePrefix("https://").removePrefix("http://")
        val balance = mintBalances[url] ?: 0L
        
        lightningMintName.text = displayName
        lightningMintUrlText.text = shortUrl
        lightningMintBalance.text = Amount(balance, Amount.Currency.BTC).toString()
        
        // Load icon
        loadLightningMintIcon(url)
    }

    private fun loadLightningMintIcon(url: String) {
        val cachedFile = MintIconCache.getCachedIconFile(url)
        if (cachedFile != null) {
            try {
                val bitmap = BitmapFactory.decodeFile(cachedFile.absolutePath)
                if (bitmap != null) {
                    lightningMintIcon.setImageBitmap(bitmap)
                    return
                }
            } catch (e: Exception) {
                // Fall through to default
            }
        }
        
        lightningMintIcon.setImageResource(R.drawable.ic_bitcoin)
    }

    private fun showRemoveConfirmation(mintUrl: String) {
        val displayName = mintManager.getMintDisplayName(mintUrl)
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.mints_remove_title))
            .setMessage(getString(R.string.mints_remove_message, displayName))
            .setPositiveButton(getString(R.string.mints_remove_confirm)) { _, _ ->
                removeMint(mintUrl)
            }
            .setNegativeButton(getString(R.string.common_cancel), null)
            .show()
    }

    private fun removeMint(mintUrl: String) {
        val item = mintItems[mintUrl] ?: return
        
        item.animateRemoval {
            mintManager.removeMint(mintUrl)
            mintBalances.remove(mintUrl)
            mintItems.remove(mintUrl)
            mintsContainer.removeView(item)
            
            // If removed mint was lightning mint, select new one
            if (selectedLightningMint == mintUrl) {
                val mints = mintManager.getAllowedMints()
                val newLightning = mints.maxByOrNull { mintBalances[it] ?: 0L }
                if (newLightning != null) {
                    setLightningMint(newLightning, animate = false)
                } else {
                    selectedLightningMint = null
                    lightningMintSection.visibility = View.GONE
                }
            }
            
            if (mintManager.getAllowedMints().isEmpty()) {
                showEmptyState()
            }
            
            Toast.makeText(this, getString(R.string.mints_removed_toast), Toast.LENGTH_SHORT).show()
        }
    }

    private fun addNewMint(rawUrl: String) {
        val mintUrl = normalizeUrl(rawUrl)
        
        if (mintManager.getAllowedMints().contains(mintUrl)) {
            Toast.makeText(this, getString(R.string.mints_already_exists), Toast.LENGTH_SHORT).show()
            return
        }

        addMintCard.setLoading(true)

        lifecycleScope.launch {
            val isValid = validateMintUrl(mintUrl)
            
            if (!isValid) {
                addMintCard.setLoading(false)
                Toast.makeText(
                    this@MintsSettingsActivity,
                    getString(R.string.mints_invalid_url),
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            val added = mintManager.addMint(mintUrl)
            if (added) {
                fetchAndStoreMintInfo(mintUrl)
                loadMintsAndBalances()
                addMintCard.clearInput()
                addMintCard.collapseIfExpanded()
                
                Toast.makeText(
                    this@MintsSettingsActivity,
                    getString(R.string.mints_added_toast),
                    Toast.LENGTH_SHORT
                ).show()
            }
            
            addMintCard.setLoading(false)
        }
    }

    private fun normalizeUrl(rawUrl: String): String {
        var url = rawUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        if (url.endsWith("/")) {
            url = url.dropLast(1)
        }
        return url
    }

    private suspend fun validateMintUrl(url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val infoUrl = "$url/v1/info"
            Log.d(TAG, "Validating mint: $infoUrl")
            
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            
            val request = Request.Builder()
                .url(infoUrl)
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val isValid = response.isSuccessful && response.code == 200
            response.close()
            isValid
        } catch (e: Exception) {
            Log.e(TAG, "Mint validation error: ${e.message}")
            false
        }
    }

    private suspend fun fetchAndStoreMintInfo(mintUrl: String) {
        withContext(Dispatchers.IO) {
            val info = CashuWalletManager.fetchMintInfo(mintUrl)
            if (info != null) {
                val json = CashuWalletManager.mintInfoToJson(info)
                mintManager.setMintInfo(mintUrl, json)
                mintManager.setMintRefreshTimestamp(mintUrl)
                
                info.iconUrl?.let { iconUrl ->
                    if (iconUrl.isNotEmpty()) {
                        MintIconCache.downloadAndCacheIcon(mintUrl, iconUrl)
                    }
                }
            }
        }
    }

    private fun refreshStaleMintInfo() {
        lifecycleScope.launch {
            val mintsToRefresh = mintManager.getMintsNeedingRefresh()
            for (mintUrl in mintsToRefresh) {
                fetchAndStoreMintInfo(mintUrl)
            }
            if (mintsToRefresh.isNotEmpty()) {
                updateLightningMintCard()
            }
        }
    }

    private fun showResetConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.mints_reset_title))
            .setMessage(getString(R.string.mints_reset_message))
            .setPositiveButton(getString(R.string.mints_reset_confirm)) { _, _ ->
                resetToDefaults()
            }
            .setNegativeButton(getString(R.string.common_cancel), null)
            .show()
    }

    private fun resetToDefaults() {
        mintManager.resetToDefaults()
        selectedLightningMint = null
        getPreferences(MODE_PRIVATE).edit().remove(PREF_LIGHTNING_MINT).apply()
        loadMintsAndBalances()
        Toast.makeText(this, getString(R.string.mints_reset_toast), Toast.LENGTH_SHORT).show()
    }

    private fun showEmptyState() {
        emptyState.visibility = View.VISIBLE
        mintsContainer.visibility = View.GONE
        lightningMintSection.visibility = View.GONE
        allMintsHeader.visibility = View.GONE
    }

    private fun hideEmptyState() {
        emptyState.visibility = View.GONE
        mintsContainer.visibility = View.VISIBLE
        allMintsHeader.visibility = View.VISIBLE
    }

    private fun startEntranceAnimations() {
        // Lightning mint section slide in
        lightningMintSection.alpha = 0f
        lightningMintSection.translationY = -30f
        lightningMintSection.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(350)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Add mint card entrance
        addMintCard.animateEntrance(400)
    }
}
