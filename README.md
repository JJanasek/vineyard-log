# Vineyard Log

[![Android CI](https://github.com/JJanasek/vineyard-log/actions/workflows/android.yml/badge.svg)](https://github.com/JJanasek/vineyard-log/actions/workflows/android.yml)

Offline Android app for keeping a vineyard + cellar diary: what was sprayed and fertilised where,
phenology dates, ripeness readings, harvest, must preparation, fermentation checks, additions and
SO₂, plus daily weather with growing-degree-days. Everything is stored locally on the phone
(Room/SQLite); a JSON backup can be exported and re-imported from Settings.

Built with Kotlin, Jetpack Compose (Material 3), Room, Navigation Compose and DataStore.
Minimum Android 8.0 (API 26). UI in English and Czech (follows the phone language, or pick one in Settings).

New here? Read the [quick start](docs/quick-start.md) (English and Czech); the app shows the same three steps on first launch.

## What it tracks

| Tab | Contents |
| --- | --- |
| **Log** | One timeline of all entries, filterable by year and vineyard/cellar. |
| **Vineyard** | Blocks (parcels): variety, area, vines, rootstock. A season checklist with per-year tick-offs, a field guide (diseases, pests, deficiencies, frost/hail/sunburn) with photos, and a harvest-date estimate from the sugar trend. Per block and season: spray/fertiliser counts, phenology dates with GDD at that date, ripening curve (sugar/TA/pH), earliest harvest date from the pre-harvest interval (PHI) of the sprays used, harvest kg. |
| **Cellar** | Batches (wine lots): vintage, style, status, vessel, yeast, source blocks. Fermentation curve (sugar + temperature by day), latest readings, SO₂ additions, cellar log. |
| **Weather** | Daily min/max, rain, humidity, frost/hail flags, typed by hand, fetched from Open-Meteo for your coordinates, or measured by the nearest ČHMÚ stations. Season GDD (configurable base and season window), cumulative GDD chart, season comparison table. |
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

### Tests and CI

Unit tests cover the calculation-heavy parts that could give wrong-but-plausible advice: growing degree days, sugar/alcohol/SO₂/chaptalization maths, the small-vineyard unit conversions, the disease-risk thresholds (3-10 rule, oidium index, botrytis wet days), PHI-based earliest harvest, reminder scheduling, the variety catalogue lookup and the spray-programme product matching.

```bash
./gradlew testDebugUnitTest      # report in app/build/reports/tests/testDebugUnitTest
```

GitHub Actions (`.github/workflows/android.yml`) runs the tests and builds the debug APK on every push and pull request; the APK is attached to the workflow run as an artifact.

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
- **Import your old markdown diary**: `python3 tools/import_markdown_log.py /path/to/vinohrad-log -o old-log.json` converts the per-variety markdown files (harvest table, additives with shop links, dated steps) into a backup; load it with *Settings → Import backup (merge)*, which adds without deleting anything.
- **Vinařský dům range**: `python3 tools/vinarskydum_catalog_to_json.py -o vinarskydum-products.json` downloads the fungicide, insecticide, herbicide, vine-fertiliser and biological-control categories (one request per second; their terms carry no database clause) for *Settings → Import products (merge)*. Keep the file to yourself.
- **Lipera catalog PDF**: Lipera publishes an official product catalog on
  [lipera.cz/dokumenty-ke-stazeni](https://www.lipera.cz/dokumenty-ke-stazeni/). Download it and run

  ```bash
  python3 tools/lipera_catalog_to_json.py katalog.pdf -o lipera-products.json
  ```

  (needs `pdftotext` from poppler), copy the JSON to the phone and use *Settings → Import products
  (merge)*. Existing names are left untouched. The parser is heuristic; expect to fix a few
  categories and doses by hand. Keep the JSON to yourself, it is the vendor's content.

### Device-only extra photos

Material you may keep for yourself but not redistribute (for example the photo sheet of the BS vinařské potřeby spray-plan leaflet) goes into `app/src/main/assets/guide-extra/` with a `credits-extra.json`; the folder is git-ignored and the guide and growth-stage pages show the photos with their credit line. `tools/bs_leaflet_tiles.py <leaflet.pdf>` cuts the 2025 leaflet's page 1 into 30 topic photos (diseases, pests, N/P/K/Mg/S/Fe deficiencies) and writes that folder. Ask BS before publishing a build that contains them.

## Website and backup viewer

`web/` holds a static site generated from the same content the app ships with, deployed to GitHub Pages by `.github/workflows/pages.yml` on every push to `main` (enable *Settings → Pages → Source: GitHub Actions* once): field guide with the Commons photos, growth stages, variety catalogue, spray programme, cellar protocols, the docs, screenshots and download links, in Czech and English. `web/viewer/` is a backup viewer that runs entirely in the browser: open the JSON backup the app writes to your synced folder (drag and drop, file picker, a public link, or pasted text) and get the Overview (risk, Šteberla, season weather, GDD by vintage, ripening and fermentation charts, harvest table), the log with filters, blocks with PHI / copper / NPK, batches, weather and products; you can add simple entries and download the modified backup for *Import backup → merge* in the app. Nothing is uploaded anywhere.

Custom domain: put the domain on one line in `web/site/CNAME` (the build copies it to the site root), point DNS at GitHub Pages (apex `A` records 185.199.108.153, 185.199.109.153, 185.199.110.153, 185.199.111.153 plus the matching `AAAA` records, and `www` as a `CNAME` to `jjanasek.github.io`), then enter the domain under *Settings → Pages → Custom domain* and tick *Enforce HTTPS* once the certificate is issued. All site links are relative, so the pages work both at `/vineyard-log/` and at a domain root.

Room schemas: bumping the database version writes a new `app/schemas/<db>/<version>.json`. **Commit it** – the auto-migration for the next version needs the previous file, and a clean checkout (CI) fails with *Schema 'N.json' required for migration was not found* without it.

Data sources: `data/guide/Sources.kt` lists the references behind the variety catalogue (ÚKZÚS, VIVC, wineofczechrepublic.cz, Pavloušek, Kraus), the BBCH growth stages (Lorenz et al. 1995, JKI monograph), the field guide (Rostlinolékařský portál, BS articles, Ekovín), the risk models (Šteberla 1982, Kast OiDiag, Baldacci), the spray programme (BS 2025 leaflet, ÚKZÚS product register), the cellar templates (Steidl, BS, producer sheets) and the weather services, plus zákon č. 321/2004 Sb. §§ 17–19 for the °NM categories (`util/SugarGrades.kt`: 14 zemské, 15 jakostní, 19 kabinetní, 21 pozdní sběr, 24 výběr z hroznů, 27 výběr z bobulí/ledové/slámové, 32 výběr z cibéb). The variety entry carries a typical must-sugar range taken from the variety's Czech Wikipedia article (blank for the seven varieties whose article states none) - there is no official per-variety dataset, grapevine is exempt from the ÚKZÚS utility-value trials. It is orientation and drives nothing: the sugar a block is picked at is set on the block (`Block.targetNm`) or in Settings. They show in the app under *Settings → Data sources* and next to the variety picker, and on the site as a *Sources* page plus a section at the bottom of each content page.

Build locally: `./gradlew testDebugUnitTest --tests '*ContentExportTest'` (writes `app/build/site-content`), then `python3 web/build_site.py` (needs `jinja2`) and open `web/dist/index.html`. Tagging `v*` runs `.github/workflows/release.yml`, which builds a signed release APK and attaches it to a GitHub release; the site links to the latest release.

## Release signing

Android refuses to update an app when the new package is signed with a different key, and a debug
build is signed with whatever debug keystore the machine happens to have - a CI runner generates a
fresh one on every run. Releases therefore need one keystore that stays put. Create it once:

```
keytool -genkeypair -v -keystore vineyard-release.jks -storetype PKCS12 \
  -keyalg RSA -keysize 4096 -validity 10000 -alias vineyard
base64 -w0 vineyard-release.jks    # paste the output into the secret below
```

Keep `vineyard-release.jks` and its passwords somewhere safe and out of the repository. Losing it
means no future build can update an installed app. Then add four repository secrets under
*Settings → Secrets and variables → Actions*:

| Secret | Value |
| --- | --- |
| `RELEASE_KEYSTORE_BASE64` | the base64 of the keystore file |
| `RELEASE_KEYSTORE_PASSWORD` | the store password |
| `RELEASE_KEY_ALIAS` | `vineyard` |
| `RELEASE_KEY_PASSWORD` | the key password |

The release workflow fails with a clear message if the keystore secret is missing, and prints the
certificate fingerprint in the run summary - it has to read the same on every release.

To build a signed APK locally, pass the same values as gradle properties (`-Pvineyard.keystore=...`,
`vineyard.keystorePassword`, `vineyard.keyAlias`, `vineyard.keyPassword`) or the matching
`VINEYARD_*` environment variables. Without them `assembleRelease` produces an unsigned APK.

Release builds are minified and resource-shrunk, so the APK is about 10 MB against 30 MB for a debug
build. F-Droid is possible once this key is in place and the build is reproducible.

**Upgrading past the debug-signed releases:** v0.1.0 and v0.2.0 were signed with throwaway debug
keys, so the first properly signed release cannot install over them. Export a backup from Settings,
uninstall the app, install the new APK and import the backup. Updates after that install normally.

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

A Czech proposal for the hobby-grower direction (units, variety templates, forecasts, charts) is in [docs/navrh-hobby-vinar.md](docs/navrh-hobby-vinar.md). Its first item is done: Settings → Small-vineyard units lets you show areas in m², ares or ha and set your sprayer tank; spray entries take the litres of mix used and every product row shows the g/10 l concentration, the amount per tank fill and the total for the mix; fertiliser rows show grams per vine; block pages show kg per vine.

- ~~**Czech UI**~~ – done: all UI strings are resources (`values` / `values-cs`); Settings → Language switches between system default, English and Czech.
- ~~**Photos and a field guide**~~ – done: entries take photos from the gallery or camera (stored downscaled in app storage, not in the JSON backup); *Vineyard → book icon* opens a bilingual field guide with 20 diseases, pests, deficiencies and disorders, 14 of them with Wikimedia Commons photos (credits in `app/src/main/assets/guide/CREDITS.md`). Each guide page can start a scouting entry.
- ~~**Weather without typing**~~ – done for the data part: set the vineyard coordinates in Settings (type them, pick them on an OpenStreetMap map, or take the phone's location) and *Weather → Fetch from Open-Meteo* fills the selected year with daily min/max, rain and humidity (typed days are never overwritten; fetched rows show a cloud icon). Hardware options are compared in [docs/weather-stations-and-probes.md](docs/weather-stations-and-probes.md) (Ecowitt is the pragmatic pick, iSpindel/Tilt for must density); a weather CSV importer is the cheap next step.
- ~~**Season checklist**~~ – done: *Vineyard → checklist icon* opens the season plan, seeded with a Moravian vineyard year (23 tasks with month windows, some tied to phenology stages); tick tasks off per year, add your own, and log a task straight into an entry of the matching type. The Vineyard tab shows how many tasks are open this month.
- ~~**Vendor catalog import**~~ – done as offline tools: `tools/lipera_catalog_to_json.py` (official Lipera PDF) and `tools/vinarskydum_catalog_to_json.py` (Vinařský dům shop, 1 request/s) produce a products JSON that Settings → Import products loads. Still open: asking the vendors for an official feed.
- ~~**Disease risk flags from the weather data**~~ – done: the Open-Meteo fetch also stores per-day leaf-wetness hours and warm/hot hours; the Weather tab shows peronospora (3-10 primary rule, wet warm nights), an oidium index (Gubler-Thomas style) and a botrytis wet-period indicator with a 3-day outlook, and the Vineyard tab a compact version. Weather-only indicators, labelled as such; the Overview also draws the season's cumulative rain against the Šteberla curves from the 1982 SHMÚ paper (zone + treatment regime); how this relates to the Šteberla and Kast models used by Czech advisory services is in [docs/disease-models.md](docs/disease-models.md), and [docs/bs-articles.md](docs/bs-articles.md) lists the BS vinařské potřeby articles the advice was checked against.
- ~~**Season export (PDF / CSV)**~~ – done: Settings → Season export writes a printable PDF (blocks with sprays, doses and PHI, harvest, cellar steps per batch, weather summary) and CSV files of entries and weather.
- ~~**Multi-device backup through a folder you control**~~ – done: Settings → Backup folder; pick a subfolder inside your Syncthing / Nextcloud / Drive folder and the app writes vineyard-log-latest.json and vineyard-log-photos.zip on demand and automatically at start when the last backup is older than a day. Restore on another phone with Import backup (merge).
- ~~**Vineyard renewal**~~ – done: entry type *Renewal* (replanting gaps, regrafting, rejuvenation cut, grubbing up, new planting) with the number of vines; the block detail sums it up (total renewed, share of the block, young vines from the last 3 years, per-year lines) and the block list shows "renewed N %". The season plan seeds a spring task for it.
- ~~**Small-vineyard units everywhere**~~ – done: with the area unit set to ares or m², the log and entry detail show spray doses as g per 10 l of mix and per are (e.g. `100 g/10 l · 40 g/a` instead of `4 kg/ha`), fertiliser quantities per are and the water volume per are; the detail keeps the per-hectare figure in brackets. Exports stay per hectare.
- ~~**Variety catalogue and prefill**~~ – done: 31 varieties grown in Moravia (`data/varieties/Varieties.kt`, PIWI flagged) with colour, ripening class, usual harvest window, target °NM and disease sensitivity. Block edit offers the catalogue (free text still allowed), block detail shows the harvest window and uses the variety's target °NM for the ripeness forecast, a new batch from a block inherits variety, colour and name, the Vineyard tab names your sensitive varieties when the risk is high, and "Restore defaults" in the season plan adds ripeness-check tasks per planted variety.
- ~~**Reminders / notifications**~~ – done: *Weather → bell icon* manages reminders (once / daily / weekly on a weekday / every N days, time, from–until, optional entry type, block or batch); templates for control sampling every Monday, vineyard check every 3 days and daily fermentation checks. Alarms use AlarmManager (exact when allowed, otherwise inexact), survive reboot and app update, and tapping the notification opens a prefilled new entry. Saving a spray with products that have a PHI adds a one-off "PHI over" reminder (Settings toggle). The Weather tab lists the next three reminders.
- ~~**ČHMÚ station data**~~ – done: Settings → ČHMÚ stations finds gauges and automated stations within 40 km of the vineyard; Weather → Fetch from ČHMÚ loads the year (current year from `recent`, older from `historical`), measured values override Open-Meteo rows while keeping their hourly aggregates, typed days stay. Rain follows the 07–07 climatological day. Past years come from the station's whole-history file (13 MB for a rain gauge, 60–100 MB for a climatological station), which is streamed and filtered on the fly with a download counter shown under the button.
- ~~**Prefill from planted varieties**~~ – done (see the variety catalogue above); cellar protocol templates per variety colour are still open.
- ~~**Overview tab**~~ – done: the fourth tab is now *Overview* (Weather moved behind its sun icon). Top: disease risk with your sensitive varieties, the next reminders and open plan tasks. Then charts and tables split by where the readings come from: **Vineyard** – season weather (T max/min, rain bars, spray markers), cumulative GDD by vintage, ripening and acids from vineyard readings per block with the variety targets, harvest by year (kg, kg/vine, °NM); **Cellar** – fermentation sugar and temperature per batch of the vintage (days since start), latest analyses per batch; **Soil** – latest soil analysis per block.
- ~~**Automatic alerts**~~ – done: a WorkManager job runs once a day (first run around 6:30) and posts alerts on a separate notification channel: high disease risk after refreshing the last two weeks of Open-Meteo data (in season, at most every 3 days per disease), season-plan tasks whose window starts or ends this month (once a month), fermenting batches whose sugar stopped dropping or without a reading for 4 days, and blocks whose sugar trend reaches the variety target within 10 days (a sampling reminder is set two days before the estimate). Each alert has a switch in Settings, plus *Run checks now*.
- **Own photos and attachments** – guide topics can take your own photos (labelled gallery that grows over the seasons); PDF attachments on entries (soil analysis, invoices, lab reports); later a small on-device classifier for the common diseases and deficiencies.
- ~~**Cellar protocol templates**~~ – done: batch page → checklist icon → *Plan protocol*: aromatic white without MLF, rosé (short maceration) or red (maceration + MLF), with a start date. Each step becomes a dated reminder with hobby-sized instructions (g/hl and per 10 l); the batch page shows the checklist (a step is ticked when an entry of its type exists), *Log* opens the entry prefilled with title and instructions, and the Reminders screen groups the steps per batch.
- ~~**Own photos and attachments**~~ – done: entries take PDF and other files (lab reports, invoices, scans) stored in the app and included in the folder-backup zip; observations logged from a guide topic remember the topic, so the topic page and the growth-stage page show *Your photos* from your own entries.
- ~~**Rain gauge and protected weather edits**~~ – done: *Weather → Rain gauge* takes a total for a period (a week, say) and spreads it over the days in proportion to the fetched rain, marking them so the fetches never overwrite them; hand-editing any day turns it into a typed day that is kept as well.
- ~~**Fertiliser rules**~~ – done: products carry an NPK analysis and an application interval in years; the fertilisation form shows the N-P₂O₅-K₂O of the application and warns when a product was used on the block more recently than its interval, the block season card sums N-P-K per hectare, and PHI on fertilisers (foliar products) counts for the earliest-harvest date and the PHI reminder like sprays.
- ~~**Frost alert**~~ – done: between 20 March and 31 May the daily check looks two days ahead in the Open-Meteo forecast and posts an alert for nights at or below 1 °C (once per night), opening the guide entry on frost with the BS articles on variety tolerance.
- ~~**Tank-mix check and rotation**~~ – done: the spray form warns about fosetyl-Al with sulphur, SC formulations or ammonium nitrogen, reminds you not to mix concentrates and to add sulphur to every in-season mix (rules from the BS leaflet), and flags the same active ingredient as in the previous spray within 35 days (sulphur and copper excluded). A spraying-conditions card under the temperature/wind fields (or the day's maximum from the weather table when nothing is typed) warns when sulphur or oils would scorch in the heat, when sulphur is too cold to work, and when wind is over 5 m/s; the water-per-hectare field only appears for label doses per hectare. Spray and fertilisation entries carry a time of day; the check then uses the Open-Meteo hourly temperature at that time (mornings are cooler than the daily maximum), and a *Fill from weather* button copies it into the entry. The product field opens a searchable list (name, active ingredient, purpose, without diacritics).
- ~~**Copper budget**~~ – done: products carry a copper content (g Cu per kg/l; parsed from the active-ingredient text or a table of common products when empty) and the block's season card shows "Cu this season: x of 4 kg/ha", red from 3 kg.
- ~~**Sampling protocol**~~ – done: the ripeness entry has the BS sampling checklist (every fifth vine, 10 berries per vine from several bunches, shaded and sunny side, about 0.5 kg) with a button that writes the ticked items into the notes.
- ~~**Yeast nutrition plan**~~ – done: Cellar → calculators: volume and °NM give the three doses in grams (20 / 15 / 25–30 g/hl), the YAN target, and one tap creates three dated reminders on a batch.
- ~~**Guide thumbnails and links**~~ – done: topics without a Commons photo use a device-only extra photo or a kind icon; topics link to the relevant BS vinařské potřeby articles.
- ~~**Growth stages with photos**~~ – done: 12 BBCH stages from wool stage to leaf fall (`data/guide/Phenology.kt`) with a Commons photo, "what you see", "what it means for the work" and the usual Moravian timing; *Field guide → Growth stages* shows them and can log a stage, and the phenology entry form picks the stage from photo cards instead of a list. The spray programme windows point at the finer stages (shoots 3–10 cm, inflorescences visible, pea size).
- ~~**Spray programme skeleton**~~ – done: Vineyard → spray-bottle icon: eight windows of the Moravian season (Phomopsis at bud break, before and after flowering, pea size, bunch closure, veraison, before harvest, after harvest) with BBCH, typical date, your logged phenology date, the target diseases, advice, the products from your catalogue that fit each target (with PHI) and what you already sprayed in that window; *Log spray* and *Remind me* buttons.
- **Weather from your own station** – CSV import (Ecowitt, Meteostanice exports) and, later, direct readers for iSpindel / Tilt density sensors.
- **Cellar calculators and forecasts** – done: sugar units and potential alcohol, chaptalization (your kg/hl/°NM factor), SO₂ from pH, acid adjustment, YAN target; days-to-dry and stuck-fermentation warning on batches; harvest date estimate from the sugar trend on blocks.
- **Diagnostics** – cross-vintage views: GDD vs harvest sugar, spray count vs disease incidence, YAN vs fermentation length, per-block spray cost and seasonal Cu/S totals.

## License

MIT, see [LICENSE](LICENSE), for the code. The field-guide photos under `app/src/main/assets/guide/` are Wikimedia Commons images under their own CC licences, listed in `CREDITS.md` there. Personal, open-source project; no affiliation with the suppliers mentioned.
