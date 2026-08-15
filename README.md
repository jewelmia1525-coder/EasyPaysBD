# EasyPayBD SMS Forwarder — Android Source

Complete Android Studio project source that wires to the EasyPayBD website endpoints (`/api/public/device-config`, `/sms-ingest`, `/device-commands`, `/device-log`, `/device-heartbeat`, `/app-release`).

## Build
1. Open this folder in **Android Studio (Hedgehog+)**.
2. Change `BASE_URL` in `app/src/main/java/com/easypaybd/smsforwarder/Config.kt` to your website URL (default: `https://easypaybd.lovable.app`).
3. Sync Gradle, then **Build > Build APK(s)** or `./gradlew assembleRelease`.

## Features (matches admin panel spec)
- Splash with animated big logo (from https://i.postimg.cc/7hbf6hfM/Chat-GPT-Image-Aug-14-2026-12-11-48-PM.png)
- Fields: Name, Description, Sender filter (any / number / `*`), Webhook URL
- Advanced: SIM slot (any / SIM 1 / SIM 2 …), JSON Payload Template, Headers, Retries (default 10), Ignore SSL, Chunked mode
- Save / Cancel
- Big animated **Run** ring button; shows connected / 402 / 403 etc.
- Top-right **Heartbeat** shortcut
- Background `ForegroundService` + `SmsReceiver` forwards every SMS to your webhook
- Polls `/device-commands` every 30s for the 10 remote features
- Reports device model, sim count, sim info, app version via heartbeat
- Auto-syncs offline SMS when internet returns
- Auto-update check via `/app-release`

## Endpoint contract used
Headers on every call: `x-device-id`, `x-device-secret` (set in-app first run).
