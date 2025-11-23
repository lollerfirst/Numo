package com.electricdreams.shellshock.feature.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.electricdreams.shellshock.R;
import com.electricdreams.shellshock.core.util.CurrencyManager;

public class CurrencySettingsActivity extends AppCompatActivity {
    
    private RadioGroup currencyRadioGroup;
    private RadioButton radioUsd;
    private RadioButton radioEur;
    private RadioButton radioGbp;
    private RadioButton radioJpy;
    private CurrencyManager currencyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_currency_settings);
        
        // Setup back button
        View backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
        
        // Initialize manager
        currencyManager = CurrencyManager.getInstance(this);
        
        // Initialize views
        currencyRadioGroup = findViewById(R.id.currency_radio_group);
        radioUsd = findViewById(R.id.radio_usd);
        radioEur = findViewById(R.id.radio_eur);
        radioGbp = findViewById(R.id.radio_gbp);
        radioJpy = findViewById(R.id.radio_jpy);
        
        // Set current selection
        setSelectedCurrency(currencyManager.getCurrentCurrency());
        
        // Auto-save on selection change
        currencyRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedCurrency = getSelectedCurrency();
            currencyManager.setPreferredCurrency(selectedCurrency);
        });
    }
    
    private void setSelectedCurrency(String currencyCode) {
        switch (currencyCode) {
            case CurrencyManager.CURRENCY_EUR:
                radioEur.setChecked(true);
                break;
            case CurrencyManager.CURRENCY_GBP:
                radioGbp.setChecked(true);
                break;
            case CurrencyManager.CURRENCY_JPY:
                radioJpy.setChecked(true);
                break;
            case CurrencyManager.CURRENCY_USD:
            default:
                radioUsd.setChecked(true);
                break;
        }
    }
    
    private String getSelectedCurrency() {
        int selectedId = currencyRadioGroup.getCheckedRadioButtonId();
        
        if (selectedId == R.id.radio_eur) {
            return CurrencyManager.CURRENCY_EUR;
        } else if (selectedId == R.id.radio_gbp) {
            return CurrencyManager.CURRENCY_GBP;
        } else if (selectedId == R.id.radio_jpy) {
            return CurrencyManager.CURRENCY_JPY;
        } else {
            return CurrencyManager.CURRENCY_USD;
        }
    }
}
