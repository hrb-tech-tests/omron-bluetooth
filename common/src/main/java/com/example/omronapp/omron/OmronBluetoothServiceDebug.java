package com.example.omronapp.omron;

import com.codename1.bluetoothle.Bluetooth;
import com.codename1.ui.Display;
import com.codename1.ui.CN;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.util.Base64;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.codename1.location.LocationManager;
import com.codename1.location.LocationListener;
import com.codename1.location.Location;
import com.codename1.ui.Dialog;

/**
 * ENHANCED DEBUG VERSION of OmronBluetoothService
 * 
 * This version includes extensive logging to help diagnose Bluetooth
 * communication issues.
 * Every step is logged with timestamps and detailed information.
 */
public class OmronBluetoothServiceDebug {

    // Bluetooth SIG standard UUIDs for Blood Pressure Profile
    private static final String BLOOD_PRESSURE_SERVICE_UUID = "00001810-0000-1000-8000-00805f9b34fb";
    private static final String BLOOD_PRESSURE_MEASUREMENT_UUID = "00002a35-0000-1000-8000-00805f9b34fb";

    // Device Information Service (for testing basic communication)
    private static final String DEVICE_INFO_SERVICE_UUID = "0000180a-0000-1000-8000-00805f9b34fb";
    private static final String MANUFACTURER_NAME_UUID = "00002a29-0000-1000-8000-00805f9b34fb";

    // Device model identifier
    private static final String DEVICE_MODEL = "HEM-7144T2";

    // Timeout for data collection (milliseconds)
    private static final long DATA_COLLECTION_TIMEOUT = 30000;

    // Debug log collector
    private final List<String> debugLogs = new ArrayList<>();
    private long startTime = 0;

    private Bluetooth bluetooth;
    private boolean bluetoothInitialized = false;
    private String initializationError = null;
    private volatile boolean aborted = false;

    /**
     * Abort the current operation
     */
    public void abort() {
        aborted = true;
        log("!!! ABORT REQUESTED BY USER !!!");
    }

    /**
     * Check if the operation has been aborted
     */
    public boolean isAborted() {
        return aborted;
    }

    /**
     * Reset the abort flag
     */
    public void resetAbort() {
        aborted = false;
        log("Abort flag reset");
    }

    /**
     * Get all debug logs collected during the last operation
     */
    public List<String> getDebugLogs() {
        return new ArrayList<>(debugLogs);
    }

    /**
     * Clear debug logs
     */
    public void clearDebugLogs() {
        debugLogs.clear();
    }

    /**
     * Add a timestamped debug log entry
     */
    private void log(String message) {
        long elapsed = startTime > 0 ? System.currentTimeMillis() - startTime : 0;
        String logEntry = String.format("[%05dms] %s", elapsed, message);
        debugLogs.add(logEntry);
        System.out.println("DEBUG: " + logEntry);
    }

    /**
     * Lazily initializes Bluetooth when first needed.
     */
    private synchronized void ensureBluetoothInitialized() throws OmronBluetoothException {
        log("=== INITIALIZING BLUETOOTH ===");

        if (bluetoothInitialized) {
            if (initializationError != null) {
                log("ERROR: Previous initialization failed: " + initializationError);
                throw new OmronBluetoothException(
                        OmronBluetoothException.ErrorType.DEVICE_NOT_FOUND,
                        initializationError);
            }
            log("Bluetooth already initialized");
            return;
        }

        bluetoothInitialized = true;

        // Check if running in simulator
        if (Display.getInstance().isSimulator()) {
            initializationError = "Bluetooth is not available in the simulator";
            log("ERROR: Running in simulator - Bluetooth not available");
            throw new OmronBluetoothException(
                    OmronBluetoothException.ErrorType.DEVICE_NOT_FOUND,
                    initializationError);
        }

        log("Platform: " + Display.getInstance().getPlatformName());

        try {
            bluetooth = new Bluetooth();
            log("Bluetooth instance created");

            bluetooth.initialize(true, false, "omron_bluetooth_debug");
            log("Bluetooth initialized successfully");

        } catch (IOException e) {
            initializationError = "Bluetooth initialization failed: " + e.getMessage();
            log("ERROR: " + initializationError);
            throw new OmronBluetoothException(
                    OmronBluetoothException.ErrorType.CONNECTION_FAILED,
                    initializationError);
        } catch (Exception e) {
            initializationError = "Bluetooth not supported: " + e.getMessage();
            log("ERROR: " + initializationError);
            throw new OmronBluetoothException(
                    OmronBluetoothException.ErrorType.DEVICE_NOT_FOUND,
                    initializationError);
        }
    }

    /**
     * Main method to retrieve data from device with extensive debugging
     */
    public String getDataFromDevice(String deviceMac) throws OmronBluetoothException {
        // Reset debug logs and start timer
        clearDebugLogs();
        startTime = System.currentTimeMillis();
        aborted = false;

        log("=== STARTING DATA RETRIEVAL ===");
        log("Target Device MAC: " + deviceMac);

        if (aborted) {
            throw new OmronBluetoothException(OmronBluetoothException.ErrorType.ABORTED);
        }

        // Initialize Bluetooth
        ensureBluetoothInitialized();

        // Check permissions on EDT
        log("=== CHECKING PERMISSIONS ===");
        if (CN.isEdt()) {
            checkPermissions();
        } else {
            CN.invokeAndBlock(() -> {
                checkPermissions();
            });
        }

        // Scan for device before connecting (often required on Android)
        try {
            scanForDevice(deviceMac, 10000); // Scan for up to 10 seconds
        } catch (InterruptedException e) {
            log("WARNING: Scan interrupted: " + e.getMessage());
        }

        // Check Bluetooth state
        log("=== CHECKING BLUETOOTH STATE ===");
        try {
            boolean isEnabled = bluetooth.isEnabled();
            log("Bluetooth enabled: " + isEnabled);

            if (!isEnabled) {
                log("Requesting Bluetooth enable...");
                bluetooth.enable();
                Thread.sleep(1000);
                log("Bluetooth enable requested, waited 1s");
            }

            boolean hasPermission = bluetooth.hasPermission();
            log("Has Bluetooth permission: " + hasPermission);

            if (!hasPermission) {
                log("Requesting Bluetooth permission...");
                bluetooth.requestPermission();
                log("Permission requested");
            }
        } catch (IOException e) {
            log("WARNING during state check: " + e.getMessage());
        } catch (InterruptedException e) {
            log("WARNING: Sleep interrupted");
        }

        // Validate MAC address
        log("=== VALIDATING MAC ADDRESS ===");
        validateMacAddress(deviceMac);
        log("MAC address validated: " + deviceMac);

        final List<OmronMeasurement> measurements = new ArrayList<>();
        final Object lock = new Object();
        final boolean[] completed = { false };
        final boolean[] connectionEstablished = { false };
        final boolean[] subscriptionStarted = { false };
        final int[] notificationCount = { 0 };
        final OmronBluetoothException[] error = { null };

        try {
            log("=== INITIATING CONNECTION ===");
            log("Calling bluetooth.connect() for: " + deviceMac);

            // Connect to device
            bluetooth.connect(new ActionListener<ActionEvent>() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    synchronized (lock) {
                        connectionEstablished[0] = true;
                    }

                    log("=== CONNECTION CALLBACK TRIGGERED ===");
                    log("Connection established to: " + deviceMac);
                    log("Event source type: "
                            + (evt.getSource() != null ? evt.getSource().getClass().getName() : "null"));

                    try {
                        // Try to read device information first (basic communication test)
                        log("=== TESTING BASIC COMMUNICATION ===");
                        testDeviceInformation(deviceMac);

                        // Subscribe to blood pressure notifications
                        log("=== SUBSCRIBING TO MEASUREMENTS ===");
                        subscribeToMeasurements(deviceMac, measurements, lock, completed, error,
                                subscriptionStarted, notificationCount);

                    } catch (Exception e) {
                        log("ERROR in connection callback: " + e.getMessage());
                        e.printStackTrace();
                        synchronized (lock) {
                            error[0] = new OmronBluetoothException(
                                    OmronBluetoothException.ErrorType.DATA_TRANSFER_FAILED, e);
                            completed[0] = true;
                            lock.notifyAll();
                        }
                    }
                }
            }, deviceMac);

            log("bluetooth.connect() call completed (async)");

            // Wait for data collection to complete or timeout
            log("=== WAITING FOR DATA ===");
            log("Timeout set to: " + (DATA_COLLECTION_TIMEOUT / 1000) + " seconds");

            synchronized (lock) {
                long waitStartTime = System.currentTimeMillis();
                int logInterval = 0;

                while (!completed[0]) {
                    long elapsed = System.currentTimeMillis() - waitStartTime;

                    // Log status every 5 seconds
                    if (elapsed / 5000 > logInterval) {
                        logInterval = (int) (elapsed / 5000);
                        log(String.format("Waiting... %ds elapsed | Connected: %s | Subscribed: %s | Notifications: %d",
                                elapsed / 1000,
                                connectionEstablished[0],
                                subscriptionStarted[0],
                                notificationCount[0]));
                    }

                    if (aborted) {
                        log("!!! OPERATION ABORTED DURING WAIT !!!");
                        throw new OmronBluetoothException(OmronBluetoothException.ErrorType.ABORTED);
                    }

                    if (elapsed >= DATA_COLLECTION_TIMEOUT) {
                        log("=== TIMEOUT REACHED ===");
                        log("Total wait time: " + (elapsed / 1000) + " seconds");
                        log("Connection established: " + connectionEstablished[0]);
                        log("Subscription started: " + subscriptionStarted[0]);
                        log("Notifications received: " + notificationCount[0]);
                        log("Measurements collected: " + measurements.size());

                        throw new OmronBluetoothException(OmronBluetoothException.ErrorType.TIMEOUT,
                                "Timeout after " + (DATA_COLLECTION_TIMEOUT / 1000) + " seconds. " +
                                        "Connection: " + connectionEstablished[0] + ", " +
                                        "Subscribed: " + subscriptionStarted[0] + ", " +
                                        "Notifications: " + notificationCount[0]);
                    }

                    lock.wait(1000); // Wait with 1 second intervals
                }
            }

            log("=== DATA COLLECTION COMPLETED ===");
            log("Measurements collected: " + measurements.size());

            // Check for errors during data collection
            if (error[0] != null) {
                log("ERROR during data collection: " + error[0].getMessage());
                throw error[0];
            }

            // Disconnect from device
            log("=== DISCONNECTING ===");
            disconnect(deviceMac);

            // Build and return JSON response
            log("=== BUILDING RESPONSE ===");
            OmronDeviceData deviceData = new OmronDeviceData(deviceMac, DEVICE_MODEL, measurements);
            String json = deviceData.toJSON();
            log("JSON response length: " + json.length() + " characters");
            log("=== SUCCESS ===");

            return json;

        } catch (IOException e) {
            log("ERROR: IOException - " + e.getMessage());
            throw new OmronBluetoothException(OmronBluetoothException.ErrorType.CONNECTION_FAILED, e);
        } catch (InterruptedException e) {
            log("ERROR: InterruptedException - " + e.getMessage());
            Thread.currentThread().interrupt();
            throw new OmronBluetoothException(OmronBluetoothException.ErrorType.DATA_TRANSFER_FAILED, e);
        }
    }

    /**
     * Test basic communication by reading device information
     */
    private void testDeviceInformation(String deviceMac) {
        try {
            log("Attempting to read manufacturer name...");
            bluetooth.read(new ActionListener<ActionEvent>() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    try {
                        Map<String, Object> result = (Map<String, Object>) evt.getSource();
                        String value = (String) result.get("value");
                        if (value != null) {
                            byte[] data = Base64.decode(value.getBytes());
                            String manufacturer = new String(data);
                            log("SUCCESS: Read manufacturer name: " + manufacturer);
                        } else {
                            log("WARNING: Manufacturer name read returned null");
                        }
                    } catch (Exception e) {
                        log("WARNING: Could not parse manufacturer name: " + e.getMessage());
                    }
                }
            }, deviceMac, DEVICE_INFO_SERVICE_UUID, MANUFACTURER_NAME_UUID);
        } catch (IOException e) {
            log("WARNING: Could not read device information: " + e.getMessage());
            log("This may be normal if device doesn't support Device Information Service");
        }
    }

    /**
     * Subscribe to blood pressure measurement notifications
     */
    private void subscribeToMeasurements(String deviceMac, List<OmronMeasurement> measurements,
            Object lock, boolean[] completed, OmronBluetoothException[] error,
            boolean[] subscriptionStarted, int[] notificationCount) {
        try {
            log("Calling bluetooth.subscribe()...");
            log("Service UUID: " + BLOOD_PRESSURE_SERVICE_UUID);
            log("Characteristic UUID: " + BLOOD_PRESSURE_MEASUREMENT_UUID);

            bluetooth.subscribe(new ActionListener<ActionEvent>() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    synchronized (lock) {
                        subscriptionStarted[0] = true;
                        notificationCount[0]++;
                    }

                    log("=== NOTIFICATION RECEIVED (#" + notificationCount[0] + ") ===");

                    try {
                        Map<String, Object> result = (Map<String, Object>) evt.getSource();
                        log("Notification source type: " + (result != null ? result.getClass().getName() : "null"));

                        String value = (String) result.get("value");
                        log("Value present: " + (value != null));
                        log("Value length: " + (value != null ? value.length() : 0));

                        if (value != null && !value.isEmpty()) {
                            log("Decoding value...");
                            byte[] data = Base64.decode(value.getBytes());
                            log("Decoded data length: " + data.length + " bytes");

                            log("Parsing measurement...");
                            OmronMeasurement measurement = parseMeasurement(data);
                            log("Measurement parsed successfully");
                            log("  Systolic: " + measurement.getSystolic());
                            log("  Diastolic: " + measurement.getDiastolic());
                            log("  Heart Rate: " + measurement.getHeartRate());

                            synchronized (lock) {
                                measurements.add(measurement);
                                log("Measurement added to list (total: " + measurements.size() + ")");
                            }
                        } else {
                            log("Empty notification received - assuming end of data transfer");
                            synchronized (lock) {
                                completed[0] = true;
                                lock.notifyAll();
                            }
                            log("Marked as completed and notified waiting thread");
                        }
                    } catch (Exception e) {
                        log("ERROR parsing notification: " + e.getMessage());
                        e.printStackTrace();
                        synchronized (lock) {
                            error[0] = new OmronBluetoothException(
                                    OmronBluetoothException.ErrorType.INVALID_DATA, e);
                            completed[0] = true;
                            lock.notifyAll();
                        }
                    }
                }
            }, deviceMac, BLOOD_PRESSURE_SERVICE_UUID, BLOOD_PRESSURE_MEASUREMENT_UUID);

            log("bluetooth.subscribe() call completed (async)");

        } catch (IOException e) {
            log("ERROR: IOException during subscribe: " + e.getMessage());
            synchronized (lock) {
                error[0] = new OmronBluetoothException(
                        OmronBluetoothException.ErrorType.DATA_TRANSFER_FAILED, e);
                completed[0] = true;
                lock.notifyAll();
            }
        }
    }

    /**
     * Parse blood pressure measurement from raw data
     */
    private OmronMeasurement parseMeasurement(byte[] data) throws OmronBluetoothException {
        log("Parsing measurement data...");

        if (data == null || data.length < 7) {
            log("ERROR: Insufficient data length: " + (data != null ? data.length : 0));
            throw new OmronBluetoothException(OmronBluetoothException.ErrorType.INVALID_DATA,
                    "Insufficient data length: " + (data != null ? data.length : 0));
        }

        try {
            // Parse blood pressure values (SFLOAT format)
            int systolic = parseSFloat(data, 1);
            int diastolic = parseSFloat(data, 3);
            int meanArterialPressure = parseSFloat(data, 5);

            log("Parsed BP values - Sys: " + systolic + ", Dia: " + diastolic + ", MAP: " + meanArterialPressure);

            // Parse timestamp if available
            long timestamp = System.currentTimeMillis();
            if (data.length >= 14) {
                timestamp = parseTimestamp(data, 7);
                log("Timestamp parsed from data");
            } else {
                log("Using current time as timestamp (data too short)");
            }

            // Parse heart rate if available
            int heartRate = 0;
            if (data.length >= 16) {
                heartRate = parseSFloat(data, 14);
                log("Heart rate parsed: " + heartRate);
            } else {
                log("No heart rate data available");
            }

            return new OmronMeasurement(systolic, diastolic, meanArterialPressure, heartRate, timestamp);

        } catch (Exception e) {
            log("ERROR parsing measurement: " + e.getMessage());
            throw new OmronBluetoothException(OmronBluetoothException.ErrorType.INVALID_DATA, e);
        }
    }

    /**
     * Parse IEEE 11073-20601 SFLOAT value
     */
    private int parseSFloat(byte[] data, int offset) {
        int value = ((data[offset + 1] & 0xFF) << 8) | (data[offset] & 0xFF);
        int mantissa = value & 0x0FFF;
        int exponent = value >> 12;

        // Handle negative mantissa (two's complement)
        if ((mantissa & 0x0800) != 0) {
            mantissa = -((~mantissa & 0x0FFF) + 1);
        }

        // Handle negative exponent (two's complement)
        if ((exponent & 0x08) != 0) {
            exponent = -((~exponent & 0x0F) + 1);
        }

        return (int) (mantissa * powerOfTen(exponent));
    }

    /**
     * Calculate 10^exponent
     */
    private double powerOfTen(int exponent) {
        if (exponent >= 0) {
            double result = 1.0;
            for (int i = 0; i < exponent; i++) {
                result *= 10.0;
            }
            return result;
        } else {
            double result = 1.0;
            for (int i = 0; i < -exponent; i++) {
                result /= 10.0;
            }
            return result;
        }
    }

    /**
     * Parse IEEE 11073-20601 timestamp
     */
    private long parseTimestamp(byte[] data, int offset) {
        // Simplified - return current time
        return System.currentTimeMillis();
    }

    /**
     * Scan for the device before attempting to connect.
     * This helps ensure the device is visible to the system.
     */
    private void scanForDevice(String targetMac, long timeoutMs) throws InterruptedException {
        log("=== STARTING SCAN PHASE ===");
        log("Scanning for: " + targetMac + " (Timeout: " + (timeoutMs / 1000) + "s)");

        // Try general scan first - USE NULL for all services
        boolean found = performScan(targetMac, null, 15000); // Increased to 15s

        // If not found, try specific scan for Blood Pressure Service
        if (!found) {
            log("General scan failed to find target. Trying specific service scan...");
            ArrayList<String> services = new ArrayList<>();
            services.add(BLOOD_PRESSURE_SERVICE_UUID);
            performScan(targetMac, services, 15000); // Increased to 15s
        }
    }

    /**
     * Performs a scan with optional service filtering
     */
    private boolean performScan(String targetMac, ArrayList<String> services, long timeoutMs)
            throws InterruptedException {
        log("Scan start (Services: " + (services != null ? services.toString() : "All") + ")");

        if (aborted) {
            log("Scan aborted before start");
            return false;
        }

        final Object scanLock = new Object();
        final boolean[] found = { false };
        final int[] totalDiscovered = { 0 };

        ActionListener<ActionEvent> scanListener = e -> {
            try {
                Object source = e.getSource();
                log("Scan callback triggered. Source type: " + (source != null ? source.getClass().getName() : "null"));

                Map<String, Object> device = (Map<String, Object>) source;
                String address = (String) device.get("address");
                String name = (String) device.get("name");
                Object rssiObj = device.get("rssi");
                int rssi = rssiObj instanceof Integer ? (Integer) rssiObj : 0;

                totalDiscovered[0]++;
                log("Discovered #" + totalDiscovered[0] + ": " + name + " [" + address + "] RSSI: " + rssi);

                if (targetMac.equalsIgnoreCase(address)) {
                    log("TARGET DEVICE FOUND!");
                    synchronized (scanLock) {
                        found[0] = true;
                        scanLock.notifyAll();
                    }
                }
            } catch (Exception ex) {
                log("WARNING in scan callback: " + ex.getMessage());
            }
        };

        try {
            log("Starting scan with Low Latency mode (2)");
            // startScan(listener, services, allowDuplicates, rssiThreshold, scanMode,
            // reportDelay, matchMode)
            // ScanMode 2 = SCAN_MODE_LOW_LATENCY
            bluetooth.startScan(scanListener, services, false, 0, 2, 0, 0);

            // Diagnostic: Check if scanning actually started
            try {
                java.lang.reflect.Method isScanning = bluetooth.getClass().getMethod("isScanning");
                log("Is Scanning (after startScan): " + isScanning.invoke(bluetooth));
            } catch (Exception e) {
                log("Diagnostic: isScanning check failed: " + e.getMessage());
            }

            synchronized (scanLock) {
                long start = System.currentTimeMillis();
                while (!found[0]) {
                    long elapsed = System.currentTimeMillis() - start;
                    if (elapsed >= timeoutMs) {
                        log("Scan timeout reached - " + totalDiscovered[0] + " devices seen total");
                        break;
                    }
                    if (aborted) {
                        log("Scan aborted during wait");
                        break;
                    }
                    scanLock.wait(1000);
                }
            }
        } catch (IOException e) {
            log("WARNING: Scan failed to start: " + e.getMessage());
        } finally {
            try {
                bluetooth.stopScan();
                log("Scan stopped");
            } catch (IOException e) {
                log("WARNING: Failed to stop scan: " + e.getMessage());
            }
        }
        return found[0];
    }

    /**
     * Disconnect from device
     */
    private void disconnect(String deviceMac) {
        try {
            log("Disconnecting from device...");
            bluetooth.disconnect(deviceMac);
            log("Disconnect call completed");
        } catch (IOException e) {
            log("WARNING during disconnect: " + e.getMessage());
        }
    }

    /**
     * Validate MAC address format
     */
    private void validateMacAddress(String mac) throws OmronBluetoothException {
        if (mac == null || mac.trim().isEmpty()) {
            throw new OmronBluetoothException(OmronBluetoothException.ErrorType.DEVICE_NOT_FOUND,
                    "MAC address cannot be null or empty");
        }

        if (!isValidMacFormat(mac)) {
            throw new OmronBluetoothException(OmronBluetoothException.ErrorType.DEVICE_NOT_FOUND,
                    "Invalid MAC address format: " + mac);
        }
    }

    /**
     * Validate MAC address format (relaxed)
     */
    private boolean isValidMacFormat(String mac) {
        if (mac.length() < 12) {
            return false;
        }
        return true;
    }

    /**
     * Check and request Android permissions
     */
    private void checkPermissions() {
        if ("and".equals(Display.getInstance().getPlatformName())) {
            log("=== REFINED PERMISSION & STATE CHECK ===");

            // 1. Check Bluetooth state
            try {
                log("Bluetooth.isEnabled(): " + bluetooth.isEnabled());
                if (!bluetooth.isEnabled()) {
                    log("Attempting to enable Bluetooth...");
                    bluetooth.enable();
                }
            } catch (Exception e) {
                log("Bluetooth enable failed: " + e.getMessage());
            }

            // 2. Check Location state
            try {
                boolean locEnabled = bluetooth.isLocationEnabled();
                log("Bluetooth.isLocationEnabled(): " + locEnabled);
                if (!locEnabled) {
                    log("!!! LOCATION SERVICES ARE DISABLED !!!");
                    Display.getInstance().callSerially(() -> {
                        Dialog.show("Location Disabled",
                                "Bluetooth scanning requires Location Services to be enabled on Android.\n\n" +
                                        "Please turn on GPS/Location in your system settings and try again.",
                                "OK", null);
                    });

                    log("Attempting to trigger Location request...");
                    bluetooth.requestLocation();
                }
            } catch (Exception e) {
                log("Location check failed: " + e.getMessage());
            }

            // 3. Advanced Permission Check via Reflection (checkForPermission)
            String[] permissions = {
                    "android.permission.BLUETOOTH_SCAN",
                    "android.permission.BLUETOOTH_CONNECT",
                    "android.permission.ACCESS_FINE_LOCATION"
            };

            Object impl = null;
            try {
                try {
                    Class<?> utilClass = Class.forName("com.codename1.io.Util");
                    java.lang.reflect.Method getImpl = utilClass.getDeclaredMethod("getImplementation");
                    getImpl.setAccessible(true);
                    impl = getImpl.invoke(null);
                } catch (Exception e) {
                    log("  Failed to get implementation object: " + e.getMessage());
                }

                if (impl != null) {
                    java.lang.reflect.Method check = null;
                    try {
                        check = impl.getClass().getMethod("checkForPermission", String.class, String.class);
                    } catch (Exception e) {
                    }

                    if (check != null) {
                        for (String p : permissions) {
                            try {
                                boolean granted = (Boolean) check.invoke(impl, p, "Bluetooth operation");
                                log("  Permission " + p + ": " + granted);
                            } catch (Exception e) {
                                log("  Failed to check " + p + ": " + e.getMessage());
                            }
                        }
                    } else {
                        log("  checkForPermission method not found in implementation");
                    }
                }
            } catch (Exception e) {
                log("Advanced permission check failed: " + e.getMessage());
            }

            // 4. Check already connected
            checkAlreadyConnected();

            // 5. Trigger System Permission Request if needed
            try {
                boolean needsRequest = false;
                if (impl != null) {
                    java.lang.reflect.Method check = null;
                    try {
                        check = impl.getClass().getMethod("checkForPermission", String.class, String.class);
                    } catch (Exception e) {
                    }

                    if (check != null) {
                        for (String p : permissions) {
                            try {
                                if (!(Boolean) check.invoke(impl, p, "Bluetooth operation")) {
                                    needsRequest = true;
                                    break;
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                }

                if (needsRequest) {
                    log("System permissions missing. Triggering request dialog...");

                    final Object pLock = new Object();
                    final boolean[] pDone = { false };

                    final Object finalImpl = impl;
                    final String[] finalPermissions = permissions;

                    Display.getInstance().callSerially(() -> {
                        try {
                            Class<?> displayClass = Class.forName("com.codename1.ui.Display");
                            Class<?> cnClass = Class.forName("com.codename1.ui.CN");
                            Class<?> actionListenerClass = Class.forName("com.codename1.ui.events.ActionListener");

                            java.lang.reflect.Method req = null;
                            Object target = null;

                            // AGGRESSIVE DISCOVERY
                            log("  Searching for 'requestPermissions' on Display...");
                            for (java.lang.reflect.Method m : displayClass.getDeclaredMethods()) {
                                if (m.getName().equals("requestPermissions")) {
                                    log("    Found on Display: " + m.toString());
                                }
                            }
                            log("  Searching for 'requestPermissions' on CN...");
                            for (java.lang.reflect.Method m : cnClass.getDeclaredMethods()) {
                                if (m.getName().equals("requestPermissions")) {
                                    log("    Found on CN: " + m.toString());
                                }
                            }
                            if (finalImpl != null) {
                                log("  Searching for 'requestPermissions' on Implementation...");
                                for (java.lang.reflect.Method m : finalImpl.getClass().getDeclaredMethods()) {
                                    if (m.getName().equals("requestPermissions")) {
                                        log("    Found on Impl: " + m.toString());
                                    }
                                }
                            }

                            // Try Display.requestPermissions(String[], ActionListener)
                            try {
                                req = displayClass.getMethod("requestPermissions", String[].class, actionListenerClass);
                                target = Display.getInstance();
                                log("  Selected: Display.requestPermissions(String[], ActionListener)");
                            } catch (Exception e1) {
                                // Try Display.requestPermissions(ActionListener, String[])
                                try {
                                    req = displayClass.getMethod("requestPermissions", actionListenerClass,
                                            String[].class);
                                    target = Display.getInstance();
                                    log("  Selected: Display.requestPermissions(ActionListener, String[])");
                                } catch (Exception e2) {
                                    // Try CN.requestPermissions(String[], ActionListener)
                                    try {
                                        req = cnClass.getMethod("requestPermissions", String[].class,
                                                actionListenerClass);
                                        target = null; // Static
                                        log("  Selected: CN.requestPermissions(String[], ActionListener)");
                                    } catch (Exception e3) {
                                        // Try CN.requestPermissions(ActionListener, String[])
                                        try {
                                            req = cnClass.getMethod("requestPermissions", actionListenerClass,
                                                    String[].class);
                                            target = null; // Static
                                            log("  Selected: CN.requestPermissions(ActionListener, String[])");
                                        } catch (Exception e4) {
                                        }
                                    }
                                }
                            }

                            if (req != null) {
                                ActionListener<ActionEvent> callback = evt -> {
                                    log("  Permissions request callback triggered");
                                    synchronized (pLock) {
                                        pDone[0] = true;
                                        pLock.notifyAll();
                                    }
                                };

                                if (req.getParameterTypes()[0].equals(String[].class)) {
                                    req.invoke(target, finalPermissions, callback);
                                } else {
                                    req.invoke(target, callback, finalPermissions);
                                }
                            } else {
                                log("  !!! COULD NOT FIND requestPermissions METHOD !!!");
                                synchronized (pLock) {
                                    pDone[0] = true;
                                    pLock.notifyAll();
                                }
                            }
                        } catch (Exception e) {
                            log("  Error triggering requestPermissions: " + e.getMessage());
                            synchronized (pLock) {
                                pDone[0] = true;
                                pLock.notifyAll();
                            }
                        }
                    });

                    synchronized (pLock) {
                        if (!pDone[0]) {
                            log("  Waiting for user response to permission dialog...");
                            pLock.wait(20000); // Wait up to 20s
                        }
                    }
                    log("  Permission request phase completed");
                } else {
                    log("All system permissions already granted");
                }
            } catch (Exception e) {
                log("Permission request logic failed: " + e.getMessage());
            }

            // 6. Standard Library Permission Request (as fallback)
            try {
                log("Requesting Library permissions (fallback)...");
                boolean reqResult = bluetooth.requestPermission();
                log("  Bluetooth.requestPermission() returned: " + reqResult);
            } catch (Exception e) {
                log("  Bluetooth.requestPermission() failed: " + e.getMessage());
            }

            // 6. Final status check
            try {
                log("Final Bluetooth.hasPermission(): " + bluetooth.hasPermission());
                log("Final Bluetooth.isLocationEnabled(): " + bluetooth.isLocationEnabled());
            } catch (Exception e) {
                log("Final status check failed: " + e.getMessage());
            }
        } else {
            log("Not Android platform, skipping permission check");
        }
    }

    private void checkAlreadyConnected() {
        log("Checking for already connected devices...");
        try {
            // Try with Blood Pressure service
            ArrayList<String> services = new ArrayList<>();
            services.add(BLOOD_PRESSURE_SERVICE_UUID);

            log("  Calling retrieveConnected with BP service...");
            bluetooth.retrieveConnected(new ActionListener<ActionEvent>() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    log("  RetrieveConnected (BP) callback triggered");
                    handleRetrieveResult(evt);
                }
            }, services);

            // Also try with NULL services (find everything)
            log("  Calling retrieveConnected with NULL services...");
            bluetooth.retrieveConnected(new ActionListener<ActionEvent>() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    log("  RetrieveConnected (NULL) callback triggered");
                    handleRetrieveResult(evt);
                }
            }, null);

        } catch (Exception e) {
            log("  RetrieveConnected failed to start: " + e.getMessage());
        }
    }

    private void handleRetrieveResult(ActionEvent evt) {
        try {
            Object source = evt.getSource();
            if (source instanceof ArrayList) {
                @SuppressWarnings("unchecked")
                ArrayList<Map<String, Object>> devices = (ArrayList<Map<String, Object>>) source;
                log("    Found " + devices.size() + " connected devices");
                for (Map<String, Object> device : devices) {
                    log("      - " + device.get("name") + " [" + device.get("address") + "]");
                }
            } else {
                log("    Source is not an ArrayList: " + (source != null ? source.getClass().getName() : "null"));
            }
        } catch (Exception e) {
            log("    Error processing retrieve result: " + e.getMessage());
        }
    }

}
