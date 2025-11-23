package com.electricdreams.shellshock.feature.settings;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.electricdreams.shellshock.R;
import com.electricdreams.shellshock.core.util.ItemManager;
import com.electricdreams.shellshock.feature.items.ItemListActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ItemsSettingsActivity extends AppCompatActivity {
    private static final String TAG = "ItemsSettingsActivity";
    
    private TextView itemsStatusText;
    private Button addItemsButton;
    private Button importItemsButton;
    private View clearItemsButton;
    
    private final ActivityResultLauncher<String> csvPickerLauncher = 
            registerForActivityResult(new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    importCsvFile(uri);
                }
            });
            
    private final ActivityResultLauncher<Intent> itemListLauncher = 
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    updateItemsStatus();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_items_settings);
        
        // Setup back button
        View backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
        
        // Initialize views
        itemsStatusText = findViewById(R.id.items_status_text);
        addItemsButton = findViewById(R.id.add_items_button);
        importItemsButton = findViewById(R.id.import_items_button);
        clearItemsButton = findViewById(R.id.clear_items_button);
        
        // Update status
        updateItemsStatus();
        
        // Set up buttons
        addItemsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ItemListActivity.class);
            itemListLauncher.launch(intent);
        });
        
        importItemsButton.setOnClickListener(v -> {
            csvPickerLauncher.launch("text/csv");
        });
        
        clearItemsButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Clear All Items")
                    .setMessage("Are you sure you want to delete ALL items from your catalog? This cannot be undone.")
                    .setPositiveButton("Delete All Items", (dialog, which) -> {
                        ItemManager itemManager = ItemManager.getInstance(this);
                        itemManager.clearItems();
                        updateItemsStatus();
                        Toast.makeText(this, "All items cleared", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateItemsStatus();
    }
    
    private void updateItemsStatus() {
        ItemManager itemManager = ItemManager.getInstance(this);
        int itemCount = itemManager.getAllItems().size();
        
        if (itemCount == 0) {
            itemsStatusText.setText("No items in catalog");
        } else {
            itemsStatusText.setText(itemCount + " items in catalog");
        }
    }
    
    private void importCsvFile(Uri uri) {
        try {
            File tempFile = new File(getCacheDir(), "import_catalog.csv");
            
            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 OutputStream outputStream = new FileOutputStream(tempFile)) {
                
                if (inputStream == null) {
                    Toast.makeText(this, "Failed to open CSV file", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            
            ItemManager itemManager = ItemManager.getInstance(this);
            int importedCount = itemManager.importItemsFromCsv(tempFile.getAbsolutePath(), true);
            
            if (importedCount > 0) {
                Toast.makeText(this, "Imported " + importedCount + " items", Toast.LENGTH_SHORT).show();
                updateItemsStatus();
            } else {
                Toast.makeText(this, "No items imported from CSV", Toast.LENGTH_SHORT).show();
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Error importing CSV file: " + e.getMessage(), e);
            Toast.makeText(this, "Error importing CSV file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
