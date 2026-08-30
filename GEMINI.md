# Project Overview

This is a multi-platform application with a backend API, and Android and iOS clients. The main applications are "PitchPerfect" and "TagMaster".

## Backend

The backend is a Node.js application located in the `api` directory. It uses Express, TypeScript, and Google Cloud services (BigQuery and Pub/Sub).

### Building and Running the API

*   **Build:** `npm run build`
*   **Run:** `npm run start`
*   **Docker Build:** `npm run docker-build`
*   **Docker Run:** `npm run docker-run`

### CI/CD

The project has CI/CD pipelines for the API and for updating the GeoIP database. The workflows are located in the `.github/workflows` directory.

*   `api.yml`: This workflow builds a Docker image and deploys it to Google Cloud Run.
*   `geoip.yml`: This workflow is a scheduled job that runs weekly to update the GeoIP database.

## Mobile Clients

The project includes Android and iOS clients for the "PitchPerfect" and "TagMaster" applications.

### Android

The Android projects are located in the `Android` directory. They are built using Gradle. The main applications are `PitchPerfect` and `TagMaster`.

### iOS

The iOS projects are located in the `iOS` directory. They use Swift Package Manager. The main applications are `pitchperfect` and `tagmaster`; both use Firebase Auth, Firestore, and FirebaseUI. Pitch Perfect also uses Firebase Functions and Google Mobile Ads.

## Development Conventions

*   The backend is written in TypeScript.
*   The Android apps are written in Kotlin and Java.
*   The iOS apps are written in Swift and Objective-C.
*   The project uses Google Cloud services for the backend.
*   The project uses GitHub Actions for CI/CD.
