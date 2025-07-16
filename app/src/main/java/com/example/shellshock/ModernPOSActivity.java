package com.example.shellshock;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ModernPOSActivity extends AppCompatActivity {

    private TextView amountDisplay;
    private Button submitButton;
    private StringBuilder currentInput = new StringBuilder();
    private AlertDialog nfcDialog;
    private NfcAdapter nfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_ShellShock);
        setContentView(R.layout.activity_modern_pos);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        amountDisplay = findViewById(R.id.amount_display);
        submitButton = findViewById(R.id.submit_button);
        GridLayout keypad = findViewById(R.id.keypad);

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
                showNfcDialog(Long.parseLong(amount));
            } else {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_top_up) {
            startActivity(new Intent(this, TopUpActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onKeypadButtonClick(String label) {
        switch (label) {
            case "C":
                currentInput.setLength(0);
                break;
            case "◀":
                if (currentInput.length() > 0) {
                    currentInput.setLength(currentInput.length() - 1);
                }
                break;
            default:
                if (currentInput.length() < 9) { // Limit input to 9 digits
                    currentInput.append(label);
                }
                break;
        }
        updateDisplay();
    }

    private void updateDisplay() {
        if (currentInput.length() == 0) {
            amountDisplay.setText("0");
        } else {
            amountDisplay.setText(currentInput.toString());
        }
    }

    private void showNfcDialog(long amount) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_ShellShock);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_nfc_modern, null);
        builder.setView(dialogView);

        TextView nfcAmountDisplay = dialogView.findViewById(R.id.nfc_amount_display);
        nfcAmountDisplay.setText(String.format("%d sats", amount));

        builder.setCancelable(true);
        nfcDialog = builder.create();
        nfcDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Toast.makeText(this, "NFC Tag Scanned!", Toast.LENGTH_SHORT).show();
            if (nfcDialog != null && nfcDialog.isShowing()) {
                nfcDialog.dismiss();
            }
        }
    }
}
