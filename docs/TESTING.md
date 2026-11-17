# Testing and validation status — 2026-11-17

- Project and source created in isolated assistant environment.
- No Android SDK, Gradle, ADB, network package downloads or attached physical Android phone were available there.
- **No APK has been built or tested yet.** All Android runtime behavior requires verification.
- `SignalMath` (pure Kotlin) can be checked locally; Gradle/JUnit tests require Android SDK and dependencies.
- GitHub remote `sinatracompany/Radar` was publicly visible as empty when checked on 2026-11-17. Nothing was pushed.

## Physical test protocol

1. `adb devices -l` and capture actual test device serial and OS.
2. `gradle testDebugUnitTest assembleDebug` (or `./Build-Windows.ps1`).
3. Install APK; confirm package `com.signalscope.app`.
4. Test denied/granted Wi-Fi and BLE permissions separately, system Location on/off.
5. Compare Wi-Fi scan age to the timestamp shown; test refresh throttling.
6. Confirm BLE updates occur when a nearby BLE broadcaster is on; then test after stopping it.
7. Verify no false bearing/range claims; radar dot tapping works for a known signal.
8. Toggle watchlist and restart app to ensure stored watch IDs persist.
9. Rotate phone, background/foreground repeatedly, and inspect Logcat for crashes or extra scanning.
10. Test battery usage, memory growth, scan performance, long sessions and Bluetooth disabled state.
11. Update this document with exact pass/fail results. Do not ship before test results are recorded.
