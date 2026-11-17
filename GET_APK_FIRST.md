# Get an installable APK **without testing first**

**Current archive status:** Source only. The APK cannot be precompiled in the current environment because the Android SDK and Gradle distribution are unavailable. **There is no APK in this ZIP.** No tests or device installs were performed. Instead, this repository contains two independent ways to compile the APK immediately. A successful build is not a guarantee the app runs correctly.

## Option A: GitHub builds the APK automatically (no local Android setup)

1. Upload **all files inside `SignalScope/`**, including the hidden `.github` directory and `.gitignore`, to your existing repository: `https://github.com/sinatracompany/Radar`. Use `main`.
2. Open the **Actions** tab in GitHub.
3. Select **Build SignalScope APK (no tests)**. A push to `main` triggers it automatically; use **Run workflow** to rerun manually.
4. When the run completes successfully, click the run, scroll to **Artifacts** and download `SignalScope-v0.1.0-debug-APK`.
5. Extract the downloaded artifact ZIP. It contains `SignalScope-v0.1.0-debug.apk` and `SHA256SUMS.txt`. Install APK on your own Android device when ready.

If the build fails, the job's logs show the precise compiler error. Correct it and push again. Do not treat a failed run as if an APK exists.

GitHub keeps Actions artifacts only for a limited time (configured here for 30 days). The debug APK is signed automatically by the Android build system for development. It is not a production/Play Store release.

## Option B: Compile on your Windows PC (no testing)

1. Install Java JDK 17+, Android SDK API 35 and Android SDK Build Tools 35.0.0. Android Studio can install these packages. Gradle 8.11.1 is downloaded by the helper if needed.
2. In the extracted `SignalScope` folder, double-click `Build-APK-NoTests.cmd`.
3. After a successful build, look for `release\SignalScope-v0.1.0-debug.apk`.

The script runs `assembleDebug` only. It deliberately **does not run unit tests or physical-device testing**.

## What this first app contains

The source implements Wi-Fi network observations, BLE observations, radar-style visualization, an in-session RSSI graph, and a simple local watchlist. It does not yet provide the entire SØPHIA-style roadmap (cellular analysis, persistent observation database, export, classifications, etc.). Its behavior on your phone is unverified.

Keep `*.apk` out of Git source control; share the APK as a **GitHub Actions artifact** or publish it as a release asset later. Do not assume all users will trust or can update an APK signed with a CI-generated debug key, because separate build machines may use different debug signing certificates.
