#!/usr/bin/env bash
# Prolazi kroz sve ekrane aplikacije na telefonu i pravi screenshot svakog.
# Usage: scripts/screens.sh <adb-serial> [izlazni folder]
# Dugmad trazi po tekstu / opisu (srpski ili engleski), ne po koordinatama.
# Ekrani merenja i dana merenja imaju FLAG_SECURE (zastita slika tela): screenshot je crn, to je namerno.
set -uo pipefail
DEV="$1"
OUT="${2:-$(cd "$(dirname "$0")/.." && pwd)/build/screens}"
ADB="${ADB:-$HOME/opt/android-sdk/platform-tools/adb} -s $DEV"
PKG=com.vukos.trening
rm -rf "$OUT"; mkdir -p "$OUT"
n=0

shot() {   # shot <ime>
  $ADB shell cmd statusbar collapse >/dev/null 2>&1
  sleep 1.2
  n=$((n + 1))
  local f; f=$(printf "%s/%02d-%s.png" "$OUT" "$n" "$1")
  $ADB exec-out screencap -p > "$f" && echo "  $f"
}

# tap <regex teksta ili content-desc>: dodirne prvi element koji odgovara; 1 ako ga nema
tap() {
  $ADB shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  local xy
  xy=$($ADB shell cat /sdcard/ui.xml | python3 -c '
import re, sys
want = re.compile(sys.argv[1], re.I)
last = len(sys.argv) > 2   # tab: najnizi pogodak na ekranu (donja navigacija)
x = sys.stdin.read()
best = None
for m in re.finditer(r"<node [^>]*>", x):
    n = m.group(0)
    t = (re.search(r" text=\"([^\"]*)\"", n) or [None, ""])[1]
    d = (re.search(r" content-desc=\"([^\"]*)\"", n) or [None, ""])[1]
    if "bounds=" not in n: continue
    if (t and want.fullmatch(t.strip())) or (d and want.fullmatch(d.strip())):
        a, b, c, e = map(int, re.search(r"bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", n).groups())
        best = ((a + c) // 2, (b + e) // 2)
        if not last: break
if best: print(*best)
' "$@")
  [ -n "$xy" ] || { echo "  (nema: $1)"; return 1; }
  $ADB shell input tap $xy
}

back()   { $ADB shell input keyevent 4; sleep 0.8; }
scroll() { $ADB shell input swipe 540 1800 540 700 350; }
tab()    { front; tap "$1" last; sleep 0.8; }
# ako je aplikacija izgubila fokus (npr. visak "nazad"), vrati je
front()  { $ADB shell dumpsys window | grep -q "mCurrentFocus=.*$PKG" || { $ADB shell am start -n $PKG/.MainActivity >/dev/null; sleep 2; }; }
top()    { $ADB shell input swipe 540 900 540 2000 150; $ADB shell input swipe 540 900 540 2000 150; $ADB shell cmd statusbar collapse >/dev/null 2>&1; sleep 0.5; }

# Ekran mora biti upaljen i otkljucan (PIN se ne moze uneti preko adb-a).
$ADB shell input keyevent KEYCODE_WAKEUP; sleep 1
if $ADB shell dumpsys window | grep -q "isKeyguardShowing=true"; then
  echo "Telefon je zakljucan. Otkljucaj ga (i ostavi ekran upaljen) pa pokreni ponovo."
  exit 2
fi
$ADB shell am force-stop $PKG
$ADB shell am start -n $PKG/.MainActivity >/dev/null
sleep 3

echo "Dnevnik"
tab "Dnevnik|Journal";             shot dnevnik
scroll;                             shot dnevnik-dole
scroll;                             shot dnevnik-kraj
tap "Statistika|Statistics" &&    { shot statistika; scroll; shot statistika-dole; scroll; shot statistika-kraj; back; }
tab "Dnevnik|Journal"; scroll; scroll
tap "Podsetnici|Reminders" &&     { shot podsetnici; tap "Uputstvo|Help" && { shot podsetnici-info; back; }; back; }

echo "Trening"
tab "Trening|Workout";             top; shot trening
scroll;                             shot trening-dole
top
tap "Planovi iz Notiona|Plans from Notion" && { shot notion-planovi; back; }
tap "Podešavanja|Settings" &&     { shot podesavanja; scroll; shot podesavanja-dole; scroll; shot podesavanja-kraj; back; }
tap "Novi plan|New plan|Napravi plan|Create plan" && { shot novi-plan; back; }
tab "Trening|Workout"; top
# Slobodan trening se odmah odbacuje (ne ostaje u istoriji)
if tap "Slobodan trening|Free workout"; then
  sleep 1; shot aktivni-trening
  tap "Dodaj vežbu|Add exercise" && { shot izbor-vezbi; back; }
  tap "Još|More" && tap "Odbaci trening|Discard workout" && tap "Odbaci|Discard"
  sleep 1
fi

echo "Profil"
tab "Profil|Profile";              top; shot profil
scroll;                             shot profil-dole
scroll;                             shot profil-kraj
tap "(Ponedeljak|Utorak|Sreda|Četvrtak|Petak|Subota|Nedelja|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday), .*" && { shot dan-merenja-FLAG_SECURE; back; }
top
tap "Uporedi slike|Compare photos" && { shot uporedi-slike; back; }
tap "Grafikon kilaže|Weight chart" && { shot grafikon-kilaze; back; }
top
tap "Unesi merenje|Log check-in" && { shot merenje-FLAG_SECURE; back; }

tab "Dnevnik|Journal"
echo "Gotovo: $n screenshotova u $OUT"
