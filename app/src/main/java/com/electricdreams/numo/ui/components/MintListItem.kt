package com.electricdreams.numo.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.electricdreams.numo.R
import com.electricdreams.numo.core.model.Amount
import com.electricdreams.numo.core.util.MintIconCache
import com.electricdreams.numo.core.util.MintManager
import com.google.android.material.card.MaterialCardView

/**
 * A clean, Material Design mint list item.
 * Supports selection state for choosing the Lightning receive mint.
 */
class MintListItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface OnMintItemListener {
        fun onMintTapped(mintUrl: String)
        fun onMintLongPressed(mintUrl: String): Boolean
    }

    private val card: MaterialCardView
    private val iconView: ImageView
    private val nameText: TextView
    private val urlText: TextView
    private val balanceText: TextView
    private val checkIcon: ImageView
    
    private var mintUrl: String = ""
    private var listener: OnMintItemListener? = null
    private var isSelectedAsPrimary = false

    init {
        LayoutInflater.from(context).inflate(R.layout.component_mint_list_item, this, true)
        
        card = findViewById(R.id.mint_card)
        iconView = findViewById(R.id.mint_icon)
        nameText = findViewById(R.id.mint_name)
        urlText = findViewById(R.id.mint_url)
        balanceText = findViewById(R.id.mint_balance)
        checkIcon = findViewById(R.id.check_icon)
        
        setupClickListeners()
    }

    private fun setupClickListeners() {
        card.setOnClickListener {
            animateTap()
            listener?.onMintTapped(mintUrl)
        }
        
        card.setOnLongClickListener {
            listener?.onMintLongPressed(mintUrl) ?: false
        }
    }

    fun bind(url: String, balance: Long, isPrimary: Boolean = false) {
        mintUrl = url
        isSelectedAsPrimary = isPrimary
        
        // Get mint info
        val mintManager = MintManager.getInstance(context)
        val displayName = mintManager.getMintDisplayName(url)
        val shortUrl = url.removePrefix("https://").removePrefix("http://")
        
        nameText.text = displayName
        urlText.text = shortUrl
        balanceText.text = Amount(balance, Amount.Currency.BTC).toString()
        
        // Update selection state
        updatePrimaryState(isPrimary, animate = false)
        
        // Load icon
        loadIcon(url)
    }

    private fun loadIcon(url: String) {
        // Try to load cached icon file
        val cachedFile = MintIconCache.getCachedIconFile(url)
        if (cachedFile != null) {
            try {
                val bitmap = BitmapFactory.decodeFile(cachedFile.absolutePath)
                if (bitmap != null) {
                    iconView.setImageBitmap(bitmap)
                    return
                }
            } catch (e: Exception) {
                // Fall through to default
            }
        }
        
        // Set default icon
        iconView.setImageResource(R.drawable.ic_bitcoin)
    }

    private fun animateIconLoad() {
        iconView.alpha = 0f
        iconView.scaleX = 0.8f
        iconView.scaleY = 0.8f
        iconView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    fun updatePrimaryState(isPrimary: Boolean, animate: Boolean = true) {
        isSelectedAsPrimary = isPrimary
        
        if (isPrimary) {
            // Show check icon
            if (animate) {
                checkIcon.visibility = View.VISIBLE
                checkIcon.alpha = 0f
                checkIcon.scaleX = 0.5f
                checkIcon.scaleY = 0.5f
                checkIcon.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(250)
                    .setInterpolator(OvershootInterpolator(2f))
                    .start()
            } else {
                checkIcon.visibility = View.VISIBLE
                checkIcon.alpha = 1f
            }
            
            // Subtle card highlight
            card.strokeWidth = resources.getDimensionPixelSize(R.dimen.card_stroke_selected)
            card.strokeColor = context.getColor(R.color.color_success_green)
        } else {
            // Hide check icon
            if (animate && checkIcon.visibility == View.VISIBLE) {
                checkIcon.animate()
                    .alpha(0f)
                    .scaleX(0.5f)
                    .scaleY(0.5f)
                    .setDuration(150)
                    .withEndAction { checkIcon.visibility = View.GONE }
                    .start()
            } else {
                checkIcon.visibility = View.GONE
            }
            
            // Remove highlight
            card.strokeWidth = 0
        }
    }

    private fun animateTap() {
        card.animate()
            .scaleX(0.97f)
            .scaleY(0.97f)
            .setDuration(80)
            .withEndAction {
                card.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            .start()
    }

    fun animateEntrance(delay: Long) {
        alpha = 0f
        translationY = 20f
        
        animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(delay)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    fun animateRemoval(onComplete: () -> Unit) {
        animate()
            .alpha(0f)
            .translationX(width.toFloat())
            .setDuration(250)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { onComplete() }
            .start()
    }

    fun setOnMintItemListener(listener: OnMintItemListener) {
        this.listener = listener
    }

    fun getMintUrl(): String = mintUrl
}
