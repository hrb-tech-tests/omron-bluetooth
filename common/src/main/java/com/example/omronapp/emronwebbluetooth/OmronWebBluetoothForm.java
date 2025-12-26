package com.example.omronapp.emronwebbluetooth;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.RoundBorder;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.Font; // Import Font

public class OmronWebBluetoothForm extends Form {

    private IwWebBrowseBluetooth webBrowser;
    private Button getCapturedDataButton;
    private Button abortButton;

    public OmronWebBluetoothForm() {
        super("OMRON Web Bluetooth", new BorderLayout());

        webBrowser = new IwWebBrowseBluetooth();
        add(BorderLayout.CENTER, webBrowser);

        // --- Create Buttons ---
        getCapturedDataButton = new Button("Get Captured Data");
        abortButton = new Button("ABORT");

        // --- Style Buttons ---
        styleButton(getCapturedDataButton, 0x007bff, true); // Blue background
        styleButton(abortButton, 0xdc3545, true); // Red background
        
        // --- Button State ---
        getCapturedDataButton.setEnabled(false); // Disabled by default

        // --- Button Listeners ---
        abortButton.addActionListener(e -> {
            webBrowser.abort();
            getCapturedDataButton.setEnabled(false);
        });

        getCapturedDataButton.addActionListener(e -> {
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
            getCapturedDataButton.setEnabled(hasData);
        });


        // --- Add buttons to the SOUTH container ---
        Container southContainer = new Container(new BoxLayout(BoxLayout.X_AXIS));
        southContainer.add(getCapturedDataButton);
        southContainer.add(abortButton);
        add(BorderLayout.SOUTH, southContainer);
    }
    
    private void styleButton(Button button, int bgColor, boolean bold) {
        Style style = button.getAllStyles();
        style.setBgColor(bgColor);
        style.setFgColor(0xFFFFFF); // White text
        style.setBorder(RoundBorder.create().rectangle(true).color(bgColor).strokeColor(0));
        style.setPadding(2, 2, 10, 10);
        style.setMargin(1, 1, 5, 5);
        if (bold) {
            // Use Font.createSystemFont with explicit parameters to create a bold font.
            // This bypasses any issues with the derive method overloads.
            style.setFont(Font.createSystemFont(style.getFont().getFace(), Font.STYLE_BOLD, style.getFont().getSize()));
        }
    }
}
