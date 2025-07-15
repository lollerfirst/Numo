import android.os.Bundle;
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
import androidx.core.content.ContextCompat;

/**
 * Modern and sleek app store design screen with integrated keyboard
 */
public class ModernCalculatorActivity extends AppCompatActivity {
    
    private TextView totalAmountTitle;
    private EditText displayField;
    private Button clearButton;
    private Button enterButton;
    private Button submitButton;
    private GridLayout keyboardGrid;
    private StringBuilder currentInput;
    private double totalAmount = 0.0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        currentInput = new StringBuilder();
        
        // Create the modern UI
        createModernUI();
        
        // Apply theme-aware styling
        applyThemeAwareColors();
    }
    
    private void createModernUI() {
        // Main container with modern styling
        LinearLayout mainContainer = new LinearLayout(this);
        mainContainer.setOrientation(LinearLayout.VERTICAL);
        mainContainer.setPadding(24, 24, 24, 24);
        mainContainer.setBackgroundColor(Color.parseColor("#F5F5F7")); // Light gray background
        
        // Title section
        createTitleSection(mainContainer);
        
        // Display field section
        createDisplaySection(mainContainer);
        
        // Action buttons section (Clear and Enter)
        createActionButtonsSection(mainContainer);
        
        // Integrated keyboard section
        createIntegratedKeyboard(mainContainer);
        
        // Submit button section
        createSubmitSection(mainContainer);
        
        setContentView(mainContainer);
    }
    
    private void createTitleSection(LinearLayout parent) {
        // Title container
        LinearLayout titleContainer = new LinearLayout(this);
        titleContainer.setOrientation(LinearLayout.VERTICAL);
        titleContainer.setPadding(0, 0, 0, 32);
        
        // Main title
        TextView mainTitle = new TextView(this);
        mainTitle.setText("Calculator");
        mainTitle.setTextSize(32);
        mainTitle.setTextColor(Color.parseColor("#1D1D1F"));
        mainTitle.setTypeface(null, Typeface.BOLD);
        mainTitle.setGravity(android.view.Gravity.CENTER);
        
        // Total amount title
        totalAmountTitle = new TextView(this);
        totalAmountTitle.setText("Total Amount: $0.00");
        totalAmountTitle.setTextSize(18);
        totalAmountTitle.setTextColor(Color.parseColor("#6E6E73"));
        totalAmountTitle.setGravity(android.view.Gravity.CENTER);
        totalAmountTitle.setPadding(0, 8, 0, 0);
        
        titleContainer.addView(mainTitle);
        titleContainer.addView(totalAmountTitle);
        parent.addView(titleContainer);
    }
    
    private void createDisplaySection(LinearLayout parent) {
        // Display field with modern styling
        displayField = new EditText(this);
        displayField.setTextSize(24);
        displayField.setTextColor(Color.parseColor("#1D1D1F"));
        displayField.setHint("Enter amount");
        displayField.setHintTextColor(Color.parseColor("#AEAEB2"));
        displayField.setGravity(android.view.Gravity.CENTER);
        displayField.setPadding(24, 20, 24, 20);
        displayField.setFocusable(false); // Prevent system keyboard
        displayField.setCursorVisible(false);
        
        // Create rounded background
        GradientDrawable displayBackground = new GradientDrawable();
        displayBackground.setColor(Color.WHITE);
        displayBackground.setCornerRadius(16);
        displayBackground.setStroke(2, Color.parseColor("#E5E5EA"));
        displayField.setBackground(displayBackground);
        
        // Add shadow effect
        displayField.setElevation(4);
        
        LinearLayout.LayoutParams displayParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        displayParams.setMargins(0, 0, 0, 24);
        displayField.setLayoutParams(displayParams);
        
        parent.addView(displayField);
    }
    
    private void createActionButtonsSection(LinearLayout parent) {
        // Action buttons container
        LinearLayout actionContainer = new LinearLayout(this);
        actionContainer.setOrientation(LinearLayout.HORIZONTAL);
        actionContainer.setGravity(android.view.Gravity.CENTER);
        
        // Clear button
        clearButton = createModernButton("Clear", Color.parseColor("#FF3B30"), Color.WHITE);
        clearButton.setOnClickListener(v -> clearDisplay());
        
        // Enter button
        enterButton = createModernButton("Enter", Color.parseColor("#007AFF"), Color.WHITE);
        enterButton.setOnClickListener(v -> enterAmount());
        
        // Add buttons to container with spacing
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        );
        buttonParams.setMargins(0, 0, 12, 0);
        clearButton.setLayoutParams(buttonParams);
        
        buttonParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
        );
        buttonParams.setMargins(12, 0, 0, 0);
        enterButton.setLayoutParams(buttonParams);
        
        actionContainer.addView(clearButton);
        actionContainer.addView(enterButton);
        
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.setMargins(0, 0, 0, 32);
        actionContainer.setLayoutParams(containerParams);
        
        parent.addView(actionContainer);
    }
    
    private void createIntegratedKeyboard(LinearLayout parent) {
        // Keyboard title
        TextView keyboardTitle = new TextView(this);
        keyboardTitle.setText("Integrated Keyboard");
        keyboardTitle.setTextSize(16);
        keyboardTitle.setTextColor(Color.parseColor("#6E6E73"));
        keyboardTitle.setGravity(android.view.Gravity.CENTER);
        keyboardTitle.setPadding(0, 0, 0, 16);
        parent.addView(keyboardTitle);
        
        // Keyboard grid
        keyboardGrid = new GridLayout(this);
        keyboardGrid.setColumnCount(3);
        keyboardGrid.setRowCount(4);
        keyboardGrid.setUseDefaultMargins(false);
        keyboardGrid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        
        // Create keyboard buttons
        String[] keyboardButtons = {
            "1", "2", "3",
            "4", "5", "6", 
            "7", "8", "9",
            ".", "0", "⌫"
        };
        
        for (int i = 0; i < keyboardButtons.length; i++) {
            Button keyButton = createKeyboardButton(keyboardButtons[i]);
            addButtonToKeyboard(keyButton, i);
        }
        
        LinearLayout.LayoutParams keyboardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        keyboardParams.setMargins(0, 0, 0, 32);
        keyboardGrid.setLayoutParams(keyboardParams);
        
        parent.addView(keyboardGrid);
    }
    
    private void createSubmitSection(LinearLayout parent) {
        // Submit button with modern styling
        submitButton = createModernButton("Submit", Color.parseColor("#34C759"), Color.WHITE);
        submitButton.setOnClickListener(v -> submitAmount());
        
        LinearLayout.LayoutParams submitParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        submitParams.setMargins(0, 16, 0, 0);
        submitButton.setLayoutParams(submitParams);
        
        parent.addView(submitButton);
    }
    
    private Button createModernButton(String text, int backgroundColor, int textColor) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(16);
        button.setTextColor(textColor);
        button.setTypeface(null, Typeface.BOLD);
        button.setPadding(24, 16, 24, 16);
        button.setAllCaps(false);
        
        // Create rounded background
        GradientDrawable buttonBackground = new GradientDrawable();
        buttonBackground.setColor(backgroundColor);
        buttonBackground.setCornerRadius(12);
        button.setBackground(buttonBackground);
        
        // Add shadow effect
        button.setElevation(6);
        
        return button;
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
        switch (input) {
            case "⌫":
                // Backspace
                if (currentInput.length() > 0) {
                    currentInput.deleteCharAt(currentInput.length() - 1);
                }
                break;
            case ".":
                // Decimal point - only allow one
                if (!currentInput.toString().contains(".")) {
                    currentInput.append(input);
                }
                break;
            default:
                // Numbers
                currentInput.append(input);
                break;
        }
        
        updateDisplay();
    }
    
    private void updateDisplay() {
        String displayText = currentInput.toString();
        if (TextUtils.isEmpty(displayText)) {
            displayField.setText("");
            displayField.setHint("Enter amount");
        } else {
            displayField.setText(displayText);
        }
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
                totalAmount += amount;
                updateTotalAmount();
                clearDisplay();
            } catch (NumberFormatException e) {
                displayField.setText("Invalid input");
            }
        }
    }
    
    private void submitAmount() {
        // Handle submit action
        String message = String.format("Total submitted: $%.2f", totalAmount);
        
        // Create a simple dialog-like effect
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
        
        // Reset total
        totalAmount = 0.0;
        updateTotalAmount();
        
        android.util.Log.d("ModernCalculator", "Submitted amount: " + message);
    }
    
    private void updateTotalAmount() {
        totalAmountTitle.setText(String.format("Total Amount: $%.2f", totalAmount));
    }
    
    private void applyThemeAwareColors() {
        // Apply theme-aware colors if dark mode is detected
        try {
            int nightModeFlags = getResources().getConfiguration().uiMode 
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            
            if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                // Dark mode adjustments
                ViewGroup mainContainer = (ViewGroup) findViewById(android.R.id.content).getRootView();
                mainContainer.setBackgroundColor(Color.parseColor("#1C1C1E"));
                
                // Update text colors for dark mode
                totalAmountTitle.setTextColor(Color.WHITE);
                displayField.setTextColor(Color.WHITE);
                displayField.setHintTextColor(Color.parseColor("#8E8E93"));
            }
        } catch (Exception e) {
            // Continue with light theme
        }
    }
}
