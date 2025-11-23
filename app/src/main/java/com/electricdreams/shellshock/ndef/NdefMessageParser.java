package com.electricdreams.shellshock.ndef;

import android.util.Log;
import java.util.Arrays;

/**
 * Parses received NDEF messages and extracts text/URI content
 */
public class NdefMessageParser {
    private static final String TAG = "NdefMessageParser";
    
    private final NdefProcessor.NdefMessageCallback callback;
    
    public NdefMessageParser(NdefProcessor.NdefMessageCallback callback) {
        this.callback = callback;
    }
    
    /**
     * Process a received NDEF message
     */
    public void processReceivedNdefMessage(byte[] ndefData, boolean processIncomingMessages) {
        Log.i(TAG, "Processing received NDEF message, process flag: " + processIncomingMessages);
        
        // Skip processing if we're not supposed to process incoming messages
        if (!processIncomingMessages) {
            Log.i(TAG, "Ignoring incoming NDEF message because processIncomingMessages is false");
            return;
        }
        
        Log.i(TAG, "Hex dump: " + NdefUtils.bytesToHex(Arrays.copyOfRange(ndefData, 0, Math.min(ndefData.length, 100))));
        
        int offset = 0;
        int totalLength = 0;
        
        // Detect framing:
        // Type 4: first two bytes form the NDEF file length
        if (ndefData.length >= 2) {
            Log.i(TAG, "Type 4 style NDEF");
            totalLength = ((ndefData[0] & 0xFF) << 8) | (ndefData[1] & 0xFF);
            Log.i(TAG, "NDEF message total length from header: " + totalLength);
            
            // Validate message length - don't process empty or very short messages
            if (totalLength <= 0) {
                Log.e(TAG, "Invalid NDEF data - zero or negative length in header, ignoring message");
                return;
            }
            
            // Ensure we have enough data
            if (totalLength + 2 > ndefData.length) {
                Log.e(TAG, "Incomplete NDEF data - header specifies " + totalLength + 
                      " bytes but we only have " + (ndefData.length - 2) + " bytes of payload");
                return;
            }
            
            offset = 2;
        } else {
            Log.e(TAG, "Invalid NDEF data - length less than 2 bytes");
            return;
        }
        
        try {
            parseNdefRecord(ndefData, offset);
        } catch (Exception e) {
            Log.e(TAG, "Error extracting data from NDEF message: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse an NDEF record starting at the given offset
     */
    private void parseNdefRecord(byte[] ndefData, int offset) {
        if (offset >= ndefData.length) {
            Log.e(TAG, "Invalid offset beyond data length");
            return;
        }
        
        // Read record header starting at offset
        byte header = ndefData[offset];
        Log.i(TAG, "NDEF header byte: 0x" + String.format("%02X", header));
        
        if (offset + 1 >= ndefData.length) {
            Log.e(TAG, "Invalid data - can't read type length");
            return;
        }
        
        int typeLength = ndefData[offset + 1] & 0xFF;
        Log.i(TAG, "NDEF type length: " + typeLength);
        
        // Additional validation for type length
        if (typeLength <= 0) {
            Log.e(TAG, "Invalid type length: " + typeLength);
            return;
        }
        
        // Determine payload length field size based on the SR flag (0x10)
        int payloadLength;
        int typeFieldStart;
        
        // Check SR (Short Record) flag
        boolean isShortRecord = (header & NdefConstants.SHORT_RECORD_FLAG) != 0;
        Log.i(TAG, "Is short record: " + isShortRecord);
        
        if (isShortRecord) { // Short record: 1 byte payload length
            if (offset + 2 >= ndefData.length) {
                Log.e(TAG, "Invalid data - can't read short record payload length");
                return;
            }
            
            payloadLength = ndefData[offset + 2] & 0xFF;
            typeFieldStart = offset + 3;
            Log.i(TAG, "Short record payload length: " + payloadLength);
        } else { // Normal record: payload length is 4 bytes
            if (offset + 5 >= ndefData.length) {
                Log.e(TAG, "Invalid data - can't read normal record payload length");
                return;
            }
            
            payloadLength = ((ndefData[offset + 2] & 0xFF) << 24) |
                    ((ndefData[offset + 3] & 0xFF) << 16) |
                    ((ndefData[offset + 4] & 0xFF) << 8) |
                    (ndefData[offset + 5] & 0xFF);
            typeFieldStart = offset + 6;
            Log.i(TAG, "Normal record payload length: " + payloadLength);
        }
        
        // Validate payload length
        if (payloadLength <= 0) {
            Log.e(TAG, "Invalid payload length: " + payloadLength);
            return;
        }
        
        // Safety check for typeFieldStart
        if (typeFieldStart >= ndefData.length) {
            Log.e(TAG, "Invalid typeFieldStart beyond data length");
            return;
        }
        
        // Check TNF (Type Name Format)
        int tnf = header & NdefConstants.TNF_MASK;
        Log.i(TAG, "TNF: " + tnf);
        
        // Check if we have a valid type field
        if (typeFieldStart + typeLength > ndefData.length) {
            Log.e(TAG, "Type field extends beyond data bounds");
            return;
        }
        
        // Get the record type
        byte[] typeField = Arrays.copyOfRange(ndefData, typeFieldStart, typeFieldStart + typeLength);
        String typeStr = new String(typeField);
        Log.i(TAG, "Record type: " + typeStr + " (hex: " + NdefUtils.bytesToHex(typeField) + ")");
        
        // For text records, verify the record type is "T" (0x54)
        // For URI records, verify the record type is "U" (0x55)
        boolean isTextRecord = (typeLength == 1 && ndefData[typeFieldStart] == NdefConstants.TEXT_RECORD_TYPE);
        boolean isUriRecord = (typeLength == 1 && ndefData[typeFieldStart] == NdefConstants.URI_RECORD_TYPE);
        
        Log.i(TAG, "Is Text Record: " + isTextRecord);
        Log.i(TAG, "Is URI Record: " + isUriRecord);
        
        if (!isTextRecord && !isUriRecord) {
            Log.w(TAG, "NDEF message is neither a Text Record nor URI Record. Type: " + 
                  NdefUtils.bytesToHex(typeField) + ", returning");
            return;
        }
        
        // Payload starts immediately after the type field
        int payloadStart = typeFieldStart + typeLength;
        Log.i(TAG, "Payload start position: " + payloadStart);
        
        if (payloadStart >= ndefData.length) {
            Log.e(TAG, "Payload start index out of bounds, returning");
            return;
        }
        
        if (isTextRecord) {
            parseTextRecord(ndefData, payloadStart, payloadLength);
        } else if (isUriRecord) {
            parseUriRecord(ndefData, payloadStart, payloadLength);
        }
    }
    
    /**
     * Parse a text record
     */
    private void parseTextRecord(byte[] ndefData, int payloadStart, int payloadLength) {
        try {
            // For a Text record, first payload byte is the status byte
            byte status = ndefData[payloadStart];
            // Lower 6 bits of status indicate the language code length
            int languageCodeLength = status & NdefConstants.LANGUAGE_CODE_MASK;
            Log.i(TAG, "Language code length: " + languageCodeLength);
            
            int textStart = payloadStart + 1 + languageCodeLength;
            int textLength = payloadLength - 1 - languageCodeLength;
            Log.i(TAG, "Text start position: " + textStart + ", length: " + textLength);
            
            if (textStart + textLength > ndefData.length) {
                Log.e(TAG, "Text extraction bounds exceed data size: " + 
                      textStart + " + " + textLength + " > " + ndefData.length);
                return;
            }
            
            byte[] textBytes = Arrays.copyOfRange(ndefData, textStart, textStart + textLength);
            String text = new String(textBytes, "UTF-8");
            
            Log.i(TAG, "Extracted text: " + text);
            
            // Call the callback if set
            if (callback != null) {
                Log.i(TAG, "Calling onNdefMessageReceived with text: " + text);
                callback.onNdefMessageReceived(text);
            } else {
                Log.e(TAG, "Callback is null, can't deliver message");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing text record: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parse a URI record
     */
    private void parseUriRecord(byte[] ndefData, int payloadStart, int payloadLength) {
        try {
            // URI Record handling - first byte is the URI identifier code
            byte uriIdentifierCode = ndefData[payloadStart];
            Log.i(TAG, "URI identifier code: " + uriIdentifierCode);
            
            int uriStart = payloadStart + 1;
            int uriLength = payloadLength - 1;
            Log.i(TAG, "URI start position: " + uriStart + ", length: " + uriLength);
            
            if (uriStart + uriLength > ndefData.length) {
                Log.e(TAG, "URI extraction bounds exceed data size");
                return;
            }
            
            byte[] uriBytes = Arrays.copyOfRange(ndefData, uriStart, uriStart + uriLength);
            String uri = new String(uriBytes, "UTF-8");
            
            // Prepend the URI prefix according to the identifier code
            String prefix = NdefUriProcessor.getUriPrefix(uriIdentifierCode);
            String fullUri = prefix + uri;
            Log.i(TAG, "Extracted URI: " + fullUri);
            
            // Call the callback if set
            if (callback != null) {
                Log.i(TAG, "Calling onNdefMessageReceived with URI: " + fullUri);
                callback.onNdefMessageReceived(fullUri);
            } else {
                Log.e(TAG, "Callback is null, can't deliver message");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing URI record: " + e.getMessage(), e);
        }
    }
}
