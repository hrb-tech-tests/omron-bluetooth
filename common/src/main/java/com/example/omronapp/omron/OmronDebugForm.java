package com.example.omronapp.omron;

import com.codename1.io.Preferences;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.components.InfiniteProgress;

import java.util.List;

/**
 * DEBUG VERSION of OmronExampleForm
 * 
 * This form uses OmronBluetoothServiceDebug and displays detailed logs.
 * Logs are accumulated across all connection attempts and can be shared via any
 * app.
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
    private final Button abortButton;
    private final Button shareLogsButton;
    private final OmronBluetoothServiceDebug debugService;

    public OmronDebugForm() {
        super("OMRON Debug 8", BoxLayout.y());

        debugService = new OmronBluetoothServiceDebug();

        // Status labels
        statusLabel = new Label("DEBUG MODE - Enhanced logging enabled");
        statusLabel.getAllStyles().setFgColor(0xFF0000); // Red to indicate debug mode
        statusLabel.getAllStyles().setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));

        formatLabel = new Label(" ");
        formatLabel.getAllStyles().setFgColor(0x0000FF);
        formatLabel.getAllStyles().setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_ITALIC, Font.SIZE_SMALL));

        // MAC address input field
        macAddressField = createMacAddressField();

        // Log display area (larger than normal result area) - MUST be before buttons
        // that use it
        logArea = new TextArea(15, 40);
        logArea.setEditable(false);
        logArea.setGrowByContent(false); // Don't grow, use scrollbars instead
        logArea.getAllStyles().setFont(
                Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        logArea.getAllStyles().setBgColor(0x000000); // Black background
        logArea.getAllStyles().setFgColor(0x00FF00); // Green text (terminal style)

        // Scan button
        scanButton = new Button("Get Data (Debug Mode)");
        scanButton.getAllStyles().setBgColor(0xFF0000); // Red for debug
        scanButton.getAllStyles().setFgColor(0xFFFFFF);
        scanButton.getAllStyles().setBgTransparency(200);
        scanButton.getAllStyles().setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        scanButton.addActionListener(e -> retrieveDeviceData());

        // Share logs button - MUST be before clearLogsButton that uses it
        shareLogsButton = new Button("");
        shareLogsButton
                .setIcon(FontImage.createMaterial(FontImage.MATERIAL_SHARE, shareLogsButton.getUnselectedStyle()));
        shareLogsButton.getAllStyles().setBgColor(0x0000FF); // Blue
        shareLogsButton.getAllStyles().setFgColor(0xFFFFFF);
        shareLogsButton.getAllStyles().setBgTransparency(200);
        shareLogsButton.setEnabled(false); // Disabled until we have logs
        shareLogsButton.addActionListener(e -> shareLogs());

        // Clear logs button
        clearLogsButton = new Button("Clear Logs");
        clearLogsButton.getAllStyles().setBgColor(0x666666);
        clearLogsButton.getAllStyles().setFgColor(0xFFFFFF);
        clearLogsButton.getAllStyles().setBgTransparency(200);
        clearLogsButton.getAllStyles()
                .setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        clearLogsButton.addActionListener(e -> {
            logArea.setText("");
            debugService.clearDebugLogs();
            statusLabel.setText("Logs cleared");
            shareLogsButton.setEnabled(false);
        });

        // Abort & Clear button
        abortButton = new Button("Abort & Clear");
        abortButton.getAllStyles().setBgColor(0xAA0000); // Darker red
        abortButton.getAllStyles().setFgColor(0xFFFFFF);
        abortButton.getAllStyles().setBgTransparency(200);
        abortButton.getAllStyles().setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        abortButton.setEnabled(false); // Only enabled during scan
        abortButton.addActionListener(e -> {
            debugService.abort();
            logArea.setText("");
            debugService.clearDebugLogs();
            statusLabel.setText("Aborted and logs cleared");
            shareLogsButton.setEnabled(false);
            resetUIState();
        });

        // Layout
        Container macContainer = new Container(new BorderLayout());
        macContainer.add(BorderLayout.WEST, new Label("MAC:"));
        macContainer.add(BorderLayout.CENTER, macAddressField);

        Container buttonContainer = new Container(new BoxLayout(BoxLayout.X_AXIS));
        buttonContainer.add(scanButton);
        buttonContainer.add(clearLogsButton);
        buttonContainer.add(shareLogsButton);

        add(statusLabel);
        add(formatLabel);
        add(macContainer);
        add(buttonContainer);
        add(new Label("Debug Logs (Accumulated):"));
        add(logArea);
        add(abortButton);

        // Load last used MAC address
        loadLastMacAddress();

        // Show initial help
        logArea.setText("DEBUG MODE ACTIVE\n" +
                "=================\n" +
                "This version logs every step of the Bluetooth communication.\n" +
                "Enter a MAC address and tap 'Get Data' to begin.\n" +
                "All operations will be logged below in real-time.\n\n" +
                "Logs accumulate across all 4 connection attempts.\n" +
                "After attempts complete, use 'Share Logs' to send via email/etc.\n\n" +
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

        // Reset abort flag before starting new cycle
        debugService.resetAbort();

        // Update UI state - disable buttons during attempts
        scanButton.setEnabled(false);
        // Use a sync icon to indicate progress
        scanButton.setIcon(FontImage.createMaterial(FontImage.MATERIAL_SYNC, scanButton.getUnselectedStyle()));

        macAddressField.setEnabled(false);
        clearLogsButton.setEnabled(false);
        shareLogsButton.setEnabled(false);
        abortButton.setEnabled(true);

        // DON'T clear logs - accumulate across all attempts
        // Add separator if this is not the first run
        String currentLogs = logArea.getText();
        if (currentLogs != null && !currentLogs.trim().isEmpty() &&
                !currentLogs.contains("DEBUG MODE ACTIVE")) {
            CN.callSerially(() -> {
                String separator = "";
                for (int j = 0; j < 60; j++)
                    separator += "=";
                logArea.setText(currentLogs + "\n\n" +
                        separator + "\n" +
                        "NEW ATTEMPT CYCLE - " + new java.util.Date() + "\n" +
                        separator + "\n\n");
            });
        }

        // Run in background thread
        new Thread(() -> {
            boolean success = false;
            String lastError = "Unknown error";

            for (int i = 0; i < macFormats.size(); i++) {
                if (debugService.isAborted()) {
                    break;
                }
                String currentMac = macFormats.get(i);
                String formatDesc = (i < formatDescriptions.length) ? formatDescriptions[i] : "Unknown Format";

                final int attemptNum = i + 1;
                final int total = macFormats.size();

                // Add visual separator for each attempt
                final String attemptSeparator = "\n" + createSeparator("=", 60) + "\n" +
                        "ATTEMPT " + attemptNum + "/" + total + ": " + formatDesc + "\n" +
                        "MAC Format: " + currentMac + "\n" +
                        createSeparator("-", 60) + "\n";

                CN.callSerially(() -> {
                    String currentText = logArea.getText();
                    logArea.setText(currentText + attemptSeparator);
                });

                // Update status on EDT
                CN.callSerially(() -> {
                    statusLabel.setText("Attempt " + attemptNum + "/" + total + ": " + formatDesc);
                    formatLabel.setText("Processing: " + currentMac);
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

                        // Enable share button and reset other UI
                        shareLogsButton.setEnabled(true);
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

                    if (ex.getErrorType() == OmronBluetoothException.ErrorType.ABORTED) {
                        break;
                    }

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

                    // Enable share button so user can share failure logs
                    shareLogsButton.setEnabled(true);
                    resetUIState();
                });
            }

        }).start();
    }

    /**
     * Update the log display with latest debug logs
     */
    private void updateLogDisplay() {
        if (debugService.isAborted()) {
            logArea.setText("");
            debugService.clearDebugLogs();
            return;
        }

        List<String> logs = debugService.getDebugLogs();
        StringBuilder sb = new StringBuilder();

        // Get existing logs
        String existing = logArea.getText();
        if (existing != null && !existing.isEmpty()) {
            sb.append(existing);
            // Only add newline if existing doesn't end with one
            if (!existing.endsWith("\n")) {
                sb.append("\n");
            }
        }

        // Append new logs
        for (String log : logs) {
            sb.append(log).append("\n");
        }

        logArea.setText(sb.toString());

        // Clear the logs in the service after they've been moved to the UI
        // to avoid duplication in the next updateLogDisplay call
        debugService.clearDebugLogs();
    }

    /**
     * Share logs using system share dialog
     */
    private void shareLogs() {
        String logs = logArea.getText();

        if (logs == null || logs.trim().isEmpty()) {
            Dialog.show("No Logs", "No logs to share yet.", "OK", null);
            return;
        }

        // Use Codename One's Display.sendMessage for sharing
        try {
            com.codename1.ui.Display.getInstance().sendMessage(
                    new String[] {}, // Empty recipients - will open share dialog
                    "OMRON Bluetooth Debug Logs",
                    new com.codename1.messaging.Message(logs));
        } catch (Exception e) {
            Dialog.show("Share Failed",
                    "Could not open share dialog: " + e.getMessage() + "\n\n" +
                            "You can manually copy the logs from the screen.",
                    "OK", null);
        }
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
        scanButton.setIcon(null);
        macAddressField.setEnabled(true);
        clearLogsButton.setEnabled(true);
        abortButton.setEnabled(false);
        // shareLogsButton state is managed separately
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

    /**
     * Create a separator line with repeated characters
     */
    private String createSeparator(String character, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(character);
        }
        return sb.toString();
    }
}
