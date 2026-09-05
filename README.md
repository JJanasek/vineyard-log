# Vineyard Log

Offline Android app for keeping a vineyard + cellar diary: what was sprayed and fertilised where,
phenology dates, ripeness readings, harvest, must preparation, fermentation checks, additions and
SO₂, plus daily weather with growing-degree-days. Everything is stored locally on the phone
(Room/SQLite); a JSON backup can be exported and re-imported from Settings.

Built with Kotlin, Jetpack Compose (Material 3), Room, Navigation Compose and DataStore.
Minimum Android 8.0 (API 26).

## What it tracks

| Tab | Contents |
| --- | --- |
| **Log** | One timeline of all entries, filterable by year and vineyard/cellar. |
| **Vineyard** | Blocks (parcels): variety, area, vines, rootstock. Per block and season: spray/fertiliser counts, phenology dates with GDD at that date, ripening curve (sugar/TA/pH), earliest harvest date from the pre-harvest interval (PHI) of the sprays used, harvest kg. |
| **Cellar** | Batches (wine lots): vintage, style, status, vessel, yeast, source blocks. Fermentation curve (sugar + temperature by day), latest readings, SO₂ additions, cellar log. |
| **Weather** | Daily min/max, rain, humidity, frost/hail flags. Season GDD (configurable base and season window), cumulative GDD chart, season comparison table. |
| **Products** | Your catalog of sprays, fertilisers, yeasts, nutrients, enzymes... with supplier (Lipera, Vinařský dům, other), label dose range, PHI, purpose and product URL. Entries pick products from here so doses are pre-filled. |

Entry types cover: spray, fertilisation, canopy work, soil work, phenology stage, scouting,
ripeness check, harvest, weather event; must preparation, yeast pitch, nutrient addition,
other addition, fermentation check, analysis, racking, SO₂ addition, fining/filtration,
malolactic, tasting note, bottling. Every entry can carry products (with dose and total used)
and numeric measurements (°Bx / °NM / °Oe, TA, pH, YAN, temperature, SG, free/total SO₂,
alcohol, residual sugar, malic, VA, soil analysis...).

## Build

Prerequisites: JDK 17 and the Android SDK (platform 36, build-tools 36). On this machine they
live in `~/.local/share/jdk/jdk-17…` and `~/Android/Sdk`; `local.properties` points at the SDK
and `~/.gradle/gradle.properties` sets `org.gradle.java.home`.

```bash
./gradlew assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```

Install on a phone with USB debugging enabled:

```bash
~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or copy the APK to the phone and open it (allow installing from unknown sources).

`./gradlew assembleRelease` produces a minified, unsigned APK; sign it with your own keystore
before distributing.

## Project layout

```
app/src/main/java/cz/janek/vineyardlog/
  data/model      Room entities + enums (Block, Product, Batch, LogEntry, ProductUsage, Measurement, WeatherDay)
  data/dao        DAOs, including EntryDao.save() and BackupDao.replaceAll()
  data/db         AppDatabase (seeds the starter product catalog on first run)
  data/seed       SeedData – starter catalog, adjust to your real products
  data/settings   DataStore-backed settings (GDD base, season window, defaults)
  data/backup     JSON backup format (kotlinx.serialization)
  util            Dates, GDD maths
  ui/nav          Bottom tabs and NavHost routes
  ui/components   Shared fields, dialogs, line chart
  ui/<feature>    Screens with their ViewModels
```

## Ideas for later

- Import a weather CSV (or pull from a nearby station) instead of typing days by hand.
- Per-block spray cost and Cu/S season totals.
- Photos on entries.
- Czech UI strings.
