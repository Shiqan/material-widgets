---
name: update-widget-screenshots
description: >-
  Re-renders and updates the transparent Material You widget screenshots in
  .github/screenshots/ and keeps the README.md Widget Gallery in sync. Use when
  widgets are added, redesigned, or visually modified and the repository
  screenshots need to be regenerated without adding assets to the Android app.
---

# Update Widget Screenshots

Generates hardware-accelerated, transparent-background PNGs for every widget registered in `WidgetPreviewRegistry.specs` by running a temporary instrumentation test on a live Android emulator/device (recommended: Android 12+ / API 31+ for Material You dynamic colors) and pulling the images into `.github/screenshots/`.

No screenshot assets or capture test code are left inside `app/`.

## Prerequisites

1. Verify an Android emulator or device is connected and booted:
   ```powershell
   adb devices
   ```
2. If no device is attached, list available AVDs and start one (e.g. `pixel_7` or `medium_phone`) in the background, then wait for boot completion:
   ```powershell
   android emulator list
   # Start the emulator as a background daemon, then wait until sys.boot_completed == 1
   adb wait-for-device
   ```
   > **Note (Windows):** If `android emulator start <avd>` exits immediately when a subshell closes, launch `$env:ANDROID_AVD_HOME = "$env:USERPROFILE\.android\avd"; C:\tools\android\emulator\emulator.exe -avd <avd> -no-window -no-audio -no-boot-anim` as a daemon process and wait for `adb shell getprop sys.boot_completed` to return `1`.

## Steps

1. **Run the capture script** from the repository root. It temporarily copies [WidgetScreenshotCaptureTest.kt](./resources/WidgetScreenshotCaptureTest.kt) into `app/src/androidTest/`, installs `debug` and `debugAndroidTest`, captures every widget via `PixelCopy` + black/white difference matting, pulls the PNGs into `.github/screenshots/`, and removes the temporary test in a `finally` / `trap` cleanup block:
   - Windows (PowerShell):
     ```powershell
     .\.agents\skills\update-widget-screenshots\scripts\capture.ps1
     ```
   - macOS / Linux:
     ```bash
     bash ./.agents/skills/update-widget-screenshots/scripts/capture.sh
     ```
   *(Pass an optional device serial as the first argument if multiple devices are connected.)*

2. **Check for new or renamed widgets:**
   - The capture script derives filenames from `WidgetPreviewRegistry.specs` (`<Name>AppWidgetReceiver` → kebab-case `<name>.png`).
   - If a widget was added, removed, or renamed in [WidgetPreviewRegistry.kt](../../../app/src/main/kotlin/dev/shiqan/materialwidgets/widget/util/WidgetPreviewRegistry.kt), update the **Widget Gallery** tables in [README.md](../../../README.md) to match.
   - If a new wide widget needs a custom render size or sample state (similar to `MusicAppWidgetReceiver` in [WidgetScreenshotCaptureTest.kt](./resources/WidgetScreenshotCaptureTest.kt)), update the resource test accordingly.

3. **Verify results:**
   - Visually inspect updated or newly added PNGs in `.github/screenshots/` to confirm rounded corners, clipping, contrast, and text fit.
   - Run `git status --short` and confirm no temporary `WidgetScreenshotCaptureTest.kt` remains under `app/src/`.
   - Stop the emulator when finished (`adb emu kill`) if you started it solely for this task.
