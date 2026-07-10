# Build and release automation

ContactGrouper uses GitHub Actions to validate changes and Gradle to create signed release artifacts.

- `Android CI` runs on pull requests and pushes to `main`. It runs debug unit tests and lint, builds a debug APK, and uploads that APK as a short-lived workflow artifact.
- Signed release APK and App Bundle builds remain explicit local Gradle operations and require `keystore.properties`.

## Version and artifact names

Release artifact names are derived from the Gradle release version:

```text
releaseVersionName = "1.0.1" -> contactgrouper-v1.0.1
```

## Release signing

Copy `keystore.properties.example` to `keystore.properties` and fill in the signing values before building a release APK or Play bundle. Do not commit `keystore.properties` or keystore files.
