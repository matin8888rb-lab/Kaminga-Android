# Kaminga Android

A fresh native Android project for Kaminga, built with Kotlin and Jetpack Compose.

## Current milestone
- Native Android app shell with a dark purple/cyan design.
- Demo home screen, comic details, genre filtering, local library saves, and reader layout toggle.
- GitHub Actions workflow intended to build and upload a debug APK on pushes to `main`.

## Important status
The comics and reader are **demo content**. Real licensed comic files, account registration, server-backed storage, SMS verification, offline downloads, and creator publishing are not implemented yet. The workflow must pass before the APK is considered verified.

## Build
Run `gradle assembleDebug` with JDK 17 and Gradle 8.9, or use GitHub Actions.

Package: `com.kaminga.reader`
