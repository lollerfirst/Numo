package com.electricdreams.shellshock.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Cash App radius system
 */
object Radius {
    val card = 12.dp           // Standard card radius
    val cardLarge = 16.dp      // Large card radius
    val pill = 999.dp          // Fully rounded (for buttons, pills)
    val bottomSheet = 24.dp    // Bottom sheet top corners
}

/**
 * Predefined shapes for common components
 */
object CashAppShapes {
    val card = RoundedCornerShape(Radius.card)
    val cardLarge = RoundedCornerShape(Radius.cardLarge)
    val pill = RoundedCornerShape(Radius.pill)
    val bottomSheet = RoundedCornerShape(
        topStart = Radius.bottomSheet,
        topEnd = Radius.bottomSheet,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
}
