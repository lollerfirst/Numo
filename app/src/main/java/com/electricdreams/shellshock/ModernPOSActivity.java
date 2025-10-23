package com.electricdreams.shellshock;

import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ScrollView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ModernPOSActivity extends AppCompatActivity {

    private static final String TAG = "ModernPOSActivity";
    private static final String PREFS_NAME = "ShellshockPrefs";
    private static final String KEY_NIGHT_MODE = "nightMode";
    
    private TextView amountDisplay;
    private Button submitButton;
    private StringBuilder currentInput = new StringBuilder();
    private AlertDialog nfcDialog;
    private TextView tokenDisplay;
    private Button copyTokenButton;
    private Button resetButton;
    private ScrollView tokenScrollContainer;
    private LinearLayout tokenActionsContainer;
    private ConstraintLayout inputModeContainer;
    private NfcAdapter nfcAdapter;
    private SatocashNfcClient satocashClient;
    private SatocashWallet satocashWallet;
    private long requestedAmount = 0;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private MenuItem themeMenuItem;
    private boolean isNightMode = false;

    // Status Words (SW) constants copied from SatocashNfcClient for error handling
    private static class SW {
        public static final int UNAUTHORIZED = 0x9C06;
        public static final int PIN_FAILED = 0x63C0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Load theme preference before setting content view
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isNightMode = prefs.getBoolean(KEY_NIGHT_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
            isNightMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_Shellshock);
        setContentView(R.layout.activity_modern_pos);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Find all views
        amountDisplay = findViewById(R.id.amount_display);
        submitButton = findViewById(R.id.submit_button);
        GridLayout keypad = findViewById(R.id.keypad);
        tokenDisplay = findViewById(R.id.token_display);
        copyTokenButton = findViewById(R.id.copy_token_button);
        resetButton = findViewById(R.id.reset_button);
        tokenScrollContainer = findViewById(R.id.token_scroll_container);
        tokenActionsContainer = findViewById(R.id.token_actions_container);
        inputModeContainer = findViewById(R.id.input_mode_container);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        String[] buttonLabels = {
                "1", "2", "3",
                "4", "5", "6",
                "7", "8", "9",
                "C", "0", "◀"
        };

        LayoutInflater inflater = LayoutInflater.from(this);

        for (String label : buttonLabels) {
            Button button = (Button) inflater.inflate(R.layout.keypad_button, keypad, false);
            button.setText(label);
            button.setOnClickListener(v -> onKeypadButtonClick(label));
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            int margin = 8; // in pixels
            params.setMargins(margin, margin, margin, margin);
            button.setLayoutParams(params);
            keypad.addView(button);
        }

        submitButton.setOnClickListener(v -> {
            String amount = currentInput.toString();
            if (!amount.isEmpty()) {
                requestedAmount = Long.parseLong(amount);
                showNfcDialog(requestedAmount);
            } else {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            }
        });

        copyTokenButton.setOnClickListener(v -> {
            String token = tokenDisplay.getText().toString();
            if (!token.isEmpty() && !token.startsWith("Error:")) {
                copyTokenToClipboard(token);
            }
        });

        resetButton.setOnClickListener(v -> resetToInputMode());

        // Ensure initial state
        resetToInputMode();
    }

    private void toggleTheme() {
        isNightMode = !isNightMode;
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(KEY_NIGHT_MODE, isNightMode);
        editor.apply();

        AppCompatDelegate.setDefaultNightMode(
            isNightMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        
        updateThemeIcon();
    }

    private void updateThemeIcon() {
        if (themeMenuItem != null) {
            themeMenuItem.setIcon(
                isNightMode ? R.drawable.ic_light_mode : R.drawable.ic_dark_mode
            );
            themeMenuItem.setTitle(
                isNightMode ? "Switch to Light Mode" : "Switch to Dark Mode"
            );
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        themeMenuItem = menu.findItem(R.id.action_theme_toggle);
        updateThemeIcon();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_top_up) {
            startActivity(new Intent(this, TopUpActivity.class));
            return true;
        } else if (itemId == R.id.action_balance_check) {
            startActivity(new Intent(this, BalanceCheckActivity.class));
            return true;
        } else if (itemId == R.id.action_theme_toggle) {
            toggleTheme();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ... rest of the code remains the same ...
}
