# GitHub Actions: Android port CI

Workflow: [Android Unity Port](../.github/workflows/android-unity.yml).

## Preflight
A manual run with `build_apk=false` checks out this overlay repository, clones the MIT-licensed [SanAndreasUnity](https://github.com/in0finite/SanAndreasUnity) `dev` branch with its submodules, installs the Android overlay and checks required files. **It does not compile Unity code or produce an APK.**

## APK attempt (experimental)
In GitHub Actions choose **Android Unity Port** -> **Run workflow** -> `build_apk=true`.
The Android job uses GameCI Unity Builder v4 and Unity 2020.3.26f1. Configure repository Actions secrets `UNITY_LICENSE` or both `UNITY_EMAIL` and `UNITY_PASSWORD` before running. Follow the current GameCI license documentation: https://game.ci/docs/github/activation/.

**Known risks:** Unity 2020 activation via CI, legacy Android plugins, submodule compatibility, runtime storage permissions, and Android 16 KB page size support are not resolved. A successful pipeline would prove a build, not that the game is playable or faster. Do not upload proprietary GTA assets.

If successful, the APK is under the workflow's Artifacts. If it fails, inspect Unity Editor.log output and fix the first actual compilation/build error rather than guessing.

## Reproducibility
The upstream is currently fetched from the moving `dev` branch. Pin a tested commit SHA before publishing builds.
