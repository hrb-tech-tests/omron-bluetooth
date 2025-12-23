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
        logger.log("Abort requested");
    }

    private synchronized void ensureInitialized() throws OmronBluetoothException {
        if (initialized) return;
        
        if (Display.getInstance().isSimulator()) {
            throw new OmronBluetoothException(OmronBluetoothException.ErrorType.DEVICE_NOT_FOUND, "Bluetooth not available in simulator");
        }

        try {
            bluetooth = new Bluetooth();
            bluetooth.initialize(true, false, "omron_app");
            initialized = true;
        } catch (Exception e) {
            throw new OmronBluetoothException(OmronBluetoothException.ErrorType.CONNECTION_FAILED, e);
        }
    }

    public String getDataFromDevice(String deviceMac) throws OmronBluetoothException {
        ensureInitialized();
        checkPermissions();
        this.aborted = false;
        logger.clear();
        logger.log("Starting retrieval for " + deviceMac);

        final List<OmronMeasurement> measurements = new ArrayList<>();
        final Object lock = new Object();
        final boolean[] completed = { false };
        final OmronBluetoothException[] error = { null };

        try {
            bluetooth.connect(evt -> {
                logger.log("Connected to device");
                performHandshakeAndSubscribe(deviceMac, measurements, lock, completed, error);
            }, deviceMac);

            synchronized (lock) {
                long start = System.currentTimeMillis();
                while (!completed[0]) {
                    if (aborted) throw new OmronBluetoothException(OmronBluetoothException.ErrorType.ABORTED);
                    if (System.currentTimeMillis() - start > DEFAULT_TIMEOUT) {
                        throw new OmronBluetoothException(OmronBluetoothException.ErrorType.TIMEOUT);
                    }
                    lock.wait(1000);
                }
            }

            if (error[0] != null) throw error[0];

            bluetooth.disconnect(deviceMac);
            return new OmronDeviceData(deviceMac, DEVICE_MODEL, measurements).toJSON();

        } catch (Exception e) {
            if (e instanceof OmronBluetoothException) throw (OmronBluetoothException)e;
            throw new OmronBluetoothException(OmronBluetoothException.ErrorType.CONNECTION_FAILED, e);
        }
    }

    private void performHandshakeAndSubscribe(String mac, List<OmronMeasurement> measurements, Object lock, boolean[] completed, OmronBluetoothException[] error) {
        try {
            bluetooth.subscribe(evt -> {
                handleNotification(evt, measurements, lock, completed, error);
            }, mac, OMRON_SERVICE_UUID, OMRON_NOTIFY_UUID);

            byte[] handshake = {0x00, 0x02, 0x00, 0x10, (byte)0x85, 0x00, 0x00, 0x10, (byte)0x8E};
            bluetooth.write(evt -> logger.log("Handshake sent"), mac, OMRON_SERVICE_UUID, OMRON_WRITE_UUID, Base64.encode(handshake), true);
            
        } catch (IOException e) {
            fail(lock, completed, error, e);
        }
    }

    private void handleNotification(ActionEvent evt, List<OmronMeasurement> measurements, Object lock, boolean[] completed, OmronBluetoothException[] error) {
        try {
            Map<String, Object> result = (Map<String, Object>) evt.getSource();
            String value = (String) result.get("value");
            
            if (value != null && !value.isEmpty()) {
                byte[] data = Base64.decode(value.getBytes());
                OmronMeasurement m = OmronProtocolParser.parse(data);
                synchronized (lock) { measurements.add(m); }
                logger.log("Measurement received: " + m);
            } else {
                synchronized (lock) { completed[0] = true; lock.notifyAll(); }
            }
        } catch (Exception e) {
            fail(lock, completed, error, e);
        }
    }

    private void fail(Object lock, boolean[] completed, OmronBluetoothException[] error, Exception e) {
        synchronized (lock) {
            error[0] = new OmronBluetoothException(OmronBluetoothException.ErrorType.DATA_TRANSFER_FAILED, e);
            completed[0] = true;
            lock.notifyAll();
        }
    }

    /**
     * Checks and requests necessary Android permissions using reflection for compatibility.
     */
    private void checkPermissions() {
        if (!"and".equals(Display.getInstance().getPlatformName())) return;
        try {
            String[] perms = {"android.permission.BLUETOOTH_SCAN", "android.permission.BLUETOOTH_CONNECT", "android.permission.ACCESS_FINE_LOCATION"};
            Class<?> displayClass = Class.forName("com.codename1.ui.Display");
            Class<?> actionListenerClass = Class.forName("com.codename1.ui.events.ActionListener");
            
            java.lang.reflect.Method hasPermission = displayClass.getMethod("has" + "Permission", String.class);
            java.lang.reflect.Method requestPermissions = displayClass.getMethod("request" + "Permissions", actionListenerClass, String[].class);

            boolean allGranted = true;
            for (String p : perms) {
                if (!(Boolean) hasPermission.invoke(Display.getInstance(), p)) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                requestPermissions.invoke(Display.getInstance(), (ActionListener) evt -> {}, perms);
            }
        } catch (Exception e) {
            logger.log("Permission check failed: " + e.getMessage());
        }
    }
}
