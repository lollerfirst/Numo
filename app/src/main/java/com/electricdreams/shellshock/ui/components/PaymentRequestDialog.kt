package com.electricdreams.shellshock.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.electricdreams.shellshock.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PaymentRequestDialog(
    amount: Long,
    paymentRequest: String,
    onCopy: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = CashAppShapes.cardLarge,
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Payment Request",
                    style = CashAppTypography.titleLarge,
                    color = Gray900
                )
                
                Spacer(modifier = Modifier.height(Spacing.xl))
                
                // Amount
                Text(
                    text = NumberFormat.getNumberInstance(Locale.US).format(amount),
                    style = CashAppTypography.displayLarge.copy(fontSize = 48.sp),
                    color = Gray900
                )
                
                Text(
                    text = "sats",
                    style = CashAppTypography.bodyMedium,
                    color = Gray500
                )
                
                Spacer(modifier = Modifier.height(Spacing.xl))
                
                // Payment Request Label
                Text(
                    text = "Payment Request",
                    style = CashAppTypography.labelMedium,
                    color = Gray500,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(Spacing.sm))
                
                // Payment Request Text (scrollable)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    shape = CashAppShapes.card,
                    colors = CardDefaults.cardColors(containerColor = Gray50)
                ) {
                    Text(
                        text = paymentRequest,
                        style = CashAppTypography.bodySmall,
                        color = Gray700,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(Spacing.md),
                        textAlign = TextAlign.Start
                    )
                }
                
                Spacer(modifier = Modifier.height(Spacing.xl))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    // Close button
                    CashAppSecondaryButton(
                        text = "Close",
                        onClick = onClose,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Copy button
                    CashAppPrimaryButton(
                        text = "Copy",
                        onClick = onCopy,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
