package com.example.omronapp.emronwebbluetooth;

import com.codename1.io.Util;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.ActionEvent;
import java.io.IOException;
import java.io.InputStream;

public class IwWebBrowseBluetooth extends BrowserComponent {

    private ActionListener<ActionEvent> loadCompletedListener;
    private ActionListener<ActionEvent> onDataReadyListener;

    public IwWebBrowseBluetooth() {
        super();
        
        try {
            // Load angular.min.js content
            InputStream angularJsStream = getClass().getResourceAsStream( "/angular.min.js");  //  getClass().getResourceAsStream("angular.min.js");
            if (angularJsStream == null) {
                throw new IOException("Resource 111122223 not found: angular.min.js");
            }
            String angularJsContent = Util.readToString(angularJsStream);

            // Load index.html content
            InputStream indexHtmlStream = Display.getInstance().getResourceAsStream(null, "/index.html");
            if (indexHtmlStream == null) {
                throw new IOException("Resource 2222333334 not found: index.html");
            }
            String indexHtmlContent = Util.readToString(indexHtmlStream);

            // Replace the script tag with the inlined content of angular.min.js
            String finalHtml = indexHtmlContent.replace(
                "<script src=\"angular.min.js\"></script>",
                "<script>" + angularJsContent + "</script>"
            );

            // Load the final self-contained HTML. Base URL can be null as there are no more external resources.
            this.setPage(finalHtml, null);

        } catch (IOException e) {
            e.printStackTrace();
            // Display an error message inside the component if loading fails.
            this.setPage("<html><body><h1>Error</h1><p>Could not load resources: " + e.getMessage() + "</p></body></html>", null);
        }

        addWebEventListener(BrowserComponent.onLoad, (evt) -> {
            if (loadCompletedListener != null) {
                loadCompletedListener.actionPerformed(null);
            }
        });
        
        // Add callback for when data is ready in JS
        addJSCallback("onDataReady", (res) -> {
            if (onDataReadyListener != null) {
                onDataReadyListener.actionPerformed(new ActionEvent(res.getValue(), ActionEvent.Type.Other));
            }
        });
    }

    /**
     * Executes JavaScript to get the JSON data from the AngularJS scope and returns it.
     * @return A JSON string with the captured data, or an empty string if not ready.
     */
    public String getJsonData() {
        String script = "return getAngularScope().getJsonData();";
        JSRef jsRef = executeAndWait(script); 
        return (jsRef != null) ? jsRef.getValue() : ""; 
    }

    /**
     * Executes the abort() function in the Javascript scope to disconnect the device and clear the UI.
     */
    public void abort() {
        execute("getAngularScope().abort();");
    }
    
    /**
     * Sets a listener that will be invoked when the page is fully loaded.
     * @param listener The listener to be invoked.
     */
    public void setLoadCompletedListener(ActionListener<ActionEvent> listener) {
        this.loadCompletedListener = listener;
    }

    /**
     * Sets a listener that will be invoked when new data is ready in the Javascript side.
     * @param listener The listener to be invoked. The ActionEvent's source will be the JSON data string.
     */
    public void setOnDataReadyListener(ActionListener<ActionEvent> listener) {
        this.onDataReadyListener = listener;
    }
}
