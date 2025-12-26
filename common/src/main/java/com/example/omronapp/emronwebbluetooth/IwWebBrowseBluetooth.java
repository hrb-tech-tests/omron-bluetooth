package com.example.omronapp.emronwebbluetooth;

import com.codename1.ui.BrowserComponent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.ActionEvent; 

public class IwWebBrowseBluetooth extends BrowserComponent {

    private ActionListener<ActionEvent> loadCompletedListener;
    private ActionListener<ActionEvent> onDataReadyListener;


    public IwWebBrowseBluetooth() {
        super();
        this.setURL("/emron-web-bluetooth/index.html");
        // Removed setJavascriptMode(BrowserComponent.JAVASCRIPT_MODE_JAVASCRIPT_LIGHT);
        // It seems unnecessary or was used with an invalid constant.

        addWebEventListener(BrowserComponent.onLoad, (evt) -> {
            if (loadCompletedListener != null) {
                loadCompletedListener.actionPerformed(null);
            }
        });
        
        // Add callback for when data is ready in JS
        addJSCallback("onDataReady", (res) -> {
            if (onDataReadyListener != null) {
                // Fixed: Use ActionEvent.Type.Other for custom event types.
                onDataReadyListener.actionPerformed(new ActionEvent(res.getValue(), ActionEvent.Type.Other));
            }
        });
    }

    /**
     * Executes JavaScript to get the JSON data from the AngularJS scope and returns it.
     * @return A JSON string with the captured data, or an empty string if not ready.
     */
    public String getJsonData() {
        // This relies on the getAngularScope() and getJsonData() functions defined in index.html
        String script = "return getAngularScope().getJsonData();";
        JSRef jsRef = executeAndWait(script); 
        return (jsRef != null) ? jsRef.getValue() : ""; 
    }

    /**
     * Executes the abort() function in the Javascript scope to disconnect the device and clear the UI.
     */
    public void abort() {
        // This calls the abort function in the AngularJS controller
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
