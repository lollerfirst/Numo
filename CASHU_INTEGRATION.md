# Cashu Integration in Modern POS

## Overview
The ModernPOSActivity now includes full Cashu payment processing capabilities, allowing users to:
- Pay with Cashu tokens using NFC cards
- Receive and import Cashu tokens to NFC cards
- Process regular transactions
- Manage Cashu proofs through the SatocashWallet

## Features

### 1. Payment Methods
The POS now supports three payment methods:
- **Cashu (NFC Card)**: Pay using proofs stored on an NFC card
- **Regular Transaction**: Standard transaction processing
- **Import Token**: Import a Cashu token to an NFC card

### 2. NFC Integration
- Automatic NFC adapter detection
- Foreground NFC dispatch for seamless card interactions
- Support for IsoDep technology
- Secure communication with Satocash applets

### 3. Cashu Wallet Integration
- PIN-based authentication
- Proof selection and coin management
- Token creation and import
- Secure channel communication

## User Flow

### Making a Payment (Streamlined)
1. **Enter the amount** using the integrated keyboard
2. **Press "Submit Transaction"**
3. **NFC Detection Dialog** appears: "Please tap your NFC card to pay"
4. **Tap NFC card** - dialog dismisses automatically
5. **Enter PIN** using the custom number pad
6. **Payment is processed** and token is generated
7. **Token can be copied** to clipboard

### Alternative Methods (via Menu)
- **Import Token**: Access via menu → "Import Proof" → Enter token → Tap card → Enter PIN
- **Regular Transaction**: Can be added as menu option if needed

## Key Changes Made
- **Streamlined Flow**: Submit button now directly prompts for NFC detection
- **Automatic Dialog Dismissal**: NFC detection dialog closes when card is detected
- **Clear Visual Feedback**: Status messages show progress at each step
- **Simplified UX**: No payment method selection - assumes Cashu payment by default

## Technical Components

### Key Classes
- **ModernPOSActivity**: Main POS interface with Cashu integration
- **SatocashWallet**: Handles Cashu operations and proof management
- **SatocashNfcClient**: NFC communication with Satocash cards
- **ImportProofActivity**: Dedicated activity for proof import

### NFC Handling
- Automatic applet discovery and selection
- Secure channel initialization
- PIN authentication with custom number pad
- Error handling and user feedback

### UI Features
- Modern design with dark theme support
- Integrated number keyboard
- Real-time status messages
- Progress indicators
- Custom dialogs for PIN entry

## Payment Modes
The application supports three payment modes:
- `NONE`: Default state, no payment method selected
- `CASHU_PAY`: NFC card payment mode
- `CASHU_RECEIVE`: Token import mode

## Security Features
- PIN-based authentication
- Secure channel communication
- Encrypted proof storage
- NFC security protocols
- Error handling and validation

## Integration Points
The Cashu functionality integrates with:
- Original POS transaction flow
- NFC card management
- Proof import/export
- Token generation and validation
- Menu system for proof management

## Usage Instructions

1. **Setup**: Ensure NFC is enabled on the device
2. **Payment**: Enter amount → Submit → Select Cashu → Tap card → Enter PIN
3. **Import**: Submit → Import Token → Paste token → Tap card → Enter PIN
4. **Regular**: Enter amount → Submit → Select Regular Transaction

## Error Handling
The system includes comprehensive error handling for:
- NFC communication failures
- PIN authentication errors
- Proof selection issues
- Token parsing problems
- Network connectivity issues

## Future Enhancements
- QR code generation for tokens
- Balance display from NFC cards
- Transaction history
- Multi-mint support
- Batch proof operations
