# Releasing VoiceJournal

VoiceJournal uses Git tags to create private GitHub Releases. There is no
in-app update check: the version shown in the drawer is build metadata only.

## What to update

Manually update these two values in `app/build.gradle.kts`:

```text
versionCode = 200
versionName = "1.2.0"
```

The drawer already reads these values through `BuildConfig`; no app code or
network logic is needed. The workflow checks that the pushed tag matches
`versionName` before building.

Use these tag styles:

```text
v1.0             stable release
v1.1             stable improvement
v1.1.1           bug fix
v1.2.0-dev1      development/pre-release
v1.2.0-dev2      next development/pre-release
```

Every tag must be unique. Increase `versionCode` for every APK you build.

## Release steps

Merge the changes you want to release into `main`, then create and push a tag:

```bash
git switch main
git pull origin main
git tag -a v1.2.0 -m "VoiceJournal v1.2.0"
git push origin main
git push origin v1.2.0
```

Pushing the tag starts `.github/workflows/release.yml`. It runs unit tests,
builds the release APK, and creates a private GitHub Release with generated
notes and the APK attached. Tags containing `-dev` are marked as pre-releases.

If a tag already exists but its workflow failed, open the GitHub Actions page,
select **Build and publish release**, choose **Run workflow**, enter the
existing tag, and run it again. This is useful for recovering a failed release
without creating another version tag.

GitHub generates notes from commits and pull requests since the previous
release. Prefer clear titles such as `feat: add rich-text alignment`,
`fix: repair audio playback state`, and `ui: improve settings spacing`.

Release from `main`, not from a feature branch. Ensure all changes and tests
are passing on `main` before creating the release tag.

## Signing

Release APKs must be signed with one permanent release keystore. The Gradle
configuration reads signing values from either a local ignored
`signing.properties` file or GitHub Actions environment variables. Never commit
the keystore or its passwords.

Add these GitHub Actions secrets before publishing a release:

```text
RELEASE_KEYSTORE_BASE64
RELEASE_STORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

Create the keystore once on a trusted machine and keep a backup in a secure
password manager or encrypted storage:

```bash
keytool -genkeypair -v \
  -keystore voicejournal-release.jks \
  -alias voicejournal-release \
  -keyalg RSA -keysize 4096 -validity 10000
```

Use the passwords requested by `keytool` for `RELEASE_STORE_PASSWORD` and
`RELEASE_KEY_PASSWORD`, and use `voicejournal-release` for
`RELEASE_KEY_ALIAS`. Convert the keystore to one line for the
`RELEASE_KEYSTORE_BASE64` secret:

```bash
base64 < voicejournal-release.jks | pbcopy
```

For local signed builds, create the ignored `signing.properties` file in the
repository root, beside `settings.gradle.kts`:

```text
storeFile=/absolute/path/to/voicejournal-release.jks
storePassword=your-keystore-password
keyAlias=voicejournal-release
keyPassword=your-key-password
```

The workflow decodes the keystore only on the temporary runner, verifies the
APK signature, and uploads a matching `.apk.sha256` checksum file.
