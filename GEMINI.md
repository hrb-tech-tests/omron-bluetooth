# GEMINI.md - Project Context: omronapp

This project, `omronapp`, is a Codename One (CN1) mobile application developed in Java. Its primary goal is to establish reliable Bluetooth Low Energy (BLE) communication with OMRON blood pressure monitors to extract measurement data.

## Your roles
You must be a senior Software Engineer expert in Code Name One Platform using the Code Name One java-to-javascrit/javascript-to-java bridges to access this project.
You must write code using the Code Name One best practices and standards.
You must document your code using JavaDoc.
You must use the 5 principles of SOLID design. Readability and easy testability are also important. 

## Project Overview

*   **Framework:** [Codename One](https://www.codenameone.com/) (Cross-platform Java for iOS, Android, Desktop, Web).
*   **Build System:** Maven (using the Codename One Maven Plugin).
*   **Approach:** The project uses a hybrid approach, combining native Codename One Java with a native web component.
    *   The web component loads an HTML page that uses AngularJS for two-way data binding of form components to a JSON model.
    *   A Codename One bridge library enables communication between the JavaScript running inside the web component and the main Java application code.
*   **Main Objective:** To create a proof-of-concept for BLE communication with OMRON devices (e.g., HEM-7144T2). The ultimate goal is to capture all device data (signal health, blood pressure, heart rate) and return it as a structured JSON object.
*   **History:** A previous approach using a direct `CN1Bluetooth` library was unsuccessful due to persistent timeout issues. This hybrid model is a complete rewrite to achieve the same goal through a different strategy.
*   **Key Dependencies:**
    *   `CN1JSON`: For JSON processing in Java.
    *   AngularJS: Used within the web component using two way data binding.

## Project Structure

*   `common/`: Contains the shared Java source code and web component assets.
    *   `src/main/java/com/example/omronapp/`: Main application logic.
    *   `src/main/java/com/example/omronapp/emronwebbluetooth/`: Contains the Java code for the web-based BLE interaction, including the form (`OmronWebBluetoothForm.java`) and the bridge (`IwWebBrowseBluetooth.java`).
    *   `src/main/resources/`: Contains the `index.html` and `angular.min.js` files used by the web component. these two files also should be replicated in the @common/src/main/java/com/example/omronapp/ folder to be compliance with a weird feature of Code Name One platform that only can access resources from the same folder as the main Java class (OmronApp.java). 
*   `android/`, `ios/`, `javase/`, `win/`, `javascript/`: Platform-specific project modules.
*   `cn1libs/`: Local Codename One libraries. Note that `CN1Bluetooth` is part of the old, unsuccessful approach but may still be present in the directory.

## Building and Running

The project includes standard Codename One build and run scripts.

### Running in Simulator
To run the app in the Codename One Simulator for rapid development:
```bash
./run.sh simulator
```

### Building for Mobile
*   **Android:** `./build.sh android`
*   **iOS (Debug):** `./build.sh ios`
*   **Desktop (JAR):** `./build.sh jar`

## Development & Key Files

*   **Main Hybrid Logic:** The core of the new hybrid approach is implemented across these files:
    *   `common/src/main/java/com/example/omronapp/emronwebbluetooth/OmronWebBluetoothForm.java`: The Codename One form that hosts the `BrowserComponent`.
    *   `common/src/main/java/com/example/omronapp/emronwebbluetooth/IwWebBrowseBluetooth.java`: Code Name One WebComponent already configured to use the Code Name One bridges between Java and JavaScript.
    *   `common/src/main/resources/index.html`: The HTML page with AngularJS that handles all the BLE communication in the browser context.
*   **Old Approach (Deprecated):** The files related to the previous, direct BLE implementation (like `OmronBluetoothService.java` and `OmronDebugForm.java`) are being phased out but can be consulted for reference, particularly regarding the desired JSON data structure for the final reposnse.
*   **All architecture parts:** Code Name One Java code, web component with its assets (browser native bluetooth api + angularJS lib), and the Code Name One javascript-to-Java/Java-to-javascript bridges should work together to achieve the desired result (A pretty JSON structured string with all the measures stored into the devices's memory (OMRON HLEM)  
*   **Codename One Libraries:** `CN1Bluetooth` isn't used in this new hybrid approach. Only `CN1JSON` is used for JSON processing.
*   ** Always compiles the code after finished any modifications to grant the latest changes can be compiled. The testing in the real device should be done manually such as bluetooth communications isn't work in simulator.

