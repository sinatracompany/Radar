# Codex instructions — SignalScope

## Project identity

- GitHub remote: `https://github.com/sinatracompany/Radar.git`
- Windows checkout: `D:\SignalScope`
- Application ID: `com.signalscope.app`
- Current version: `0.1.0` (Phase 1, unverified)
- This project is independent of YourIPTV and all other apps.

## Owner's immediate instruction: get an APK first, test later

1. Read `GET_APK_FIRST.md`, `README.md`, current app sources and build files. Inspect `git status -sb` and `git remote -v`, preserve existing changes and history.
2. **Compile first**, using `Build-APK-NoTests.cmd` on Windows or `gradle --no-daemon assembleDebug`. Do **not** require unit tests, emulator tests, or physical-device tests before creating this initial development APK.
3. Fix compile errors if any; report an actual APK only if the build produced one. Copy successful debug APK to `release/SignalScope-v0.1.0-debug.apk` and record its SHA-256 digest.
4. The GitHub Actions workflow automatically attempts the same no-test build on each `main` push and uploads the APK as a build artifact. The artifact is not a production release.
5. Do not claim the app's behavior is verified. Note all physical and functional tests as pending until actually run. Later, with owner permission, test via ADB and address issues in a new version.
6. Once the first APK is available, prioritize real Wi-Fi scanning, BLE scanning, radar updates, RSSI monitoring and permissions. See `docs/ROADMAP.md` for later features.

## Core rules

- Use real Android APIs. Do not invent or simulate actual radio detections in the production interface.
- Radar markers are visualization only; they do not indicate transmitter bearing or distance.
- Respect Android Wi-Fi throttling, BLE permission rules, and unsupported hardware.
- Scanning is visible/foreground-only; no covert monitoring.
- No network upload, telemetry, accounts, subscriptions, or paywalls.
- Do not copy proprietary SØPHIA assets/code.
- Do not commit credentials, signing keys, or `local.properties`.
- Never force push or overwrite unrelated files. Treat untested debug builds as experimental, not production-ready.
