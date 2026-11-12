# Trening (lična Android aplikacija)

Logovanje treninga po uzoru na Hevy: planovi po danima, baza vežbi sa slikama, kilaže, grafovi napretka, kalendar treninga, ishrana, kilaža tela i slike tela. Podaci se čuvaju lokalno (Room) i sinhronizuju sa Notionom.

## Build

```bash
./install.sh            # build + instalacija na povezan telefon (USB ili adb wireless)
```
Toolchain: `~/opt/jdk` (17), `~/opt/android-sdk` (platform 34), `~/opt/gradle-8.7`.
Testovi: `gradle testDebugUnitTest`. Test sa pravim Notionom (samo čitanje) radi samo uz `NOTION_TOKEN_FILE=~/opt/keys/notion-trening.token`, inače se preskače.

## Notion (stranica „Trening")

Baze: Exercises, Routines, Workouts (Kalendar prikaz), Sets, Body, Food.
ID-jevi baza nisu u repozitorijumu (lokalni fajl `notion.local.md`, u aplikaciji se unose u podešavanjima).

Svaka baza ima `AppId` (ID reda u aplikaciji) za uparivanje.
**Notion treninzi:** red u Routines bez `AppId` je plan napisan u Notionu. Aplikacija ga prikazuje sa dugmetom „Dodaj", a posle uvoza upisuje `AppId` i plan postaje „Dodato ✓". Format vežbi (polje Plan ili telo stranice), jedna po redu: `Dumbbell Bench Press 3x8-12 [20kg]`.

## Faze

| # | Faza | Status |
|---|---|---|
| 0 | Notion baze + Kalendar prikaz | ✅ |
| 1 | Kostur, Room (ceo model), 876 vežbi, pretraga + filteri, detalji, nova vežba | ✅ |
| 2 | Telo: kilaža jednom sedmično (termin nedelja), slike svake druge sedmice (od 15.11.2026), crveno ako se propusti, notifikacije, 3 šifrovane slike (spreda/bok/leđa) | ✅ |
| 3 | Planovi po danima + Notion treninzi (uvoz) + Notion token u podešavanjima | ✅ |
| 4 | Logovanje treninga (Prethodno / kg / reps / ✓), trening u toku, istorija | ✅ |
| 5 | ⏳ Dnevnik: kalendar treninga + ishrana po danima + uvidi | |
| 6 | Statistika: grafovi po vežbi (težina, 1RM, volumen) + grafikon kilaže | |
| 7 | Puna sinhronizacija sa Notionom (slanje, povlačenje, restore, upload slika) | |

Pravila za podsetnike su u `data/CheckInSchedule.kt`: merenje jednom sedmično, termin je nedelja (kilaža), a svake druge sedmice i slike (15.11, 29.11, ...). U nedelju je to običan podsetnik, a ako se propusti, od ponedeljka je crveno dok se ne unese.

## Privatnost slika tela

Slike (spreda, bok, leđa) se čuvaju samo u internom skladištu aplikacije (`files/body_photos`), šifrovane AES-256-GCM ključem iz Android Keystore-a. Ne idu u galeriju, Android backup (`allowBackup=false`) ni Notion (tamo je samo oznaka „Photos taken"). Ekran merenja ima FLAG_SECURE (bez screenshota i pregleda u nedavnim aplikacijama). Posledica: ako se aplikacija deinstalira ili se telefon izgubi, slike se ne mogu vratiti.
