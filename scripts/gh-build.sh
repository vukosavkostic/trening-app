#!/usr/bin/env bash
# Pokrece build na GitHub-u, ceka da se zavrsi, skida APK i instalira ga na telefon (+ screenshot).
# Usage: scripts/gh-build.sh [ip:port telefona]     (bez argumenta: prvi povezan adb uredjaj)
# Treba: gh (GitHub CLI) prijavljen sa `gh auth login`.
set -euo pipefail
REPO=vukosavkostic/trening-app
WF=build.yml
ADB="${ADB:-$HOME/opt/android-sdk/platform-tools/adb}"
DIR="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$DIR/build/gh-apk"

command -v gh >/dev/null || { echo "Nema gh. Instaliraj GitHub CLI pa: gh auth login"; exit 1; }
gh auth status >/dev/null 2>&1 || { echo "gh nije prijavljen. Pokreni: gh auth login"; exit 1; }

# 1) Novi build (workflow_dispatch na main)
before=$(gh run list -R "$REPO" -w "$WF" -L 1 --json databaseId -q '.[0].databaseId' 2>/dev/null || echo 0)
gh workflow run "$WF" -R "$REPO" --ref main
echo "Build pokrenut, cekam da se pojavi..."
run=""
for _ in $(seq 1 30); do
  sleep 4
  run=$(gh run list -R "$REPO" -w "$WF" -L 1 --json databaseId -q '.[0].databaseId')
  [ -n "$run" ] && [ "$run" != "$before" ] && break
done
[ -n "$run" ] && [ "$run" != "$before" ] || { echo "Build se nije pojavio"; exit 1; }
echo "Run $run: https://github.com/$REPO/actions/runs/$run"

# 2) Cekanje kraja (izlaz != 0 ako build padne)
gh run watch "$run" -R "$REPO" --exit-status --interval 15

# 3) APK
rm -rf "$OUT"; mkdir -p "$OUT"
gh run download "$run" -R "$REPO" -n Trening-apk -D "$OUT"
APK=$(ls "$OUT"/*.apk | head -1)
echo "APK: $APK"

# 4) Telefon: instalacija, pokretanje, screenshot
if [ $# -gt 0 ]; then
  [[ "$1" == *:* ]] && "$ADB" connect "$1" >/dev/null
  DEV="$1"
else
  DEV=$("$ADB" devices | awk 'NR>1 && $2=="device"{print $1; exit}')
fi
[ -n "$DEV" ] || { echo "Nema povezanog telefona (adb). APK je u $OUT"; exit 1; }
"$ADB" -s "$DEV" install -r "$APK"
"$ADB" -s "$DEV" shell am force-stop com.vukos.trening
"$ADB" -s "$DEV" shell am start -n com.vukos.trening/.MainActivity >/dev/null
sleep 4
"$ADB" -s "$DEV" exec-out screencap -p > "$OUT/screen.png"
echo "Verzija na telefonu: $("$ADB" -s "$DEV" shell dumpsys package com.vukos.trening | grep -m1 versionName | tr -d ' ')"
echo "Screenshot: $OUT/screen.png"

# 5) Screenshot svih ekrana
"$DIR/scripts/screens.sh" "$DEV" "$OUT/screens" || true
