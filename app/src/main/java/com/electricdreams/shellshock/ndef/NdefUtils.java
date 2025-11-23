package com.electricdreams.shellshock.ndef;

/**
 * Utility methods for NDEF processing
 */
public class NdefUtils {
    
    /**
     * Convert a byte array to a hex string
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
