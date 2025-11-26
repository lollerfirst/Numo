package com.electricdreams.shellshock.feature.items

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
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
    // Chip data: emoji + product name pairs with assigned colors
    private val chipData = listOf(
        ChipItem("🥜", "PEANUTS", R.drawable.bg_chip_ribbon_lime, true),
        ChipItem("📚", "BOOKS", R.drawable.bg_chip_ribbon_cyan, true),
        ChipItem("👕", "T-SHIRTS", R.drawable.bg_chip_ribbon_white, true),
        ChipItem("☕", "COFFEE", R.drawable.bg_chip_ribbon_orange, true),
        ChipItem("🎸", "GUITARS", R.drawable.bg_chip_ribbon_purple, false),
        ChipItem("🍕", "PIZZA", R.drawable.bg_chip_ribbon_pink, true),
        ChipItem("💎", "JEWELRY", R.drawable.bg_chip_ribbon_yellow, true),
        ChipItem("🎧", "HEADPHONES", R.drawable.bg_chip_ribbon_green, false),
        ChipItem("🌮", "TACOS", R.drawable.bg_chip_ribbon_lime, true),
        ChipItem("🎨", "ART", R.drawable.bg_chip_ribbon_cyan, true),
        ChipItem("🍩", "DONUTS", R.drawable.bg_chip_ribbon_pink, true),
        ChipItem("💵", "FIAT", R.drawable.bg_chip_ribbon_green, false),
        ChipItem("🎁", "GIFTS", R.drawable.bg_chip_ribbon_purple, false),
        ChipItem("🧁", "CUPCAKES", R.drawable.bg_chip_ribbon_white, true),
        ChipItem("🎮", "GAMES", R.drawable.bg_chip_ribbon_yellow, true),
        ChipItem("🍺", "DRINKS", R.drawable.bg_chip_ribbon_orange, true)
    )

    // Animation durations for each row (ms) - varied for parallax effect
    private val rowDurations = listOf(20000L, 16000L, 18000L, 14000L, 22000L)
    
    // Starting offsets for each row (percentage of row width to offset start)
    private val rowStartOffsets = listOf(0f, 0.2f, 0.1f, 0.35f, 0.15f)

    // Active animators (to be cancelled on destroy)
    private val animators = mutableListOf<ValueAnimator>()

    // Track if started
    private var isStarted = false

    data class ChipItem(
        val emoji: String,
        val name: String,
        val backgroundRes: Int,
        val darkText: Boolean
    )

    /**
     * Start the animation. Call this in onResume or when the view becomes visible.
     */
    fun start() {
        if (isStarted) return
        isStarted = true
        
        // Stop any existing animations first
        stop()
        
        // Find row containers
        val row1 = ribbonContainer.findViewById<LinearLayout>(R.id.ribbon_row_1)
        val row2 = ribbonContainer.findViewById<LinearLayout>(R.id.ribbon_row_2)
        val row3 = ribbonContainer.findViewById<LinearLayout>(R.id.ribbon_row_3)
        val row4 = ribbonContainer.findViewById<LinearLayout>(R.id.ribbon_row_4)
        val row5 = ribbonContainer.findViewById<LinearLayout>(R.id.ribbon_row_5)
        val rowsWrapper = ribbonContainer.findViewById<FrameLayout>(R.id.ribbon_rows_wrapper)

        val rowContainers = listOf(row1, row2, row3, row4, row5)

        // Apply rotation to the wrapper after layout
        rowsWrapper?.post {
            rowsWrapper.pivotX = rowsWrapper.width / 2f
            rowsWrapper.pivotY = rowsWrapper.height / 2f
            rowsWrapper.rotation = -12f
            rowsWrapper.scaleX = 1.4f
            rowsWrapper.scaleY = 1.4f
        }

        // Populate and animate each row
        rowContainers.forEachIndexed { index, row ->
            row?.let { setupRow(it, index) }
        }
    }

    /**
     * Setup a single row with chips and animation.
     */
    private fun setupRow(row: LinearLayout, rowIndex: Int) {
        row.removeAllViews()
        
        // Get a varied subset of chips for this row
        val startIndex = (rowIndex * 3) % chipData.size
        
        // Add chips - enough to cover screen width plus extra for seamless loop
        val chipsForRow = mutableListOf<ChipItem>()
        repeat(3) { // Triple the chips for seamless looping
            for (i in 0 until 6) {
                chipsForRow.add(chipData[(startIndex + i) % chipData.size])
            }
        }
        
        // Add chip views to row
        chipsForRow.forEach { chip ->
            row.addView(createChipView(chip))
        }
        
        // Start animation after layout
        row.post {
            startRowAnimation(row, rowIndex)
        }
    }

    /**
     * Stop all animations. Call this in onPause or when view is hidden.
     */
    fun stop() {
        isStarted = false
        animators.forEach { it.cancel() }
        animators.clear()
    }

    /**
     * Create a single chip view with emoji and text.
     */
    private fun createChipView(chip: ChipItem): TextView {
        val density = context.resources.displayMetrics.density
        
        return TextView(context).apply {
            text = "${chip.emoji}  ${chip.name}"
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(if (chip.darkText) Color.BLACK else Color.WHITE)
            
            setBackgroundResource(chip.backgroundRes)
            
            // Padding
            val hPadding = (14 * density).toInt()
            val vPadding = (10 * density).toInt()
            setPadding(hPadding, vPadding, hPadding, vPadding)
            
            gravity = Gravity.CENTER
            
            // Layout params with margin
            val margin = (6 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(margin, 0, margin, 0)
            }
        }
    }

    /**
     * Start continuous scrolling animation for a row.
     */
    private fun startRowAnimation(row: LinearLayout, rowIndex: Int) {
        val rowWidth = row.width.toFloat()
        if (rowWidth <= 0) return

        // Width of one set of chips (we have 3 sets)
        val chipSetWidth = rowWidth / 3f
        
        // Initial offset based on row index for staggered look
        val initialOffset = -chipSetWidth * rowStartOffsets[rowIndex]
        
        // Start with chips visible (negative X to show from left edge)
        row.translationX = initialOffset

        val animator = ValueAnimator.ofFloat(0f, chipSetWidth).apply {
            duration = rowDurations[rowIndex]
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                // Move right to left (subtract from initial position)
                row.translationX = initialOffset - value
            }
        }

        animators.add(animator)
        animator.start()
    }
}
