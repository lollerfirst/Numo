package com.electricdreams.numo.feature.pin

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.electricdreams.numo.R
import com.google.android.material.button.MaterialButton

/**
 * Activity for setting up a new PIN or changing an existing PIN.
 * 
 * Two-step flow:
 * 1. Enter new PIN (4-16 digits)
 * 2. Confirm PIN by re-entering
 */
class PinSetupActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val MODE_CREATE = "create"
        const val MODE_CHANGE = "change"
    }

    private enum class Step {
        ENTER_PIN,
        CONFIRM_PIN
    }

    private lateinit var pinManager: PinManager
    private lateinit var pinDots: PinDotsView
    private lateinit var pinKeypad: PinKeypadView
    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var pinLengthLabel: TextView
    private lateinit var errorMessage: TextView
    private lateinit var continueButton: MaterialButton
    private lateinit var backButton: ImageButton
    private lateinit var step1Indicator: View
    private lateinit var step2Indicator: View

    private var currentStep = Step.ENTER_PIN
    private val enteredPin = StringBuilder()
    private var firstPin: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private var mode = MODE_CREATE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_setup)

        pinManager = PinManager.getInstance(this)
        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_CREATE

        initViews()
        setupListeners()
        updateUIForStep()
    }

    private fun initViews() {
        pinDots = findViewById(R.id.pin_dots)
        pinKeypad = findViewById(R.id.pin_keypad)
        titleText = findViewById(R.id.title)
        subtitleText = findViewById(R.id.subtitle)
        pinLengthLabel = findViewById(R.id.pin_length_label)
        errorMessage = findViewById(R.id.error_message)
        continueButton = findViewById(R.id.continue_button)
        backButton = findViewById(R.id.back_button)
        step1Indicator = findViewById(R.id.step1_indicator)
        step2Indicator = findViewById(R.id.step2_indicator)
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            if (currentStep == Step.CONFIRM_PIN) {
                // Go back to first step
                currentStep = Step.ENTER_PIN
                enteredPin.clear()
                firstPin = ""
                pinDots.clear()
                updateUIForStep()
            } else {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }

        pinKeypad.setOnKeyListener(object : PinKeypadView.OnKeyListener {
            override fun onDigitPressed(digit: String) {
                if (enteredPin.length < PinManager.MAX_PIN_LENGTH) {
                    enteredPin.append(digit)
                    pinDots.addDigit()
                    hideError()
                    updateContinueButton()
                    updatePinLengthLabel()
                }
            }

            override fun onDeletePressed() {
                if (enteredPin.isNotEmpty()) {
                    enteredPin.deleteCharAt(enteredPin.length - 1)
                    pinDots.removeDigit()
                    hideError()
                    updateContinueButton()
                    updatePinLengthLabel()
                }
            }
        })

        continueButton.setOnClickListener {
            handleContinue()
        }
    }

    private fun updateUIForStep() {
        when (currentStep) {
            Step.ENTER_PIN -> {
                titleText.text = if (mode == MODE_CHANGE) "New PIN" else "Create a PIN"
                subtitleText.text = "Choose a 4–16 digit PIN to protect sensitive settings"
                
                step1Indicator.background = ContextCompat.getDrawable(this, R.drawable.bg_pin_dot_filled)
                step2Indicator.background = ContextCompat.getDrawable(this, R.drawable.bg_pin_dot_empty)
            }
            Step.CONFIRM_PIN -> {
                titleText.text = "Confirm PIN"
                subtitleText.text = "Re-enter your PIN to confirm"
                
                step1Indicator.background = ContextCompat.getDrawable(this, R.drawable.bg_pin_dot_filled)
                step2Indicator.background = ContextCompat.getDrawable(this, R.drawable.bg_pin_dot_filled)
            }
        }
        updateContinueButton()
        updatePinLengthLabel()
    }

    private fun updateContinueButton() {
        val isValid = enteredPin.length >= PinManager.MIN_PIN_LENGTH
        continueButton.isEnabled = isValid
        continueButton.alpha = if (isValid) 1f else 0.4f
        
        continueButton.text = when (currentStep) {
            Step.ENTER_PIN -> "Continue"
            Step.CONFIRM_PIN -> "Set PIN"
        }
    }

    private fun updatePinLengthLabel() {
        val length = enteredPin.length
        pinLengthLabel.text = when {
            length == 0 -> "Enter at least 4 digits"
            length < PinManager.MIN_PIN_LENGTH -> "$length digit${if (length > 1) "s" else ""} · ${PinManager.MIN_PIN_LENGTH - length} more needed"
            else -> "$length digit${if (length > 1) "s" else ""}"
        }
        
        pinLengthLabel.setTextColor(
            ContextCompat.getColor(
                this,
                if (length >= PinManager.MIN_PIN_LENGTH) R.color.color_success_green else R.color.color_text_tertiary
            )
        )
    }

    private fun handleContinue() {
        when (currentStep) {
            Step.ENTER_PIN -> {
                if (!pinManager.isValidPin(enteredPin.toString())) {
                    showError("PIN must be 4–16 digits")
                    return
                }
                
                firstPin = enteredPin.toString()
                currentStep = Step.CONFIRM_PIN
                enteredPin.clear()
                pinDots.clear()
                updateUIForStep()
            }
            Step.CONFIRM_PIN -> {
                if (enteredPin.toString() != firstPin) {
                    showError("PINs don't match")
                    pinDots.showError()
                    handler.postDelayed({
                        enteredPin.clear()
                        pinDots.clear()
                        updatePinLengthLabel()
                        updateContinueButton()
                    }, 500)
                    return
                }
                
                // PINs match - save it
                if (pinManager.setPin(firstPin)) {
                    pinDots.showSuccess()
                    Toast.makeText(this, "PIN set successfully", Toast.LENGTH_SHORT).show()
                    handler.postDelayed({
                        setResult(Activity.RESULT_OK)
                        finish()
                    }, 400)
                } else {
                    showError("Failed to set PIN")
                }
            }
        }
    }

    private fun showError(message: String) {
        errorMessage.text = message
        errorMessage.visibility = View.VISIBLE
    }

    private fun hideError() {
        errorMessage.visibility = View.INVISIBLE
    }

    override fun onBackPressed() {
        if (currentStep == Step.CONFIRM_PIN) {
            currentStep = Step.ENTER_PIN
            enteredPin.clear()
            firstPin = ""
            pinDots.clear()
            updateUIForStep()
        } else {
            setResult(Activity.RESULT_CANCELED)
            super.onBackPressed()
        }
    }
}
