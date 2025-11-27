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
 * Uses true infinite scroll with seamless looping - no visible resets.
 */
class EmptyStateAnimator(
    private val context: Context,
    private val ribbonContainer: View
) {
    // 6 visually distinct chips
    private val chipData = listOf(
        ChipItem("👕", "SHIRTS", R.drawable.bg_chip_ribbon_cyan, true),
        ChipItem("🥩", "STEAKS", R.drawable.bg_chip_ribbon_pink, true),
        ChipItem("🌿", "PLANTS", R.drawable.bg_chip_ribbon_lime, true),
        ChipItem("🥜", "PEANUTS", R.drawable.bg_chip_ribbon_green, false),
        ChipItem("💵", "FIAT", R.drawable.bg_chip_ribbon_purple, false),
        ChipItem("🐮", "TALLOW", R.drawable.bg_chip_ribbon_yellow, true)
    )

    // Fast animation with slight variation per row for organic feel
    private val rowDurations = listOf(4000L, 4400L, 3800L)
    
    // Different starting phases so rows aren't synchronized
    private val rowPhases = listOf(0f, 0.33f, 0.66f)

    private val animators = mutableListOf<ValueAnimator>()
    private var isStarted = false

    data class ChipItem(
        val emoji: String,
        val name: String,
        val backgroundRes: Int,
        val darkText: Boolean
    )

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

        rowContainers.forEachIndexed { index, row ->
            row?.let { setupRow(it, index) }
        }
    }

    private fun setupRow(row: LinearLayout, rowIndex: Int) {
        row.removeAllViews()
        
        // Vary the starting chip for visual diversity between rows
        val startIndex = (rowIndex * 2) % chipData.size
        
        // Create 3 COMPLETE sets of all chips for seamless infinite loop
        // [SET 1] [SET 2] [SET 3]
        // We scroll through SET 1, then seamlessly reset to the start
        // This creates the illusion of infinite motion
        val chipViews = mutableListOf<TextView>()
        repeat(3) {
            for (i in chipData.indices) {
                val chip = chipData[(startIndex + i) % chipData.size]
                chipViews.add(createChipView(chip))
            }
        }
        
        // Add all chips to row
        chipViews.forEach { row.addView(it) }
        
        // Start animation after layout is complete
        row.post {
            startInfiniteScroll(row, rowIndex)
        }
    }

    fun stop() {
        isStarted = false
        animators.forEach { it.cancel() }
        animators.clear()
    }

    private fun createChipView(chip: ChipItem): TextView {
        val density = context.resources.displayMetrics.density
        
        return TextView(context).apply {
            text = "${chip.emoji}  ${chip.name}"
            textSize = 15f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setTextColor(if (chip.darkText) Color.BLACK else Color.WHITE)
            setBackgroundResource(chip.backgroundRes)
            
            val hPadding = (16 * density).toInt()
            val vPadding = (10 * density).toInt()
            setPadding(hPadding, vPadding, hPadding, vPadding)
            
            gravity = Gravity.CENTER
            maxLines = 1
            isSingleLine = true
            
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
     * True infinite scroll with seamless looping.
     * - Row contains [SET1][SET2][SET3]
     * - Starts with SET2 visible (middle position)
     * - Scrolls left continuously
     * - When SET1 would finish, seamlessly resets to identical visual position
     * - User never sees a "jump" or reset
     */
    private fun startInfiniteScroll(row: LinearLayout, rowIndex: Int) {
        val rowWidth = row.width.toFloat()
        if (rowWidth <= 0) return

        // Width of one complete set of chips (we have 3 sets)
        val setWidth = rowWidth / 3f
        
        // Start position: Begin with chips already filling the screen
        // Position so the MIDDLE set (SET2) is centered on screen initially
        // This means SET1 is partially visible on left, SET2 in center, SET3 on right
        val startPosition = -setWidth * rowPhases[rowIndex]
        
        // Set initial position
        row.translationX = startPosition

        val animator = ValueAnimator.ofFloat(0f, setWidth).apply {
            duration = rowDurations[rowIndex]
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                // Scroll left (negative direction)
                row.translationX = startPosition - progress
            }
            
            // No special reset logic needed - ValueAnimator.RESTART handles it
            // The animation restarts from 0, which visually is identical to the end
            // because we have 3 identical sets of chips
        }

        animators.add(animator)
        animator.start()
    }
}
