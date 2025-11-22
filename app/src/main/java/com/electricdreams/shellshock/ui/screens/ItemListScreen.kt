package com.electricdreams.shellshock.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.electricdreams.shellshock.core.model.Item
import com.electricdreams.shellshock.ui.components.CashAppCard
import com.electricdreams.shellshock.ui.components.CashAppTopBar
import com.electricdreams.shellshock.ui.theme.CashAppTypography
import com.electricdreams.shellshock.ui.theme.CashGreen
import com.electricdreams.shellshock.ui.theme.Gray400
import com.electricdreams.shellshock.ui.theme.Gray700

@Composable
fun ItemListScreen(
    items: List<Item>,
    onAddItemClick: () -> Unit,
    onEditItemClick: (Item) -> Unit,
    onDeleteItemClick: (Item) -> Unit,
    onBackClick: () -> Unit,
    itemImageLoader: (Item) -> Bitmap?
) {
    Scaffold(
        topBar = {
            CashAppTopBar(
                title = "Items Catalog",
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddItemClick,
                containerColor = CashGreen,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No items in catalog",
                    style = CashAppTypography.bodyLarge,
                    color = Gray400
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
                items(items) { item ->
                    ItemListItem(
                        item = item,
                        onEditClick = { onEditItemClick(item) },
                        onDeleteClick = { onDeleteItemClick(item) },
                        imageBitmap = itemImageLoader(item)
                    )
                }
            }
        }
    }
}

@Composable
fun ItemListItem(
    item: Item,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    imageBitmap: Bitmap?
) {
    CashAppCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image
            Surface(
                modifier = Modifier.size(60.dp),
                shape = MaterialTheme.shapes.small,
                color = Gray400.copy(alpha = 0.2f)
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap.asImageBitmap(),
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = CashAppTypography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!item.variationName.isNullOrEmpty()) {
                    Text(
                        text = item.variationName,
                        style = CashAppTypography.bodySmall,
                        color = Gray400
                    )
                }
                Text(
                    text = "$${String.format("%.2f", item.price)}",
                    style = CashAppTypography.bodyMedium,
                    color = CashGreen,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Stock: ${item.quantity}",
                    style = CashAppTypography.bodySmall,
                    color = Gray700
                )
            }

            // Actions
            Row {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Gray700
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Gray400
                    )
                }
            }
        }
    }
}
