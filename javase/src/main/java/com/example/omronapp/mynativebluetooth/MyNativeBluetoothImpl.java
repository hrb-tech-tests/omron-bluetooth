package com.example.omronapp.mynativebluetooth;

import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.ActionEvent;
import com.codename1.io.Log;

public class MyNativeBluetoothImpl implements MyNativeBluetooth {
    public void initialize(ActionListener<ActionEvent> onInitialized) {
        Log.p("[MyNativeBluetoothImpl] Simulator stub: initialize");
        onInitialized.actionPerformed(new ActionEvent(false));
    }

    public void startScan(ActionListener<ActionEvent> onDeviceFound) {
        Log.p("[MyNativeBluetoothImpl] Simulator stub: startScan");
    }

    public void stopScan() {
        Log.p("[MyNativeBluetoothImpl] Simulator stub: stopScan");
    }

    public void connect(String deviceId, ActionListener<ActionEvent> onConnected) {
        Log.p("[MyNativeBluetoothImpl] Simulator stub: connect to " + deviceId);
        onConnected.actionPerformed(new ActionEvent(false));
    }

    public void disconnect(String deviceId) {
        Log.p("[MyNativeBluetoothImpl] Simulator stub: disconnect from " + deviceId);
    }

    public void subscribe(String deviceId, String serviceUUID, String characteristicUUID, ActionListener<ActionEvent> onData) {
        Log.p("[MyNativeBluetoothImpl] Simulator stub: subscribe to " + characteristicUUID);
    }

    public void write(String deviceId, String serviceUUID, String characteristicUUID, byte[] data, ActionListener<ActionEvent> onWrite) {
        Log.p("[MyNativeBluetoothImpl] Simulator stub: write to " + characteristicUUID);
        onWrite.actionPerformed(new ActionEvent(false));
    }

    public boolean isSupported() {
        Log.p("[MyNativeBluetoothImpl] Simulator stub: isSupported returning false");
        return false;
    }
}
