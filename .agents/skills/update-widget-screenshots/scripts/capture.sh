#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKILL_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$SKILL_DIR/../../.." && pwd)"

TEST_SOURCE="$SKILL_DIR/resources/WidgetScreenshotCaptureTest.kt"
TEMP_TEST_TARGET="$REPO_ROOT/app/src/androidTest/kotlin/dev/shiqan/materialwidgets/WidgetScreenshotCaptureTest.kt"
OUTPUT_DIR="$REPO_ROOT/.github/screenshots"
REMOTE_DIR="/sdcard/Android/data/dev.shiqan.materialwidgets/files/widget_screenshots"

ADB_ARGS=()
if [[ $# -ge 1 && -n "$1" ]]; then
  ADB_ARGS=(-s "$1")
fi

if ! adb devices | grep -Eq '^[[:graph:]]+[[:space:]]+device$'; then
  echo "No online Android device or emulator found. Start an emulator first and retry." >&2
  exit 1
fi

if [[ -e "$TEMP_TEST_TARGET" ]]; then
  echo "Refusing to overwrite existing $TEMP_TEST_TARGET" >&2
  exit 1
fi

cleanup() {
  rm -f "$TEMP_TEST_TARGET"
}
trap cleanup EXIT

mkdir -p "$(dirname "$TEMP_TEST_TARGET")"
cp "$TEST_SOURCE" "$TEMP_TEST_TARGET"

cd "$REPO_ROOT"
./gradlew installDebug installDebugAndroidTest

INSTRUMENT_OUTPUT="$(adb "${ADB_ARGS[@]}" shell am instrument -w -e class dev.shiqan.materialwidgets.WidgetScreenshotCaptureTest dev.shiqan.materialwidgets.test/androidx.test.runner.AndroidJUnitRunner)"
echo "$INSTRUMENT_OUTPUT"
if ! echo "$INSTRUMENT_OUTPUT" | grep -Eq 'OK \([0-9]+ tests?\)'; then
  echo "Instrumentation capture failed." >&2
  exit 1
fi

mkdir -p "$OUTPUT_DIR"
rm -f "$OUTPUT_DIR"/*.png

TMP_PULL="$(mktemp -d)"
trap 'rm -rf "$TMP_PULL"; cleanup' EXIT
adb "${ADB_ARGS[@]}" pull "$REMOTE_DIR/." "$TMP_PULL"
cp "$TMP_PULL"/*.png "$OUTPUT_DIR/"

echo "Captured $(ls -1 "$OUTPUT_DIR"/*.png | wc -l | tr -d ' ') widget screenshots into .github/screenshots/."
