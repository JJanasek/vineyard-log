# Weather stations and probes for the vineyard and cellar

Research notes for the "connect real sensors" roadmap item (September 2026). The question was:
which hardware is the most open and the most portable, and how would it feed Vineyard Log.

## 0. Before buying anything: free data

| Source | What you get | Openness | Notes |
| --- | --- | --- | --- |
| **Open-Meteo** (open-meteo.com) | Daily min/max, rain, humidity, ET₀ for any coordinates; archive back to 1940 (ERA5/DWD models), forecast 16 days | Free, no key, CC BY 4.0 | Interpolated model data at ~1–9 km resolution, not your slope. Good enough for GDD and seasonal comparison. **Built into the app** (Weather → fetch). |
| **ČHMÚ open data** (opendata.chmi.cz) | Czech station measurements (10-min / daily), incl. stations around Mikulov, Velké Pavlovice, Znojmo | CC BY 4.0 | Real measurements but the nearest station may be 10–20 km away. Candidate for a later importer. |
| **Amateur networks** (Netatmo Weathermap, Wunderground, Meteostanice.cz) | Neighbours' stations | Cloud, per-network terms | Useful to find out whether a neighbour already runs a station you could copy from. |

Recommendation: use Open-Meteo for the seasonal picture and only buy a station if you want
leaf wetness, frost alarms or the micro-climate of a specific block.

## 1. Vineyard weather stations

| Station | Price (approx.) | Data access | Open-source friendliness | Portability / power | Verdict |
| --- | --- | --- | --- | --- | --- |
| **Ecowitt** WS69 / WS90 (Wittboy) array + GW2000 / GW1200 gateway | 3–7 k Kč | Local HTTP/JSON on your Wi-Fi, "custom server" push (Ecowitt/Wunderground protocol), plus cloud | Best supported by open-source software: WeeWX, Home Assistant, ecowitt2mqtt. Firmware closed, protocol documented. | Sensor array is solar + AA battery; gateway needs USB power and Wi-Fi. Add-ons: **WN35 leaf wetness**, WH51 soil moisture, WN34 soil/water temperature, WH31 extra temp/humidity in the canopy | **Best value for a small vineyard.** Everything the disease models need (leaf wetness, RH, rain, temperature) for the price of two sprays. |
| **WeatherFlow Tempest** | ~9–12 k Kč | Local UDP broadcast of every reading + cloud REST API | Local UDP is fully documented; used by WeeWX/HA. No moving parts (haptic rain, sonic wind). | Solar powered, one unit, needs a Wi-Fi hub nearby | Nicest hardware, rain sensor less precise than a tipping bucket; no leaf-wetness option. |
| **Davis Vantage Vue / Pro2** + WeatherLink Live | 12–30 k Kč | WeatherLink Live: local HTTP API; console has serial | Industry standard, extremely robust; local API documented, cloud subscription for history | Sensor suite solar/battery, Live hub needs power | Pro choice; pay for reliability over ten years. Leaf wetness only with the Pro2 add-on stations. |
| **Barani MeteoHelix / MeteoWind** (Slovak, IoT) | 15–25 k Kč | LoRaWAN / Sigfox / NB-IoT to cloud, API export | Pro-grade, WMO-style radiation shield; cloud first | **Fully autonomous** (solar + LoRa), works with no Wi-Fi or mains in the vineyard | The right answer for a remote plot without power; overkill for a hobby vineyard. |
| **Netatmo, Bresser, Sencor, cheap "Wi-Fi" stations** | 2–6 k Kč | Cloud app only (Netatmo has an official API; the others usually nothing) | Poor | Fine | Avoid if you want the data in your own app. |
| **DIY: ESP32 + ESPHome / Arduino** with BME280, tipping bucket, leaf-wetness plate; or **Raspberry Pi + WeeWX** | 1–3 k Kč parts | Whatever you build (MQTT, HTTP) | 100 % open | Battery + solar possible, LoRaWAN (The Things Network) for remote plots | Most open and cheapest, but a project in itself; only if you enjoy tinkering. |

"Most open source and portable" summarised:

- **Most open**: DIY ESP32/ESPHome or Raspberry Pi + WeeWX. Everything is yours.
- **Most open among ready-made**: Ecowitt (documented local protocol, huge open-source ecosystem),
  then WeatherFlow Tempest (documented local UDP).
- **Most portable/autonomous**: Barani MeteoHelix (LoRa/Sigfox, solar); among hobby units the
  Ecowitt sensor array runs for a year on batteries, only the gateway needs power and Wi-Fi.

## 2. Cellar: must density and temperature

| Probe | Price | Data access | Openness | Notes |
| --- | --- | --- | --- | --- |
| **iSpindel** | ~0.5–1 k Kč (DIY) or ~2 k Kč assembled | Wi-Fi → HTTP/MQTT/TCP to your own server or to services like Ubidots/Brewfather | Fully open source (hardware + firmware) | Floating tilt hydrometer: density + temperature every few minutes. Needs an open vessel; skins/cap in red must interfere, fine for whites in tank. |
| **Tilt Hydrometer** | ~3–4 k Kč | Bluetooth LE beacon, documented format; phone app; Tilt Pi bridge | Closed hardware, open protocol | Plug-and-play; an Android app can read the BLE beacon directly (needs Bluetooth permission). |
| **RAPT Pill** (KegLand) | ~2.5 k Kč | Wi-Fi to RAPT cloud (+ BLE) | Closed, cloud API | Rechargeable; cloud dependency. |
| **Inkbird IBS-TH2 / Govee H5075** | ~0.5 k Kč | BLE temperature/humidity logger | Closed, BLE protocol reverse-engineered (openly used by HA) | Cheapest way to log fermentation temperature on a tank wall or in the cellar. |
| **Plaato Airlock** | ~3 k Kč | Cloud | Closed | Measures CO₂ bubbles; interesting for fermentation kinetics, not density. |

For a white-wine cellar with tanks the **iSpindel** (open) or **Tilt** (easy) plus an Inkbird
for cellar temperature covers 90 % of the value.

## 3. How this reaches Vineyard Log

1. **Now**: Open-Meteo fetch for daily weather (done); manual entry for probe readings.
2. **Next, cheap**: CSV import for weather days. Every station ecosystem (Ecowitt app,
   WeatherLink, WeeWX, Home Assistant) exports CSV; one importer covers all of them.
3. **Later, direct**:
   - Ecowitt gateway on the same Wi-Fi: poll `http://<gateway>/get_livedata_info` and aggregate
     to daily min/max/rain; or let the gateway push to a tiny endpoint.
   - Tilt / Inkbird over BLE from the phone: scan for the beacon, store density + temperature as
     measurements on the active batch.
   - iSpindel: point it at an MQTT broker or simple HTTP endpoint; the app reads from there.
4. **Leaf wetness + temperature** from an Ecowitt WN35 would make a real downy-mildew infection
   model possible (e.g. simplified "3-10" rule alerts) – the best reason to buy the station.

## 4. Practical tips for placement

- Put the station in the vineyard, not by the house: 1.5–2 m above ground in the row, away from
  the windbreak, rain gauge level and clear of the canopy drip.
- One extra temperature/humidity sensor inside the canopy at bunch height gives the numbers the
  disease models actually use.
- For frost: a sensor at 0.5 m in the lowest part of the vineyard, with an alarm at +1 °C.
