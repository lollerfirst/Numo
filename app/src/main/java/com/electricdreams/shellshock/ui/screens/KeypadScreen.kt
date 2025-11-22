package com.electricdreams.shellshock.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.electricdreams.shellshock.ui.components.CashAppKeypad
import com.electricdreams.shellshock.ui.components.CashAppPrimaryButton
import com.electricdreams.shellshock.ui.theme.CashAppTypography
import com.electricdreams.shellshock.ui.theme.CashGreen
import com.electricdreams.shellshock.ui.theme.White

@androidx.compose.animation.ExperimentalAnimationApi
@Composable
fun KeypadScreen(
    amount: String,
    currencySymbol: String,
    isUsdMode: Boolean,
    onKeyPress: (String) -> Unit,
    onDelete: () -> Unit,
    onToggleCurrency: () -> Unit,
    onRequestClick: () -> Unit,
    onPayClick: () -> Unit,
    onQrClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CashGreen)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar (QR and Profile)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(top = 16.dp), // Status bar padding
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onQrClick) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan QR",
                        tint = White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                IconButton(onClick = onProfileClick) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        tint = White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Amount Display Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated amount display
                androidx.compose.animation.AnimatedContent(
                    targetState = if (amount.isEmpty()) "$currencySymbol 0" else "$currencySymbol $amount",
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) with
                                fadeOut(animationSpec = tween(300))
                    },
                    label = "amountAnimation"
                ) { displayText ->
                    Text(
                        text = displayText,
                        style = CashAppTypography.displayLarge,
                        color = White
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Currency Selector / Toggle with animation
                androidx.compose.animation.AnimatedContent(
                    targetState = if (isUsdMode) "USD" else "SATS",
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(200)) + 
                         scaleIn(initialScale = 0.8f, animationSpec = tween(200))) with
                        (fadeOut(animationSpec = tween(200)) + 
                         scaleOut(targetScale = 0.8f, animationSpec = tween(200)))
                    },
                    label = "currencyToggleAnimation"
                ) { currencyText ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(White.copy(alpha = 0.2f))
                            .clickable(onClick = onToggleCurrency)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = currencyText,
                            style = CashAppTypography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = White
                        )
                    }
                }
            }

            // Keypad
            CashAppKeypad(
                onKeyPress = onKeyPress,
                onDelete = onDelete,
                modifier = Modifier.padding(horizontal = 48.dp),
                textColor = White
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CashAppPrimaryButton(
                    text = "Request",
                    onClick = onRequestClick,
                    modifier = Modifier.weight(1f),
                    backgroundColor = White.copy(alpha = 0.9f),
                    contentColor = CashGreen
                )
                
                CashAppPrimaryButton(
                    text = "Pay",
                    onClick = onPayClick,
                    modifier = Modifier.weight(1f),
                    backgroundColor = White,
                    contentColor = CashGreen
                )
            }
        }
    }
}
