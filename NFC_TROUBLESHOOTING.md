# NFC Integration Troubleshooting Guide

## What Was Completed

✅ **NFC Integration Setup**
- Added NFC permissions to AndroidManifest.xml
- Added NFC intent filter to ModernPOSActivity
- Set `android:launchMode="singleTop"` for proper NFC handling
- Added comprehensive NFC detection logic

✅ **Code Implementation**
- Enhanced `initializeNFC()` to check if NFC is enabled
- Added detailed logging in `onNewIntent()` for debugging
- Updated `handleNFCTag()` to handle multiple NFC action types
- Added proper error handling and user feedback

✅ **Payment Flow**
- Submit button sets payment mode to `CASHU_PAY`
- Shows NFC detection dialog immediately
- NFC foreground dispatch is properly configured
- Auto-dismisses dialog when card is detected

## Current Issue

The application is not detecting NFC devices when Submit is clicked. Here's what to check:

### 1. Check NFC Status
When you run the app, check the logs for:
```
NFC adapter initialized and enabled
```
If you see:
```
NFC is not enabled
```
Then you need to enable NFC in your device settings.

### 2. Check NFC Foreground Dispatch
Look for these log messages:
```
NFC foreground dispatch enabled
```
This should appear when the activity resumes.

### 3. Test NFC Detection
1. Enter an amount (e.g., "100")
2. Click "Submit Transaction"
3. You should see the NFC detection dialog
4. Tap an NFC card/device
5. Check logs for: `onNewIntent called with action: android.nfc.action.TECH_DISCOVERED`

## Debugging Steps

### Step 1: Check Device NFC
```bash
# Check if NFC is enabled in device settings
# Settings > Connected devices > NFC
```

### Step 2: Check App Logs
```bash
# Run this to see app logs:
adb logcat | grep "ModernPOS"
```

### Step 3: Test with Any NFC Card
Try with:
- Credit/debit cards with NFC
- Transit cards
- NFC-enabled phone (Android Beam)
- Any NFC tag

### Step 4: Check Manifest Registration
The ModernPOSActivity should have these in AndroidManifest.xml:
```xml
<activity android:name=".ModernPOSActivity"
    android:launchMode="singleTop">
    <intent-filter>
        <action android:name="android.nfc.action.TECH_DISCOVERED" />
    </intent-filter>
    <meta-data android:name="android.nfc.action.TECH_DISCOVERED"
        android:resource="@xml/nfc_tech_filter" />
</activity>
```

## Expected Behavior

1. **Click Submit** → NFC detection dialog appears
2. **Tap NFC card** → Dialog dismisses, shows "NFC card detected!"
3. **PIN dialog** → Enter PIN for authentication
4. **Payment processing** → Token generation and success message

## If Still Not Working

### Try Alternative NFC Actions
The code now handles multiple NFC actions:
- `android.nfc.action.TECH_DISCOVERED`
- `android.nfc.action.TAG_DISCOVERED`
- `android.nfc.action.NDEF_DISCOVERED`

### Check NFC Tech Filter
Make sure `/res/xml/nfc_tech_filter.xml` contains:
```xml
<tech-list>
    <tech>android.nfc.tech.IsoDep</tech>
</tech-list>
```

### Test with Different Cards
Different NFC cards use different technologies:
- Credit cards: Usually IsoDep
- Some tags: NfcA, NfcB, NfcF
- May need to expand tech filter

## Next Steps

1. **Enable NFC** on your device if not already enabled
2. **Run the app** and check logs for NFC initialization
3. **Test with any NFC card** to see if `onNewIntent` is called
4. **Check logs** for detailed debugging information
5. **Report what you see** in the logs when tapping NFC cards

The code is now properly set up for NFC detection. The issue is likely one of:
- NFC not enabled on device
- Need to use different NFC technology in filter
- Need to test with different types of NFC cards
