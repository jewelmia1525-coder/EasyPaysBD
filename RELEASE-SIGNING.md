# EasyPayBD release signing

The workflow supports a permanent Android signing key through four GitHub Actions secrets:

- `ANDROID_KEYSTORE_B64` — base64-encoded `.jks` keystore
- `ANDROID_KEYSTORE_PASSWORD` — keystore password
- `ANDROID_KEY_ALIAS` — key alias
- `ANDROID_KEY_PASSWORD` — key password

If these secrets are not configured, the workflow creates a temporary signing key for that build. This is useful for testing, but it is **not suitable for production updates**, because a later build with a different key cannot update the installed app.

Do not commit a private keystore or its passwords to the repository.

Note: signing alone does not guarantee that Google Play Protect will allow installation. This app requests SMS-related permissions, and Play Protect may still warn or block sideloaded builds based on its own risk assessment.
