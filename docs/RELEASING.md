# Releasing VoiceJournal

VoiceJournal uses Git tags to create private GitHub Releases. There is no
in-app update check: the version shown in the drawer is build metadata only.

## What to update

For a release, update nothing in the app source. The release tag supplies the
version to the CI build:

```text
v0.2.0
```

The workflow builds the APK as `versionName = 0.2.0` and `versionCode = 200`.
The code is calculated as `major * 10000 + minor * 100 + patch`.

## Release steps

Merge the changes you want to release into `main`, then create and push a tag:

```bash
git switch main
git pull origin main
git tag -a v0.2.0 -m "VoiceJournal v0.2.0"
git push origin main
git push origin v0.2.0
```

Pushing the tag starts `.github/workflows/release.yml`. It runs unit tests,
builds the release APK, and creates a private GitHub Release with generated
notes and the APK attached.

GitHub generates notes from commits and pull requests since the previous
release. Prefer clear titles such as `feat: add rich-text alignment`,
`fix: repair audio playback state`, and `ui: improve settings spacing`.

Release from `main`, not from a feature branch. The current checkout is on
`refinement/richtext`; merge it into `main` before creating the first release.

## Signing

The project currently has no production signing configuration. The workflow
can compile the release variant, but add a protected release keystore through
GitHub Actions secrets before distributing the APK widely. Never commit the
keystore or its passwords.
