package com.example.omronapp.omron;

/**
 * Custom exception for OMRON Bluetooth operations.
 * Provides clear error messages for different failure scenarios.
 */
public class OmronBluetoothException extends Exception {

    public enum ErrorType {
        DEVICE_NOT_FOUND("Device not found or not in range"),
        CONNECTION_FAILED("Failed to establish Bluetooth connection"),
        AUTHENTICATION_FAILED("Device authentication failed"),
        DATA_TRANSFER_FAILED("Failed to retrieve data from device"),
        INVALID_DATA("Received invalid or corrupted data"),
        TIMEOUT("Operation timed out"),
        BLUETOOTH_DISABLED("Bluetooth is not enabled"),
        PERMISSION_DENIED("Bluetooth permission denied"),
        ABORTED("Operation aborted by user");

        private final String description;

        ErrorType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private final ErrorType errorType;

    public OmronBluetoothException(ErrorType errorType) {
        super(errorType.getDescription());
        this.errorType = errorType;
    }

    public OmronBluetoothException(ErrorType errorType, String additionalInfo) {
        super(errorType.getDescription() + ": " + additionalInfo);
        this.errorType = errorType;
    }

    public OmronBluetoothException(ErrorType errorType, Throwable cause) {
        super(errorType.getDescription(), cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
