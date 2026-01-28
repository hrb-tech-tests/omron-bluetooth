# GEMINI.md - Project Context: Omron Bluetooth Flutter

This project is a Flutter application for connecting to and reading data from OMRON Bluetooth blood pressure monitors. It is a from-scratch rewrite of a previous Codename One project, with the goal of creating a more stable and maintainable solution using modern Flutter practices.

## Project Overview

*   **Framework:** Flutter
*   **Language:** Dart
*   **State Management:** Riverpod
*   **Bluetooth:** flutter_blue_plus
*   **Architecture:** Clean Architecture (Data, Domain, Presentation)

## Project Structure

*   `lib/`: Main application code.
    *   `data/`: Data sources and repositories.
        *   `services/`: Bluetooth service implementation.
    *   `domain/`: Business logic and entities.
        *   `entities/`: Data models for the application.
        *   `repositories/`: Abstract repository interfaces.
    *   `presentation/`: UI and state management.
        *   `providers/`: Riverpod providers.
        *   `screens/`: UI screens.
        *   `widgets/`: Reusable UI widgets.
*   `test/`: Unit and widget tests.
*   `pubspec.yaml`: Project dependencies.
*   `GEMINI.md`: This file.

## Getting Started

1.  Make sure you have Flutter installed.
2.  Run `flutter pub get` to install dependencies.
3.  Run `flutter run` to start the application.

## Your roles
You must be a senior Software Engineer expert in Flutter using the best practices and standards.
You must document your code using DartDoc.
You must use the 5 principles of SOLID design. Readability and easy testability are also important.
