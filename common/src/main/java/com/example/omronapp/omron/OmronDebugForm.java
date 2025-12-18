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

import java.util.List;

/**
 * DEBUG VERSION of OmronExampleForm
 * 
 * This form uses OmronBluetoothServiceDebug and displays detailed logs
 * to help diagnose Bluetooth communication issues.
 */
public class OmronDebugForm extends Form {

    private static final String PREF_LAST_MAC_ADDRESS = "omron_last_mac_address";
    private static final String DEFAULT_MAC_HINT = "AA:BB:CC:DD:EE:FF";

    private final TextField macAddressField;
    private final TextArea logArea;
    private final Label statusLabel;
    private final Label formatLabel;
    private final Button scanButton;
    private final Button clearLogsButton;
    private final OmronBluetoothServiceDebug debugService;

    public OmronDebugForm() {
        super("OMRON Debug Mode", BoxLayout.y());

        debugService = new OmronBluetoothServiceDebug();

        // Status labels
        statusLabel = new Label("DEBUG MODE - Enhanced logging enabled");
        statusLabel.getAllStyles().setFgColor(0xFF0000); // Red to indicate debug mode
        statusLabel.getAllStyles().setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM));

        formatLabel = new Label(" ");
        formatLabel.getAllStyles().setFgColor(0x0000FF);
        formatLabel.getAllStyles().setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_ITALIC, Font.SIZE_SMALL));

        // MAC address input field
        macAddressField = createMacAddressField();

        // Log display area (larger than normal result area) - MUST be before
        // clearLogsButton
        logArea = new TextArea(15, 40);
        logArea.setEditable(false);
        logArea.getAllStyles().setFont(
                Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        logArea.getAllStyles().setBgColor(0x000000); // Black background
        logArea.getAllStyles().setFgColor(0x00FF00); // Green text (terminal style)

        // Scan button
        scanButton = new Button("Get Data (Debug Mode)");
        scanButton.getAllStyles().setBgColor(0xFF0000); // Red for debug
        scanButton.getAllStyles().setFgColor(0xFFFFFF);
        scanButton.getAllStyles().setBgTransparency(200);
        scanButton.addActionListener(e -> retrieveDeviceData());

        // Clear logs button
        clearLogsButton = new Button("Clear Logs");
        clearLogsButton.getAllStyles().setBgColor(0x666666);
        clearLogsButton.getAllStyles().setFgColor(0xFFFFFF);
        clearLogsButton.getAllStyles().setBgTransparency(200);
        clearLogsButton.addActionListener(e -> {
            logArea.setText("");
            OmronBluetoothServiceDebug.clearDebugLogs();
            statusLabel.setText("Logs cleared");
        });

        // Layout
        Container macContainer = new Container(new BorderLayout());
        macContainer.add(BorderLayout.WEST, new Label("MAC:"));
        macContainer.add(BorderLayout.CENTER, macAddressField);

        Container buttonContainer = new Container(new BoxLayout(BoxLayout.X_AXIS));
        buttonContainer.add(scanButton);
        buttonContainer.add(clearLogsButton);

        add(statusLabel);
        add(formatLabel);
        add(macContainer);
        add(buttonContainer);
        add(new Label("Debug Logs (Real-time):"));
        add(logArea);

        // Load last used MAC address
        loadLastMacAddress();

        // Show initial help
        logArea.setText("DEBUG MODE ACTIVE\n" +
                "=================\n" +
                "This version logs every step of the Bluetooth communication.\n" +
                "Enter a MAC address and tap 'Get Data' to begin.\n" +
                "All operations will be logged below in real-time.\n\n" +
                "Look for:\n" +
                "- Connection status\n" +
                "- Subscription status\n" +
                "- Notification count\n" +
                "- Any errors or warnings\n\n" +
                "Ready to start...\n");
    }

    private TextField createMacAddressField() {
        TextField field = new TextField();
        field.setHint(DEFAULT_MAC_HINT);

        Style style = field.getAllStyles();
        style.setFont(
                Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        style.setFgColor(0x00008B);

        field.addDataChangedListener((type, index) -> {
            String text = field.getText();
            if (text != null && !text.equals(text.toUpperCase())) {
                int cursorPos = field.getCursorPosition();
                field.setText(text.toUpperCase());
                if (cursorPos <= field.getText().length()) {
                    field.setCursorPosition(cursorPos);
                }
            }
        });

        return field;
    }

    private void loadLastMacAddress() {
        String lastMac = Preferences.get(PREF_LAST_MAC_ADDRESS, "");
        if (lastMac != null && !lastMac.isEmpty()) {
            macAddressField.setText(lastMac);
        }
    }

    private void saveMacAddress(String mac) {
        if (mac != null && !mac.isEmpty()) {
            Preferences.set(PREF_LAST_MAC_ADDRESS, mac);
        }
    }

    private void retrieveDeviceData() {
        String inputMac = macAddressField.getText();

        if (inputMac == null || inputMac.trim().isEmpty()) {
            Dialog.show("Input Required", "Please enter the device MAC address.", "OK", null);
            return;
        }

        // Generate formats
        java.util.List<String> macFormats = generateMacFormats(inputMac);
        String[] formatDescriptions = {
                "Colon Separated (AA:BB...)",
                "Dash Separated (AA-BB...)",
                "Space Separated (AA BB...)",
                "No Separator (AABB...)"
        };

        if (macFormats.isEmpty()) {
            Dialog.show("Invalid Format",
                    "Could not parse MAC address. Please ensure you entered 12 hex digits.",
                    "OK", null);
            return;
        }

        saveMacAddress(inputMac);

        // Update UI state
        scanButton.setEnabled(false);
        macAddressField.setEnabled(false);
        clearLogsButton.setEnabled(false);
        logArea.setText("Starting debug session...\n");

        // Run in background thread
        new Thread(() -> {
            boolean success = false;
            String lastError = "Unknown error";

            for (int i = 0; i < macFormats.size(); i++) {
                String currentMac = macFormats.get(i);
                String formatDesc = (i < formatDescriptions.length) ? formatDescriptions[i] : "Unknown Format";

                final int attemptNum = i + 1;
                final int total = macFormats.size();

                // Update status on EDT
                CN.callSerially(() -> {
                    statusLabel.setText("Attempt " + attemptNum + "/" + total + ": " + formatDesc);
                    formatLabel.setText("Connecting to: " + currentMac);
                    updateLogDisplay();
                });

                try {
                    // Try to connect
                    String jsonData = debugService.getDataFromDevice(currentMac);

                    // If we get here, it worked!
                    CN.callSerially(() -> {
                        statusLabel.setText("SUCCESS! Connected using " + formatDesc);
                        formatLabel.setText("Device: " + currentMac);
                        updateLogDisplay();

                        // Show success dialog with option to view JSON
                        if (Dialog.show("Success!",
                                "Data retrieved successfully!\n\nView JSON data?",
                                "Yes", "No")) {
                            showJsonDialog(jsonData);
                        }

                        resetUIState();
                    });
                    success = true;
                    break;

                } catch (OmronBluetoothException ex) {
                    lastError = ex.getMessage();

                    // Update logs after each attempt
                    CN.callSerially(() -> {
                        updateLogDisplay();
                    });

                    if (i < macFormats.size() - 1) {
                        CN.callSerially(() -> {
                            statusLabel.setText("Attempt " + attemptNum + " failed. Waiting 10s...");
                        });
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException ie) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    lastError = e.getMessage();
                    e.printStackTrace();

                    CN.callSerially(() -> {
                        updateLogDisplay();
                    });

                    if (i < macFormats.size() - 1) {
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException ie) {
                            break;
                        }
                    }
                }
            }

            if (!success) {
                final String finalError = lastError;
                CN.callSerially(() -> {
                    statusLabel.setText("All attempts failed");
                    formatLabel.setText(" ");
                    updateLogDisplay();

                    Dialog.show("Connection Failed",
                            "Failed to connect using any MAC format.\n\n" +
                                    "Check the debug logs for details.\n\n" +
                                    "Last error: " + finalError,
                            "OK", null);
                    resetUIState();
                });
            }

        }).start();
    }

    /**
     * Update the log display with latest debug logs
     */
    private void updateLogDisplay() {
        List<String> logs = OmronBluetoothServiceDebug.getDebugLogs();
        StringBuilder sb = new StringBuilder();

        for (String log : logs) {
            sb.append(log).append("\n");
        }

        logArea.setText(sb.toString());
    }

    /**
     * Show JSON data in a dialog
     */
    private void showJsonDialog(String json) {
        Form jsonForm = new Form("JSON Data", new BorderLayout());

        TextArea jsonArea = new TextArea(20, 40);
        jsonArea.setEditable(false);
        jsonArea.setText(formatJson(json));
        jsonArea.getAllStyles().setFont(
                Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL));

        Button closeButton = new Button("Close");
        closeButton.addActionListener(e -> jsonForm.showBack());

        jsonForm.add(BorderLayout.CENTER, jsonArea);
        jsonForm.add(BorderLayout.SOUTH, closeButton);
        jsonForm.show();
    }

    private void resetUIState() {
        scanButton.setEnabled(true);
        macAddressField.setEnabled(true);
        clearLogsButton.setEnabled(true);
    }

    private java.util.List<String> generateMacFormats(String input) {
        java.util.List<String> formats = new java.util.ArrayList<>();
        if (input == null)
            return formats;

        String raw = input.replaceAll("[^0-9A-Fa-f]", "").toUpperCase();

        if (raw.length() != 12) {
            return formats;
        }

        StringBuilder colon = new StringBuilder();
        StringBuilder dash = new StringBuilder();
        StringBuilder space = new StringBuilder();

        for (int i = 0; i < 12; i += 2) {
            String byteStr = raw.substring(i, i + 2);

            colon.append(byteStr);
            dash.append(byteStr);
            space.append(byteStr);

            if (i < 10) {
                colon.append(":");
                dash.append("-");
                space.append(" ");
            }
        }

        formats.add(colon.toString());
        formats.add(dash.toString());
        formats.add(space.toString());
        formats.add(raw);

        return formats;
    }

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

    private void appendIndent(StringBuilder sb, int level) {
        for (int i = 0; i < level * 2; i++) {
            sb.append(' ');
        }
    }
}
