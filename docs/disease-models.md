# Disease models: what the app computes and what the Czech advisory services use

Written 6 September 2026 after looking at the weekly *Situační zpráva o výskytu patogenů révy vinné* published by BS vinařské potřeby and the signalisation pages of amet.cz.

## What BS vinařské potřeby publish

Every week in season a 7-page PDF (see `/clanky/detail/ochrana-revy-vinne-NN-tyden…`): state of vegetation with BBCH and photos of varieties, protection measures, product tips split into *profi / hobby / eko*, a per-station table of infection pressure (weak / medium / strong) for downy mildew, powdery mildew and botrytis for ~25 stations from Slovácko to Znojemsko and Bohemia, a product "traffic light" with the PHI of each product, and a 7-day weather outlook (ČHMÚ × NOAA models for Hustopeče). The models named in the footnote: *plíseň révová – model hodnocení podle Šteberly*, *padlí – model signalizace podle Kasta*. The photos and product lists are theirs; the app links to the article list instead of copying anything.

## Šteberla (downy mildew, SHMÚ Bratislava)

Rainfall is summed from 1 May; from 15 May the weekly cumulative total is plotted into a prognostic graph with two curves. Below curve A: non-calamitous occurrence, treatment signalled after flowering and once more 10–14 days later. Between A and B: sporadic-calamitous, treatment before flowering for two weeks and twice after flowering at 10–14 days. Above B: calamitous, regular treatment every 5–14 days until berry softening. amet.cz publishes South-Moravian maps of the resulting severity (0–100 non-calamitous, 101–200 sporadic-calamitous, above 200 calamitous). Oospores need a temperature sum of about 160 °C above 8 °C to break dormancy, and the 3×10 rule (≥10 °C, ≥10 mm in 24–48 h, shoots ≥10 cm) marks the primary infection.

**Implemented as an approximation** (`util/Steberla.kt`, 6 September 2026). The original graphs are in SHMÚ / VÚVV methodologies from the 1970s–80s and in Ackermann's integrated-protection handbooks (Mendel University, Lednice), not online. The curves in the app are reconstructed from the published description: two sigmoid curves over decades from 1 May to the end of August, A passing about 110 mm at the end of June, B rising through 180–220 mm during June, both flattening in July–August. Anchor points (days since 1 May → mm):

| day | 0 | 14 (15.5.) | 31 (1.6.) | 45 (15.6.) | 60 (30.6.) | 75 (15.7.) | 91 (31.7.) | 106 (15.8.) | 122 (31.8.) |
|---|---|---|---|---|---|---|---|---|---|
| A | 5 | 20 | 45 | 75 | 110 | 140 | 165 | 185 | 200 |
| B | 15 | 40 | 90 | 180 | 220 | 250 | 275 | 295 | 315 |

The Overview draws the cumulative rain of the season against both curves, names the zone and the treatment regime that goes with it, and the daily check alerts once a week while the curve is in the calamitous zone. Treat the boundaries as indicative until someone types the original values into the two tables – nothing else needs to change. The app keeps its own 3-10 primary-infection and secondary-window indicators next to it.

## Kast / OiDiag (powdery mildew, Geisenheim)

Three consecutive days with at least six hours between 21 and 30 °C start the index at 60; each further such day adds 20, a day without them subtracts 10, temperatures above 32–35 °C knock it down. Version 2.2 also weights the ontogenic resistance of the bunches (Kast & Bleyer 2010). **This is the index the app already uses** (`DiseaseRisk`, "oidium index"), computed from the hourly aggregates of the Open-Meteo fetch; the risk card now says so.

## Botrytis

BS rate botrytis pressure from rain and dew; the app counts wet, mild days (≥2 mm or ≥6 wet hours at 15–25 °C) over the last three days. Both are only weather indicators – bunch damage, canopy and variety matter more.

## Sources

- BS vinařské potřeby, *Situační zpráva* week 35/2026: https://www.vinarskepotreby.cz/clanky/detail/ochrana-revy-vinne-35-tyden-0.htm
- amet.cz, *Signalizace chorob na révě*: http://amet.cz/steberla.htm
- Spolek Ekovín bulletins quoting the Šteberla procedure: https://ekovin.cz/2022/07/17/zprava-c-4-11/
- Kast, W.K. (1997) OiDiag; Kast & Bleyer (2010) OiDiag-2.2 – summarised at https://metos.global/en/disease-models-grapevine/
