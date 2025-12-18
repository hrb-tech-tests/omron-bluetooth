# OMRON Bluetooth Debugging Guide

## Current Issue
Connection succeeds but timeout occurs when waiting for data.

## Debugging Steps

### 1. Verify Actual Connection
Add more detailed logging to confirm connection state:
```java
// After bluetooth.connect() callback
System.out.println("Connected! Checking connection state...");
// Log all available services
```

### 2. Discover All Services and Characteristics
Instead of assuming the Blood Pressure Service, scan all:
```java
// Use bluetooth.discoverServices() to see what's actually available
// Log all UUIDs found
```

### 3. Check if Device Requires Pairing
Try pairing before connecting:
```java
bluetooth.pair(deviceMac);
Thread.sleep(2000); // Wait for pairing
bluetooth.connect(...);
```

### 4. Try Reading Instead of Subscribing
The device might not support notifications:
```java
// Instead of subscribe(), try read()
bluetooth.read(callback, deviceMac, serviceUUID, characteristicUUID);
```

### 5. Check for Control Characteristics
Some devices need a "start transfer" command:
```java
// Write a command to trigger data transfer
// Common values: 0x01, 0x02, or specific protocol commands
bluetooth.write(deviceMac, serviceUUID, controlCharUUID, commandBytes);
```

### 6. Use Bluetooth Scanner App
Install "nRF Connect" or similar on your test device:
1. Scan for the OMRON device
2. Connect to it
3. Note all services and characteristics
4. Try reading/subscribing to each
5. See which ones return data

### 7. Check OMRON Documentation
Look for:
- Official developer documentation
- Communication protocol specifications
- Required initialization sequences

## Alternative: Native Implementation

If Codename One's Bluetooth library doesn't work, create a native interface:

### Android Native (cn1lib)
```java
// Use Android's BluetoothGatt API directly
// Or integrate OMRON's official Android SDK
```

### iOS Native (cn1lib)
```java
// Use CoreBluetooth framework
// Or integrate OMRON's official iOS SDK
```

## Recommended Next Steps

1. **Use nRF Connect** to understand the device's actual Bluetooth profile
2. **Search for OMRON developer resources** - they may have official SDKs
3. **Check if pairing is required** before data transfer
4. **Consider native implementation** if CN1's Bluetooth library is insufficient
5. **Look for existing implementations** - search GitHub for OMRON + Bluetooth projects
