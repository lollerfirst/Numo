package com.electricdreams.shellshock.feature.items

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.electricdreams.shellshock.R

/**
 * Handles the animated chip ribbon for the empty state.
 * Creates smooth, continuous scrolling animations of product chips
 * flowing from right to left across multiple rows.
 */
class EmptyStateAnimator(
    private val context: Context,
    private val ribbonContainer: View
) {
    // 6 visually distinct chips
    private val chipData = listOf(
        ChipItem("📚", "BOOKS", R.drawable.bg_chip_ribbon_cyan, true),
        ChipItem("🍕", "PIZZA", R.drawable.bg_chip_ribbon_pink, true),
        ChipItem("🌮", "TACOS", R.drawable.bg_chip_ribbon_lime, true),
        ChipItem("🎧", "HEADPHONES", R.drawable.bg_chip_ribbon_green, false),
        ChipItem("🎁", "GIFTS", R.drawable.bg_chip_ribbon_purple, false),
        ChipItem("🎮", "GAMES", R.drawable.bg_chip_ribbon_yellow, true)
    )

    // Fast animation durations (6-8 seconds per cycle)
    private val rowDurations = listOf(7000L, 8000L, 6500L)
    
    // Starting offsets for staggered appearance
    private val rowStartOffsets = listOf(0f, 0.4f, 0.2f)

    // Active animators
    private val animators = mutableListOf<ValueAnimator>()

    // Track state
    private var isStarted = false

    data class ChipItem(
        val emoji: String,
        val name: String,
        val backgroundRes: Int,
        val darkText: Boolean
    )

    /**
     * Start the animation.
     */
    fun start() {
        if (isStarted) return
        isStarted = true
        
        stop()
        
        val row1 = ribbonContainer.findViewById<LinearLayout>(R.id.ribbon_row_1)
        val row2 = ribbonContainer.findViewById<LinearLayout>(R.id.ribbon_row_2)
        val row3 = ribbonContainer.findViewById<LinearLayout>(R.id.ribbon_row_3)
        val rowsWrapper = ribbonContainer.findViewById<FrameLayout>(R.id.ribbon_rows_wrapper)

        val rowContainers = listOf(row1, row2, row3)

        // Apply rotation after layout
        rowsWrapper?.post {
            rowsWrapper.pivotX = rowsWrapper.width / 2f
            rowsWrapper.pivotY = rowsWrapper.height / 2f
            rowsWrapper.rotation = -12f
            rowsWrapper.scaleX = 1.3f
            rowsWrapper.scaleY = 1.3f
        }

        // Setup each row
        rowContainers.forEachIndexed { index, row ->
            row?.let { setupRow(it, index) }
        }
    }

    /**
     * Setup a single row with pre-rendered chips.
     */
    private fun setupRow(row: LinearLayout, rowIndex: Int) {
        row.removeAllViews()
        
        // Vary starting chip for each row
        val startIndex = (rowIndex * 2) % chipData.size
        
        // Pre-create ALL chip views first (3 complete sets for seamless loop)
        val chipViews = mutableListOf<TextView>()
        repeat(3) {
            for (i in chipData.indices) {
                val chip = chipData[(startIndex + i) % chipData.size]
                chipViews.add(createChipView(chip))
            }
        }
        
        // Add all pre-rendered chips to row at once
        chipViews.forEach { chipView ->
            row.addView(chipView)
        }
        
        // Start animation after layout is complete
        row.post {
            startRowAnimation(row, rowIndex)
        }
    }

    /**
     * Stop all animations.
     */
    fun stop() {
        isStarted = false
        animators.forEach { it.cancel() }
        animators.clear()
    }

    /**
     * Create a fully-rendered chip view.
     */
    private fun createChipView(chip: ChipItem): TextView {
        val density = context.resources.displayMetrics.density
        
        return TextView(context).apply {
            // Set text with emoji and name
            text = "${chip.emoji}  ${chip.name}"
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(if (chip.darkText) Color.BLACK else Color.WHITE)
            
            // Background
            setBackgroundResource(chip.backgroundRes)
            
            // Padding
            val hPadding = (16 * density).toInt()
            val vPadding = (10 * density).toInt()
            setPadding(hPadding, vPadding, hPadding, vPadding)
            
            gravity = Gravity.CENTER
            maxLines = 1
            isSingleLine = true
            
            // Layout params
            val margin = (8 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(margin, 0, margin, 0)
            }
        }
    }

    /**
     * Start continuous, gapless scrolling animation.
     */
    private fun startRowAnimation(row: LinearLayout, rowIndex: Int) {
        val rowWidth = row.width.toFloat()
        if (rowWidth <= 0) return

        // Width of one complete set of chips (we have 3 sets)
        val chipSetWidth = rowWidth / 3f
        
        // Initial offset for staggered look
        val initialOffset = -chipSetWidth * rowStartOffsets[rowIndex]
        
        // Position row
        row.translationX = initialOffset

        val animator = ValueAnimator.ofFloat(0f, chipSetWidth).apply {
            duration = rowDurations[rowIndex]
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                row.translationX = initialOffset - value
            }
        }

        animators.add(animator)
        animator.start()
    }
}
