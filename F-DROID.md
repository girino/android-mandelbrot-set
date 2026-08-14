# F-Droid publication

This repository is prepared for submission to the official F-Droid repository.
The application ID is `org.girino.frac.android.foss` and the license reported
to F-Droid must be `BSD-2-Clause`. The project is dual-licensed; see `README.md`
and `LICENSE.GAL` for details.

## Before submitting the first stable release

1. Replace the alpha version with `versionName = "1.0.0"` and increment
   `versionCode` from `1` to `2` in `app/build.gradle.kts`.
2. Add localized changelogs named `2.txt` below both Fastlane locale
   directories.
3. Review the current FOSS screenshots in
   `fastlane/metadata/android/en-US/images/phoneScreenshots/`. Replace or add
   images there whenever the visible interface changes; never reuse the
   historical screenshots that show AdMob or the old application name.
4. Run `./gradlew clean test assembleRelease lintRelease` without release
   signing environment variables. F-Droid builds and signs its own APK.
5. Commit the release state and create the stable tag `v1.0.0` on that exact
   commit. Do not submit `v1.0.0-alpha` as the first stable F-Droid version.

## fdroiddata metadata

After `v1.0.0` exists, create
`metadata/org.girino.frac.android.foss.yml` in a fork of
<https://gitlab.com/fdroid/fdroiddata> using the following starting point.
Replace `V1.0.0_COMMIT_SHA` with the full commit hash referenced by the tag.

```yaml
Categories:
  - Science & Education
License: BSD-2-Clause
AuthorName: Girino Vey
SourceCode: https://github.com/girino/android-mandelbrot-set
IssueTracker: https://github.com/girino/android-mandelbrot-set/issues
Changelog: https://github.com/girino/android-mandelbrot-set/blob/master/CHANGELOG.md

RepoType: git
Repo: https://github.com/girino/android-mandelbrot-set.git

Builds:
  - versionName: 1.0.0
    versionCode: 2
    commit: V1.0.0_COMMIT_SHA
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version v%v
UpdateCheckMode: Tags ^v[0-9]+\.[0-9]+\.[0-9]+$
CurrentVersion: 1.0.0
CurrentVersionCode: 2
```

In the `fdroiddata` checkout, validate the final file with:

```shell
fdroid readmeta
fdroid rewritemeta org.girino.frac.android.foss
fdroid checkupdates org.girino.frac.android.foss
fdroid lint org.girino.frac.android.foss
fdroid build -v -l org.girino.frac.android.foss
```

Then open a merge request against `fdroid/fdroiddata`. An RFP issue is the
simpler alternative, but a tested metadata merge request generally requires
less work from the F-Droid maintainers.

## Signing

The normal F-Droid build is signed by F-Droid and therefore does not use the
GitHub release keystore. Users can still install the F-Droid edition alongside
the historical Google Play edition because their application IDs differ.
