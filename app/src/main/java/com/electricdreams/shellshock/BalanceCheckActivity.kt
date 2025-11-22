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
import com.electricdreams.shellshock.ui.screens.BalanceScreen
import com.electricdreams.shellshock.ui.theme.CashAppTheme
import java.io.IOException

class BalanceCheckActivity : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var satocashClient: SatocashNfcClient? = null

    private val balanceState = mutableStateOf<Long?>(null)
    private val statusMessageState = mutableStateOf("Tap your NFC card to check balance")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        setContent {
            CashAppTheme {
                BalanceScreen(
                    balance = balanceState.value,
                    statusMessage = statusMessageState.value,
                    onBackClick = { finish() }
                )
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
            tag?.let { checkNfcBalance(it) }
        }
    }

    private fun checkNfcBalance(tag: Tag) {
        statusMessageState.value = "Reading card..."
        
        Thread {
            try {
                satocashClient = SatocashNfcClient(tag)
                satocashClient?.connect()
                satocashClient?.selectApplet(SatocashNfcClient.SATOCASH_AID)
                satocashClient?.initSecureChannel()

                val totalBalance = getCardBalance()
                
                runOnUiThread {
                    balanceState.value = totalBalance
                    statusMessageState.value = "Balance check complete"
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error checking balance", e)
                runOnUiThread {
                    statusMessageState.value = "Error: ${e.message}"
                }
            } finally {
                try {
                    satocashClient?.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Error closing NFC", e)
                }
            }
        }.start()
    }

    private fun getCardBalance(): Long {
        val client = satocashClient ?: return 0
        
        try {
            val status = client.status
            val nbProofsUnspent = (status["nb_proofs_unspent"] as? Number)?.toInt() ?: 0
            val nbProofsSpent = (status["nb_proofs_spent"] as? Number)?.toInt() ?: 0
            val totalProofs = nbProofsUnspent + nbProofsSpent

            if (totalProofs == 0) return 0

            val proofStates = client.getProofInfo(
                SatocashNfcClient.Unit.SAT,
                SatocashNfcClient.ProofInfoType.METADATA_STATE,
                0,
                totalProofs
            )

            if (proofStates.isEmpty()) return 0

            val amounts = client.getProofInfo(
                SatocashNfcClient.Unit.SAT,
                SatocashNfcClient.ProofInfoType.METADATA_AMOUNT_EXPONENT,
                0,
                totalProofs
            )

            var totalBalance = 0L
            for (i in proofStates.indices) {
                if (proofStates[i] == 1) { // 1 = unspent (based on Java code logic)
                    val amountExponent = amounts[i]
                    totalBalance += Math.pow(2.0, amountExponent.toDouble()).toLong()
                }
            }
            return totalBalance

        } catch (e: Exception) {
            Log.e(TAG, "Error getting card balance", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "BalanceCheckActivity"
    }
}
