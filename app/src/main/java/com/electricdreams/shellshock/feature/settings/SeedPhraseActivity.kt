package com.electricdreams.shellshock.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.gridlayout.widget.GridLayout
import com.electricdreams.shellshock.R
import com.electricdreams.shellshock.core.cashu.CashuWalletManager
import com.google.android.material.button.MaterialButton

/**
 * Activity for displaying the wallet's 12-word seed phrase.
 * Shows words in a numbered grid format with a single copy button.
 */
class SeedPhraseActivity : AppCompatActivity() {

    private lateinit var seedWordsGrid: GridLayout
    private lateinit var copyButton: MaterialButton
    private var mnemonic: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seed_phrase)

        seedWordsGrid = findViewById(R.id.seed_words_grid)
        copyButton = findViewById(R.id.copy_button)

        findViewById<View>(R.id.back_button).setOnClickListener { 
            finish() 
        }

        // Load and display the mnemonic
        loadMnemonic()

        copyButton.setOnClickListener {
            copyToClipboard()
        }
    }

    private fun loadMnemonic() {
        mnemonic = CashuWalletManager.getMnemonic()
        
        if (mnemonic.isNullOrBlank()) {
            Toast.makeText(this, "Wallet not initialized", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        displaySeedWords(mnemonic!!.split(" "))
    }

    private fun displaySeedWords(words: List<String>) {
        seedWordsGrid.removeAllViews()
        
        val inflater = LayoutInflater.from(this)
        
        words.forEachIndexed { index, word ->
            val wordView = inflater.inflate(R.layout.item_seed_word, seedWordsGrid, false)
            
            val indexText = wordView.findViewById<TextView>(R.id.word_index)
            val wordText = wordView.findViewById<TextView>(R.id.word_text)
            
            indexText.text = "${index + 1}"
            wordText.text = word
            
            // Set GridLayout params for 2-column layout
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(index % 2, 1f)
                rowSpec = GridLayout.spec(index / 2)
            }
            wordView.layoutParams = params
            
            seedWordsGrid.addView(wordView)
        }
    }

    private fun copyToClipboard() {
        mnemonic?.let { phrase ->
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Seed Phrase", phrase)
            clipboard.setPrimaryClip(clip)
            
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        // Clear clipboard when leaving the activity for security
        // Note: This is optional and may be too aggressive for some users
    }
}
