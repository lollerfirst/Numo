package com.electricdreams.shellshock.ndef;

/**
 * Constants used in NDEF processing operations
 */
public class NdefConstants {
    
    // Timeout for waiting for NDEF message completion (3 seconds)
    public static final long MESSAGE_TIMEOUT_MS = 3000;
    
    // Command Headers
    public static final byte[] NDEF_SELECT_FILE_HEADER = {0x00, (byte) 0xA4, 0x00, 0x0C};
    
    // Step 1: Select AID (Application Identifier)
    public static final byte[] NDEF_SELECT_AID = {
            0x00,                     // CLA (Class)
            (byte) 0xA4,              // INS (Instruction)
            0x04,                     // P1 (Parameter 1)
            0x00,                     // P2 (Parameter 2)
            0x07,                     // Lc (Length of data)
            (byte) 0xD2, 0x76, 0x00, 0x00, (byte) 0x85, 0x01, 0x01, // AID (Application Identifier)
            0x00                      // Le (Length of expected response)
    };

    // Step 2: Select CC File (Capability Container)
    public static final byte[] CC_FILE_ID = {(byte) 0xE1, 0x03};
    public static final byte[] CC_FILE = {
            0x00, 0x0F,               // CCLEN = 15
            0x20,                     // Mapping version (2.0)
            0x00, 0x3B,               // MLe (max read)
            0x00, 0x34,               // MLc (max write)
            0x04,                     // T (NDEF File Control TLV)
            0x06,                     // L
            (byte) 0xE1, 0x04,        // File ID
            (byte) 0x70, (byte) 0xFF, // Size (0x70FF = 28,671 bytes)
            0x00,                     // Read access (unrestricted)
            0x00                      // Write access (unrestricted)
    };

    // Step 3: Select NDEF File
    public static final byte[] NDEF_FILE_ID = {(byte) 0xE1, 0x04};
    
    // Step 4: Read Binary Header
    public static final byte[] NDEF_READ_BINARY_HEADER = {0x00, (byte) 0xB0};
    
    // Step 5: Update Binary Header
    public static final byte[] NDEF_UPDATE_BINARY_HEADER = {0x00, (byte) 0xD6};

    // Success and Error Responses
    public static final byte[] NDEF_RESPONSE_OK = {(byte) 0x90, 0x00};
    public static final byte[] NDEF_RESPONSE_ERROR = {(byte) 0x6A, (byte) 0x82};
    
    // Record type flags
    public static final byte TEXT_RECORD_TYPE = 0x54; // 'T'
    public static final byte URI_RECORD_TYPE = 0x55;  // 'U'
    
    // Header flags
    public static final byte SHORT_RECORD_FLAG = 0x10; // SR flag
    public static final byte TNF_MASK = 0x07;          // Type Name Format mask
    public static final byte LANGUAGE_CODE_MASK = 0x3F; // Lower 6 bits for language code length
    
    // Buffer sizes
    public static final int MAX_NDEF_DATA_SIZE = 65536; // 64KB max
}
