package com.electricdreams.shellshock.feature.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.electricdreams.shellshock.R

/**
 * Security & Privacy settings screen.
 * Provides access to:
 * - Backup Mnemonic: View and copy the 12-word seed phrase
 * - Restore Wallet: Recover wallet from an existing seed phrase
 */
class SecuritySettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security_settings)

        findViewById<View>(R.id.back_button).setOnClickListener { 
            finish() 
        }

        findViewById<View>(R.id.backup_mnemonic_item).setOnClickListener {
            startActivity(Intent(this, SeedPhraseActivity::class.java))
        }

        findViewById<View>(R.id.restore_wallet_item).setOnClickListener {
            startActivity(Intent(this, RestoreWalletActivity::class.java))
        }
    }
}
