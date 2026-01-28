package com.example.omronapp.mynativebluetooth;

import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.ActionEvent;

public interface MyNativeBluetooth extends com.codename1.system.NativeInterface {
    void initialize(ActionListener onInitialized);
    void startScan(ActionListener onDeviceFound);
    void stopScan();
    void connect(String deviceId, ActionListener onConnected);
    void disconnect(String deviceId);
    void subscribe(String deviceId, String serviceUUID, String characteristicUUID, ActionListener onData);
    void write(String deviceId, String serviceUUID, String characteristicUUID, byte[] data, ActionListener onWrite);
    boolean isSupported();
}
