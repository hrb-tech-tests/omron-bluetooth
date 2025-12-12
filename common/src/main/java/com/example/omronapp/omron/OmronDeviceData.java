package com.example.omronapp.omron;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable wrapper for batch data retrieved from an OMRON device.
 * Contains device metadata and all measurements.
 * 
 * Follows Clean Code principles:
 * - Immutable (defensive copy of list)
 * - Single Responsibility
 * - Interface Segregation (focused public API)
 */
public final class OmronDeviceData {

    private final String deviceMac;
    private final String deviceModel;
    private final long retrievalTimestamp;
    private final List<OmronMeasurement> measurements;

    /**
     * Creates a new device data container.
     * 
     * @param deviceMac    MAC address of the OMRON device
     * @param deviceModel  Model identifier (e.g., "HEM-7144T2")
     * @param measurements List of measurements (will be copied defensively)
     */
    public OmronDeviceData(String deviceMac, String deviceModel, List<OmronMeasurement> measurements) {
        this.deviceMac = deviceMac;
        this.deviceModel = deviceModel;
        this.retrievalTimestamp = System.currentTimeMillis();
        // Defensive copy to ensure immutability
        this.measurements = new ArrayList<>(measurements);
    }

    public String getDeviceMac() {
        return deviceMac;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public long getRetrievalTimestamp() {
        return retrievalTimestamp;
    }

    /**
     * Returns an unmodifiable view of the measurements.
     * 
     * @return Immutable list of measurements
     */
    public List<OmronMeasurement> getMeasurements() {
        return Collections.unmodifiableList(measurements);
    }

    /**
     * Converts the entire device data to a JSON string.
     * This is the primary output format for integration.
     * 
     * @return Valid JSON string containing all device data and measurements
     */
    public String toJSON() {
        StringBuilder json = new StringBuilder();
        json.append("{");

        // Device metadata
        json.append("\"deviceMac\":\"").append(deviceMac).append("\",");
        json.append("\"deviceModel\":\"").append(deviceModel).append("\",");
        json.append("\"retrievalTimestamp\":").append(retrievalTimestamp).append(",");
        json.append("\"measurementCount\":").append(measurements.size()).append(",");

        // Measurements array
        json.append("\"measurements\":[");
        for (int i = 0; i < measurements.size(); i++) {
            json.append(measurements.get(i).toJSON());
            if (i < measurements.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");

        json.append("}");
        return json.toString();
    }

    @Override
    public String toString() {
        return "OMRON " + deviceModel + " [" + deviceMac + "] - " + measurements.size() + " measurements";
    }
}
