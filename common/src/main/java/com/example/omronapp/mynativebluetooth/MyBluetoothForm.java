package com.example.omronapp.mynativebluetooth;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.List;

public class MyBluetoothForm extends Form {

    private MyBluetoothManager bluetoothManager;
    private DefaultListModel<String> deviceListModel;
    private List<String> deviceList;
    private TextArea logArea;
    private TextArea jsonDataArea;

    public MyBluetoothForm() {
        super("My Native Bluetooth", new BorderLayout());
        bluetoothManager = MyBluetoothManager.getInstance();

        // UI Components
        Button scanButton = new Button("Scan");
        Button connectButton = new Button("Connect");
        Button disconnectButton = new Button("Disconnect");
        deviceListModel = new DefaultListModel<>();
        deviceList = new List<>(deviceListModel);
        logArea = new TextArea(5, 20);
        jsonDataArea = new TextArea(5, 20);

        // Layout
        Container top = new Container(new BoxLayout(BoxLayout.X_AXIS));
        top.add(scanButton);
        top.add(connectButton);
        top.add(disconnectButton);

        Container center = new Container(new BorderLayout());
        center.add(BorderLayout.NORTH, new Label("Found Devices:"));
        center.add(BorderLayout.CENTER, deviceList);

        Container south = new Container(new BoxLayout(BoxLayout.Y_AXIS));
        south.add(new Label("Logs:"));
        south.add(logArea);
        south.add(new Label("JSON Data:"));
        south.add(jsonDataArea);

        add(BorderLayout.NORTH, top);
        add(BorderLayout.CENTER, center);
        add(BorderLayout.SOUTH, south);

        // Actions
        scanButton.addActionListener(evt -> {
            log("Scanning...");
            deviceListModel.removeAll();
            bluetoothManager.startScan(e -> {
                String deviceId = (String) e.getSource();
                log("Device found: " + deviceId);
                deviceListModel.addItem(deviceId);
            });
        });

        connectButton.addActionListener(evt -> {
            String selectedDevice = deviceList.getSelectedItem();
            if (selectedDevice != null) {
                log("Connecting to " + selectedDevice);
                bluetoothManager.connect(selectedDevice, e -> {
                    if (e.getSource() != null && (Boolean) e.getSource()) {
                        log("Connected to " + selectedDevice);
                    } else {
                        log("Failed to connect to " + selectedDevice);
                    }
                });
            }
        });

        disconnectButton.addActionListener(evt -> {
            log("Disconnecting...");
            bluetoothManager.disconnect();
        });

        // Initialize Bluetooth
        bluetoothManager.initialize(evt -> {
            if (evt.getSource() != null && (Boolean)evt.getSource()) {
                log("Bluetooth initialized.");
            } else {
                log("Bluetooth not supported or not enabled.");
            }
        });
    }

    private void log(String message) {
        logArea.setText(logArea.getText() + "\n" + message);
    }
}
