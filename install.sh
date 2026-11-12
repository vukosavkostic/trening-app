#!/usr/bin/env bash
# Build + instalacija na telefon. Usage: ./install.sh [serial ili ip:port za adb wireless]
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
export JAVA_HOME="${JAVA_HOME:-$HOME/opt/jdk}" ANDROID_HOME="${ANDROID_HOME:-$HOME/opt/android-sdk}"
GRADLE="${GRADLE:-$HOME/opt/gradle-8.7/bin/gradle}"
(cd "$DIR" && "$GRADLE" --no-daemon -q assembleDebug)
APK="$DIR/app/build/outputs/apk/debug/app-debug.apk"
if [ $# -gt 0 ]; then
  [[ "$1" == *:* ]] && adb connect "$1" >/dev/null
  adb -s "$1" install -r "$APK"
  adb -s "$1" shell am start -n com.vukos.trening/.MainActivity >/dev/null
else
  adb install -r "$APK"
  adb shell am start -n com.vukos.trening/.MainActivity >/dev/null
fi
echo "Instalirano."
