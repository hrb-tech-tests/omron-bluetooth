package com.example.omronapp.omron;

/**
 * Immutable data model representing a single blood pressure measurement
 * from an OMRON HEM-7144T2 device.
 * 
 * Follows Clean Code principles:
 * - Immutable (thread-safe)
 * - Single Responsibility (data container only)
 * - Clear naming
 */
public final class OmronMeasurement {

    private final int systolic;
    private final int diastolic;
    private final int meanArterialPressure;
    private final int heartRate;
    private final long timestamp;

    /**
     * Creates a new measurement record.
     * 
     * @param systolic             Systolic pressure in mmHg
     * @param diastolic            Diastolic pressure in mmHg
     * @param meanArterialPressure Mean arterial pressure in mmHg
     * @param heartRate            Heart rate in BPM
     * @param timestamp            Unix timestamp in milliseconds when measurement
     *                             was taken
     */
    public OmronMeasurement(int systolic, int diastolic, int meanArterialPressure,
            int heartRate, long timestamp) {
        this.systolic = systolic;
        this.diastolic = diastolic;
        this.meanArterialPressure = meanArterialPressure;
        this.heartRate = heartRate;
        this.timestamp = timestamp;
    }

    public int getSystolic() {
        return systolic;
    }

    public int getDiastolic() {
        return diastolic;
    }

    public int getMeanArterialPressure() {
        return meanArterialPressure;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Converts this measurement to a JSON object string.
     * 
     * @return JSON representation of the measurement
     */
    public String toJSON() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"systolic\":").append(systolic).append(",");
        json.append("\"diastolic\":").append(diastolic).append(",");
        json.append("\"meanArterialPressure\":").append(meanArterialPressure).append(",");
        json.append("\"heartRate\":").append(heartRate).append(",");
        json.append("\"timestamp\":").append(timestamp);
        json.append("}");
        return json.toString();
    }

    @Override
    public String toString() {
        return "BP: " + systolic + "/" + diastolic + " mmHg, HR: " + heartRate + " bpm";
    }
}
