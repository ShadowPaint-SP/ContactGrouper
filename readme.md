<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" alt="Contact Grouper app icon" width="180">
</p>

# Contact Grouper

Contact Grouper is an Android app for organizing device contacts into groups and assigning a ringtone to each group. It works directly with Android's contacts provider, so group memberships and ringtone changes are reflected in the system contacts database.

## Features

- Browse and search contacts and contact groups.
- Create local groups or import groups already stored on the device.
- Assign one or more contacts to multiple groups at once.
- Add or remove group memberships from a contact's detail screen.
- Assign a ringtone to a group and apply it to its members.
- Sync device-group changes manually or automatically.
- Use English or German UI text.

When a contact belongs to several groups, the newest assigned group with a ringtone takes precedence. If that group has no ringtone, Contact Grouper falls back through older memberships until it finds one.

## Privacy and permissions

Contact Grouper processes contact, group, and ringtone data locally on the device. It requires Android's read and write contacts permissions to display contacts and update group memberships and ringtone settings. The app does not request internet access.

See the [privacy policy](docs/privacy-policy.md) for details.

## Requirements

- Android 10 (API 29) or newer
- JDK 17 for local development
- Android SDK 36

## Build and test

Clone the repository and run the Gradle wrapper from its root:

```bash
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

Run the JVM tests and static analysis with:

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
```

Instrumentation tests require a connected device or emulator:

```bash
./gradlew :app:connectedDebugAndroidTest
```

GitHub Actions runs unit tests, lint, and a debug build for pushes and pull requests targeting `main`.

## Release builds

Release builds require a local signing configuration. Copy `keystore.properties.example` to `keystore.properties`, fill in the signing values, and keep both the properties file and keystore out of version control.

See the [release guide](docs/release.md) for the release commands and artifact locations.

## Project structure

```text
app/src/main/java/de/drvlabs/contactgrouper/
├── contacts/    Contact queries, list state, and contact details
├── groups/      Group storage, device sync, memberships, and ringtones
├── permission/  Contacts permission state
├── search/      Shared search UI and filtering
├── settings/    Persistent app settings
└── ui/theme/    Compose theme
```

The app is written in Kotlin using Jetpack Compose, Room, Navigation Compose, and Coil.
