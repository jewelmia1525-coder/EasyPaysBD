# PaysBD SMS Forwarder

Professional Android SMS Forwarder with HTTP Shortcut export, powered by Ornov Store.

## Production backend

`https://paysbd.lovable.app`

The app uses these production endpoints:

- `/api/public/device-config`
- `/api/public/sms-ingest`
- `/api/public/device-commands`
- `/api/public/device-log`
- `/api/public/device-heartbeat`
- `/api/public/app-release`
- `/api/public/device-enroll`
- `/api/public/app-about`

Authentication uses `x-device-id` and `x-device-secret`.

## Build on GitHub

1. Create a new GitHub repository.
2. Upload **all files in this repository root** (do not upload the outer ZIP folder as a nested directory).
3. Commit to `main`.
4. Open **Actions** → **Build PaysBD SMS Forwarder APK**.
5. Wait for the workflow to finish.
6. Open the completed workflow run → **Artifacts** → `PaysBD-SMS-Forwarder-Release`.
7. Download the APK and install it on the Android device.

### Important signing note

The included workflow creates a temporary release keystore so the repository can build immediately. For long-term production updates, replace this with a permanent signing key stored in GitHub Actions Secrets. Do not commit a keystore or passwords to GitHub.

## Branding

- PaysBD
- SMS Forwarder
- HTTP Shortcut
- Powered by Ornov Store
