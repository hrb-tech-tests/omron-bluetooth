# Omron Bluetooth Communication App

This project is a Codename One application designed to connect to OMRON blood pressure monitors (e.g., HEM-7144T2 model) via Bluetooth Low Energy (BLE) and extract measurement data.

## Project Goal

The project is a proof-of-concept for BLE communication with OMRON devices.
From tag v0.3.0, using a new approach from scratch. A hybrid approach, it uses pure Code Name One java integrated with a native Code Nme One webComponent. 
The native browser component loads a html page that has angularJS to make all two way binding of all HTML form components and its respective values into the json model.
Codenameone also has a bridge lib to make the communication between the two worlds: JavaScript inside the webComponent and the pure java Code Name One code.  
In this HYBRID approach, the 3 parts: java code, WebComponent (html page + angulaJS lib) and the Codenameone-Java-javascript-bridges works together to reach the final goal.
capture all data from the device. As the final response, this ṕroject will return a json object with all the data from the device. (signal health, blood pressure, heart rate).
You can use the previous backed tag v0.3.0 to see the old approach and make this new approach. reuse all you consider useful. JSON structure of the resposnse, and so on. 
Is it important to mention that the first approach, that uses directly a cn1lib bluetooth lib never worked, so we decided to try a new complete approach to reach the same gol.

The pragmatic goal is to successfully extract all measurement data stored in the device's memory and return it as a JSON object or its respective string representation.

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

## Project Structure

- `common/`: Shared Java source code for UI and business logic. The core BLE communication logic is in `omron/OmronBluetoothService.java`.
- `cn1libs/`: Local Codename One libraries (`CN1Bluetooth`, `CN1JSON`).
- `android/`, `ios/`, `javase/`, etc.: Platform-specific native code and project modules.
- `tools/`: IDE launch configurations for Eclipse and NetBeans.
- `docs/`: Contains logs and data captured from external debugging tools like nRF Connect.