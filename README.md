# Vineyard Log

Offline Android app for keeping a vineyard + cellar diary: what was sprayed and fertilised where,
phenology dates, ripeness readings, harvest, must preparation, fermentation checks, additions and
SO₂, plus daily weather with growing-degree-days. Everything is stored locally on the phone
(Room/SQLite); a JSON backup can be exported and re-imported from Settings.

Built with Kotlin, Jetpack Compose (Material 3), Room, Navigation Compose and DataStore.
Minimum Android 8.0 (API 26). UI in English and Czech (follows the phone language, or pick one in Settings).

## What it tracks

| Tab | Contents |
| --- | --- |
| **Log** | One timeline of all entries, filterable by year and vineyard/cellar. |
| **Vineyard** | Blocks (parcels): variety, area, vines, rootstock. A season checklist with per-year tick-offs and a field guide (diseases, pests, deficiencies, frost/hail/sunburn) with photos. Per block and season: spray/fertiliser counts, phenology dates with GDD at that date, ripening curve (sugar/TA/pH), earliest harvest date from the pre-harvest interval (PHI) of the sprays used, harvest kg. |
| **Cellar** | Batches (wine lots): vintage, style, status, vessel, yeast, source blocks. Fermentation curve (sugar + temperature by day), latest readings, SO₂ additions, cellar log. |
| **Weather** | Daily min/max, rain, humidity, frost/hail flags, typed by hand or fetched from Open-Meteo for your coordinates. Season GDD (configurable base and season window), cumulative GDD chart, season comparison table. |
| **Products** | Your catalog of sprays, fertilisers, yeasts, nutrients, enzymes... with supplier (Lipera, Vinařský dům, other), label dose range, PHI, purpose and product URL. Entries pick products from here so doses are pre-filled. |

Entry types cover: spray, fertilisation, canopy work, soil work, phenology stage, scouting,
ripeness check, harvest, weather event; must preparation, yeast pitch, nutrient addition,
other addition, fermentation check, analysis, racking, SO₂ addition, fining/filtration,
malolactic, tasting note, bottling. Every entry can carry photos, products (with dose and total used)
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

## Product catalog from the suppliers

Both suppliers' terms ask you not to bulk-copy their product databases, so the app does not
scrape them. Two lighter ways are built in:

- **Fill from a product page**: in *Products → +*, paste (or share from the browser) the URL of a
  product on lipera.cz or vinarskydum.cz and tap the download arrow. The app reads that one page
  and pre-fills name, supplier, category, package, price, dose range, PHI and links to technical
  sheets. Check the dose against the label before trusting it.
- **Lipera catalog PDF**: Lipera publishes an official product catalog on
  [lipera.cz/dokumenty-ke-stazeni](https://www.lipera.cz/dokumenty-ke-stazeni/). Download it and run

  ```bash
  python3 tools/lipera_catalog_to_json.py katalog.pdf -o lipera-products.json
  ```

  (needs `pdftotext` from poppler), copy the JSON to the phone and use *Settings → Import products
  (merge)*. Existing names are left untouched. The parser is heuristic; expect to fix a few
  categories and doses by hand. Keep the JSON to yourself, it is the vendor's content.

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
  data/web        ProductPageFetcher – reads one product page on request (jsoup)
tools/            lipera_catalog_to_json.py – desktop helper for the official catalog PDF
```

## Roadmap / ideas

- ~~**Czech UI**~~ – done: all UI strings are resources (`values` / `values-cs`); Settings → Language switches between system default, English and Czech.
- ~~**Photos and a field guide**~~ – done: entries take photos from the gallery or camera (stored downscaled in app storage, not in the JSON backup); *Vineyard → book icon* opens a bilingual field guide with 20 diseases, pests, deficiencies and disorders, 14 of them with Wikimedia Commons photos (credits in `app/src/main/assets/guide/CREDITS.md`). Each guide page can start a scouting entry.
- ~~**Weather without typing**~~ – done for the data part: set the vineyard coordinates in Settings and *Weather → Fetch from Open-Meteo* fills the selected year with daily min/max, rain and humidity (typed days are never overwritten; fetched rows show a cloud icon). Hardware options are compared in [docs/weather-stations-and-probes.md](docs/weather-stations-and-probes.md) (Ecowitt is the pragmatic pick, iSpindel/Tilt for must density); a weather CSV importer is the cheap next step.
- ~~**Season checklist**~~ – done: *Vineyard → checklist icon* opens the season plan, seeded with a Moravian vineyard year (23 tasks with month windows, some tied to phenology stages); tick tasks off per year, add your own, and log a task straight into an entry of the matching type. The Vineyard tab shows how many tasks are open this month.
- **Vendor catalog import** – one-off import of the enology / plant-protection ranges from Lipera and Vinařský dům (both Shoptet shops with public sitemaps) into the product table, refreshed manually a few times a year.
- **Disease risk flags from the weather data** – peronospora primary-infection rule (10 °C / 10 mm / 10 cm shoots) and secondary-infection windows from temperature and leaf wetness (rain or RH > 90 %), plus an oidium index of the Gubler-Thomas type from hourly temperatures; Open-Meteo already serves hourly values, so no hardware is needed for a first version. Shows "spray risk: high" on the Weather and Vineyard tabs instead of raw numbers.
- **Season export (PDF / CSV)** – a printable season log per block (sprays with products, doses, PHI, harvest, weather summary) for a co-op, buyer or certification body, and a CSV of entries and weather.
- **Multi-device backup through a folder you control** – write the JSON backup (and later photos) into a folder synced by Syncthing, Nextcloud or a Google Drive folder, with automatic daily export; no server to run or pay for.
- **Photo intelligence** – once there are real scouting photos: a labelled reference gallery first, later a small on-device classifier for the common diseases and deficiencies.
- **Diagnostics** – cross-vintage views: GDD vs harvest sugar, spray count vs disease incidence, YAN vs fermentation length, per-block spray cost and seasonal Cu/S totals.

## License

MIT, see [LICENSE](LICENSE), for the code. The field-guide photos under `app/src/main/assets/guide/` are Wikimedia Commons images under their own CC licences, listed in `CREDITS.md` there. Personal, open-source project; no affiliation with the suppliers mentioned.
