package com.electricdreams.shellshock.feature.items

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import com.electricdreams.shellshock.core.model.Item
import com.electricdreams.shellshock.core.util.ItemManager
import com.electricdreams.shellshock.ui.screens.ItemListScreen
import com.electricdreams.shellshock.ui.theme.CashAppTheme

class ItemListActivity : AppCompatActivity() {

    private lateinit var itemManager: ItemManager
    private val itemsState = mutableStateOf<List<Item>>(emptyList())

    private val addItemLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            refreshItems()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        itemManager = ItemManager.getInstance(this)
        refreshItems()

        setContent {
            CashAppTheme {
                ItemListScreen(
                    items = itemsState.value,
                    onAddItemClick = {
                        val intent = Intent(this, ItemEntryActivity::class.java)
                        addItemLauncher.launch(intent)
                    },
                    onEditItemClick = { item ->
                        val intent = Intent(this, ItemEntryActivity::class.java)
                        intent.putExtra(ItemEntryActivity.EXTRA_ITEM_ID, item.id)
                        addItemLauncher.launch(intent)
                    },
                    onDeleteItemClick = { item -> showDeleteConfirmation(item) },
                    onBackClick = { finish() },
                    itemImageLoader = { item -> itemManager.loadItemImage(item) }
                )
            }
        }
    }

    private fun refreshItems() {
        itemsState.value = itemManager.allItems
    }

    private fun showDeleteConfirmation(item: Item) {
        AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete this item?")
            .setPositiveButton("Delete") { _, _ ->
                itemManager.removeItem(item.id)
                refreshItems()
                Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
