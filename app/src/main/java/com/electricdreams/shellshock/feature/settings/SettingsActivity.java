package com.electricdreams.shellshock.feature.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.electricdreams.shellshock.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        // Setup back button
        View backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
        
        // Setup navigation to sub-settings
        findViewById(R.id.currency_settings_item).setOnClickListener(v -> {
            startActivity(new Intent(this, CurrencySettingsActivity.class));
        });
        
        findViewById(R.id.mints_settings_item).setOnClickListener(v -> {
            startActivity(new Intent(this, MintsSettingsActivity.class));
        });
        
        findViewById(R.id.items_settings_item).setOnClickListener(v -> {
            startActivity(new Intent(this, ItemsSettingsActivity.class));
        });
    }
}
