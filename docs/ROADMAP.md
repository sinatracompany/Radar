# Development Roadmap

## Phase 1 — active source implementation (v0.1.0)
- [x] Gradle/Kotlin/Compose project structure
- [x] Wi-Fi and BLE scanning modules using native APIs
- [x] Radio statuses and runtime permission requests
- [x] Real-observation radar visualization with clear uncertainty label
- [x] Wi-Fi/BLE lists, timestamps, signal charts, watchlist toggle
- [x] Basic RSSI math unit tests and Github Actions build pipeline
- [ ] Run Gradle build and unit tests on environment with Android SDK and internet
- [ ] Install on physical Android phone, validate scanning and performance
- [ ] Fix issues found in device tests and publish a tested APK

## Phase 2 — reliability
- [ ] Full separation into scanner repositories, ViewModel state, and UI modules
- [ ] Dynamic Wi-Fi throttling policies and reliable scan age/missing result handling
- [ ] Graceful Bluetooth on/off changes and scan failure backoff
- [ ] Background-thread data processing, better binder-callback buffering
- [ ] History DB with Room, proper TTLs and export redaction
- [ ] Bluetooth address randomization handling without over-identification
- [ ] Full automated tests for state transitions and Android permission variants

## Phase 3 — extended features
- [ ] Cellular connection data via legal Android Telephony APIs
- [ ] Licensed local manufacturer/OUI database and explainable classification
- [ ] Local-only anomaly rules and user-configured watchlist notifications
- [ ] CSV/JSON/HTML reports, complete settings, accessibility, localization
- [ ] Profiling and device-specific scan benchmarks

## Non-goals
- No claim of universal RF, UHF/VHF, proprietary broadcast capture, true radar position or direction-finding, surveillance detection certainty, Android restriction bypass, or hidden monitoring.
