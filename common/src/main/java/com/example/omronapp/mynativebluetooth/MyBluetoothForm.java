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
import com.example.omronapp.omron.OmronMeasurement;
import java.util.ArrayList;

public class MyBluetoothForm extends Form {

    private MyBluetoothManager bluetoothManager;
    private DefaultListModel<String> deviceListModel;
    private List<String> deviceList;
    private TextArea logArea;
    private TextArea jsonDataArea;
    private java.util.List<OmronMeasurement> measurements = new ArrayList<>();

    public MyBluetoothForm() {
        super("My Native Bluetooth", new BorderLayout());
        bluetoothManager = MyBluetoothManager.getInstance();

        // UI Components
        Button scanButton = new Button("Scan");
        Button connectButton = new Button("Connect & Read");
        Button disconnectButton = new Button("Disconnect");
        deviceListModel = new DefaultListModel<String>();
        deviceList = new List<String>(deviceListModel);
        logArea = new TextArea(8, 20);
        logArea.setEditable(false);
        jsonDataArea = new TextArea(8, 20);
        jsonDataArea.setEditable(false);

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
        scanButton.addActionListener(new com.codename1.ui.events.ActionListener() {
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                log("Scanning...");
                deviceListModel.removeAll();
                bluetoothManager.startScan(new com.codename1.ui.events.ActionListener() {
                    public void actionPerformed(com.codename1.ui.events.ActionEvent e) {
                        String deviceId = (String) e.getSource();
                        log("Device found: " + deviceId);
                        deviceListModel.addItem(deviceId);
                    }
                });
            }
        });

        connectButton.addActionListener(new com.codename1.ui.events.ActionListener() {
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                final String selectedDevice = deviceList.getSelectedItem();
                if (selectedDevice != null) {
                    log("Connecting to " + selectedDevice + "...");
                    bluetoothManager.connect(selectedDevice, new com.codename1.ui.events.ActionListener() {
                        public void actionPerformed(com.codename1.ui.events.ActionEvent e) {
                            if (e.getSource() != null && (Boolean) e.getSource()) {
                                log("Connected to " + selectedDevice + ". Starting data retrieval...");
                                measurements.clear();
                                jsonDataArea.setText("");
                                bluetoothManager.startDataRetrieval(new com.codename1.ui.events.ActionListener() {
                                    public void actionPerformed(com.codename1.ui.events.ActionEvent dataEvt) {
                                        OmronMeasurement m = (OmronMeasurement) dataEvt.getSource();
                                        measurements.add(m);
                                        updateJsonArea();
                                    }
                                }, new com.codename1.ui.events.ActionListener() {
                                    public void actionPerformed(com.codename1.ui.events.ActionEvent logEvt) {
                                        log((String) logEvt.getSource());
                                    }
                                });
                            } else {
                                log("Failed to connect to " + selectedDevice);
                            }
                        }
                    });
                } else {
                    log("Please select a device first.");
                }
            }
        });

        disconnectButton.addActionListener(new com.codename1.ui.events.ActionListener() {
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                log("Disconnecting...");
                bluetoothManager.disconnect();
            }
        });

        // Initialize Bluetooth
        bluetoothManager.initialize(new com.codename1.ui.events.ActionListener() {
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                if (evt.getSource() != null && (Boolean)evt.getSource()) {
                    log("Bluetooth initialized.");
                } else {
                    log("Bluetooth not supported or not enabled.");
                }
            }
        });
    }

    private void log(String message) {
        logArea.setText(logArea.getText() + (logArea.getText().length() > 0 ? "\n" : "") + message);
        // Removed setCursorPosition as it's not available for TextArea
    }

    private void updateJsonArea() {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < measurements.size(); i++) {
            sb.append("  ").append(measurements.get(i).toJSON());
            if (i < measurements.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        jsonDataArea.setText(sb.toString());
    }
}
