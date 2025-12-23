package com.example.omronapp.omron;

/**
 * Parser for IEEE 11073-20601 Blood Pressure Measurement format.
 * 
 * SRP: Dedicated to protocol data conversion only.
 */
public class OmronProtocolParser {

    /**
     * Parses a blood pressure measurement from raw Bluetooth data.
     */
    public static OmronMeasurement parse(byte[] data) throws OmronBluetoothException {
        if (data == null || data.length < 7) {
            throw new OmronBluetoothException(OmronBluetoothException.ErrorType.INVALID_DATA,
                    "Insufficient data length: " + (data != null ? data.length : 0));
        }

        try {
            int systolic = parseSFloat(data, 1);
            int diastolic = parseSFloat(data, 3);
            int meanArterialPressure = parseSFloat(data, 5);

            long timestamp = System.currentTimeMillis();
            if (data.length >= 14) {
                timestamp = parseTimestamp(data, 7);
            }

            int heartRate = 0;
            if (data.length >= 16) {
                heartRate = parseSFloat(data, 14);
            }

            return new OmronMeasurement(systolic, diastolic, meanArterialPressure, heartRate, timestamp);
        } catch (Exception e) {
            throw new OmronBluetoothException(OmronBluetoothException.ErrorType.INVALID_DATA, e);
        }
    }

    private static int parseSFloat(byte[] data, int offset) {
        int value = ((data[offset + 1] & 0xFF) << 8) | (data[offset] & 0xFF);
        int mantissa = value & 0x0FFF;
        int exponent = value >> 12;

        if ((mantissa & 0x0800) != 0) {
            mantissa = -((~mantissa & 0x0FFF) + 1);
        }

        if ((exponent & 0x08) != 0) {
            exponent = -((~exponent & 0x0F) + 1);
        }

        return (int) (mantissa * powerOfTen(exponent));
    }

    private static double powerOfTen(int exponent) {
        if (exponent >= 0) {
            double result = 1.0;
            for (int i = 0; i < exponent; i++) {
                result *= 10.0;
            }
            return result;
        } else {
            double result = 1.0;
            for (int i = 0; i < -exponent; i++) {
                result /= 10.0;
            }
            return result;
        }
    }

    private static long parseTimestamp(byte[] data, int offset) {
        // Full implementation omitted for brevity, returns current time as placeholder
        return System.currentTimeMillis();
    }
}
