package com.electricdreams.shellshock.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.electricdreams.shellshock.core.model.BasketItem
import com.electricdreams.shellshock.ui.components.CashAppCard
import com.electricdreams.shellshock.ui.components.CashAppPrimaryButton
import com.electricdreams.shellshock.ui.components.CashAppSecondaryButton
import com.electricdreams.shellshock.ui.components.CashAppTopBar
import com.electricdreams.shellshock.ui.theme.CashAppTypography
import com.electricdreams.shellshock.ui.theme.CashGreen
import com.electricdreams.shellshock.ui.theme.Gray400
import com.electricdreams.shellshock.ui.theme.Gray700

@Composable
fun BasketScreen(
    basketItems: List<BasketItem>,
    totalPrice: String,
    onRemoveItem: (BasketItem) -> Unit,
    onClearBasket: () -> Unit,
    onContinueShopping: () -> Unit,
    onCheckout: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            CashAppTopBar(
                title = "Basket",
                onBackClick = onBackClick,
                actions = {
                    if (basketItems.isNotEmpty()) {
                        TextButton(onClick = onClearBasket) {
                            Text("Clear", color = Color.Red)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (basketItems.isNotEmpty()) {
                BasketBottomBar(
                    total = totalPrice,
                    onCheckout = onCheckout
                )
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        if (basketItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Your basket is empty",
                    style = CashAppTypography.bodyLarge,
                    color = Gray400
                )
                Spacer(modifier = Modifier.height(16.dp))
                CashAppSecondaryButton(
                    text = "Continue Shopping",
                    onClick = onContinueShopping,
                    modifier = Modifier.width(200.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(basketItems) { item ->
                    BasketItemCard(
                        item = item,
                        onRemove = { onRemoveItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun BasketItemCard(
    item: BasketItem,
    onRemove: () -> Unit
) {
    CashAppCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.item.name,
                    style = CashAppTypography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!item.item.variationName.isNullOrEmpty()) {
                    Text(
                        text = item.item.variationName,
                        style = CashAppTypography.bodySmall,
                        color = Gray400
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Qty: ${item.quantity}",
                    style = CashAppTypography.bodySmall,
                    color = Gray700
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${String.format("%.2f", item.item.price * item.quantity)}", // Assuming USD for simplicity
                    style = CashAppTypography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = Gray400
                    )
                }
            }
        }
    }
}

@Composable
fun BasketBottomBar(
    total: String,
    onCheckout: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 16.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Total",
                    style = CashAppTypography.bodySmall,
                    color = Gray700
                )
                Text(
                    text = total,
                    style = CashAppTypography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            CashAppPrimaryButton(
                text = "Checkout",
                onClick = onCheckout,
                modifier = Modifier.width(120.dp)
            )
        }
    }
}
