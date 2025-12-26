# Omron Bluetooth Communication App

This project is a Codename One application designed to connect to OMRON blood pressure monitors (e.g., HEM-7144T2) via Bluetooth Low Energy (BLE) and extract measurement data.

## Project Goal

The primary objective of this project is to establish reliable BLE communication and resolve persistent timeout errors that occur after the initial connection is made. The pragmatic goal is to successfully extract all measurement data stored in the device's memory.

## Current Status

The application is able to connect to the OMRON device, but it consistently fails with a timeout when attempting to discover services or read data. This project serves as a minimal, reproducible example of the issue.

## Building and Running

The project includes scripts for common tasks.

### Running in Simulator

To run the app in the Codename One Simulator for rapid development:

```bash
./run.sh simulator
```

### Building for Devices

To build the application for a specific platform:

- **Android:** `./build.sh android`
- **iOS (Debug Build):** `./build.sh ios`
- **iOS (Xcode Project):** `./build.sh ios_source`
- **Desktop (Runnable JAR):** `./build.sh jar`

## Debugging

This project is configured for in-depth BLE debugging.

- **Debugging Guides:** For detailed instructions on troubleshooting, please refer to:
    - `DEBUGGING_GUIDE.md`: Steps for troubleshooting BLE timeouts.
    - `DEBUG_VERSION_GUIDE.md`: How to use the specialized debug version of the app.
    - `NRF_CONNECT_GUIDE.md`: Instructions for using the nRF Connect app to independently verify device behavior.

- **Debug Code:** The `common` module contains a specialized debug UI (`OmronDebugForm.java`) that provides millisecond-precision logging for all BLE events.

## Project Structure

- `common/`: Shared Java source code for UI and business logic. The core BLE communication logic is in `omron/OmronBluetoothService.java`.
- `cn1libs/`: Local Codename One libraries (`CN1Bluetooth`, `CN1JSON`).
- `android/`, `ios/`, `javase/`, etc.: Platform-specific native code and project modules.
- `tools/`: IDE launch configurations for Eclipse and NetBeans.
- `docs/`: Contains logs and data captured from external debugging tools like nRF Connect.