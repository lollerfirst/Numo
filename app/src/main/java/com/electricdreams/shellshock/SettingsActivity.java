package com.electricdreams.shellshock;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.electricdreams.shellshock.util.CurrencyManager;
import com.electricdreams.shellshock.util.MintManager;

import java.util.List;

public class SettingsActivity extends AppCompatActivity implements MintsAdapter.MintRemoveListener {
    private static final String TAG = "SettingsActivity";
    
    private RadioGroup currencyRadioGroup;
    private RadioButton radioUsd;
    private RadioButton radioEur;
    private RadioButton radioGbp;
    private RadioButton radioJpy;
    private Button saveButton;
    private RecyclerView mintsRecyclerView;
    private MintsAdapter mintsAdapter;
    private EditText newMintEditText;
    private Button addMintButton;
    private Button resetMintsButton;
    
    private CurrencyManager currencyManager;
    private MintManager mintManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        // Initialize managers
        currencyManager = CurrencyManager.getInstance(this);
        mintManager = MintManager.getInstance(this);
        
        // Initialize currency views
        currencyRadioGroup = findViewById(R.id.currency_radio_group);
        radioUsd = findViewById(R.id.radio_usd);
        radioEur = findViewById(R.id.radio_eur);
        radioGbp = findViewById(R.id.radio_gbp);
        radioJpy = findViewById(R.id.radio_jpy);
        
        // Initialize mints views
        mintsRecyclerView = findViewById(R.id.mints_recycler_view);
        newMintEditText = findViewById(R.id.new_mint_edit_text);
        addMintButton = findViewById(R.id.add_mint_button);
        resetMintsButton = findViewById(R.id.reset_mints_button);
        
        // Set up the RecyclerView
        mintsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mintsRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        
        // Set up the adapter
        mintsAdapter = new MintsAdapter(mintManager.getAllowedMints(), this);
        mintsRecyclerView.setAdapter(mintsAdapter);
        
        // Set up add mint button
        addMintButton.setOnClickListener(v -> addNewMint());
        
        // Set up reset mints button
        resetMintsButton.setOnClickListener(v -> resetMintsToDefaults());
        
        // Set up EditText done action
        newMintEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addNewMint();
                return true;
            }
            return false;
        });
        
        // Set current currency selection based on saved preference
        setSelectedCurrency(currencyManager.getCurrentCurrency());
        
        // Initialize save button
        saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> saveSettings());
    }
    
    /**
     * Set the radio button selection based on the current currency
     */
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
    
    /**
     * Get the selected currency from the radio group
     */
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
    
    /**
     * Add a new mint to the allowed list
     */
    private void addNewMint() {
        String mintUrl = newMintEditText.getText().toString().trim();
        if (TextUtils.isEmpty(mintUrl)) {
            Toast.makeText(this, "Please enter a valid mint URL", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Add the mint
        boolean added = mintManager.addMint(mintUrl);
        if (added) {
            // Update the adapter
            mintsAdapter.updateMints(mintManager.getAllowedMints());
            // Clear the input field
            newMintEditText.setText("");
            Toast.makeText(this, "Mint added", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Mint already in the list", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Reset mints to the default list
     */
    private void resetMintsToDefaults() {
        mintManager.resetToDefaults();
        mintsAdapter.updateMints(mintManager.getAllowedMints());
        Toast.makeText(this, "Mints reset to defaults", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Remove a mint from the allowed list (callback from adapter)
     */
    @Override
    public void onMintRemoved(String mintUrl) {
        if (mintManager.removeMint(mintUrl)) {
            mintsAdapter.updateMints(mintManager.getAllowedMints());
            Toast.makeText(this, "Mint removed", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Save the selected settings
     */
    private void saveSettings() {
        // Save currency preference
        String selectedCurrency = getSelectedCurrency();
        Log.d(TAG, "Saving currency preference: " + selectedCurrency);
        boolean currencySuccess = currencyManager.setPreferredCurrency(selectedCurrency);
        
        // Get current list of allowed mints for logging
        List<String> allowedMints = mintManager.getAllowedMints();
        Log.d(TAG, "Current allowed mints (" + allowedMints.size() + "): " + TextUtils.join(", ", allowedMints));
        
        if (currencySuccess) {
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error saving settings", Toast.LENGTH_SHORT).show();
        }
    }
}
