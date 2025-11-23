package com.electricdreams.shellshock.ndef;

import java.nio.charset.Charset;

/**
 * Builder for creating NDEF messages from text content
 */
public class NdefMessageBuilder {
    
    /**
     * Create an NDEF Text record message from a string.
     *
     * Uses a short record (SR = 1, 1-byte payload length) when payload <= 255 bytes,
     * and a normal record (SR = 0, 4-byte payload length) for larger payloads.
     */
    /**
     * Create an NDEF Text record message from a string.
     *
     * Uses a short record (SR = 1, 1-byte payload length) when payload <= 255 bytes,
     * and a normal record (SR = 0, 4-byte payload length) for larger payloads.
     */
    public static byte[] createNdefMessage(String message) {
        byte[] languageCode = "en".getBytes(Charset.forName("US-ASCII"));
        byte[] textBytes = message.getBytes(Charset.forName("UTF-8"));
        byte statusByte = (byte) languageCode.length; // UTF-8 + language length

        // Create payload: [status][languageCode][text]
        byte[] payload = new byte[1 + languageCode.length + textBytes.length];
        payload[0] = statusByte;
        System.arraycopy(languageCode, 0, payload, 1, languageCode.length);
        System.arraycopy(textBytes, 0, payload, 1 + languageCode.length, textBytes.length);

        // Type "T" (Text)
        byte[] type = "T".getBytes(Charset.forName("US-ASCII"));

        // Decide whether to use a short record based on payload length
        boolean isShortRecord = payload.length <= 255; // SR requires 1-byte payload length

        // Header size depends on SR flag:
        //  short: 1 (header) + 1 (typeLen) + 1 (payloadLen)
        //  normal: 1 (header) + 1 (typeLen) + 4 (payloadLen)
        int variableHeaderLen = isShortRecord ? 3 : 6;
        byte[] recordHeader = new byte[variableHeaderLen + type.length + payload.length];

        // MB + ME + TNF=1 (well-known). Start from 0xD1 (MB=1, ME=1, SR=1, TNF=1)
        byte headerByte = (byte) 0xD1;
        if (!isShortRecord) {
            // Clear the SR bit (0x10) for normal records
            headerByte = (byte) (headerByte & ~0x10);
        }
        recordHeader[0] = headerByte;

        // Type length
        recordHeader[1] = (byte) type.length;

        int idx;
        if (isShortRecord) {
            // 1-byte payload length
            recordHeader[2] = (byte) payload.length;
            idx = 3;
        } else {
            // 4-byte payload length (big-endian)
            int pl = payload.length;
            recordHeader[2] = (byte) ((pl >> 24) & 0xFF);
            recordHeader[3] = (byte) ((pl >> 16) & 0xFF);
            recordHeader[4] = (byte) ((pl >> 8) & 0xFF);
            recordHeader[5] = (byte) (pl & 0xFF);
            idx = 6;
        }

        // Type field
        System.arraycopy(type, 0, recordHeader, idx, type.length);
        idx += type.length;

        // Payload
        System.arraycopy(payload, 0, recordHeader, idx, payload.length);

        // Create full NDEF message: 2-byte length + record contents
        int ndefLength = recordHeader.length;
        byte[] fullMessage = new byte[2 + recordHeader.length];
        fullMessage[0] = (byte) ((ndefLength >> 8) & 0xFF);
        fullMessage[1] = (byte) (ndefLength & 0xFF);
        System.arraycopy(recordHeader, 0, fullMessage, 2, recordHeader.length);

        return fullMessage;
    }
}
