# GEMINI.md - Project Context: omronapp

This project, `omronapp`, is a Codename One (CN1) mobile application developed in Java. Its primary goal is to establish reliable Bluetooth Low Energy (BLE) communication with OMRON blood pressure monitors to extract measurement data.

## Project Overview

*   **Framework:** [Codename One](https://www.codenameone.com/) (Cross-platform Java for iOS, Android, Desktop, Web).
*   **Build System:** Maven (using the Codename One Maven Plugin).
*   **Main Objective:** Isolate and solve Bluetooth communication issues (specifically timeouts) when connecting to OMRON devices (e.g., HEM-7144T2).
*   **Key Dependencies:**
    *   `CN1Bluetooth`: Custom Codename One library for BLE.
    *   `CN1JSON`: Custom Codename One library for JSON processing.

## Project Structure

*   `common/`: Contains the shared Java source code (UI and logic).
    *   `src/main/java/com/example/omronapp/`: Main application logic.
    *   `src/main/java/com/example/omronapp/omron/`: OMRON-specific Bluetooth services and data models.
*   `android/`, `ios/`, `javase/`, `win/`, `javascript/`: Platform-specific project modules and native implementation stubs.
*   `cn1libs/`: Local Codename One libraries used by the project.
*   `tools/`: IDE launch configurations (Eclipse/NetBeans).

## Building and Running

The project includes convenient shell scripts (`run.sh`, `build.sh`) and batch files (`run.bat`, `build.bat`).

### Running in Simulator
To run the app in the Codename One Simulator for rapid development:
```bash
./run.sh simulator
# or
./mvnw verify -Psimulator -DskipTests -Dcodename1.platform=javase
```

### Building for Mobile
Note: Building for iOS/Android typically requires the Codename One Build Server unless building from source locally.

*   **Android:** `./build.sh android`
*   **iOS (Debug):** `./build.sh ios`
*   **iOS (Source/Xcode):** `./build.sh ios_source` (Requires a Mac with Xcode)
*   **Desktop (JAR):** `./build.sh jar`

### Utility Commands
*   **Update CN1:** `./run.sh update`
*   **Open Settings:** `./run.sh settings`

## Development & Debugging Conventions

*   **Java Version:** Java 1.8 (strictly followed for Codename One compatibility).
*   **Debugging Strategy:** 
    *   A specialized debug version of the app is provided via `OmronBluetoothServiceDebug.java` and `OmronDebugForm.java`.
    *   These tools provide millisecond-precision logging for BLE events (connection, discovery, subscription, notifications).
    *   Consult `DEBUGGING_GUIDE.md` and `DEBUG_VERSION_GUIDE.md` for detailed troubleshooting procedures.
*   **BLE Profile:** The app attempts to use the standard Blood Pressure Service (UUID `00001810-0000-1000-8000-00805f9b34fb`) and Measurement Characteristic (UUID `00002a35-0000-1000-8000-00805f9b34fb`).
*   **Data Models:** `OmronDeviceData` and `OmronMeasurement` are the primary models for holding device info and blood pressure readings.

## Key Files for Reference
*   `common/src/main/java/com/example/omronapp/omron/OmronBluetoothService.java`: Main BLE logic.
*   `README.md`: High-level goals and alternative approach suggestions.
*   `DEBUGGING_GUIDE.md`: Troubleshooting steps for BLE timeouts.
*   `NRF_CONNECT_GUIDE.md`: Instructions for using the nRF Connect app to verify device behavior.
