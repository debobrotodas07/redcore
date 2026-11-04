# REDCORE GAME ENGINE — build registry

GOAL: Ship a working, premium native Android gaming utility (`com.redcore.gameengine`,
Kotlin + Compose M3 + MVVM + Room + DataStore) as a signed-in-debug APK, built on
GitHub Actions (no local JDK/SDK on the phone).

TARGET: Android 14 (API 34), device MEIZU Lucky 08. minSdk 26.

Build path: project lives at `~/redcore` → push to GitHub → Actions `assembleDebug`
→ APK in Releases + artifact.

## PHASES

- [x] P0 Environment inspection (no JDK/gradle/Android SDK in Termux → cloud build)
- [x] P1 Gradle scaffold (AGP 8.5.2 / Kotlin 1.9.24 / KSP / Compose BOM 2024.09)
- [x] P1 Manifest + permissions + FGS types + `<queries>` + adaptive icon + fonts
- [x] P2 Design system (colors, type, glass components, animated logo/background)
- [x] P3 Data layer: Room (games, profiles, sessions) + DataStore settings + repos
- [x] P4 System layer: CPU/battery/thermal/memory monitors, permission helper,
      profile applier (real actions only)
- [x] P5 Services: OverlayService (floating sidebar, Compose), CaptureService
      (MediaProjection screenshot + MediaRecorder screen recording)
- [x] P6 UI: Dashboard, Library, Game detail, Performance, Monitor, Audio Studio,
      Crosshair Studio, Capture, Settings, Permissions, About, Privacy, Tutorial, More hub
- [x] P7 ViewModels wired to Room/DataStore
- [x] P8 Backup/restore JSON via SAF, session tracking, haptics, update info
- [x] P9 Push repo + run Actions build, fix compile errors to green (run 2 green, build-2 released)
- [x] P10 Deliver APK link + install steps + honest limitation list

- Release: https://github.com/debobrotodas07/redcore/releases/download/build-2/app-debug.apk (18.5 MB)

## DECISIONS

- Single `:app` module, package-layered (data/sys/service/ui/util) + manual
  ServiceLocator (no Hilt → fewer moving parts, faster CI).
- FPS: honest only. Own-window frame timing (Choreographer + FrameMetrics) and
  display refresh rate. Other apps' FPS needs root/privileged access → labelled
  unavailable, never faked.
- Performance profiles never claim CPU/GPU frequency control. Real levers:
  screen brightness (WRITE_SETTINGS), refresh-rate mode of our window, DND
  (policy access), plus deep-link shortcuts to system settings.
- Voice changer is our own record→DSP→playback chain, explicitly not system-wide.
- Custom fonts: Orbitron (brand) + Rajdhani (display) from google/fonts.
- Crosshair setters live directly on SettingsViewModel (no *Compat aliases).
- Update check reads `BuildConfig.UPDATE_REPO` (Gradle property `-PupdateRepo=user/repo`).

## NOW
P10 — build green (run 2). APK: https://github.com/debobrotodas07/redcore/releases/download/build-2/app-debug.apk
Next: user installs on the MEIZU, reports any crash/UX issue; then polish pass.

## VERIFY
- Cloud build must be green: `gh api repos/<user>/redcore/actions/runs`
- On device: permissions flow, sidebar overlay start/stop, screenshot, 5s recording.
