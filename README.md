# NETHA X ULTIMATE — Native Android Edition

This ZIP converts the original WebView-based dashboard into a native Android app using **Kotlin + Jetpack Compose**. The HTML files are kept only as reference copies in `reference-html/`; they are not included in the Android app and are not loaded at runtime.

## Native modules

- **Dashboard:** Native Compose interface with the original dark purple/cyan visual direction and press animations.
- **AI Background Remover:** Native image picker, multipart upload to the existing Poof endpoint, result preview, and Gallery save. A valid Poof API key is required.
- **Translation Camera:** Native CameraX preview, on-device ML Kit text recognition and language identification, and Sinhala translation using the same MyMemory endpoint.
- **Mobile Optimizer:** Safely clears temporary cache files owned by NETHA X. Android does not allow ordinary apps to silently clear other apps' private caches, so this native version does not request broad all-files access or claim that it can clean other apps.

## Build with GitHub Actions

1. Upload the contents of this ZIP to your GitHub repository (the folder containing this README should be the repository root).
2. Open **Actions** in the repository.
3. Select **Build NETHA X ULTIMATE Native APK** and press **Run workflow**, or push to `main` / `master`.
4. Open the completed workflow run and download the `netha-x-ultimate-native-debug` artifact. The APK inside is `app-debug.apk`.

The workflow uses JDK 17, Gradle 8.11.1, and Android Gradle Plugin 8.10.1.

## Poof API key

The original project contained only the placeholder `PASTE_YOUR_NEW_POOF_API_KEY_HERE`; no real key was present in the ZIP. The Background Remover therefore shows a setup message until a valid key is configured.

For GitHub Actions, add a repository secret named `POOF_API_KEY` under **Settings → Secrets and variables → Actions**. The workflow passes it to Gradle as an environment variable. For a local build, set the `POOF_API_KEY` environment variable or add `poofApiKey=YOUR_KEY` to your uncommitted `local.properties` file.

**Security note:** A key compiled into a client APK can be extracted. For a public production app, use a small authenticated backend to keep the Poof key on the server instead of embedding it in the APK.

## Notes

- Package name remains `com.nethax.ultimate`.
- Version is now `2.0-native` (versionCode 2).
- Minimum Android version remains Android 6.0 (API 23).
- Internet permission is used by Background Remover and online translation. Camera permission is requested only when Translation Camera is started.
- `reference-html/` contains the original HTML files for reference only. There is no WebView dependency or WebView usage in the native app.
