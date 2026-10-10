# NORA TUNNEL - build fix notes

- GitHub Actions now installs Gradle 8.11.1 itself and runs `gradle assembleDebug`, so the missing
  `gradle-wrapper.jar` no longer blocks the build. (Optional: run `gradle wrapper` once on a PC and commit the jar.)
- Debug APK: Actions tab -> latest run -> Artifacts -> Nora-Tunnel-APKs.
- Release build is optional and unsigned unless KEYSTORE_* secrets are set.
