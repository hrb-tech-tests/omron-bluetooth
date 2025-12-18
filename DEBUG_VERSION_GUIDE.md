# Debug Version Usage Guide

## Overview
I've created an enhanced debugging version of the OMRON Bluetooth app that logs every step of the communication process. This will help us identify exactly where the failure occurs.

## New Files Created

### 1. `OmronBluetoothServiceDebug.java`
Enhanced version of the Bluetooth service with:
- **Timestamped logging** - Every action is logged with millisecond precision
- **State tracking** - Monitors connection, subscription, and notification states
- **Detailed error reporting** - Captures and logs all errors with context
- **Progress indicators** - Shows status every 5 seconds during wait
- **Basic communication test** - Attempts to read device information first

### 2. `OmronDebugForm.java`
Debug UI that:
- **Real-time log display** - Shows logs as they happen
- **Terminal-style interface** - Black background with green text for easy reading
- **Log persistence** - Keeps all logs until cleared
- **Clear logs button** - Reset logs between attempts
- **Success dialog** - Shows JSON data when successful

## How to Use

### Step 1: Modify Your Main App Class

Update your main application class to use the debug form:

```java
// In your main app class (e.g., MyApplication.java)
import com.example.omronapp.omron.OmronDebugForm;

public class MyApplication {
    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        
        // Use debug form instead of regular form
        Form debugForm = new OmronDebugForm();
        debugForm.show();
    }
}
```

### Step 2: Build and Deploy to Real Device

```bash
# Build for Android
mvn clean package -P android

# Or build for iOS
mvn clean package -P ios
```

### Step 3: Run the App on Your Device

1. **Install the app** on your Android or iOS device
2. **Turn on your OMRON device** and put it in pairing mode
3. **Open the app** - you'll see "DEBUG MODE ACTIVE" in red
4. **Enter the MAC address** of your OMRON device
5. **Tap "Get Data (Debug Mode)"**

### Step 4: Watch the Logs

The app will display detailed logs in real-time. You'll see:

```
[00000ms] === STARTING DATA RETRIEVAL ===
[00001ms] Target Device MAC: AA:BB:CC:DD:EE:FF
[00002ms] === INITIALIZING BLUETOOTH ===
[00010ms] Bluetooth already initialized
[00011ms] === CHECKING PERMISSIONS ===
[00015ms] Permissions - Scan: true, Connect: true, Location: true
[00016ms] === CHECKING BLUETOOTH STATE ===
[00020ms] Bluetooth enabled: true
[00021ms] Has Bluetooth permission: true
[00022ms] === VALIDATING MAC ADDRESS ===
[00023ms] MAC address validated: AA:BB:CC:DD:EE:FF
[00024ms] === INITIATING CONNECTION ===
[00025ms] Calling bluetooth.connect() for: AA:BB:CC:DD:EE:FF
[00026ms] bluetooth.connect() call completed (async)
[00027ms] === WAITING FOR DATA ===
[00028ms] Timeout set to: 30 seconds
[00500ms] === CONNECTION CALLBACK TRIGGERED ===
[00501ms] Connection established to: AA:BB:CC:DD:EE:FF
[00502ms] === TESTING BASIC COMMUNICATION ===
[00510ms] Attempting to read manufacturer name...
[00520ms] === SUBSCRIBING TO MEASUREMENTS ===
[00521ms] Calling bluetooth.subscribe()...
[00522ms] Service UUID: 00001810-0000-1000-8000-00805f9b34fb
[00523ms] Characteristic UUID: 00002a35-0000-1000-8000-00805f9b34fb
[00524ms] bluetooth.subscribe() call completed (async)
[05000ms] Waiting... 5s elapsed | Connected: true | Subscribed: false | Notifications: 0
[10000ms] Waiting... 10s elapsed | Connected: true | Subscribed: false | Notifications: 0
...
```

## What to Look For

### Success Indicators
✅ `Connection established to: [MAC]`
✅ `Subscription started: true`
✅ `Notifications received: [count > 0]`
✅ `Measurement parsed successfully`

### Failure Indicators
❌ Connection callback never triggers
❌ `Subscription started: false` after 30 seconds
❌ `Notifications received: 0` throughout
❌ `ERROR` messages at any point

## Common Scenarios and What They Mean

### Scenario 1: Connection Never Establishes
```
[00027ms] === WAITING FOR DATA ===
[05000ms] Waiting... 5s elapsed | Connected: false | Subscribed: false | Notifications: 0
[30000ms] === TIMEOUT REACHED ===
[30001ms] Connection established: false
```
**Meaning**: The device isn't responding to connection attempts
**Possible causes**:
- Wrong MAC address
- Device not in pairing mode
- Device already connected to another app
- Bluetooth permission issues

### Scenario 2: Connection Succeeds, No Subscription
```
[00500ms] === CONNECTION CALLBACK TRIGGERED ===
[00501ms] Connection established to: AA:BB:CC:DD:EE:FF
[00524ms] bluetooth.subscribe() call completed (async)
[30000ms] === TIMEOUT REACHED ===
[30001ms] Connection established: true
[30002ms] Subscription started: false
```
**Meaning**: Connected but subscription never starts
**Possible causes**:
- Service UUID is wrong
- Characteristic UUID is wrong
- Device doesn't support notifications
- Need to enable indications instead

### Scenario 3: Subscription Works, No Notifications
```
[00500ms] === CONNECTION CALLBACK TRIGGERED ===
[00524ms] bluetooth.subscribe() call completed (async)
[01000ms] === NOTIFICATION RECEIVED (#1) ===
[01001ms] Empty notification received - assuming end of data transfer
[30000ms] === TIMEOUT REACHED ===
[30001ms] Connection established: true
[30002ms] Subscription started: true
[30003ms] Notifications received: 1
```
**Meaning**: Subscription works but device sends empty notification immediately
**Possible causes**:
- Device has no stored measurements
- Need to trigger data transfer with a write command first
- Device is waiting for user action (button press)

### Scenario 4: Notifications Arrive But Parse Fails
```
[01000ms] === NOTIFICATION RECEIVED (#1) ===
[01001ms] Value present: true
[01002ms] Value length: 24
[01003ms] Decoding value...
[01004ms] ERROR parsing notification: Invalid data format
```
**Meaning**: Data is arriving but in unexpected format
**Possible causes**:
- Device uses custom protocol (not standard Blood Pressure Profile)
- Data encoding is different than expected
- Need to use different characteristic

## Sharing Results

After running the debug version, please share:

1. **Complete log output** - Copy all text from the log area
2. **Screenshots** - Capture the debug screen showing the logs
3. **Which scenario** matches what you see (1, 2, 3, or 4 above)
4. **Any error dialogs** that appear

This information will tell us exactly what needs to be fixed!

## Reverting to Normal Mode

To go back to the regular (non-debug) version:

```java
// In your main app class
import com.example.omronapp.omron.OmronExampleForm;

public class MyApplication {
    public void start() {
        Form normalForm = new OmronExampleForm();
        normalForm.show();
    }
}
```

## Next Steps Based on Results

Once we see the debug logs, we can:
- **Fix the service/characteristic UUIDs** if they're wrong
- **Add initialization commands** if device needs triggering
- **Switch to read instead of subscribe** if notifications don't work
- **Implement custom protocol** if device doesn't use standard profile
- **Add pairing logic** if that's required
- **Create native implementation** if Codename One library has limitations
