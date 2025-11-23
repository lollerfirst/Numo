package com.electricdreams.shellshock.ndef;

import android.util.Log;
import java.util.Arrays;

/**
 * Manages the state of NDEF processing operations
 */
public class NdefStateManager {
    private static final String TAG = "NdefStateManager";
    
    // Message to be sent when in write mode
    private String messageToSend = "";
    
    // Flag to indicate if the processor is in write mode (NDEF tag emulation)
    private boolean isInWriteMode = false;
    
    // Flag to control whether incoming messages should be processed
    private boolean processIncomingMessages = false;
    
    // NDEF buffer for received data
    private byte[] ndefData = new byte[NdefConstants.MAX_NDEF_DATA_SIZE];
    private int expectedNdefLength = -1;
    
    // Selected file during operation
    private byte[] selectedFile = null;
    
    // Track last message activity time for timeout handling
    private long lastMessageActivityTime = 0;
    
    private NdefProcessor.NdefMessageCallback callback;
    
    public NdefStateManager(NdefProcessor.NdefMessageCallback callback) {
        this.callback = callback;
    }
    
    // Getters and Setters
    public String getMessageToSend() {
        return messageToSend;
    }
    
    public void setMessageToSend(String message) {
        this.messageToSend = message;
        Log.i(TAG, "Message to send set: " + message);
    }
    
    public boolean isInWriteMode() {
        return isInWriteMode;
    }
    
    public void setWriteMode(boolean enabled) {
        this.isInWriteMode = enabled;
        Log.i(TAG, "Write mode set to " + enabled);
        
        if (enabled) {
            // When enabling write mode, also enable processing incoming messages
            this.processIncomingMessages = true;
            Log.i(TAG, "Processor is now in write mode, ready to send message: " + 
                  (messageToSend.isEmpty() ? "<empty>" : messageToSend));
            Log.i(TAG, "Incoming message processing enabled");
        } else {
            // Keep the message when disabling write mode, just don't send it
            Log.i(TAG, "Processor is now in read-only mode, message preserved but not being sent");
            
            // When disabling write mode, also disable processing incoming messages by default
            this.processIncomingMessages = false;
            Log.i(TAG, "Incoming message processing disabled");
        }
    }
    
    public boolean isProcessIncomingMessages() {
        return processIncomingMessages;
    }
    
    public void setProcessIncomingMessages(boolean enabled) {
        this.processIncomingMessages = enabled;
        Log.i(TAG, "Process incoming messages set to: " + enabled);
    }
    
    public byte[] getSelectedFile() {
        return selectedFile;
    }
    
    public void setSelectedFile(byte[] file) {
        this.selectedFile = file;
    }
    
    public byte[] getNdefData() {
        return ndefData;
    }
    
    public int getExpectedNdefLength() {
        return expectedNdefLength;
    }
    
    public void setExpectedNdefLength(int length) {
        this.expectedNdefLength = length;
    }
    
    public long getLastMessageActivityTime() {
        return lastMessageActivityTime;
    }
    
    public void updateLastMessageActivityTime() {
        this.lastMessageActivityTime = System.currentTimeMillis();
    }
    
    public void resetLastMessageActivityTime() {
        this.lastMessageActivityTime = 0;
    }
    
    public NdefProcessor.NdefMessageCallback getCallback() {
        return callback;
    }
    
    public void resetForNextMessage() {
        expectedNdefLength = -1;
        Arrays.fill(ndefData, (byte) 0);
        lastMessageActivityTime = 0;
    }
}
