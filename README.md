# EasyPayBD SMS Forwarder — GitHub APK Build

This project can be built on GitHub Actions without installing Android Studio on your PC.

## Before building
Edit:
`android-app/app/src/main/java/com/easypaybd/smsforwarder/Config.kt`

and set the correct EasyPayBD backend URL if it is different from the current value.

## GitHub build
1. Upload this project to a GitHub repository.
2. Push to `main`/`master`, or open **Actions → Build EasyPayBD Android APK → Run workflow**.
3. Wait for the workflow to finish.
4. Open the completed workflow run.
5. Under **Artifacts**, download **EasyPayBD-APKs**.
6. Extract it and install the APK on Android.

The workflow uses Java 17 and Gradle 8.7 and builds both release and debug APKs.

Note: the current release build uses the project's debug signing configuration, so it is suitable for testing/internal installation. For Play Store production publishing, a proper release keystore/signing setup should be added separately.
