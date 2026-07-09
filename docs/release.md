# Release automation

ContactGrouper has two GitHub Actions workflows:

- `Android CI` runs on pull requests and pushes to `main`. It runs debug unit tests, debug lint, and an unsigned `assembleRelease` build, then uploads the generated release APK as a short-lived workflow artifact.
- `Release APK` runs on pushes to `main` that change `app/build.gradle.kts`, and can also be started manually from the Actions tab. It reads `releaseVersionName` from Gradle, creates or updates the `v<releaseVersionName>` GitHub prerelease, uploads the APK as both a workflow artifact and release asset, and can promote the previous prerelease to the latest stable release.

## Version and tags

The release tag is always derived from the Gradle release version:

```text
releaseVersionName = "1.0.1" -> tag v1.0.1
```

If a release for that tag already exists, the workflow only updates it when it is still a prerelease. A stable release with the same tag is left untouched and the workflow fails, because publishing a new prerelease requires a new Gradle version.

## Signing secrets

The release workflow can build without signing secrets. In that mode the APK artifact and release asset are marked `unsigned`, which is useful for validating automation but is not a production installable release.

To publish signed release APKs, configure all of these repository secrets:

- `RELEASE_KEYSTORE_BASE64`: base64-encoded release keystore file.
- `RELEASE_KEYSTORE_PASSWORD`: password for the keystore.
- `RELEASE_KEY_ALIAS`: signing key alias.
- `RELEASE_KEY_PASSWORD`: password for the signing key.

Generate the base64 secret from a local keystore with:

```sh
base64 -i path/to/contactgrouper-release.jks | pbcopy
```

For local Play bundle builds, copy `keystore.properties.example` to `keystore.properties` and fill in the same signing values. Do not commit `keystore.properties` or keystore files.

## Promotion behavior

By default, the release workflow promotes the most recent existing prerelease to a stable latest release before publishing the new prerelease. This matches the intended flow where the previous tested prerelease becomes latest and the newly built version becomes the active prerelease.

Promotion only considers non-draft GitHub releases that are currently marked prerelease. It does not compare semantic version ordering; it chooses the most recently created eligible prerelease. Manual runs can disable promotion with the `promote_previous_prerelease` input.
