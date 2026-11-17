# SignalScope — Advanced Wireless Radar (initial Android source)

**Official destination:** https://github.com/sinatracompany/Radar.git  
**Working directory:** `D:\SignalScope`  
**Version:** `0.1.0`  
**APK status:** Not yet compiled. The included GitHub Actions workflow will **compile an APK on your push without running tests**.

> **Start here:** [GET_APK_FIRST.md](GET_APK_FIRST.md)

This is an independent Android application for observing nearby Wi-Fi and BLE broadcasts using the phone's internal hardware. It is inspired by the category of wireless radar tools, but does not copy SØPHIA CIVOPS code or assets.

The Phase 1 Kotlin/Jetpack Compose source includes:

- Wi-Fi scan-result observations (SSID, BSSID, RSSI, frequency, metadata and freshness)
- BLE broadcast observations (when available), with de-duplication
- A conceptual radar display: **positions do not show measured bearing or range**
- In-session RSSI graph and device detail panel
- Local persistent watchlist identifiers
- Permission handling, foreground-only scanning, status information

**Not included yet:** Cellular scanner, manufacturer database, automatic classification, persistent session history, alerts, export/reports, and a Play Store-ready signed release. Unverified source should not be described as a tested application.

### Getting the APK without testing

**Easiest route:** Upload the source to your existing GitHub `main` branch, then in **Actions → Build SignalScope APK (no tests) → latest run → Artifacts** download the APK ZIP. See [GET_APK_FIRST.md](GET_APK_FIRST.md).

**Windows route:** With an Android SDK (API 35) and JDK 17+ installed, run `Build-APK-NoTests.cmd`. The resulting APK will appear under `release/` if compilation succeeds.

### Privacy and technical limits

No `INTERNET` permission, required account, tracking SDK, or telemetry. The source records scan data locally. Modern Android limits Wi-Fi scan frequency, and Bluetooth devices may randomize their addresses. Neither RSSI nor this radar indicates exact device direction or distance. No cellular interception or universal RF spectrum scanning is possible through ordinary Android APIs.

### Source repository upload

Do not upload an enclosing `SignalScope` folder as a single folder inside the repository. Upload its **contents**, including the hidden `.github/workflows/android.yml` file (required for automatic APK builds). Keep `*.apk`, `local.properties`, and signing keys out of the repository.
