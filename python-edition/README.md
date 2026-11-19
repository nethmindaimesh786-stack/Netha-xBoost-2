# NETHA X PYTHON LAB

A fresh Android app edition built with Python and Kivy.

## What is included in version 0.1.0

- Quick Calc — safe basic arithmetic
- Text Forge — text case tools and word/character count
- Unit Shift — kilometres to miles and Celsius to Fahrenheit
- Focus Flow — 25-minute focus timer
- Color Studio — generate and copy HEX colors

The interface and application logic are written in Python. Buildozer and
python-for-android package the Kivy project as an Android APK.

This edition is separate from the original Kotlin/Compose application.
It does not copy the original app's modules or screens.

## Build

The Android project is configured in the buildozer.spec file.
On GitHub, open Actions and run "Build NETHA X Python Edition".
A successful run uploads a debug APK artifact.

For a local Linux build, install Buildozer and run:

    buildozer android debug

## Notes

- Offline-first: the current tools do not need an account or network access.
- Android build target: API 35, minimum API 24, ARM64.
- Package ID: com.nethax.nethaxpython.
