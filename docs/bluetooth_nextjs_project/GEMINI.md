# GEMINI.md - Project Context: Omron Bluetooth Next.js PWA

This project is a Next.js Progressive Web App (PWA) for connecting to and reading data from OMRON Bluetooth blood pressure monitors. It is a from-scratch rewrite of previous native and hybrid projects, with the goal of creating a cross-platform solution using web technologies.

## Project Overview

*   **Framework:** Next.js
*   **Language:** TypeScript
*   **Styling:** Tailwind CSS
*   **State Management:** Zustand
*   **Bluetooth:** Web Bluetooth API
*   **PWA:** next-pwa

## Project Structure

*   `src/`: Main application code.
    *   `app/`: App router pages and layouts.
    *   `components/`: Reusable React components.
    *   `services/`: Bluetooth service implementation.
    *   `store/`: Zustand state management store.
*   `public/`: Static assets, including the PWA manifest and icons.
*   `next.config.ts`: Next.js configuration, including PWA settings.
*   `GEMINI.md`: This file.

## Getting Started

1.  Make sure you have Node.js and npm installed.
2.  Run `npm install` to install dependencies.
3.  Run `npm run dev` to start the development server.

## Your roles
You must be a senior Software Engineer expert in Next.js with TypeScript using the best practices and standards.
You must document your code using JSDoc.
You must use the 5 principles of SOLID design. Readability and easy testability are also important.
