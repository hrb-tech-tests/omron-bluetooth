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
    private static final List<String> debugLogs = new ArrayList<>();
    private static long startTime = 0;

    private Bluetooth bluetooth;
    private boolean bluetoothInitialized = false;
    private String initializationError = null;

    /**
     * Get all debug logs collected during the last operation
     */
    public static List<String> getDebugLogs() {
        return new ArrayList<>(debugLogs);
    }

    /**
     * Clear debug logs
     */
    public static void clearDebugLogs() {
        debugLogs.clear();
    }

    /**
     * Add a timestamped debug log entry
     */
    private static void log(String message) {
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

        log("=== STARTING DATA RETRIEVAL ===");
        log("Target Device MAC: " + deviceMac);

        // Initialize Bluetooth
        ensureBluetoothInitialized();

        // Check permissions
        log("=== CHECKING PERMISSIONS ===");
        checkPermissions();

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
            bluetooth.connect(new ActionListener() {
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
            bluetooth.read(new ActionListener() {
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

            bluetooth.subscribe(new ActionListener() {
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
            try {
                String pScan = "android.permission.BLUETOOTH_SCAN";
                String pConnect = "android.permission.BLUETOOTH_CONNECT";
                String pLocation = "android.permission.ACCESS_FINE_LOCATION";

                Class<?> displayClass = Class.forName("com.codename1.ui.Display");
                Class<?> actionListenerClass = Class.forName("com.codename1.ui.events.ActionListener");

                java.lang.reflect.Method hasPermission = displayClass.getMethod("hasPermission", String.class);
                java.lang.reflect.Method requestPermissions = displayClass.getMethod("requestPermissions",
                        actionListenerClass, String[].class);

                boolean scanGranted = (Boolean) hasPermission.invoke(Display.getInstance(), pScan);
                boolean connectGranted = (Boolean) hasPermission.invoke(Display.getInstance(), pConnect);
                boolean locationGranted = (Boolean) hasPermission.invoke(Display.getInstance(), pLocation);

                log("Permissions - Scan: " + scanGranted + ", Connect: " + connectGranted + ", Location: "
                        + locationGranted);

                if (!scanGranted || !connectGranted || !locationGranted) {
                    log("Requesting missing permissions...");
                    requestPermissions.invoke(Display.getInstance(), new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent evt) {
                            log("Permission request callback triggered");
                        }
                    }, new String[] { pScan, pConnect, pLocation });
                }
            } catch (Exception e) {
                log("WARNING: Permission check failed: " + e.getMessage());
            }
        } else {
            log("Not Android platform, skipping permission check");
        }
    }
}
