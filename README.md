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

### Emulator (optional)

An API 36 x86_64 image and the emulator are installed in the SDK. On this Fedora machine the
emulator only runs with the ANGLE renderer and without the desktop display variables:

```bash
unset DISPLAY WAYLAND_DISPLAY
~/Android/Sdk/emulator/emulator -avd vlog36 -no-window -no-audio -gpu angle_indirect -accel on -no-snapshot &
~/Android/Sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Drop `-no-window` to get a visible window.

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

## Roadmap / ideas

- **Czech UI** – move strings to resources and add `values-cs`.
- **Photos and a field guide** – attach photos to entries (disease pressure, deficiency symptoms), and a built-in reference of common vine diseases (peronospora, oidium, botrytis, black rot, esca, phomopsis) and nutrient deficiencies (N, K, Mg, Fe chlorosis, B, Zn) with symptom descriptions and example images.
- **Weather without typing** – first pull daily min/max/rain for the vineyard's GPS position from a free API (Open-Meteo or ČHMÚ open data); later connect a station or fermentation probe (research which hardware has an open/local API: Ecowitt gateways, WeatherFlow Tempest, iSpindel / Tilt for must density, WeeWX-compatible stations).
- **Season checklist** – a yearly to-do template (pruning, tying, shoot thinning, first spray at 10 cm shoots, leaf pulling, netting, harvest prep) with per-year tick-offs, optionally tied to phenology stages.
- **Vendor catalog import** – one-off import of the enology / plant-protection ranges from Lipera and Vinařský dům (both Shoptet shops with public sitemaps) into the product table, refreshed manually a few times a year.
- **Diagnostics** – cross-vintage views: GDD vs harvest sugar, spray count vs disease incidence, YAN vs fermentation length, per-block spray cost and seasonal Cu/S totals.
