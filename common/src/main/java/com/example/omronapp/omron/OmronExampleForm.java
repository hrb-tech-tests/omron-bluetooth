package com.example.omronapp.omron;

import com.codename1.io.Preferences;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

/**
 * Example form demonstrating OMRON Bluetooth integration usage.
 * 
 * This form allows testing the OMRON HEM-7144T2 blood pressure monitor
 * Bluetooth integration. Features:
 * - MAC address input field with persistence
 * - Real-time status updates
 * - JSON data display
 * - Error handling with user-friendly messages
 * 
 * Note: Bluetooth only works on real devices (iOS/Android), not in the
 * simulator.
 */
public class OmronExampleForm extends Form {

    // Preference key for persisting the last used MAC address
    private static final String PREF_LAST_MAC_ADDRESS = "omron_last_mac_address";

    // Default MAC address placeholder
    private static final String DEFAULT_MAC_HINT = "AA:BB:CC:DD:EE:FF";

    private final TextField macAddressField;
    private final TextArea resultArea;
    private final Label statusLabel;
    private final Button scanButton;

    public OmronExampleForm() {
        super("OMRON Integration", BoxLayout.y());

        // Status label
        statusLabel = new Label("Enter device MAC address and tap 'Get Data'");
        statusLabel.getAllStyles().setFgColor(0x0000FF);

        // MAC address input field
        macAddressField = createMacAddressField();

        // Scan button
        scanButton = new Button("Get Data from Device");
        scanButton.getAllStyles().setBgColor(0x0000FF);
        scanButton.getAllStyles().setFgColor(0xFFFFFF);
        scanButton.getAllStyles().setBgTransparency(200);
        scanButton.addActionListener(e -> retrieveDeviceData());

        // Result display area
        resultArea = new TextArea(8, 40);
        resultArea.setEditable(false);
        resultArea.getAllStyles().setFont(
                Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL));

        // Layout
        Container macContainer = new Container(new BorderLayout());
        macContainer.add(BorderLayout.WEST, new Label("MAC:"));
        macContainer.add(BorderLayout.CENTER, macAddressField);

        add(statusLabel);
        add(macContainer);
        add(scanButton);
        add(new Label("Result (JSON):"));
        add(resultArea);

        // Load last used MAC address
        loadLastMacAddress();
    }

    /**
     * Creates the MAC address input field with proper styling.
     */
    private TextField createMacAddressField() {
        TextField field = new TextField();
        field.setHint(DEFAULT_MAC_HINT);

        Style style = field.getAllStyles();
        style.setFont(
                Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        style.setFgColor(0x00008B);

        // Auto-format: convert to uppercase as user types
        field.addDataChangedListener((type, index) -> {
            String text = field.getText();
            if (text != null && !text.equals(text.toUpperCase())) {
                // Convert to uppercase without moving cursor
                int cursorPos = field.getCursorPosition();
                field.setText(text.toUpperCase());
                if (cursorPos <= field.getText().length()) {
                    field.setCursorPosition(cursorPos);
                }
            }
        });

        return field;
    }

    /**
     * Loads the last used MAC address from preferences.
     */
    private void loadLastMacAddress() {
        String lastMac = Preferences.get(PREF_LAST_MAC_ADDRESS, "");
        if (lastMac != null && !lastMac.isEmpty()) {
            macAddressField.setText(lastMac);
        }
    }

    /**
     * Saves the current MAC address to preferences for future use.
     */
    private void saveMacAddress(String mac) {
        if (mac != null && !mac.isEmpty()) {
            Preferences.set(PREF_LAST_MAC_ADDRESS, mac);
        }
    }

    /**
     * Validates the MAC address format.
     * Expected format: AA:BB:CC:DD:EE:FF or AA-BB-CC-DD-EE-FF
     */
    private boolean isValidMacAddress(String mac) {
        if (mac == null || mac.length() != 17) {
            return false;
        }

        for (int i = 0; i < mac.length(); i++) {
            char c = mac.charAt(i);
            if (i % 3 == 2) {
                // Separator position - must be : or -
                if (c != ':' && c != '-') {
                    return false;
                }
            } else {
                // Hex digit position
                if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f'))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Retrieves data from the OMRON device using the entered MAC address.
     */
    private void retrieveDeviceData() {
        String deviceMac = macAddressField.getText();

        // Validate input
        if (deviceMac == null || deviceMac.trim().isEmpty()) {
            Dialog.show("Input Required",
                    "Please enter the device MAC address.\nFormat: AA:BB:CC:DD:EE:FF",
                    "OK", null);
            return;
        }

        deviceMac = deviceMac.trim().toUpperCase();

        // Validate format
        if (!isValidMacAddress(deviceMac)) {
            Dialog.show("Invalid Format",
                    "MAC address must be in format:\nAA:BB:CC:DD:EE:FF\n\nExample: 5C:02:72:AB:CD:EF",
                    "OK", null);
            return;
        }

        // Save for future use
        saveMacAddress(deviceMac);

        // Update UI state
        statusLabel.setText("Connecting to " + deviceMac + "...");
        resultArea.setText("");
        scanButton.setEnabled(false);
        macAddressField.setEnabled(false);

        // Capture for use in thread
        final String finalDeviceMac = deviceMac;

        // Run in background thread to avoid blocking UI
        new Thread(() -> {
            try {
                // Single method call - returns complete JSON
                String jsonData = OmronBluetoothService.INSTANCE.getDataFromDevice(finalDeviceMac);

                // Update UI on EDT
                CN.callSerially(() -> {
                    statusLabel.setText("Success! Data retrieved from " + finalDeviceMac);
                    resultArea.setText(formatJson(jsonData));
                    resetUIState();
                });

            } catch (OmronBluetoothException ex) {
                // Handle specific error types
                CN.callSerially(() -> {
                    statusLabel.setText("Error: " + ex.getErrorType());
                    resultArea.setText(ex.getMessage());
                    Dialog.show("Connection Error", ex.getMessage(), "OK", null);
                    resetUIState();
                });
            }
        }).start();
    }

    /**
     * Resets UI elements to enabled state after operation completes.
     */
    private void resetUIState() {
        scanButton.setEnabled(true);
        macAddressField.setEnabled(true);
    }

    /**
     * Simple JSON formatter for better readability.
     * Adds line breaks after commas and braces.
     */
    private String formatJson(String json) {
        if (json == null) {
            return "";
        }

        StringBuilder formatted = new StringBuilder();
        int indent = 0;
        boolean inString = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
            }

            if (!inString) {
                if (c == '{' || c == '[') {
                    formatted.append(c);
                    formatted.append('\n');
                    indent++;
                    appendIndent(formatted, indent);
                } else if (c == '}' || c == ']') {
                    formatted.append('\n');
                    indent--;
                    appendIndent(formatted, indent);
                    formatted.append(c);
                } else if (c == ',') {
                    formatted.append(c);
                    formatted.append('\n');
                    appendIndent(formatted, indent);
                } else if (c == ':') {
                    formatted.append(c);
                    formatted.append(' ');
                } else {
                    formatted.append(c);
                }
            } else {
                formatted.append(c);
            }
        }

        return formatted.toString();
    }

    /**
     * Appends indentation spaces.
     */
    private void appendIndent(StringBuilder sb, int level) {
        for (int i = 0; i < level * 2; i++) {
            sb.append(' ');
        }
    }
}
