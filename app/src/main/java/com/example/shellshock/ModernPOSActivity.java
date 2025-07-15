package com.example.shellshock;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.GridLayout;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.text.TextUtils;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

/**
 * Modern POS (Point of Sale) system with integrated number keyboard
 */
public class ModernPOSActivity extends AppCompatActivity {
    
    private TextView displayTextView;
    private EditText displayField;
    private Button clearButton;
    private Button enterButton;
    private Button submitButton;
    private GridLayout keyboardGrid;
    private StringBuilder currentInput;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pos);
        
        currentInput = new StringBuilder();
        
        // Setup toolbar
        setupToolbar();
        
        // Initialize views
        initializeViews();
        
        // Setup keyboard
        setupKeyboard();
        
        // Apply theme-aware styling
        applyThemeAwareColors();
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Point of Sale");
        }
    }
    
    private void initializeViews() {
        displayTextView = findViewById(R.id.displayTextView);
        displayField = findViewById(R.id.displayField);
        clearButton = findViewById(R.id.clearButton);
        enterButton = findViewById(R.id.enterButton);
        submitButton = findViewById(R.id.submitButton);
        keyboardGrid = findViewById(R.id.keyboardGrid);
        
        // Set click listeners
        clearButton.setOnClickListener(v -> clearDisplay());
        enterButton.setOnClickListener(v -> enterAmount());
        submitButton.setOnClickListener(v -> submitTransaction());
    }
    
    private void setupKeyboard() {
        // Create number keyboard buttons - changed period to "C" (clear)
        String[] keyboardButtons = {
            "1", "2", "3",
            "4", "5", "6", 
            "7", "8", "9",
            "C", "0", "⌫"
        };
        
        for (int i = 0; i < keyboardButtons.length; i++) {
            Button keyButton = createKeyboardButton(keyboardButtons[i]);
            addButtonToKeyboard(keyButton, i);
        }
    }
    
    private Button createKeyboardButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(20);
        button.setTextColor(Color.parseColor("#1D1D1F"));
        button.setTypeface(null, Typeface.BOLD);
        button.setPadding(16, 16, 16, 16);
        button.setAllCaps(false);
        
        // Create rounded background
        GradientDrawable buttonBackground = new GradientDrawable();
        buttonBackground.setColor(Color.WHITE);
        buttonBackground.setCornerRadius(12);
        buttonBackground.setStroke(1, Color.parseColor("#E5E5EA"));
        button.setBackground(buttonBackground);
        
        // Add shadow effect
        button.setElevation(2);
        
        // Set click listener
        button.setOnClickListener(v -> handleKeyboardInput(text));
        
        return button;
    }
    
    private void addButtonToKeyboard(Button button, int index) {
        int row = index / 3;
        int column = index % 3;
        
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.rowSpec = GridLayout.spec(row, 1f);
        params.columnSpec = GridLayout.spec(column, 1f);
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.setMargins(4, 4, 4, 4);
        
        button.setLayoutParams(params);
        keyboardGrid.addView(button);
    }
    
    private void handleKeyboardInput(String input) {
        android.util.Log.d("ModernPOS", "Button clicked: " + input);
        
        switch (input) {
            case "⌫":
                // Backspace - remove last character
                if (currentInput.length() > 0) {
                    currentInput.deleteCharAt(currentInput.length() - 1);
                }
                break;
            case "C":
                // Clear all input
                currentInput.setLength(0);
                break;
            case "0":
            case "1":
            case "2":
            case "3":
            case "4":
            case "5":
            case "6":
            case "7":
            case "8":
            case "9":
                // Numbers - add to current input
                currentInput.append(input);
                break;
            default:
                // Any other character (shouldn't happen with our keyboard)
                android.util.Log.w("ModernPOS", "Unknown input: " + input);
                break;
        }
        
        android.util.Log.d("ModernPOS", "Current input after button: " + currentInput.toString());
        updateDisplay();
    }
    
    private void updateDisplay() {
        String displayText = currentInput.toString();
        
        // Update the main display TextView
        if (displayTextView != null) {
            if (TextUtils.isEmpty(displayText)) {
                displayTextView.setText("0");
            } else {
                displayTextView.setText(displayText);
            }
        }
        
        // Also update the hidden EditText for compatibility
        if (displayField != null) {
            if (TextUtils.isEmpty(displayText)) {
                displayField.setText("");
                displayField.setHint("Enter amount");
            } else {
                displayField.setText(displayText);
            }
        }
        
        android.util.Log.d("ModernPOS", "Display updated to: " + (TextUtils.isEmpty(displayText) ? "0" : displayText));
    }
    
    private void clearDisplay() {
        currentInput.setLength(0);
        updateDisplay();
    }
    
    private void enterAmount() {
        String input = currentInput.toString();
        if (!TextUtils.isEmpty(input)) {
            try {
                double amount = Double.parseDouble(input);
                // Amount entered successfully
                android.util.Log.d("ModernPOS", "Amount entered: $" + amount);
                clearDisplay();
            } catch (NumberFormatException e) {
                displayField.setText("Invalid input");
            }
        }
    }
    
    private void submitTransaction() {
        String input = currentInput.toString();
        if (!TextUtils.isEmpty(input)) {
            try {
                double amount = Double.parseDouble(input);
                String message = String.format("Transaction completed: $%.2f", amount);
                
                // Create a simple status message
                TextView statusMessage = new TextView(this);
                statusMessage.setText(message);
                statusMessage.setTextSize(16);
                statusMessage.setTextColor(Color.parseColor("#34C759"));
                statusMessage.setGravity(android.view.Gravity.CENTER);
                statusMessage.setPadding(24, 16, 24, 16);
                
                // Add to parent temporarily
                ViewGroup parent = (ViewGroup) submitButton.getParent();
                parent.addView(statusMessage);
                
                // Remove after 3 seconds
                statusMessage.postDelayed(() -> parent.removeView(statusMessage), 3000);
                
                // Clear input
                clearDisplay();
                
                android.util.Log.d("ModernPOS", "Transaction submitted: " + message);
            } catch (NumberFormatException e) {
                displayField.setText("Invalid amount");
            }
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pos_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.menu_import_proof) {
            // Launch Import Proof Activity
            Intent intent = new Intent(this, ImportProofActivity.class);
            startActivity(intent);
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void applyThemeAwareColors() {
        // Apply theme-aware colors if dark mode is detected
        try {
            int nightModeFlags = getResources().getConfiguration().uiMode 
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            
            if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                // Dark mode adjustments
                ViewGroup mainContainer = (ViewGroup) findViewById(android.R.id.content).getRootView();
                if (mainContainer != null) {
                    mainContainer.setBackgroundColor(Color.parseColor("#1C1C1E"));
                }
                
                // Update text colors for dark mode
                if (displayField != null) {
                    displayField.setTextColor(Color.WHITE);
                    displayField.setHintTextColor(Color.parseColor("#8E8E93"));
                }
            }
        } catch (Exception e) {
            // Continue with light theme
            android.util.Log.e("ModernPOS", "Error applying theme colors: " + e.getMessage());
        }
    }
}
