package com.example.omronapp.mynativebluetooth;

import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class MyBluetoothManager {
    private static MyBluetoothManager instance;
    private MyNativeBluetooth nativeBluetooth;

    private List<String> foundDevices = new ArrayList<String>();
    private String connectedDevice;

    private MyBluetoothManager() {
        nativeBluetooth = MyNativeBluetoothFactory.getInstance();
    }

    public static MyBluetoothManager getInstance() {
        if (instance == null) {
            instance = new MyBluetoothManager();
        }
        return instance;
    }

    public void initialize(ActionListener<ActionEvent> onInitialized) {
        nativeBluetooth.initialize(onInitialized);
    }

    public void startScan(ActionListener<ActionEvent> onDeviceFound) {
        foundDevices.clear();
        nativeBluetooth.startScan(evt -> {
            String deviceId = (String)evt.getSource();
            if (!foundDevices.contains(deviceId)) {
                foundDevices.add(deviceId);
                onDeviceFound.actionPerformed(evt);
            }
        });
    }

    public void stopScan() {
        nativeBluetooth.stopScan();
    }

    public void connect(String deviceId, ActionListener<ActionEvent> onConnected) {
        nativeBluetooth.connect(deviceId, evt -> {
            if (evt.getSource() != null && (Boolean)evt.getSource()) {
                connectedDevice = deviceId;
            }
            onConnected.actionPerformed(evt);
        });
    }

    public void disconnect() {
        if (connectedDevice != null) {
            nativeBluetooth.disconnect(connectedDevice);
            connectedDevice = null;
        }
    }

    public void subscribe(String serviceUUID, String characteristicUUID, ActionListener<ActionEvent> onData) {
        if (connectedDevice != null) {
            nativeBluetooth.subscribe(connectedDevice, serviceUUID, characteristicUUID, onData);
        }
    }

    public void write(String serviceUUID, String characteristicUUID, byte[] data, ActionListener<ActionEvent> onWrite) {
        if (connectedDevice != null) {
            nativeBluetooth.write(connectedDevice, serviceUUID, characteristicUUID, data, onWrite);
        }
    }

    public List<String> getFoundDevices() {
        return foundDevices;
    }

    public String getConnectedDevice() {
        return connectedDevice;
    }

    public boolean isSupported() {
        return nativeBluetooth.isSupported();
    }
}
