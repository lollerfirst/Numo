package com.electricdreams.shellshock.nfc;

import android.util.Log;

import com.cashujdk.nut18.PaymentRequest;
import com.cashujdk.nut18.Transport;
import com.cashujdk.nut18.TransportTag;

import java.util.Optional;

/**
 * Helper class for Cashu payment-related operations
 */
public class CashuPaymentHelper {
    private static final String TAG = "CashuPaymentHelper";

    /**
     * Create a Cashu payment request for a specific amount
     * @param amount Amount in sats
     * @param description Optional description for the payment
     * @return Payment request string (creq...)
     */
    public static String createPaymentRequest(long amount, String description) {
        try {
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.amount = Optional.of(amount);
            paymentRequest.unit = Optional.of("sat");
            paymentRequest.description = Optional.of(description != null ? description : "Payment for " + amount + " sats");
            
            // Generate a random ID
            String id = java.util.UUID.randomUUID().toString().substring(0, 8);
            paymentRequest.id = Optional.of(id);
            
            // Set single-use flag
            paymentRequest.singleUse = Optional.of(true);
            
            // Encode and return
            String encodedRequest = paymentRequest.encode();
            Log.d(TAG, "Created payment request: " + encodedRequest);
            return encodedRequest;
        } catch (Exception e) {
            Log.e(TAG, "Error creating payment request: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Check if a string is a valid Cashu token
     * @param text Text to check
     * @return True if it's a valid Cashu token (starts with "cashuB"), false otherwise
     */
    public static boolean isCashuToken(String text) {
        return text != null && text.startsWith("cashuB");
    }

    /**
     * Check if a string is a valid Cashu payment request
     * @param text Text to check
     * @return True if it's a valid Cashu payment request (starts with "creq"), false otherwise
     */
    public static boolean isCashuPaymentRequest(String text) {
        return text != null && text.startsWith("creqA");
    }
}
