# nRF Connect Testing Guide for OMRON HEM-7144T2

## Prerequisites
- ✅ nRF Connect app installed
- ✅ nRF Logger app installed (for detailed logs)
- OMRON HEM-7144T2 device powered on and in pairing mode
- Bluetooth enabled on your phone

## Step 1: Connect to Your OMRON Device

### 1.1 Prepare the Device
1. **Turn on your OMRON HEM-7144T2**
2. **Put it in pairing mode** (check device manual, usually involves pressing a button)
   - Some OMRON devices enter pairing mode automatically when turned on
   - Others require holding a specific button combination

### 1.2 Scan for the Device
1. Open **nRF Connect** app
2. Tap **SCAN** button at the top
3. Look for your device in the list:
   - Device name might be: `BLEsmart_xxxx`, `OMRON_xxxx`, or similar
   - Look for devices with strong signal strength (closer to 0 dBm)
   - Your device MAC address should match what you've been using

### 1.3 Connect
1. Tap **CONNECT** next to your OMRON device
2. Wait for connection to establish
3. You should see "Connected" status

**📝 Note:** If connection fails, try:
- Restarting the OMRON device
- Ensuring no other app is connected to it
- Moving closer to the device

---

## Step 2: Discover Services and Characteristics

Once connected, nRF Connect will automatically discover all services.

### 2.1 Identify Services
You'll see a list of services with UUIDs. Look for:

**Standard Bluetooth Services:**
- `0x1810` - Blood Pressure Service (what we're currently using)
- `0x180A` - Device Information Service
- `0x180F` - Battery Service

**Custom OMRON Services:**
- Any service with a longer UUID (128-bit) is likely OMRON-specific
- Example: `0000xxxx-0000-1000-8000-00805f9b34fb`

### 2.2 Document All Services
For each service, note down:
1. **Service UUID**
2. **Service Name** (if recognized)
3. **All characteristics** within that service

### 2.3 Examine Characteristics
For each characteristic, you'll see icons indicating its properties:
- 📖 **Read** - Can read data
- ✏️ **Write** - Can write data
- 🔔 **Notify** - Can subscribe to notifications
- 📣 **Indicate** - Can subscribe to indications

**Take screenshots of all services and characteristics!**

---

## Step 3: Trigger Data Transfer Manually

### 3.1 Try Reading from Blood Pressure Measurement
1. Find the **Blood Pressure Service** (`0x1810`)
2. Locate the **Blood Pressure Measurement** characteristic (`0x2A35`)
3. Tap the **↓ (Read)** icon
4. Check if data is returned

**Expected Result:**
- ✅ Data appears → Device supports direct reading
- ❌ Error/No data → Device requires notifications or different approach

### 3.2 Subscribe to Notifications
1. On the same characteristic (`0x2A35`)
2. Tap the **🔔 (Notify)** icon to enable notifications
3. Observe if any data arrives automatically

**Expected Result:**
- ✅ Data arrives → Device sends data via notifications
- ❌ No data → May need to trigger transfer first

### 3.3 Look for Control Characteristics
Some devices need a "start transfer" command:

1. Look for characteristics with **Write** property
2. Common names: "Control Point", "Command", "Request"
3. Try writing common trigger values:
   - `0x01` (Start)
   - `0x02` (Get stored data)
   - `0x03` (Clear data)

**How to write:**
1. Tap the **↑ (Write)** icon
2. Select **BYTE ARRAY** format
3. Enter hex value (e.g., `01`)
4. Tap **SEND**
5. Check if notifications start arriving

### 3.4 Check Device Information Service
1. Find **Device Information Service** (`0x180A`)
2. Read characteristics like:
   - Manufacturer Name
   - Model Number
   - Serial Number
   - Firmware Revision

This confirms basic communication works.

---

## Step 4: Verify Issue Source

### 4.1 If Data Transfer Works in nRF Connect
**✅ This means:**
- The device itself is working correctly
- Bluetooth communication is possible
- **The issue is with the Codename One implementation**

**Next steps:**
1. Document the exact sequence that works in nRF Connect
2. Compare with our current code
3. Identify what's different (service UUID, characteristic UUID, read vs notify, etc.)

### 4.2 If Data Transfer Fails in nRF Connect
**❌ This means:**
- The device may require special initialization
- There might be a pairing/bonding requirement
- The device might need the official app first

**Next steps:**
1. Check if device is paired in phone's Bluetooth settings
2. Try using official OMRON app first, then retry nRF Connect
3. Look for device-specific documentation

### 4.3 If Notifications Never Arrive
**Possible causes:**
1. **Need to enable indications instead of notifications**
   - Try the **📣** icon instead of **🔔**
2. **Need to write to Client Characteristic Configuration Descriptor (CCCD)**
   - This should happen automatically, but verify
3. **Need to trigger transfer with a write command first**
   - See Step 3.3 above

---

## Step 5: Enable Detailed Logging

### 5.1 Using nRF Logger
1. Open **nRF Logger** app
2. Tap **Start Logging**
3. Go back to **nRF Connect**
4. Perform your connection and data transfer attempts
5. Return to **nRF Logger** to view detailed logs

### 5.2 What to Look For in Logs
- Connection parameters
- Service discovery results
- Characteristic read/write/notify operations
- Any error codes
- Timing information

---

## Step 6: Document Your Findings

Create a document with:

### 6.1 Device Information
```
Device Name: [from scan]
MAC Address: [from scan]
Signal Strength: [from scan]
```

### 6.2 Services Found
```
Service 1: [UUID] - [Name]
  Characteristic 1: [UUID] - [Properties: R/W/N/I]
  Characteristic 2: [UUID] - [Properties: R/W/N/I]

Service 2: [UUID] - [Name]
  ...
```

### 6.3 Working Sequence
```
1. Connect to device
2. Discover services
3. [Write 0x01 to characteristic XXXX] (if needed)
4. [Subscribe to notifications on characteristic YYYY]
5. [Data arrives / doesn't arrive]
```

### 6.4 Comparison with Current Code
```
Current Code Uses:
- Service: 0x1810 (Blood Pressure Service)
- Characteristic: 0x2A35 (Blood Pressure Measurement)
- Method: Subscribe to notifications

nRF Connect Working Method:
- Service: [what actually works]
- Characteristic: [what actually works]
- Method: [read/notify/indicate/write first]
```

---

## Common OMRON Device Patterns

Based on similar devices, here are common patterns:

### Pattern 1: Standard Blood Pressure Profile
```
1. Connect
2. Subscribe to 0x2A35 notifications
3. Data arrives automatically
```

### Pattern 2: Requires Trigger Command
```
1. Connect
2. Write 0x01 to control characteristic
3. Subscribe to 0x2A35 notifications
4. Data arrives
```

### Pattern 3: Custom OMRON Protocol
```
1. Connect
2. Use custom OMRON service (not standard 0x1810)
3. Write specific command sequence
4. Read from custom characteristic
```

### Pattern 4: Requires Pairing First
```
1. Pair device in phone settings
2. Connect in app
3. Subscribe to notifications
4. Data arrives
```

---

## Troubleshooting Tips

### Connection Issues
- Ensure device is in pairing mode
- Restart both device and phone
- Forget device in Bluetooth settings and re-pair
- Check battery level

### No Data Received
- Try both notifications AND indications
- Look for write-enabled characteristics to trigger transfer
- Check if device needs to be in a specific mode
- Verify device has stored measurements

### Intermittent Issues
- Check signal strength (should be > -70 dBm)
- Reduce distance between phone and device
- Avoid interference from other Bluetooth devices

---

## Next Steps After Testing

Once you've completed this testing, share:

1. **Screenshots** of all services and characteristics
2. **The exact sequence** that successfully retrieves data (if any)
3. **Any error messages** you encountered
4. **Comparison** with what our current code is doing

This information will help us:
- Fix the Codename One implementation if the device works
- Determine if we need a different approach if it doesn't
- Identify any missing initialization steps
- Understand the actual protocol the device uses
