package com.example.omronapp.mynativebluetooth;

import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.ActionEvent;
import com.example.omronapp.omron.OmronBluetoothException;
import com.example.omronapp.omron.OmronMeasurement;
import com.example.omronapp.omron.OmronProtocolParser;
import java.util.ArrayList;
import java.util.List;

public class MyBluetoothManager {
    private static MyBluetoothManager instance;
    private MyNativeBluetooth nativeBluetooth;

    private List<String> foundDevices = new ArrayList<String>();
    private String connectedDevice;

    // UUIDs
    public static final String OMRON_SERVICE_UUID = "49123040-aee8-11e1-a74d-0002a5d5c51b";
    public static final String OMRON_WRITE_UUID = "49123041-aee8-11e1-a74d-0002a5d5c51b";
    public static final String OMRON_NOTIFY_UUID = "49123042-aee8-11e1-a74d-0002a5d5c51b";

    private MyBluetoothManager() {
        nativeBluetooth = MyNativeBluetoothFactory.getInstance();
    }

    public static MyBluetoothManager getInstance() {
        if (instance == null) {
            instance = new MyBluetoothManager();
        }
        return instance;
    }

    public void initialize(ActionListener onInitialized) {
        nativeBluetooth.initialize(onInitialized);
    }

    public void startScan(final ActionListener onDeviceFound) {
        foundDevices.clear();
        nativeBluetooth.startScan(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                String deviceId = (String)evt.getSource();
                if (!foundDevices.contains(deviceId)) {
                    foundDevices.add(deviceId);
                    onDeviceFound.actionPerformed(evt);
                }
            }
        });
    }

    public void stopScan() {
        nativeBluetooth.stopScan();
    }

    public void connect(final String deviceId, final ActionListener onConnected) {
        nativeBluetooth.connect(deviceId, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (evt.getSource() != null && (Boolean)evt.getSource()) {
                    connectedDevice = deviceId;
                }
                onConnected.actionPerformed(evt);
            }
        });
    }

    public void disconnect() {
        if (connectedDevice != null) {
            nativeBluetooth.disconnect(connectedDevice);
            connectedDevice = null;
        }
    }

    public void startDataRetrieval(final ActionListener onDataReceived, final ActionListener onLog) {
        if (connectedDevice == null) {
            onLog.actionPerformed(new ActionEvent("Error: Not connected to any device."));
            return;
        }

        onLog.actionPerformed(new ActionEvent("Starting handshake..."));
        
        // Handshake: Write [0x01, 0x00] to the write characteristic
        byte[] handshakeData = new byte[]{0x01, 0x00};
        nativeBluetooth.write(connectedDevice, OMRON_SERVICE_UUID, OMRON_WRITE_UUID, handshakeData, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent writeEvt) {
                if (writeEvt.getSource() != null && (Boolean)writeEvt.getSource()) {
                    onLog.actionPerformed(new ActionEvent("Handshake sent successfully. Subscribing to notifications..."));
                    
                    nativeBluetooth.subscribe(connectedDevice, OMRON_SERVICE_UUID, OMRON_NOTIFY_UUID, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent notifyEvt) {
                            byte[] data = (byte[]) notifyEvt.getSource();
                            onLog.actionPerformed(new ActionEvent("Notification received, length: " + (data != null ? data.length : 0)));
                            
                            if (data != null && data.length > 0) {
                                try {
                                    OmronMeasurement measurement = OmronProtocolParser.parse(data);
                                    onLog.actionPerformed(new ActionEvent("Measurement parsed: " + measurement.toString()));
                                    onDataReceived.actionPerformed(new ActionEvent(measurement));
                                } catch (OmronBluetoothException e) {
                                    onLog.actionPerformed(new ActionEvent("Error parsing measurement: " + e.getMessage()));
                                }
                            }
                        }
                    });
                } else {
                    onLog.actionPerformed(new ActionEvent("Failed to send handshake."));
                }
            }
        });
    }

    public void subscribe(String serviceUUID, String characteristicUUID, ActionListener onData) {
        if (connectedDevice != null) {
            nativeBluetooth.subscribe(connectedDevice, serviceUUID, characteristicUUID, onData);
        }
    }

    public void write(String serviceUUID, String characteristicUUID, byte[] data, ActionListener onWrite) {
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
