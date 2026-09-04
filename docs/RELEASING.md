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
