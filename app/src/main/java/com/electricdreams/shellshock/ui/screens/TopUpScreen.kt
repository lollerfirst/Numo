package com.electricdreams.shellshock.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.electricdreams.shellshock.ui.components.CashAppPrimaryButton
import com.electricdreams.shellshock.ui.components.CashAppTopBar
import com.electricdreams.shellshock.ui.theme.CashAppTypography
import com.electricdreams.shellshock.ui.theme.CashGreen
import com.electricdreams.shellshock.ui.theme.Gray700

@Composable
fun TopUpScreen(
    tokenInput: String,
    onTokenInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    statusMessage: String?,
    isSuccess: Boolean,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            CashAppTopBar(
                title = "Top Up",
                onBackClick = onBackClick
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Enter Cashu Token",
                style = CashAppTypography.titleMedium,
                color = Gray700,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = tokenInput,
                onValueChange = onTokenInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = { Text("cashuA...") },
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() })
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            CashAppPrimaryButton(
                text = "Import Proofs",
                onClick = onSubmit,
                enabled = tokenInput.isNotBlank()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (statusMessage != null) {
                Surface(
                    color = if (isSuccess) CashGreen.copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusMessage,
                        style = CashAppTypography.bodyMedium,
                        color = if (isSuccess) CashGreen else Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NfcDialog(
    title: String = "Ready to Scan",
    message: String = "Tap your card to import proofs",
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PinDialog(
    onPinEntered: (String) -> Unit,
    onCancel: () -> Unit
) {
    var pin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Enter PIN") },
        text = {
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 8) pin = it }, // Limit length reasonably
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onPinEntered(pin) },
                enabled = pin.isNotEmpty()
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}
