# Google Play releases

Releases are started manually in [GitHub Actions](https://github.com/ShadowPaint-SP/ContactGrouper/actions/workflows/play-release.yml).
Pushing or merging to `main` runs ordinary Android CI, but **never publishes a release**.

## Publish an update

1. Merge the app changes and updated `release-notes/whatsnew-en-US` and
   `release-notes/whatsnew-de-DE` into `main`. Each language needs 1–500 characters.
2. Open **Actions → Publish Play release → Run workflow**, using `main`.
3. Enter a version name such as `1.1.0` and a new version code such as `3`.
   The code must exceed every previous Play upload, including testing releases.
   Names use `major.minor.patch`; successful uploads get a unique `v<version>` tag.
4. Leave **Upload to Play internal testing and create a GitHub release** unchecked for a
   build-only check. Check it when ready to upload.
5. Start the workflow. It tests, lints, signs, packages, checks Play access/version codes,
   and optionally uploads the App Bundle to **internal testing**.

The inputs override the version for that build. No source version bump is required;
ordinary local/debug builds retain the defaults in `app/build.gradle.kts`.

```bash
# Test the signed build and Play access without uploading.
gh workflow run play-release.yml --repo ShadowPaint-SP/ContactGrouper --ref main \
  -f version_name=1.1.0 -f version_code=3 -f publish=false

# Upload to internal testing.
gh workflow run play-release.yml --repo ShadowPaint-SP/ContactGrouper --ref main \
  -f version_name=1.1.0 -f version_code=3 -f publish=true
```

Build-only checks create and discard an unpublished Play API edit to inspect artifacts and
tracks. They do not upload bundles or change tracks. Avoid editing releases in Play Console
while an upload is running, since simultaneous Play edits can invalidate one another.

## Test, then promote to production

Use Play Console's internal-testing release and tester opt-in link to test the installed
app. Tester lists remain managed in Play Console.

When ready, promote that existing release to production in Play Console, or select its
existing version code from the bundle library when creating a production release.
Use the **same uploaded bundle**; do not rebuild or upload the same code again.
Review notes and rollout settings before submitting. Google's review and publishing controls apply.

The GitHub publisher has testing access only. Production promotion is a deliberate Play
Console action. The GitHub release records the internal upload and remains marked as a
prerelease; it can be marked as a full release after production rollout.

## Artifacts and release history

Each successful build saves a workflow artifact for 90 days containing the signed AAB,
localized notes, `SHA256SUMS`, `release.md`, and `release.json`. The JSON records package,
version name/code, exact source commit, intended track, bundle SHA-256, and workflow URL.

After a successful upload, the workflow creates a GitHub prerelease with the bundle,
notes, and metadata, tagged `v<version>` at the source commit. Its description includes
GitHub-generated change notes. Release assets persist beyond workflow-artifact retention.

```bash
git fetch origin --tags
git log v1.1.0..origin/main --first-parent --oneline
git diff --stat v1.1.0..origin/main
```

The previous manual release was `1.0.1`, code `2`, uploaded June 25, 2026 and live in
production as `CG-1.0.1`. Its likely source baseline is `b6487f5`. Old manual uploads have
no source tags, so this mapping cannot be proven from Play metadata alone.

## What was configured

| Resource | Value / purpose |
| --- | --- |
| Repository | `ShadowPaint-SP/ContactGrouper` |
| Workflow | `.github/workflows/play-release.yml`, manual dispatch only |
| GitHub environment | `google-play`, restricted to the `main` branch |
| Cloud project | `contactgrouper-releases`, number `580588931356`, dedicated to release authentication |
| Service account | `github-play-publisher@contactgrouper-releases.iam.gserviceaccount.com` |
| Identity pool / provider | `github` / `contactgrouper`, location `global` |
| Play app | `de.drvlabs.contactgrouper` |
| Play permissions | View app information and release to testing tracks for Contact Grouper only |

Enabled APIs: Android Publisher, IAM, IAM Credentials, and Security Token Service.
No billing account is attached as part of this setup.

GitHub uses OpenID Connect and Workload Identity Federation. The provider accepts this
repository's numeric ID (`1070805874`), owner ID (`71901885`), `main` branch, and the
`play-release.yml` workflow. This identity may impersonate only the publisher service account.
The service account has no broad Google Cloud project roles. No permanent Google
service-account key is downloaded or stored in GitHub.

The `google-play` environment holds four encrypted secrets copied from the existing local
upload-signing configuration:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

It also holds these non-secret configuration variables:

```text
PLAY_SERVICE_ACCOUNT=github-play-publisher@contactgrouper-releases.iam.gserviceaccount.com
PLAY_WORKLOAD_IDENTITY_PROVIDER=projects/580588931356/locations/global/workloadIdentityPools/github/providers/contactgrouper
```

The runner restores the upload keystore into a temporary file for the build and removes
it afterward. Local signing still uses ignored `keystore.properties` and the existing
keystore. Keep a separate backup of that keystore and its passwords.

## Local release build

Use JDK 17 and Android SDK 36. With `keystore.properties` configured:

```bash
./gradlew :app:testReleaseUnitTest :app:lintRelease :app:bundleRelease \
  -PreleaseVersionName=1.1.0 -PreleaseVersionCode=3
```

The bundle is `app/build/outputs/bundle/release/contactgrouper-v1.1.0-release.aab`.
CI supplies `ANDROID_KEYSTORE_FILE`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, and
`ANDROID_KEY_PASSWORD`; these override local signing properties.

## Recovering a failed run

- **Tests, signing, or authentication failed:** fix the problem and rerun. The same version/code
  can be reused if no upload occurred.
- **Version code already used:** inspect Play Console and choose a higher code for a new build.
  Never delete a release tag to reuse a published version.
- **Upload succeeded but GitHub release creation failed:** download the run's build artifact.
  It contains the exact uploaded bundle and metadata. Create the missing release at the
  `commit` in `release.json`. Do not rerun the upload or rebuild the bundle. After extracting
  the artifact and checking the recorded commit:

  ```bash
  gh release create v1.1.0 *.aab release.json SHA256SUMS whatsnew-* \
    --repo ShadowPaint-SP/ContactGrouper --target <recorded-commit> \
    --title 'Contact Grouper 1.1.0 (3)' --notes-file release.md \
    --generate-notes --prerelease --latest=false
  ```

## Disable or remove the setup

To pause releases while retaining configuration:

```bash
gh workflow disable play-release.yml --repo ShadowPaint-SP/ContactGrouper
```

Re-enable with `gh workflow enable play-release.yml --repo ShadowPaint-SP/ContactGrouper`.
Disabling does not stop an already-running job; cancel an active run separately if needed.

To remove automation completely:

1. Disable the workflow and ensure no release run is active.
2. In **Play Console → Users and permissions**, remove
   `github-play-publisher@contactgrouper-releases.iam.gserviceaccount.com`.
3. Delete the dedicated GitHub environment, including its secrets and variables:

   ```bash
   gh api --method DELETE repos/ShadowPaint-SP/ContactGrouper/environments/google-play
   ```

4. With Google Cloud CLI signed into the owning account, delete the identity pool and service account:

   ```bash
   gcloud iam workload-identity-pools delete github \
     --location=global --project=contactgrouper-releases
   gcloud iam service-accounts delete \
     github-play-publisher@contactgrouper-releases.iam.gserviceaccount.com \
     --project=contactgrouper-releases
   ```

   Alternatively, if this dedicated project still contains only the release setup, delete
   it with `gcloud projects delete contactgrouper-releases`. Check for unrelated resources first.

5. Remove `.github/workflows/play-release.yml`, `scripts/check_play_version.py`, its test, and
   `release-notes/` through a repository change. The Gradle version/signing overrides can
   stay for local builds or be reverted if no longer needed.

Keep the local upload keystore, passwords, GitHub release history, and Play bundles.
Removing automation does not require an upload-key reset or unpublish the app.

Google Cloud CLI was downloaded to `/tmp/contactgrouper-release-tools/` for setup, without
modifying the shell PATH. Normal releases do not need it. Its login is saved in gcloud's
normal user configuration directory. Revoke that local login with
`gcloud auth revoke <your-account>` when no longer needed, then remove the temporary tools
directory if desired. Revoking the local login does not disable GitHub's federated identity.

## References

- [Google Play API setup](https://developers.google.com/android-publisher/getting_started)
- [Google authentication for GitHub Actions](https://github.com/google-github-actions/auth)
- [Google Play upload action](https://github.com/r0adkll/upload-google-play)
