param(
    [string]$DeviceSerial = ""
)

$ErrorActionPreference = "Stop"

$SkillDir = Split-Path -Parent $PSScriptRoot
$RepoRoot = (Resolve-Path (Join-Path $SkillDir "..\..\..")).Path
$TestSource = Join-Path $SkillDir "resources\WidgetScreenshotCaptureTest.kt"
$TempTestTarget = Join-Path $RepoRoot "app\src\androidTest\kotlin\dev\shiqan\materialwidgets\WidgetScreenshotCaptureTest.kt"
$OutputDir = Join-Path $RepoRoot ".github\screenshots"
$RemoteDir = "/sdcard/Android/data/dev.shiqan.materialwidgets/files/widget_screenshots"

Push-Location $RepoRoot
try {
    $adbArgs = if ($DeviceSerial) { @("-s", $DeviceSerial) } else { @() }

    $devicesOutput = & adb devices
    $onlineDevices = $devicesOutput | Select-String "^\S+\s+device$"
    if (-not $onlineDevices) {
        throw "No online Android device or emulator found. Start an emulator first (e.g., android emulator start pixel_7) and retry."
    }

    if (Test-Path $TempTestTarget) {
        throw "Refusing to overwrite existing $TempTestTarget"
    }

    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $TempTestTarget) | Out-Null
    Copy-Item -Path $TestSource -Destination $TempTestTarget

    try {
        & .\gradlew.bat installDebug installDebugAndroidTest
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle installDebug installDebugAndroidTest failed with exit code $LASTEXITCODE"
        }

        $instrumentOutput = & adb @adbArgs shell am instrument -w -e class dev.shiqan.materialwidgets.WidgetScreenshotCaptureTest dev.shiqan.materialwidgets.test/androidx.test.runner.AndroidJUnitRunner
        $instrumentOutput | ForEach-Object { Write-Output $_ }
        if ($LASTEXITCODE -ne 0 -or -not ($instrumentOutput -match "OK \(\d+ tests?\)")) {
            throw "Instrumentation capture failed."
        }

        New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
        Get-ChildItem -Path $OutputDir -Filter *.png -ErrorAction SilentlyContinue | Remove-Item -Force

        $tempPull = Join-Path $env:TEMP ("mw_screenshots_" + [guid]::NewGuid().ToString("N"))
        New-Item -ItemType Directory -Force -Path $tempPull | Out-Null
        try {
            & adb @adbArgs pull "$RemoteDir/." $tempPull
            if ($LASTEXITCODE -ne 0) {
                throw "Failed to pull screenshots from $RemoteDir"
            }
            Copy-Item -Path (Join-Path $tempPull "*.png") -Destination $OutputDir -Force
        } finally {
            Remove-Item -Path $tempPull -Recurse -Force -ErrorAction SilentlyContinue
        }

        $captured = Get-ChildItem -Path $OutputDir -Filter *.png | Sort-Object Name
        Write-Output "Captured $($captured.Count) widget screenshots into .github/screenshots/:"
        $captured | ForEach-Object { Write-Output " - $($_.Name) ($($_.Length) bytes)" }
    } finally {
        Remove-Item -Path $TempTestTarget -Force -ErrorAction SilentlyContinue
    }
} finally {
    Pop-Location
}
