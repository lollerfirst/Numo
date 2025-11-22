package com.electricdreams.shellshock.feature.items

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.electricdreams.shellshock.core.model.Item
import com.electricdreams.shellshock.core.util.ItemManager
import com.electricdreams.shellshock.ui.screens.ItemEntryScreen
import com.electricdreams.shellshock.ui.theme.CashAppTheme
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ItemEntryActivity : AppCompatActivity() {

    private lateinit var itemManager: ItemManager
    private var editItemId: String? = null
    private var isEditMode = false
    private var currentItem: Item? = null
    private var selectedImageUri: Uri? = null
    private var currentPhotoPath: String? = null

    private val itemState = mutableStateOf<Item?>(null)
    private val itemImageState = mutableStateOf<Bitmap?>(null)

    private val selectGalleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            updateImagePreview()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && selectedImageUri != null) {
            updateImagePreview()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        itemManager = ItemManager.getInstance(this)
        editItemId = intent.getStringExtra(EXTRA_ITEM_ID)
        isEditMode = !editItemId.isNullOrEmpty()

        if (isEditMode) {
            loadItemData()
        }

        setContent {
            CashAppTheme {
                ItemEntryScreen(
                    item = itemState.value,
                    isEditMode = isEditMode,
                    itemImage = itemImageState.value,
                    onImageClick = { showImageSourceDialog() },
                    onRemoveImageClick = { removeImage() },
                    onSaveClick = { item -> saveItem(item) },
                    onBackClick = { finish() }
                )
            }
        }
    }

    private fun loadItemData() {
        currentItem = itemManager.allItems.find { it.id == editItemId }
        currentItem?.let { item ->
            itemState.value = item
            if (item.imagePath != null) {
                itemImageState.value = itemManager.loadItemImage(item)
            }
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(this)
            .setTitle("Add Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> takePicture()
                    1 -> selectFromGallery()
                }
            }
            .show()
    }

    private fun selectFromGallery() {
        selectGalleryLauncher.launch("image/*")
    }

    private fun takePicture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_IMAGE_CAPTURE)
            return
        }

        try {
            val photoFile = createImageFile()
            selectedImageUri = FileProvider.getUriForFile(
                this,
                "com.electricdreams.shellshock.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(selectedImageUri)
        } catch (ex: IOException) {
            Toast.makeText(this, "Error creating image file: ${ex.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun removeImage() {
        selectedImageUri = null
        itemImageState.value = null
        if (isEditMode && currentItem?.imagePath != null) {
            itemManager.deleteItemImage(currentItem)
        }
    }

    private fun updateImagePreview() {
        selectedImageUri?.let { uri ->
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                itemImageState.value = bitmap
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePicture()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveItem(item: Item) {
        if (item.name.isBlank()) {
            Toast.makeText(this, "Item name is required", Toast.LENGTH_SHORT).show()
            return
        }
        if (item.price < 0) {
            Toast.makeText(this, "Price must be positive", Toast.LENGTH_SHORT).show()
            return
        }

        if (isEditMode) {
            item.id = editItemId
            // Preserve existing image path if not changed
            if (currentItem?.imagePath != null && selectedImageUri == null) {
                item.imagePath = currentItem?.imagePath
            }
        } else {
            item.id = UUID.randomUUID().toString()
        }

        val success = if (isEditMode) {
            itemManager.updateItem(item)
        } else {
            itemManager.addItem(item)
        }

        if (!success) {
            Toast.makeText(this, "Failed to save item", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUri != null) {
            val imageSaved = itemManager.saveItemImage(item, selectedImageUri)
            if (!imageSaved) {
                Toast.makeText(this, "Item saved but image could not be saved", Toast.LENGTH_LONG).show()
            }
        }

        Toast.makeText(this, if (isEditMode) "Item updated" else "Item added", Toast.LENGTH_SHORT).show()
        setResult(Activity.RESULT_OK)
        finish()
    }

    companion object {
        const val EXTRA_ITEM_ID = "extra_item_id"
        private const val REQUEST_IMAGE_CAPTURE = 1001
    }
}
