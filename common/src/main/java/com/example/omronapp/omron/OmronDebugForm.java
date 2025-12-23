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
import com.codename1.ui.Display;

import com.codename1.io.FileSystemStorage;
import java.io.OutputStream;
import java.util.List;

/**
 * DEBUG VERSION of OmronExampleForm
 */
public class OmronDebugForm extends Form {

    private static final String APP_TITLE = "OMERON debug 20";
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

    private final OmronBluetoothService service = OmronBluetoothService.INSTANCE;
    private final OmronLogger.Verbose debugLogger = new OmronLogger.Verbose();

    public OmronDebugForm() {
        super(APP_TITLE, BoxLayout.y());

        service.setLogger(debugLogger);

        statusLabel = new Label("DEBUG MODE - Enhanced logging enabled");
        statusLabel.getAllStyles().setFgColor(0xFF0000);
        statusLabel.getAllStyles().setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL));

        formatLabel = new Label(" ");
        formatLabel.getAllStyles().setFgColor(0x0000FF);
        formatLabel.getAllStyles().setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_ITALIC, Font.SIZE_SMALL));

        macAddressField = createMacAddressField();

        logArea = new TextArea(15, 40);
        logArea.setEditable(false);
        logArea.setGrowByContent(false);
        logArea.getAllStyles().setFont(Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        logArea.getAllStyles().setBgColor(0x000000);
        logArea.getAllStyles().setFgColor(0x00FF00);

        scanButton = new Button("Get Data (Debug Mode)");
        scanButton.getAllStyles().setBgColor(0x4FC3F7);
        scanButton.getAllStyles().setFgColor(0xFFFFFF);
        scanButton.getAllStyles().setBgTransparency(200);
        scanButton.getAllStyles().setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        scanButton.addActionListener(e -> retrieveDeviceData());

        shareLogsButton = new Button("");
        shareLogsButton
                .setIcon(FontImage.createMaterial(FontImage.MATERIAL_SHARE, shareLogsButton.getUnselectedStyle()));
        shareLogsButton.getAllStyles().setBgColor(0x90EE90);
        shareLogsButton.getAllStyles().setFgColor(0xFFFFFF);
        shareLogsButton.getAllStyles().setBgTransparency(200);
        shareLogsButton.setEnabled(false);
        shareLogsButton.addActionListener(e -> shareLogs());

        clearLogsButton = new Button("Clear Logs");
        clearLogsButton.getAllStyles().setBgColor(0xFFA500);
        clearLogsButton.getAllStyles().setFgColor(0xFFFFFF);
        clearLogsButton.getAllStyles().setBgTransparency(200);
        clearLogsButton.getAllStyles()
                .setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        clearLogsButton.addActionListener(e -> {
            logArea.setText("");
            debugLogger.clear();
            statusLabel.setText("Logs cleared");
            shareLogsButton.setEnabled(false);
        });

        abortButton = new Button("Abort & Clear");
        abortButton.getAllStyles().setBgColor(0xAA0000);
        abortButton.getAllStyles().setFgColor(0xFFFFFF);
        abortButton.getAllStyles().setBgTransparency(200);
        abortButton.getAllStyles().setFont(Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL));
        abortButton.setEnabled(false);
        abortButton.addActionListener(e -> {
            service.abort();
            logArea.setText("");
            debugLogger.clear();
            statusLabel.setText("Aborted and logs cleared");
            shareLogsButton.setEnabled(false);
            resetUIState();
        });

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

        loadLastMacAddress();
        logArea.setText("Debug Mode Initialized.\nReady to start...\n");
    }

    private TextField createMacAddressField() {
        TextField field = new TextField();
        field.setHint(DEFAULT_MAC_HINT);
        Style style = field.getAllStyles();
        style.setFont(Font.createSystemFont(Font.FACE_MONOSPACE, Font.STYLE_BOLD, Font.SIZE_MEDIUM));
        style.setFgColor(0x00008B);
        field.addDataChangedListener((type, index) -> {
            String text = field.getText();
            if (text != null && !text.equals(text.toUpperCase())) {
                field.setText(text.toUpperCase());
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

    private void retrieveDeviceData() {
        String inputMac = macAddressField.getText();
        if (inputMac == null || inputMac.trim().isEmpty())
            return;

        Preferences.set(PREF_LAST_MAC_ADDRESS, inputMac);

        scanButton.setEnabled(false);
        scanButton.setIcon(FontImage.createMaterial(FontImage.MATERIAL_SYNC, scanButton.getUnselectedStyle()));
        abortButton.setEnabled(true);
        debugLogger.clear();

        new Thread(() -> {
            try {
                String jsonData = service.getDataFromDevice(inputMac);
                CN.callSerially(() -> {
                    updateLogDisplay();
                    Dialog.show("Success", "Data retrieved!", "OK", null);
                    showJsonDialog(jsonData);
                    resetUIState();
                });
            } catch (OmronBluetoothException ex) {
                CN.callSerially(() -> {
                    updateLogDisplay();
                    Dialog.show("Error", ex.getMessage(), "OK", null);
                    resetUIState();
                });
            } catch (Exception e) {
                CN.callSerially(() -> {
                    updateLogDisplay();
                    Dialog.show("Error", e.getMessage(), "OK", null);
                    resetUIState();
                });
            }
        }).start();

        // Background log update without blocking
        new Thread(() -> {
            while (!scanButton.isEnabled()) {
                CN.callSerially(() -> updateLogDisplay());
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                }
            }
        }).start();
    }

    private void updateLogDisplay() {
        List<String> logs = debugLogger.getLogs();
        StringBuilder sb = new StringBuilder();
        for (String log : logs) {
            sb.append(log).append("\n");
        }
        logArea.setText(sb.toString());
        if (sb.length() > 0)
            shareLogsButton.setEnabled(true);
    }

    private void shareLogs() {
        try {
            String path = FileSystemStorage.getInstance().getAppHomePath() + "omron_logs.txt";
            try (OutputStream os = FileSystemStorage.getInstance().openOutputStream(path)) {
                os.write(logArea.getText().getBytes("UTF-8"));
            }
            Display.getInstance().share(null, path, "text/plain");
        } catch (Exception e) {
            Dialog.show("Error", "Share failed", "OK", null);
        }
    }

    private void showJsonDialog(String json) {
        Form f = new Form("JSON Data", new BorderLayout());
        TextArea area = new TextArea(json);
        area.setEditable(false);
        f.add(BorderLayout.CENTER, area);
        Button closeBtn = new Button("Close");
        closeBtn.addActionListener(e -> f.showBack());
        f.add(BorderLayout.SOUTH, closeBtn);
        f.show();
    }

    private void resetUIState() {
        scanButton.setEnabled(true);
        scanButton.setIcon(null);
        abortButton.setEnabled(false);
    }
}
