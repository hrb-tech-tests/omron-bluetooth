package com.example.omronapp.emronwebbluetooth;

import com.codename1.ui.*;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.RoundBorder;
import com.codename1.ui.plaf.Style;

public class OmronWebBluetoothForm extends Form {

    private final IwWebBrowseBluetooth webBrowser;
    private final Button capturedDataButton;
    private final Button abortButton;

    public OmronWebBluetoothForm() {
        super("IW OMRON Bluetooth v0.4.9", new BorderLayout());

        // Reduce title font size
        com.codename1.ui.Toolbar tb = getToolbar(); // Get the toolbar
        com.codename1.ui.Component titleComponent = tb.getTitleComponent(); // Get the component used for the title
        if (titleComponent instanceof com.codename1.ui.Label) {
            com.codename1.ui.Label titleLabel = (com.codename1.ui.Label)titleComponent;
            com.codename1.ui.plaf.Style labelStyle = titleLabel.getAllStyles(); // Get all styles for this specific label
            com.codename1.ui.Font currentFont = labelStyle.getFont();
            // Reduce font size by 2 points (adjust as needed)
            labelStyle.setFont(com.codename1.ui.Font.createSystemFont(currentFont.getFace(), currentFont.getStyle(), currentFont.getSize() - 2));
        }


        webBrowser = new IwWebBrowseBluetooth();
        add(BorderLayout.CENTER, webBrowser);

        // --- Create Buttons ---
        capturedDataButton = new Button("Capture"); // Corrected label
        abortButton = new Button("Abort");

        // --- Style Buttons ---
        styleButton(abortButton, 0xdc3545, false); // Red background for Abort
        styleEnabledButton(capturedDataButton); // Style for enabled state
        styleDisabledButton(capturedDataButton); // Style for disabled state

        // --- Button State ---
        capturedDataButton.setEnabled(false); // Disabled by default
        abortButton.setEnabled(true); // Always enabled

        // --- Button Listeners ---
        abortButton.addActionListener(e -> {
            webBrowser.abort();
            capturedDataButton.setEnabled(false); // Disable on abort
        });

        capturedDataButton.addActionListener(e -> {
            // On capture, just close the form. Data will be retrieved by the caller.
            showBack();
        });

        // Listener to enable/disable the "Capture" button based on data from JS
        webBrowser.setOnDataReadyListener(evt -> {
            String jsonData = (String) evt.getSource();
            boolean hasData = jsonData != null && !jsonData.isEmpty() && !jsonData.equals("null") && !jsonData.equals("[]");
            capturedDataButton.setEnabled(hasData);
        });

        // --- Add buttons to the NORTH container ---
        Container northContainer = new Container(new FlowLayout(Component.CENTER));
        northContainer.add(capturedDataButton);
        northContainer.add(abortButton);
        add(BorderLayout.NORTH, northContainer);
        setScrollableY(true);
    }

    /**
     * Retrieves the captured JSON data from the web component.
     * @return The JSON data string, or an empty string if not available.
     */
    public String getJsonData() {
        return webBrowser.getJsonData();
    }

    /**
     * Retrieves the logs from the web component.
     * @return The log string, or an empty string if not available.
     */
    public String getLogs() {
        return webBrowser.getLogs();
    }

    private void styleButton(Button button, int bgColor, boolean bold) {
        Style style = button.getAllStyles();
        style.setBgColor(bgColor);
        style.setFgColor(0xFFFFFF); // White text
        style.setBorder(RoundBorder.create().rectangle(true).color(bgColor).strokeColor(0));
        style.setPadding(2, 2, 1, 1);
        style.setMargin(1, 1, 10, 5);
        if (bold) {
            style.setFont(Font.createSystemFont(style.getFont().getFace(), Font.STYLE_BOLD, style.getFont().getSize()));
        }
    }

    private void styleEnabledButton(Button button) {
        Style style = button.getUnselectedStyle();
        style.setBgColor(0x007bff); // Blue background for enabled
        style.setFgColor(0xFFFFFF);
        style.setBorder(RoundBorder.create().rectangle(true).color(0x007bff).strokeColor(0));
        style.setPadding(2, 2, 1, 1);
        style.setMargin(1, 1, 10, 5);
    }

    private void styleDisabledButton(Button button) {
        Style style = button.getDisabledStyle();
        style.setBgColor(0xcccccc); // Gray background for disabled
        style.setFgColor(0x666666);
        style.setBorder(RoundBorder.create().rectangle(true).color(0xcccccc).strokeColor(0));
        style.setPadding(2, 2, 1, 1);
        style.setMargin(1, 1, 10, 5);
    }
}
