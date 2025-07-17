package com.example.shellshock;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ModernPOSActivity extends AppCompatActivity {

    private static final String TAG = "ModernPOSActivity";
    private TextView balanceDisplay;
    private Button receiveButton;
    private Button sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modern_pos);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        balanceDisplay = findViewById(R.id.balance_display);
        receiveButton = findViewById(R.id.receive_button);
        sendButton = findViewById(R.id.send_button);

        receiveButton.setOnClickListener(v -> {
            // In a real app, this would open a menu to choose between scanning a QR code or entering a token
            startActivity(new Intent(this, TopUpActivity.class));
        });

        sendButton.setOnClickListener(v -> {
            // In a real app, this would open a menu to choose between creating a token or paying an invoice
            startActivity(new Intent(this, BalanceCheckActivity.class));
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
        } else if (item.getItemId() == R.id.action_balance_check) {
            startActivity(new Intent(this, BalanceCheckActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
