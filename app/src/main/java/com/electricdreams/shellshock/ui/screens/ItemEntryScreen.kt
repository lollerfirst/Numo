package com.electricdreams.shellshock.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.electricdreams.shellshock.core.model.Item
import com.electricdreams.shellshock.ui.components.CashAppPrimaryButton
import com.electricdreams.shellshock.ui.components.CashAppTopBar
import com.electricdreams.shellshock.ui.theme.CashAppTypography
import com.electricdreams.shellshock.ui.theme.CashGreen
import com.electricdreams.shellshock.ui.theme.Gray200
import com.electricdreams.shellshock.ui.theme.Gray400
import com.electricdreams.shellshock.ui.theme.Gray700

@Composable
fun ItemEntryScreen(
    item: Item?,
    isEditMode: Boolean,
    itemImage: Bitmap?,
    onImageClick: () -> Unit,
    onRemoveImageClick: () -> Unit,
    onSaveClick: (Item) -> Unit,
    onBackClick: () -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var variation by remember { mutableStateOf(item?.variationName ?: "") }
    var price by remember { mutableStateOf(item?.price?.toString() ?: "") }
    var sku by remember { mutableStateOf(item?.sku ?: "") }
    var description by remember { mutableStateOf(item?.description ?: "") }
    var category by remember { mutableStateOf(item?.category ?: "") }
    var quantity by remember { mutableStateOf(item?.quantity?.toString() ?: "") }
    var isAlertEnabled by remember { mutableStateOf(item?.isAlertEnabled ?: false) }
    var alertThreshold by remember { mutableStateOf(item?.alertThreshold?.toString() ?: "5") }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CashAppTopBar(
                title = if (isEditMode) "Edit Item" else "Add Item",
                onBackClick = onBackClick
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image Picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Gray200)
                    .clickable { onImageClick() },
                contentAlignment = Alignment.Center
            ) {
                if (itemImage != null) {
                    Image(
                        bitmap = itemImage.asImageBitmap(),
                        contentDescription = "Item Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = onRemoveImageClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Image",
                            tint = Color.White
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Add Photo",
                            tint = Gray400,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Add Photo",
                            style = CashAppTypography.bodyMedium,
                            color = Gray700
                        )
                    }
                }
            }

            // Fields
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Item Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = variation,
                onValueChange = { variation = it },
                label = { Text("Variation (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = sku,
                onValueChange = { sku = it },
                label = { Text("SKU (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // Low Stock Alert
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isAlertEnabled,
                    onCheckedChange = { isAlertEnabled = it },
                    colors = CheckboxDefaults.colors(checkedColor = CashGreen)
                )
                Text(text = "Low Stock Alert", style = CashAppTypography.bodyMedium)
            }

            if (isAlertEnabled) {
                OutlinedTextField(
                    value = alertThreshold,
                    onValueChange = { alertThreshold = it },
                    label = { Text("Alert Threshold") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            CashAppPrimaryButton(
                text = "Save Item",
                onClick = {
                    val newItem = (item ?: Item()).apply {
                        this.name = name
                        this.variationName = variation
                        this.price = price.toDoubleOrNull() ?: 0.0
                        this.sku = sku
                        this.description = description
                        this.category = category
                        this.quantity = quantity.toIntOrNull() ?: 0
                        this.isAlertEnabled = isAlertEnabled
                        this.alertThreshold = alertThreshold.toIntOrNull() ?: 5
                    }
                    onSaveClick(newItem)
                }
            )
        }
    }
}
