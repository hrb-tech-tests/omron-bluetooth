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
 * Singleton service for OMRON HEM-7144T2 Bluetooth integration.
 * 
 * Uses Java 8 Enum pattern for thread-safe singleton implementation.
 * Provides a single, simple API: getDataFromDevice(deviceMac) -> JSON
 * 
 * Clean Code Principles Applied:
 * - Single Responsibility: Only handles OMRON device communication
 * - Open/Closed: Extensible through inheritance if needed
 * - Dependency Inversion: Depends on Bluetooth abstraction
 * - Interface Segregation: Minimal public API
 * 
 * Note: Bluetooth features only work on real devices (iOS/Android).
 * In the CN1 simulator, this service will throw DEVICE_NOT_FOUND errors.
 * 
 * Usage:
 * 
 * <pre>
 * try {
 *     String json = OmronBluetoothService.INSTANCE.getDataFromDevice("AA:BB:CC:DD:EE:FF");
 *     // Process JSON...
 * } catch (OmronBluetoothException e) {
 *     // Handle error...
 * }
 * </pre>
 */
public enum OmronBluetoothService {

    INSTANCE;

    // Bluetooth SIG standard UUIDs for Blood Pressure Profile
    private static final String BLOOD_PRESSURE_SERVICE_UUID = "00001810-0000-1000-8000-00805f9b34fb";
    private static final String BLOOD_PRESSURE_MEASUREMENT_UUID = "00002a35-0000-1000-8000-00805f9b34fb";

    // Device model identifier
    private static final String DEVICE_MODEL = "HEM-7144T2";

    // Timeout for data collection (milliseconds)
    private static final long DATA_COLLECTION_TIMEOUT = 30000;

    // Lazy-initialized Bluetooth instance (not initialized in constructor to avoid
    // simulator issues)
    private Bluetooth bluetooth;
    private boolean bluetoothInitialized = false;
    private String initializationError = null;

    /**
     * Enum constructor - called once when INSTANCE is first accessed.
     * Does NOT initialize Bluetooth here to avoid simulator crashes.
     */
    OmronBluetoothService() {
        // Intentionally empty - Bluetooth is lazily initialized in
        // ensureBluetoothInitialized()
    }

    /**
     * Lazily initializes Bluetooth when first needed.
     * This prevents class initialization failures in the CN1 simulator.
     * 
     * @throws OmronBluetoothException if Bluetooth cannot be initialized (e.g.,
     *                                 simulator environment)
     */
    private synchronized void ensureBluetoothInitialized() throws OmronBluetoothException {
        if (bluetoothInitialized) {
            if (initializationError != null) {
                throw new OmronBluetoothException(
                        OmronBluetoothException.ErrorType.DEVICE_NOT_FOUND,
                        initializationError);
            }
            return;
        }

        bluetoothInitialized = true;

        // Check if running in simulator
        if (Display.getInstance().isSimulator()) {
            initializationError = "Bluetooth is not available in the simulator. Please test on a real device.";
            throw new OmronBluetoothException(
                    OmronBluetoothException.ErrorType.DEVICE_NOT_FOUND,
                    initializationError);
        }

        try {
            bluetooth = new Bluetooth();
            bluetooth.initialize(true, false, null);
        } catch (IOException e) {
            initializationError = "Bluetooth initialization failed: " + e.getMessage();
            throw new OmronBluetoothException(
                    OmronBluetoothException.ErrorType.CONNECTION_FAILED,
                    initializationError);
        } catch (Exception e) {
            initializationError = "Bluetooth not supported on this device: " + e.getMessage();
            throw new OmronBluetoothException(
                    OmronBluetoothException.ErrorType.DEVICE_NOT_FOUND,
                    initializationError);
        }
    }

    /**
     * Retrieves all measurement data from the specified OMRON device.
     * 
     * This method:
     * 1. Connects to the device via Bluetooth LE
     * 2. Subscribes to blood pressure measurement notifications
     * 3. Collects all measurements from device memory
     * 4. Returns a JSON string with device metadata and all measurements
     * 
     * @param deviceMac MAC address of the OMRON device (format:
     *                  "AA:BB:CC:DD:EE:FF")
     * @return JSON string containing device data and all measurements
     * @throws OmronBluetoothException if any error occurs during the process
     */
    public String getDataFromDevice(String deviceMac) throws OmronBluetoothException {
        // Lazily initialize Bluetooth (prevents simulator crashes)
        ensureBluetoothInitialized();

        // Check and request permissions if needed
        checkPermissions();

        validateMacAddress(deviceMac);

        final List<OmronMeasurement> measurements = new ArrayList<>();
        final Object lock = new Object();
        final boolean[] completed = { false };
        final OmronBluetoothException[] error = { null };

        try {
            System.out.println("OmronBluetoothService: Connecting to " + deviceMac);
            // Connect to device
            bluetooth.connect(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    System.out.println("OmronBluetoothService: Connected to " + deviceMac);
                    try {
                        // Subscribe to blood pressure notifications
                        subscribeToMeasurements(deviceMac, measurements, lock, completed, error);
                    } catch (Exception e) {
                        System.out.println("OmronBluetoothService: Error in connection callback: " + e.getMessage());
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

            // Wait for data collection to complete or timeout
            synchronized (lock) {
                long startTime = System.currentTimeMillis();
                while (!completed[0]) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    if (elapsed >= DATA_COLLECTION_TIMEOUT) {
                        System.out.println("OmronBluetoothService: Timeout waiting for data");
                        throw new OmronBluetoothException(OmronBluetoothException.ErrorType.TIMEOUT,
                                "after " + (DATA_COLLECTION_TIMEOUT / 1000) + " seconds");
                    }
                    lock.wait(1000); // Wait with 1 second intervals
                }
            }

            // Check for errors during data collection
            if (error[0] != null) {
                throw error[0];
            }

            // Disconnect from device
            disconnect(deviceMac);

            // Build and return JSON response
            OmronDeviceData deviceData = new OmronDeviceData(deviceMac, DEVICE_MODEL, measurements);
            return deviceData.toJSON();

        } catch (IOException e) {
            throw new OmronBluetoothException(OmronBluetoothException.ErrorType.CONNECTION_FAILED, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OmronBluetoothException(OmronBluetoothException.ErrorType.DATA_TRANSFER_FAILED, e);
        }
    }

    /**
     * Subscribes to blood pressure measurement notifications from the device.
     */
    private void subscribeToMeasurements(String deviceMac, List<OmronMeasurement> measurements,
            Object lock, boolean[] completed,
            OmronBluetoothException[] error) {
        try {
            System.out.println("OmronBluetoothService: Subscribing to measurements");
            bluetooth.subscribe(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    try {
                        Map<String, Object> result = (Map<String, Object>) evt.getSource();
                        String value = (String) result.get("value");
                        System.out.println("OmronBluetoothService: Received notification. Value: " + value);

                        if (value != null && !value.isEmpty()) {
                            // Decode and parse measurement
                            byte[] data = Base64.decode(value.getBytes());
                            OmronMeasurement measurement = parseMeasurement(data);

                            synchronized (lock) {
                                measurements.add(measurement);
                            }
                        } else {
                            // Empty notification indicates end of data transfer
                            synchronized (lock) {
                                completed[0] = true;
                                lock.notifyAll();
                            }
                        }
                    } catch (Exception e) {
                        synchronized (lock) {
                            error[0] = new OmronBluetoothException(
                                    OmronBluetoothException.ErrorType.INVALID_DATA, e);
                            completed[0] = true;
                            lock.notifyAll();
                        }
                    }
                }
            }, deviceMac, BLOOD_PRESSURE_SERVICE_UUID, BLOOD_PRESSURE_MEASUREMENT_UUID);

        } catch (IOException e) {
            synchronized (lock) {
                error[0] = new OmronBluetoothException(
                        OmronBluetoothException.ErrorType.DATA_TRANSFER_FAILED, e);
                completed[0] = true;
                lock.notifyAll();
            }
        }
    }

    /**
     * Parses a blood pressure measurement from raw Bluetooth data.
     * 
     * Follows IEEE 11073-20601 Blood Pressure Measurement format:
     * - Byte 0: Flags
     * - Bytes 1-2: Systolic (SFLOAT)
     * - Bytes 3-4: Diastolic (SFLOAT)
     * - Bytes 5-6: Mean Arterial Pressure (SFLOAT)
     * - Bytes 7-13: Timestamp (optional)
     * - Bytes 14-15: Pulse Rate (optional)
     */
    private OmronMeasurement parseMeasurement(byte[] data) throws OmronBluetoothException {
        if (data == null || data.length < 7) {
            throw new OmronBluetoothException(OmronBluetoothException.ErrorType.INVALID_DATA,
                    "Insufficient data length: " + (data != null ? data.length : 0));
        }

        try {
            // Parse blood pressure values (SFLOAT format)
            int systolic = parseSFloat(data, 1);
            int diastolic = parseSFloat(data, 3);
            int meanArterialPressure = parseSFloat(data, 5);

            // Parse timestamp if available
            long timestamp = System.currentTimeMillis();
            if (data.length >= 14) {
                timestamp = parseTimestamp(data, 7);
            }

            // Parse heart rate if available
            int heartRate = 0;
            if (data.length >= 16) {
                heartRate = parseSFloat(data, 14);
            }

            return new OmronMeasurement(systolic, diastolic, meanArterialPressure, heartRate, timestamp);

        } catch (Exception e) {
            throw new OmronBluetoothException(OmronBluetoothException.ErrorType.INVALID_DATA, e);
        }
    }

    /**
     * Parses an IEEE 11073-20601 SFLOAT value.
     * SFLOAT is a 16-bit floating point with 12-bit mantissa and 4-bit exponent.
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

        // Use CN1-compatible power of 10 calculation (Math.pow not supported)
        return (int) (mantissa * powerOfTen(exponent));
    }

    /**
     * Calculates 10^exponent without using Math.pow() for CN1 compatibility.
     * Supports exponents from -7 to 7 (typical range for SFLOAT values).
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
     * Parses IEEE 11073-20601 timestamp (7 bytes).
     */
    private long parseTimestamp(byte[] data, int offset) {
        // For simplicity, return current time
        // Full implementation would parse year, month, day, hour, minute, second
        return System.currentTimeMillis();
    }

    /**
     * Disconnects from the specified device.
     */
    private void disconnect(String deviceMac) {
        try {
            bluetooth.disconnect(deviceMac);
        } catch (IOException e) {
            // Log but don't throw - disconnection errors are non-critical
            System.err.println("Disconnect warning: " + e.getMessage());
        }
    }

    /**
     * Validates MAC address format.
     * Uses manual validation instead of regex for Codename One compatibility.
     */
    private void validateMacAddress(String mac) throws OmronBluetoothException {
        if (mac == null || mac.trim().isEmpty()) {
            throw new OmronBluetoothException(OmronBluetoothException.ErrorType.DEVICE_NOT_FOUND,
                    "MAC address cannot be null or empty");
        }

        // Manual format validation (AA:BB:CC:DD:EE:FF or AA-BB-CC-DD-EE-FF)
        // CN1 does not fully support regex, so we validate manually
        if (!isValidMacFormat(mac)) {
            throw new OmronBluetoothException(OmronBluetoothException.ErrorType.DEVICE_NOT_FOUND,
                    "Invalid MAC address format: " + mac);
        }
    }

    /**
     * Validates MAC address format manually without using regex.
     * Expected format: AA:BB:CC:DD:EE:FF or AA-BB-CC-DD-EE-FF
     * 
     * @param mac the MAC address string to validate
     * @return true if valid format, false otherwise
     */
    private boolean isValidMacFormat(String mac) {
        // MAC address should be exactly 17 characters: XX:XX:XX:XX:XX:XX
        if (mac.length() != 17) {
            return false;
        }

        for (int i = 0; i < mac.length(); i++) {
            char c = mac.charAt(i);

            // Every 3rd character (index 2, 5, 8, 11, 14) should be ':' or '-'
            if (i % 3 == 2) {
                if (c != ':' && c != '-') {
                    return false;
                }
            } else {
                // Other characters should be hexadecimal digits
                if (!isHexDigit(c)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks and requests necessary Android permissions.
     */
    private void checkPermissions() {
        if ("and".equals(Display.getInstance().getPlatformName())) {
            if (!CN.hasPermission("android.permission.BLUETOOTH_SCAN") ||
                    !CN.hasPermission("android.permission.BLUETOOTH_CONNECT") ||
                    !CN.hasPermission("android.permission.ACCESS_FINE_LOCATION")) {

                CN.requestPermissions(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        // Permissions processed
                    }
                }, "android.permission.BLUETOOTH_SCAN",
                        "android.permission.BLUETOOTH_CONNECT",
                        "android.permission.ACCESS_FINE_LOCATION");
            }
        }
    }

    /**
     * Checks if a character is a valid hexadecimal digit (0-9, A-F, a-f).
     */
    private boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') ||
                (c >= 'A' && c <= 'F') ||
                (c >= 'a' && c <= 'f');
    }
}
