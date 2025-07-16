package com.example.shellshock;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

public class TopUpActivity extends AppCompatActivity {

    private TextInputEditText topUpAmountEditText;
    private Button topUpSubmitButton;
    private AlertDialog nfcDialog;
    private NfcAdapter nfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_ShellShock);
        setContentView(R.layout.activity_top_up);

        topUpAmountEditText = findViewById(R.id.top_up_amount_edit_text);
        topUpSubmitButton = findViewById(R.id.top_up_submit_button);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        topUpSubmitButton.setOnClickListener(v -> {
            String amount = topUpAmountEditText.getText().toString();
            if (!amount.isEmpty()) {
                showNfcDialog(Long.parseLong(amount));
            } else {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            }
        });
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
