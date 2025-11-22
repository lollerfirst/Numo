package com.electricdreams.shellshock

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import com.electricdreams.shellshock.ui.screens.NfcDialog
import com.electricdreams.shellshock.ui.screens.PinDialog
import com.electricdreams.shellshock.ui.screens.TopUpScreen
import com.electricdreams.shellshock.ui.theme.CashAppTheme
import java.io.IOException
import java.util.concurrent.CompletableFuture

class TopUpActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var satocashClient: SatocashNfcClient? = null
    private var satocashWallet: SatocashWallet? = null

    private val tokenInputState = mutableStateOf("")
    private val statusMessageState = mutableStateOf<String?>(null)
    private val isSuccessState = mutableStateOf(false)
    
    private val showNfcDialogState = mutableStateOf(false)
    private val showPinDialogState = mutableStateOf(false)
    private val nfcDialogMessageState = mutableStateOf("Tap your card to import proofs")
    
    private var pendingProofToken: String? = null
    private var savedPin: String? = null
    private var waitingForRescan = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // Handle incoming share intent
        if (Intent.ACTION_SEND == intent.action && "text/plain" == intent.type) {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
                tokenInputState.value = sharedText
                pendingProofToken = sharedText
                statusMessageState.value = "Token ready to be imported"
                isSuccessState.value = true
                showNfcDialogState.value = true
            }
        }

        setContent {
            CashAppTheme {
                TopUpScreen(
                    tokenInput = tokenInputState.value,
                    onTokenInputChange = { tokenInputState.value = it },
                    onSubmit = {
                        if (tokenInputState.value.isNotBlank()) {
                            pendingProofToken = tokenInputState.value
                            statusMessageState.value = "Tap your card to import the proofs"
                            isSuccessState.value = true
                            showNfcDialogState.value = true
                        } else {
                            statusMessageState.value = "Please enter a Cashu proof token"
                            isSuccessState.value = false
                        }
                    },
                    statusMessage = statusMessageState.value,
                    isSuccess = isSuccessState.value,
                    onBackClick = { finish() }
                )

                if (showNfcDialogState.value) {
                    NfcDialog(
                        message = nfcDialogMessageState.value,
                        onCancel = { 
                            showNfcDialogState.value = false 
                            waitingForRescan = false
                            savedPin = null
                        }
                    )
                }

                if (showPinDialogState.value) {
                    PinDialog(
                        onPinEntered = { pin ->
                            showPinDialogState.value = false
                            savedPin = pin
                            waitingForRescan = true
                            nfcDialogMessageState.value = "PIN accepted. Please scan your card again to complete import."
                            showNfcDialogState.value = true
                        },
                        onCancel = {
                            showPinDialogState.value = false
                            statusMessageState.value = "Operation cancelled"
                            isSuccessState.value = false
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.let { adapter ->
            val pendingIntent = PendingIntent.getActivity(
                this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE
            )
            val techList = arrayOf(arrayOf(IsoDep::class.java.name))
            adapter.enableForegroundDispatch(this, pendingIntent, null, techList)
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            tag?.let { handleNfcImport(it) }
        }
    }

    private fun handleNfcImport(tag: Tag) {
        if (pendingProofToken.isNullOrEmpty()) {
            runOnUiThread {
                statusMessageState.value = "No proof token set to import"
                isSuccessState.value = false
            }
            return
        }

        if (waitingForRescan && savedPin != null) {
            processImportWithSavedPin(tag)
            return
        }

        waitingForRescan = false

        Thread {
            try {
                satocashClient = SatocashNfcClient(tag)
                satocashClient?.connect()
                satocashWallet = SatocashWallet(satocashClient)

                satocashClient?.selectApplet(SatocashNfcClient.SATOCASH_AID)
                satocashClient?.initSecureChannel()

                // Try import without PIN
                try {
                    val importFuture = satocashWallet?.importProofsFromToken(pendingProofToken)
                    val importedCount = importFuture?.join() ?: 0
                    
                    runOnUiThread {
                        statusMessageState.value = "Success: Imported $importedCount proofs"
                        isSuccessState.value = true
                        showNfcDialogState.value = false
                        tokenInputState.value = ""
                        pendingProofToken = null
                    }
                } catch (e: RuntimeException) {
                    val cause = e.cause
                    if (cause is SatocashNfcClient.SatocashException) {
                        if (cause.sw == 0x9C06) { // PIN required
                            try {
                                satocashClient?.close()
                                satocashClient = null
                            } catch (ioe: IOException) {
                                Log.e(TAG, "Error closing NFC", ioe)
                            }

                            runOnUiThread {
                                showNfcDialogState.value = false
                                showPinDialogState.value = true
                            }
                        } else {
                            throw cause
                        }
                    } else {
                        throw e
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error importing proofs", e)
                runOnUiThread {
                    statusMessageState.value = "Error: ${e.message}"
                    isSuccessState.value = false
                    // Don't dismiss dialog immediately on error, let user see it or cancel?
                    // Or maybe dismiss and show error in status bar
                    showNfcDialogState.value = false
                }
            } finally {
                try {
                    if (satocashClient != null && !waitingForRescan) {
                        satocashClient?.close()
                        satocashClient = null
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Error closing NFC", e)
                }
            }
        }.start()
    }

    private fun processImportWithSavedPin(tag: Tag) {
        if (savedPin == null) return

        Thread {
            try {
                runOnUiThread { nfcDialogMessageState.value = "Processing Import..." }

                satocashClient = SatocashNfcClient(tag)
                satocashClient?.connect()
                satocashWallet = SatocashWallet(satocashClient)

                satocashClient?.selectApplet(SatocashNfcClient.SATOCASH_AID)
                satocashClient?.initSecureChannel()

                val authenticated = satocashWallet?.authenticatePIN(savedPin)?.join() ?: false

                if (authenticated) {
                    val importFuture = satocashWallet?.importProofsFromToken(pendingProofToken)
                    val importedCount = importFuture?.join() ?: 0

                    runOnUiThread {
                        statusMessageState.value = "Success: Imported $importedCount proofs"
                        isSuccessState.value = true
                        showNfcDialogState.value = false
                        tokenInputState.value = ""
                        pendingProofToken = null
                        waitingForRescan = false
                        savedPin = null
                    }
                } else {
                    runOnUiThread {
                        statusMessageState.value = "PIN Verification Failed"
                        isSuccessState.value = false
                        showNfcDialogState.value = false
                        waitingForRescan = false
                        savedPin = null
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error importing with PIN", e)
                runOnUiThread {
                    statusMessageState.value = "Error: ${e.message}"
                    isSuccessState.value = false
                    showNfcDialogState.value = false
                    waitingForRescan = false
                    savedPin = null
                }
            } finally {
                try {
                    satocashClient?.close()
                    satocashClient = null
                } catch (e: IOException) {
                    Log.e(TAG, "Error closing NFC", e)
                }
            }
        }.start()
    }

    companion object {
        private const val TAG = "TopUpActivity"
    }
}
