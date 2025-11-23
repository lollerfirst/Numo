package com.electricdreams.shellshock.ndef;

import android.util.Log;
import java.util.Arrays;

/**
 * Main coordinator for NDEF (NFC Data Exchange Format) message processing. 
 * Handles APDU commands and coordinates the various NDEF processing components.
 */
public class NdefProcessor {
    private static final String TAG = "NdefProcessor";

    // Components for handling different aspects of NDEF processing
    private final NdefStateManager stateManager;
    private final NdefApduHandler apduHandler;
    private final NdefUpdateBinaryHandler updateBinaryHandler;
    private final NdefMessageParser messageParser;

    // Callbacks interface
    public interface NdefMessageCallback {
        void onNdefMessageReceived(String message);
        void onMessageSent();
    }

    public NdefProcessor(NdefMessageCallback callback) {
        // Initialize state manager with callback
        this.stateManager = new NdefStateManager(callback);
        
        // Initialize message parser
        this.messageParser = new NdefMessageParser(callback);
        
        // Initialize APDU handler 
        this.apduHandler = new NdefApduHandler(stateManager);
        
        // Initialize update binary handler
        this.updateBinaryHandler = new NdefUpdateBinaryHandler(stateManager, messageParser);
    }

    /**
     * Set the message to be sent when in write mode
     */
    public void setMessageToSend(String message) {
        stateManager.setMessageToSend(message);
    }

    /**
     * Set whether the processor is in write mode (NDEF tag emulation)
     * When write mode is enabled, it will also send messages
     */
    public void setWriteMode(boolean enabled) {
        stateManager.setWriteMode(enabled);
    }
    
    /**
     * Control whether to process incoming NDEF messages
     * This allows finer control than setWriteMode() - can be used to enable/disable
     * just the receiving capability without affecting the sending capability.
     * 
     * @param enabled true to process incoming messages, false to ignore them
     */
    public void setProcessIncomingMessages(boolean enabled) {
        stateManager.setProcessIncomingMessages(enabled);
    }

    /**
     * Process an APDU command and return the appropriate response
     */
    public byte[] processCommandApdu(byte[] commandApdu) {
        // Check if NDEF AID is selected
        if (Arrays.equals(commandApdu, NdefConstants.NDEF_SELECT_AID)) {
            Log.d(TAG, "NDEF AID selected (write mode: " + stateManager.isInWriteMode() + 
                  ", has message: " + !stateManager.getMessageToSend().isEmpty() + ")");
            return NdefConstants.NDEF_RESPONSE_OK;
        }
        
        // Handle File Selection
        if (isSelectFileCommand(commandApdu)) {
            Log.d(TAG, "SELECT FILE command received");
            return apduHandler.handleSelectFile(commandApdu);
        }
        
        // Handle Read Binary
        if (isReadBinaryCommand(commandApdu)) {
            Log.d(TAG, "READ BINARY command received");
            return apduHandler.handleReadBinary(commandApdu);
        }
        
        // Handle Update Binary
        if (isUpdateBinaryCommand(commandApdu)) {
            Log.d(TAG, "UPDATE BINARY command received: " + NdefUtils.bytesToHex(commandApdu));
            return updateBinaryHandler.handleUpdateBinary(commandApdu);
        }
        
        Log.d(TAG, "Invalid APDU received: " + NdefUtils.bytesToHex(commandApdu));
        return NdefConstants.NDEF_RESPONSE_ERROR;
    }

    /**
     * Check if the command is a SELECT FILE command
     */
    private boolean isSelectFileCommand(byte[] commandApdu) {
        return commandApdu.length >= 7 && 
               Arrays.equals(Arrays.copyOfRange(commandApdu, 0, 4), NdefConstants.NDEF_SELECT_FILE_HEADER);
    }

    /**
     * Check if the command is a READ BINARY command
     */
    private boolean isReadBinaryCommand(byte[] commandApdu) {
        return commandApdu.length >= 2 && 
               Arrays.equals(Arrays.copyOfRange(commandApdu, 0, 2), NdefConstants.NDEF_READ_BINARY_HEADER);
    }

    /**
     * Check if the command is an UPDATE BINARY command
     */
    private boolean isUpdateBinaryCommand(byte[] commandApdu) {
        return commandApdu.length >= 2 && 
               Arrays.equals(Arrays.copyOfRange(commandApdu, 0, 2), NdefConstants.NDEF_UPDATE_BINARY_HEADER);
    }
}
