package com.example.shellshock

import android.annotation.SuppressLint
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.SecureRandom

class MainActivity : ComponentActivity() {
    private val TAG = "com.example.shellshock.MainActivity"
    private lateinit var textView: TextView
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "OnCreate was called")

        // Find UI components
        textView = findViewById(R.id.textView)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            textView.text = "NFC is not available on this device."
            return
        }

        // Handle NFC intent if the app was launched by an NFC tag
        // The manifest filter with aid-filter will ensure this intent is already filtered
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            handleNfcIntent(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Enable foreground dispatch to give this app priority for NFC intents
        // This is still useful if the app is already running and a new tag is discovered.
        // The techLists array ensures only IsoDep tags are dispatched.
        nfcAdapter?.let {
            val pendingIntent = android.app.PendingIntent.getActivity(
                this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                android.app.PendingIntent.FLAG_MUTABLE
            )
            val techLists = arrayOf(arrayOf(IsoDep::class.java.name))
            it.enableForegroundDispatch(this, pendingIntent, null, techLists)
            Log.d(TAG, "Foreground dispatch enabled.")
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
        Log.d(TAG, "Foreground dispatch disabled.")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // The manifest filter with aid-filter will ensure this intent is already filtered
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            handleNfcIntent(intent)
        }
    }

    @SuppressLint("SetTextI19n")
    private fun handleNfcIntent(intent: Intent) {
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            textView.text = "NFC Tag discovered: ${tag.id?.toHexString()}"
            Log.d(TAG, "NFC Tag discovered: ${tag.id?.toHexString()}")

            lifecycleScope.launch(Dispatchers.IO) {
                var satocashClient: SatocashNfcClient? = null
                try {
                    satocashClient = SatocashNfcClient(tag)
                    satocashClient.connect()

                    // --- Satocash Client Interaction Example ---
                    // This is where you'd call the SatocashNfcClient methods
                    // based on your application's logic.

                    // 1. Select applet
                    // This step might be redundant if the AID filter already selected it,
                    // but it's good practice to explicitly select it or confirm.
                    satocashClient.selectApplet(SatocashNfcClient.SATOCASH_AID)
                    withContext(Dispatchers.Main) {
                        textView.text = "Satocash Applet found and selected!"
                    }
                    Log.d(TAG, "Satocash Applet selected.")

                    // 2. Initialize Secure Channel
                    satocashClient.initSecureChannel()
                    withContext(Dispatchers.Main) {
                        textView.text = "Secure Channel Initialized!"
                    }
                    Log.d(TAG, "Secure Channel Initialized.")

                    // 3. Get Card Status (using secure channel)
                    val status = satocashClient.getStatus()
                    withContext(Dispatchers.Main) {
                        textView.append("\nCard Status: ${status["applet_version"]}, PIN tries: ${status["pin_tries_remaining"]}")
                    }
                    Log.d(TAG, "Card Status: $status")

                    // 4. Verify PIN (example)
                    try {
                        val pin = "1234" // Replace with actual PIN input
                        satocashClient.verifyPin(pin, 0)
                        withContext(Dispatchers.Main) {
                            textView.append("\nPIN Verified! Card Ready.")
                        }
                        Log.d(TAG, "PIN Verified.")
                    } catch (e: SatocashNfcClient.SatocashException) {
                        withContext(Dispatchers.Main) {
                            textView.append("\nPIN Verification Failed: ${e.message} (SW: ${String.format("0x%04X", e.sw)})")
                        }
                        Log.e(TAG, "PIN Verification Failed: ${e.message} (SW: ${String.format("0x%04X", e.sw)})")
                    }

                    // Example: Get Card Label
                    try {
                        val label = satocashClient.getCardLabel()
                        withContext(Dispatchers.Main) {
                            textView.append("\nCard Label: $label")
                        }
                        Log.d(TAG, "Card Label: $label")
                    } catch (e: SatocashNfcClient.SatocashException) {
                        withContext(Dispatchers.Main) {
                            textView.append("\nFailed to get card label: ${e.message}")
                        }
                        Log.e(TAG, "Failed to get card label: ${e.message}")
                    }

                    // Example: Import a dummy mint
                    try {
                        val dummyMintUrl = "https://dummy.mint.example.com"
                        val mintIndex = satocashClient.importMint(dummyMintUrl)
                        withContext(Dispatchers.Main) {
                            textView.append("\nImported mint at index: $mintIndex")
                        }
                        Log.d(TAG, "Imported mint at index: $mintIndex")
                    } catch (e: SatocashNfcClient.SatocashException) {
                        withContext(Dispatchers.Main) {
                            textView.append("\nFailed to import mint: ${e.message}")
                        }
                        Log.e(TAG, "Failed to import mint: ${e.message}")
                    }
                } catch (e: IOException) {
                    withContext(Dispatchers.Main) {
                        textView.text = "NFC Communication Error: ${e.message}"
                    }
                    Log.e(TAG, "NFC Communication Error: ${e.message}", e)
                } catch (e: SatocashNfcClient.SatocashException) {
                    withContext(Dispatchers.Main) {
                        textView.text = "Satocash Card Error: ${e.message} (SW: ${String.format("0x%04X", e.sw)})"
                    }
                    Log.e(TAG, "Satocash Card Error: ${e.message} (SW: ${String.format("0x%04X", e.sw)})", e)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        textView.text = "An unexpected error occurred: ${e.message}"
                    }
                    Log.e(TAG, "An unexpected error occurred: ${e.message}", e)
                } finally {
                    try {
                        satocashClient?.close()
                    } catch (e: IOException) {
                        Log.e(TAG, "Error closing IsoDep connection: ${e.message}", e)
                    }
                }
            }
        }
    }

    // Extension function to convert ByteArray to Hex String for logging
    private fun ByteArray.toHexString(): String =
        joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
}
