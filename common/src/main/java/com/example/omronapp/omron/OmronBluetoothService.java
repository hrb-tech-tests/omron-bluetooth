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
 * Unified service for OMRON Bluetooth integration.
 * Refactored to comply with Clean Code principles.
 */
public enum OmronBluetoothService {
    INSTANCE;

    // UUIDs
    private static final String OMRON_SERVICE_UUID = "49123040-aee8-11e1-a74d-0002a5d5c51b";
    private static final String OMRON_WRITE_UUID = "49123041-aee8-11e1-a74d-0002a5d5c51b";
    private static final String OMRON_NOTIFY_UUID = "49123042-aee8-11e1-a74d-0002a5d5c51b";
    
    private static final long DEFAULT_TIMEOUT = 30000;
    private static final String DEVICE_MODEL = "HEM-7144T2";

    private Bluetooth bluetooth;
    private boolean initialized = false;
    private OmronLogger logger = new OmronLogger.Silent();
    private volatile boolean aborted = false;

    public void setLogger(OmronLogger logger) {
        this.logger = logger != null ? logger : new OmronLogger.Silent();
    }

    public void abort() {
        this.aborted = true;
        logger.log("Abort requested by user.");
    }

    private synchronized void ensureInitialized() throws OmronBluetoothException {
        logger.log("Ensuring Bluetooth is initialized...");
        if (initialized) {
            logger.log("Bluetooth already initialized.");
            return;
        }
        
        if (Display.getInstance().isSimulator()) {
            logger.log("Running in simulator, Bluetooth not supported.");
            throw new OmronBluetoothException(OmronBluetoothException.ErrorType.DEVICE_NOT_FOUND, "Bluetooth not available in simulator");
        }

        try {
            logger.log("Initializing Bluetooth...");
            bluetooth = new Bluetooth();
            bluetooth.initialize(true, false, "omron_app");
            initialized = true;
            logger.log("Bluetooth initialized successfully.");
        } catch (Exception e) {
            logger.log("Bluetooth initialization failed: " + e.getMessage());
            throw new OmronBluetoothException(OmronBluetoothException.ErrorType.CONNECTION_FAILED, e);
        }
    }

    public String getDataFromDevice(String deviceMac) throws OmronBluetoothException {
        ensureInitialized();
        checkPermissions();
        this.aborted = false;
        logger.clear();
        logger.log("Starting data retrieval for device: " + deviceMac);

        final List<OmronMeasurement> measurements = new ArrayList<>();
        final Object lock = new Object();
        final boolean[] completed = { false };
        final OmronBluetoothException[] error = { null };

        try {
            logger.log("Starting BLE scan...");
            bluetooth.startScan(evt -> {
                try {
                    logger.log("Scan event received: " + evt.getSource());
                    Map<String, Object> scanResult = (Map<String, Object>) evt.getSource();
                    String address = (String) scanResult.get("address");
                    logger.log("Device found: " + address);

                    if (deviceMac.equalsIgnoreCase(address)) {
                        logger.log("Target device found: " + address);
                        bluetooth.stopScan();
                        logger.log("Attempting to connect to " + deviceMac + "...");
                        bluetooth.connect(connectEvt -> {
                            logger.log("Connection event received: " + connectEvt.getSource());
                            performHandshakeAndSubscribe(deviceMac, measurements, lock, completed, error);
                        }, deviceMac);
                    }
                } catch (IOException e) {
                    fail(lock, completed, error, e);
                }
            }, null, false, Bluetooth.SCAN_MODE_LOW_LATENCY, Bluetooth.MATCH_MODE_STICKY, Bluetooth.MATCH_NUM_MAX_ADVERTISEMENT, Bluetooth.CALLBACK_TYPE_ALL_MATCHES);


            synchronized (lock) {
                long start = System.currentTimeMillis();
                logger.log("Waiting for data transfer to complete... Timeout set to " + DEFAULT_TIMEOUT + "ms.");
                while (!completed[0]) {
                    if (aborted) {
                        logger.log("Operation aborted.");
                        throw new OmronBluetoothException(OmronBluetoothException.ErrorType.ABORTED);
                    }
                    if (System.currentTimeMillis() - start > DEFAULT_TIMEOUT) {
                        logger.log("Operation timed out.");
                        bluetooth.stopScan();
                        throw new OmronBluetoothException(OmronBluetoothException.ErrorType.TIMEOUT);
                    }
                    lock.wait(1000);
                }
                logger.log("Data transfer lock released.");
            }

            if (error[0] != null) {
                logger.log("An error occurred during data transfer.");
                throw error[0];
            }

            logger.log("Disconnecting from " + deviceMac);
            bluetooth.disconnect(deviceMac);
            logger.log("Data retrieval successful.");
            return new OmronDeviceData(deviceMac, DEVICE_MODEL, measurements).toJSON();

        } catch (Exception e) {
            logger.log("Exception during data retrieval: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            if (e instanceof OmronBluetoothException) throw (OmronBluetoothException)e;
            throw new OmronBluetoothException(OmronBluetoothException.ErrorType.CONNECTION_FAILED, e);
        }
    }

    private void performHandshakeAndSubscribe(String mac, List<OmronMeasurement> measurements, Object lock, boolean[] completed, OmronBluetoothException[] error) {
        try {
            logger.log("Subscribing to notifications...");
            bluetooth.subscribe(evt -> {
                handleNotification(evt, measurements, lock, completed, error);
            }, mac, OMRON_SERVICE_UUID, OMRON_NOTIFY_UUID);
            logger.log("Subscription successful. Sending handshake...");

            byte[] handshake = {0x00, 0x02, 0x00, 0x10, (byte)0x85, 0x00, 0x00, 0x10, (byte)0x8E};
            bluetooth.write(evt -> logger.log("Handshake sent successfully."), mac, OMRON_SERVICE_UUID, OMRON_WRITE_UUID, Base64.encode(handshake), true);
            
        } catch (IOException e) {
            logger.log("Handshake/Subscription failed: " + e.getMessage());
            fail(lock, completed, error, e);
        }
    }

    private void handleNotification(ActionEvent evt, List<OmronMeasurement> measurements, Object lock, boolean[] completed, OmronBluetoothException[] error) {
        try {
            Map<String, Object> result = (Map<String, Object>) evt.getSource();
            String value = (String) result.get("value");
            
            if (value != null && !value.isEmpty()) {
                byte[] data = Base64.decode(value.getBytes());
                logger.log("Notification received. Bytes: " + data.length);
                OmronMeasurement m = OmronProtocolParser.parse(data);
                synchronized (lock) { measurements.add(m); }
                logger.log("Parsed Measurement: " + m);
            } else {
                logger.log("Empty notification received, signaling end of transmission.");
                synchronized (lock) { completed[0] = true; lock.notifyAll(); }
            }
        } catch (Exception e) {
            logger.log("Failed to handle notification: " + e.getMessage());
            fail(lock, completed, error, e);
        }
    }

    private void fail(Object lock, boolean[] completed, OmronBluetoothException[] error, Exception e) {
        logger.log("Operation failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        synchronized (lock) {
            if (error[0] == null) {
                error[0] = new OmronBluetoothException(OmronBluetoothException.ErrorType.DATA_TRANSFER_FAILED, e);
            }
            completed[0] = true;
            lock.notifyAll();
        }
    }

    /**
     * Checks and requests necessary Android permissions using reflection for compatibility.
     */
    private void checkPermissions() {
        if (!"and".equals(Display.getInstance().getPlatformName())) {
            logger.log("Skipping permission check: Not on Android.");
            return;
        }
        try {
            logger.log("Checking Android permissions...");
            String[] perms = {"android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT", "android.permission.ACCESS_FINE_LOCATION"};
            Class<?> displayClass = Class.forName("com.codename1.ui.Display");
            Class<?> actionListenerClass = Class.forName("com.codename1.ui.events.ActionListener");
            
            java.lang.reflect.Method hasPermission = displayClass.getMethod("has" + "Permission", String.class);
            java.lang.reflect.Method requestPermissions = displayClass.getMethod("request" + "Permissions", actionListenerClass, String[].class);

            boolean allGranted = true;
            for (String p : perms) {
                if (!(Boolean) hasPermission.invoke(Display.getInstance(), p)) {
                    logger.log("Missing permission: " + p);
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                logger.log("All required permissions are granted.");
            } else {
                logger.log("Requesting missing permissions...");
                requestPermissions.invoke(Display.getInstance(), (ActionListener) evt -> {
                    logger.log("Permission request dialog closed.");
                }, perms);
            }
        } catch (Exception e) {
            logger.log("Permission check failed catastrophically: " + e.getMessage());
        }
    }
}
