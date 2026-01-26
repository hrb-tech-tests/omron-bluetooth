package com.example.omronapp.mynativebluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import com.codename1.impl.android.AndroidNativeUtil;
import com.codename1.io.Log;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MyNativeBluetoothImpl implements MyNativeBluetooth {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private Map<String, BluetoothGatt> connectedGatts = new HashMap<>();
    private Context context;
    private ActionListener<ActionEvent> onDeviceFoundListener;
    private Map<String, ActionListener<ActionEvent>> onConnectedListeners = new HashMap<>();
    private Map<String, ActionListener<ActionEvent>> onDataListeners = new HashMap<>();
    private Map<String, ActionListener<ActionEvent>> onWriteListeners = new HashMap<>();

    private ScanCallback leScanCallback;
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String deviceId = gatt.getDevice().getAddress();
            ActionListener<ActionEvent> listener = onConnectedListeners.get(deviceId);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.p("[MyNativeBluetoothImpl] Connected to " + deviceId);
                connectedGatts.put(deviceId, gatt);
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.p("[MyNativeBluetoothImpl] Disconnected from " + deviceId);
                gatt.close();
                connectedGatts.remove(deviceId);
                if (listener != null) {
                    Display.getInstance().callSerially(() -> listener.actionPerformed(new ActionEvent(false)));
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            String deviceId = gatt.getDevice().getAddress();
            ActionListener<ActionEvent> listener = onConnectedListeners.get(deviceId);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.p("[MyNativeBluetoothImpl] Services discovered for " + deviceId);
                if (listener != null) {
                    Display.getInstance().callSerially(() -> listener.actionPerformed(new ActionEvent(true)));
                }
            } else {
                Log.p("[MyNativeBluetoothImpl] onServicesDiscovered received: " + status);
                if (listener != null) {
                    Display.getInstance().callSerially(() -> listener.actionPerformed(new ActionEvent(false)));
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            String deviceId = gatt.getDevice().getAddress();
            ActionListener<ActionEvent> listener = onDataListeners.get(deviceId + characteristic.getUuid().toString());
            if (listener != null) {
                Display.getInstance().callSerially(() -> listener.actionPerformed(new ActionEvent(characteristic.getValue())));
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            String deviceId = gatt.getDevice().getAddress();
            ActionListener<ActionEvent> listener = onWriteListeners.get(deviceId + characteristic.getUuid().toString());
            if (listener != null) {
                Display.getInstance().callSerially(() -> listener.actionPerformed(new ActionEvent(status == BluetoothGatt.GATT_SUCCESS)));
            }
        }
    };


    @Override
    public void initialize(ActionListener<ActionEvent> onInitialized) {
        context = AndroidNativeUtil.getActivity();
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.p("[MyNativeBluetoothImpl] Bluetooth is not supported or not enabled.");
            Display.getInstance().callSerially(() -> onInitialized.actionPerformed(new ActionEvent(false)));
            return;
        }
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        Log.p("[MyNativeBluetoothImpl] Initialization successful.");
        Display.getInstance().callSerially(() -> onInitialized.actionPerformed(new ActionEvent(true)));
    }

    @Override
    public void startScan(ActionListener<ActionEvent> onDeviceFound) {
        this.onDeviceFoundListener = onDeviceFound;
        leScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                BluetoothDevice device = result.getDevice();
                String deviceName = device.getName();
                if (deviceName != null && !deviceName.isEmpty()) {
                    Log.p("[MyNativeBluetoothImpl] Device found: " + deviceName + " (" + device.getAddress() + ")");
                    Display.getInstance().callSerially(() -> onDeviceFoundListener.actionPerformed(new ActionEvent(device.getAddress())));
                }
            }
        };
        bluetoothLeScanner.startScan(leScanCallback);
        Log.p("[MyNativeBluetoothImpl] Scan started.");
    }

    @Override
    public void stopScan() {
        if (leScanCallback != null) {
            bluetoothLeScanner.stopScan(leScanCallback);
            leScanCallback = null;
            Log.p("[MyNativeBluetoothImpl] Scan stopped.");
        }
    }

    @Override
    public void connect(String deviceId, ActionListener<ActionEvent> onConnected) {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceId);
        if (device == null) {
            Log.p("[MyNativeBluetoothImpl] Device not found: " + deviceId);
            Display.getInstance().callSerially(() -> onConnected.actionPerformed(new ActionEvent(false)));
            return;
        }
        
        if (connectedGatts.containsKey(deviceId)) {
            Log.p("[MyNativeBluetoothImpl] Already connected to " + deviceId);
            Display.getInstance().callSerially(() -> onConnected.actionPerformed(new ActionEvent(true)));
            return;
        }

        onConnectedListeners.put(deviceId, onConnected);
        Log.p("[MyNativeBluetoothImpl] Connecting to " + deviceId);
        device.connectGatt(context, false, gattCallback);
    }

    @Override
    public void disconnect(String deviceId) {
        BluetoothGatt gatt = connectedGatts.get(deviceId);
        if (gatt != null) {
            Log.p("[MyNativeBluetoothImpl] Disconnecting from " + deviceId);
            gatt.disconnect();
        }
    }

    @Override
    public void subscribe(String deviceId, String serviceUUID, String characteristicUUID, ActionListener<ActionEvent> onData) {
        BluetoothGatt gatt = connectedGatts.get(deviceId);
        if (gatt == null) {
            Log.p("[MyNativeBluetoothImpl] Not connected to " + deviceId);
            return;
        }
        BluetoothGattService service = gatt.getService(UUID.fromString(serviceUUID));
        if (service == null) {
            Log.p("[MyNativeBluetoothImpl] Service not found: " + serviceUUID);
            return;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));
        if (characteristic == null) {
            Log.p("[MyNativeBluetoothImpl] Characteristic not found: " + characteristicUUID);
            return;
        }
        gatt.setCharacteristicNotification(characteristic, true);
        
        UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);
        onDataListeners.put(deviceId + characteristicUUID, onData);
        Log.p("[MyNativeBluetoothImpl] Subscribed to " + characteristicUUID);
    }

    @Override
    public void write(String deviceId, String serviceUUID, String characteristicUUID, byte[] data, ActionListener<ActionEvent> onWrite) {
        BluetoothGatt gatt = connectedGatts.get(deviceId);
        if (gatt == null) {
            Log.p("[MyNativeBluetoothImpl] Not connected to " + deviceId);
            Display.getInstance().callSerially(() -> onWrite.actionPerformed(new ActionEvent(false)));
            return;
        }
        BluetoothGattService service = gatt.getService(UUID.fromString(serviceUUID));
        if (service == null) {
            Log.p("[MyNativeBluetoothImpl] Service not found: " + serviceUUID);
            Display.getInstance().callSerially(() -> onWrite.actionPerformed(new ActionEvent(false)));
            return;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));
        if (characteristic == null) {
            Log.p("[MyNativeBluetoothImpl] Characteristic not found: " + characteristicUUID);
            Display.getInstance().callSerially(() -> onWrite.actionPerformed(new ActionEvent(false)));
            return;
        }
        characteristic.setValue(data);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        onWriteListeners.put(deviceId + characteristicUUID, onWrite);
        if (!gatt.writeCharacteristic(characteristic)) {
            Log.p("[MyNativeBluetoothImpl] Failed to write characteristic");
            Display.getInstance().callSerially(() -> onWrite.actionPerformed(new ActionEvent(false)));
        }
    }
    
    @Override
    public boolean isSupported() {
        return context.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le");
    }
}
