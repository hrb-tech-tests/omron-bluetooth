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
    private Map<String, BluetoothGatt> connectedGatts = new HashMap<String, BluetoothGatt>();
    private Context context;
    private ActionListener onDeviceFoundListener;
    private Map<String, ActionListener> onConnectedListeners = new HashMap<String, ActionListener>();
    private Map<String, ActionListener> onDataListeners = new HashMap<String, ActionListener>();
    private Map<String, ActionListener> onWriteListeners = new HashMap<String, ActionListener>();

    private ScanCallback leScanCallback;
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String deviceId = gatt.getDevice().getAddress();
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.p("[MyNativeBluetoothImpl] Connected to " + deviceId);
                connectedGatts.put(deviceId, gatt);
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.p("[MyNativeBluetoothImpl] Disconnected from " + deviceId);
                gatt.close();
                connectedGatts.remove(deviceId);
                final ActionListener listener = onConnectedListeners.get(deviceId);
                if (listener != null) {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            listener.actionPerformed(new ActionEvent(false));
                        }
                    });
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            final String deviceId = gatt.getDevice().getAddress();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.p("[MyNativeBluetoothImpl] Services discovered for " + deviceId);
                final ActionListener listener = onConnectedListeners.get(deviceId);
                if (listener != null) {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            listener.actionPerformed(new ActionEvent(true));
                        }
                    });
                }
            } else {
                Log.p("[MyNativeBluetoothImpl] onServicesDiscovered received: " + status);
                final ActionListener listener = onConnectedListeners.get(deviceId);
                if (listener != null) {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            listener.actionPerformed(new ActionEvent(false));
                        }
                    });
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            String deviceId = gatt.getDevice().getAddress();
            String key = deviceId + characteristic.getUuid().toString();
            ActionListener listener = onDataListeners.get(key);
            if (listener != null) {
                final byte[] value = characteristic.getValue();
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        listener.actionPerformed(new ActionEvent(value));
                    }
                });
            } else {
                Log.p("[MyNativeBluetoothImpl] No data listener for key: " + key);
            }
        }

        // Add older version of onCharacteristicChanged for compatibility
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
            onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            String deviceId = gatt.getDevice().getAddress();
            String key = deviceId + characteristic.getUuid().toString();
            final ActionListener listener = onWriteListeners.get(key);
            if (listener != null) {
                final boolean success = (status == BluetoothGatt.GATT_SUCCESS);
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        listener.actionPerformed(new ActionEvent(success));
                    }
                });
            } else {
                Log.p("[MyNativeBluetoothImpl] No write listener for key: " + key);
            }
        }
    };


    @Override
    public void initialize(final ActionListener onInitialized) {
        context = AndroidNativeUtil.getActivity();
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.p("[MyNativeBluetoothImpl] Bluetooth is not supported or not enabled.");
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    onInitialized.actionPerformed(new ActionEvent(false));
                }
            });
            return;
        }
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        Log.p("[MyNativeBluetoothImpl] Initialization successful.");
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                onInitialized.actionPerformed(new ActionEvent(true));
            }
        });
    }

    @Override
    public void startScan(ActionListener onDeviceFound) {
        this.onDeviceFoundListener = onDeviceFound;
        leScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                BluetoothDevice device = result.getDevice();
                String deviceName = device.getName();
                Log.p("[MyNativeBluetoothImpl] Device found: " + (deviceName != null ? deviceName : "Unknown") + " (" + device.getAddress() + ")");
                final String address = device.getAddress();
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        onDeviceFoundListener.actionPerformed(new ActionEvent(address));
                    }
                });
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
    public void connect(String deviceId, final ActionListener onConnected) {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceId);
        if (device == null) {
            Log.p("[MyNativeBluetoothImpl] Device not found: " + deviceId);
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    onConnected.actionPerformed(new ActionEvent(false));
                }
            });
            return;
        }
        
        if (connectedGatts.containsKey(deviceId)) {
            Log.p("[MyNativeBluetoothImpl] Already connected to " + deviceId);
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    onConnected.actionPerformed(new ActionEvent(true));
                }
            });
            return;
        }

        onConnectedListeners.put(deviceId, onConnected);
        Log.p("[MyNativeBluetoothImpl] Connecting to " + deviceId);
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            device.connectGatt(context, false, gattCallback, 2); // 2 = TRANSPORT_LE
        } else {
            device.connectGatt(context, false, gattCallback);
        }
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
    public void subscribe(String deviceId, String serviceUUID, String characteristicUUID, ActionListener onData) {
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
    public void write(String deviceId, String serviceUUID, String characteristicUUID, byte[] data, final ActionListener onWrite) {
        BluetoothGatt gatt = connectedGatts.get(deviceId);
        if (gatt == null) {
            Log.p("[MyNativeBluetoothImpl] Not connected to " + deviceId);
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    onWrite.actionPerformed(new ActionEvent(false));
                }
            });
            return;
        }
        BluetoothGattService service = gatt.getService(UUID.fromString(serviceUUID));
        if (service == null) {
            Log.p("[MyNativeBluetoothImpl] Service not found: " + serviceUUID);
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    onWrite.actionPerformed(new ActionEvent(false));
                }
            });
            return;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));
        if (characteristic == null) {
            Log.p("[MyNativeBluetoothImpl] Characteristic not found: " + characteristicUUID);
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    onWrite.actionPerformed(new ActionEvent(false));
                }
            });
            return;
        }
        onWriteListeners.put(deviceId + characteristicUUID, onWrite);
        characteristic.setValue(data);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        if (!gatt.writeCharacteristic(characteristic)) {
            Log.p("[MyNativeBluetoothImpl] Failed to write characteristic");
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    onWrite.actionPerformed(new ActionEvent(false));
                }
            });
        }
    }
    
    @Override
    public boolean isSupported() {
        return context.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le");
    }
}
