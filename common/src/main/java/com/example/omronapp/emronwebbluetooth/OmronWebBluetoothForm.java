package com.example.omronapp.emronwebbluetooth;

import com.codename1.ui.*;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.RoundBorder;
import com.codename1.ui.plaf.Style;

public class OmronWebBluetoothForm extends Form {

    private final IwWebBrowseBluetooth webBrowser;
    private final Button capturedDataButton;
    private final Button abortButton;

    public OmronWebBluetoothForm() {
        super("IW OMRON Bluetooth", new BorderLayout());

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
        styleButton(capturedDataButton, 0x007bff, false); // Blue background
        styleButton(abortButton, 0xdc3545, false); // Red background
        
        // --- Button State ---
        capturedDataButton.setEnabled(false); // Disabled by default

        // --- Button Listeners ---
        abortButton.addActionListener(e -> {
            webBrowser.abort();
            capturedDataButton.setEnabled(false);
        });

        capturedDataButton.addActionListener(e -> {
            String jsonData = webBrowser.getJsonData();
            if (jsonData != null && !jsonData.isEmpty() && !jsonData.equals("null") && !jsonData.equals("[]")) {
                Dialog.show("Captured Data", jsonData, "OK", null);
            } else {
                Dialog.show("No Data", "No data has been captured from the device.", "OK", null);
            }
        });
        
        // Listener to enable the "Get Captured Data" button when data is ready from JS
        webBrowser.setOnDataReadyListener(evt -> {
            // evt.getSource() will be the JSON data string from JS
            String jsonData = (String)evt.getSource();
            boolean hasData = jsonData != null && !jsonData.isEmpty() && !jsonData.equals("null") && !jsonData.equals("[]");
            capturedDataButton.setEnabled(hasData);
        });


        // --- Add buttons to the SOUTH container ---
        Container southContainer = new Container(new GridLayout(1, 4));
        southContainer.add(new Label());
        southContainer.add(capturedDataButton);
        southContainer.add(abortButton);
        southContainer.add(new Label());
        add(BorderLayout.SOUTH, southContainer);
    }
    
    private void styleButton(Button button, int bgColor, boolean bold) {
        Style style = button.getAllStyles();
        style.setBgColor(bgColor);
        style.setFgColor(0xFFFFFF); // White text
        style.setBorder(RoundBorder.create().rectangle(true).color(bgColor).strokeColor(0));
        style.setPadding(2, 2, 1, 1);
        style.setMargin(1, 1, 10, 5);
        if (bold) {
            // Use Font.createSystemFont with explicit parameters to create a bold font.
            // This bypasses any issues with the derive method overloads.
            style.setFont(Font.createSystemFont(style.getFont().getFace(), Font.STYLE_PLAIN, style.getFont().getSize()));
        }
    }
}
