# Paste into VS Code Codex after extracting the project into D:\SignalScope

You are developing SignalScope v0.1.0, package `com.signalscope.app`, using the existing repository https://github.com/sinatracompany/Radar.git.

Owner direction: **Create an initial installable APK NOW, without waiting for tests.** This first build may have bugs. Do not make the APK conditional on automated or physical-device tests. The GitHub Actions workflow in `.github/workflows/android.yml` deliberately runs `assembleDebug` only and uploads the APK as a downloadable workflow artifact. `Build-APK-NoTests.cmd` is the local Windows equivalent.

Begin by reading README.md, GET_APK_FIRST.md, AGENTS.md, and the existing Kotlin source. Inspect `git status -sb` and `git remote -v`; preserve user changes and do not recreate or overwrite an existing repository. Inspect JDK, Android SDK, Gradle and ADB. Run the APK build first with `assembleDebug`, fix **compilation** failures, and produce `release/SignalScope-v0.1.0-debug.apk`. Do not claim an APK exists until it actually exists, and do not claim it has been tested.

After the initial APK is produced, continue iteratively on Wi-Fi scanning, BLE scanning, radar, signal-strength tracking, permission handling, and performance. Later roadmap items include supported cellular information, Room persistence, offline manufacturer lookup, watchlist alerts, and export. Android OS rate limits and device support must be respected; never fake detections, direction, distance, or capabilities. Keep data local by default.

Do not modify YourIPTV or other apps. Do not add subscriptions or paywalls. Never force-push, and do not upload signing keys. Return exact APK path and whether the build actually succeeded. Physical tests can remain pending at the owner's request.
