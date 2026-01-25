package com.example.omronapp.mynativebluetooth;

import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.ActionEvent;

public interface MyNativeBluetooth extends com.codename1.system.NativeInterface {
    void initialize(ActionListener<ActionEvent> onInitialized);
    void startScan(ActionListener<ActionEvent> onDeviceFound);
    void stopScan();
    void connect(String deviceId, ActionListener<ActionEvent> onConnected);
    void disconnect(String deviceId);
    void subscribe(String deviceId, String serviceUUID, String characteristicUUID, ActionListener<ActionEvent> onData);
    void write(String deviceId, String serviceUUID, String characteristicUUID, byte[] data, ActionListener<ActionEvent> onWrite);
    boolean isSupported();
}
