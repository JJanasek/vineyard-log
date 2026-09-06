# Disease models: what the app computes and what the Czech advisory services use

Written 6 September 2026 after looking at the weekly *Situační zpráva o výskytu patogenů révy vinné* published by BS vinařské potřeby and the signalisation pages of amet.cz.

## What BS vinařské potřeby publish

Every week in season a 7-page PDF (see `/clanky/detail/ochrana-revy-vinne-NN-tyden…`): state of vegetation with BBCH and photos of varieties, protection measures, product tips split into *profi / hobby / eko*, a per-station table of infection pressure (weak / medium / strong) for downy mildew, powdery mildew and botrytis for ~25 stations from Slovácko to Znojemsko and Bohemia, a product "traffic light" with the PHI of each product, and a 7-day weather outlook (ČHMÚ × NOAA models for Hustopeče). The models named in the footnote: *plíseň révová – model hodnocení podle Šteberly*, *padlí – model signalizace podle Kasta*. The photos and product lists are theirs; the app links to the article list instead of copying anything.

## Šteberla (downy mildew, SHMÚ Bratislava)

Rainfall is summed from 1 May; from 15 May the weekly cumulative total is plotted into a prognostic graph with two curves. Below curve A: non-calamitous occurrence, treatment signalled after flowering and once more 10–14 days later. Between A and B: sporadic-calamitous, treatment before flowering for two weeks and twice after flowering at 10–14 days. Above B: calamitous, regular treatment every 5–14 days until berry softening. amet.cz publishes South-Moravian maps of the resulting severity (0–100 non-calamitous, 101–200 sporadic-calamitous, above 200 calamitous). Oospores need a temperature sum of about 160 °C above 8 °C to break dormancy, and the 3×10 rule (≥10 °C, ≥10 mm in 24–48 h, shoots ≥10 cm) marks the primary infection.

**Implemented from the original paper** (`util/Steberla.kt`): P. Šteberla, A. Vančová, G. Valuš, V. Zeman (SHMÚ), *Agrometeorologická predpoveď peronospóry viniča*, Meteorologické zprávy 35 (1982), pp. 150–153, scan at http://www.amet.cz/steberla.pdf. Data: Malokarpatská region 1951–1975, downy-mildew intensity ranked 1–25 per year; correlation with monthly weather was weak except for cumulative rain (r = 0.81 for 1 May–31 July), so the prognosis uses weekly cumulative rain from 1 May.

Two second-degree regression curves, x = week index (week 1 ends 7 May, week 2 ends 14 May …):

- curve A (maximum cumulative rain of years without calamitous occurrence): y = −37.529410 + 24.536249·x − 0.380418·x², r = 0.99
- curve B (minimum cumulative rain of calamitous years): y = −1.073529 + 18.625645·x + 0.650154·x², r = 0.97

Table 3 of the paper (observed / computed mm):

| interval | A obs. | B obs. | A calc. | B calc. |
|---|---|---|---|---|
| 1.5.–14.5. | 18 | 61 | 10.0 | 38.8 |
| 1.5.–21.5. | 25 | 69 | 32.7 | 60.7 |
| 1.5.–28.5. | 34 | 83 | 54.5 | 83.8 |
| 1.5.–4.6. | 66 | 114 | 75.6 | 108.3 |
| 1.5.–11.6. | 93 | 133 | 96.0 | 134.1 |
| 1.5.–18.6. | 131 | 148 | 115.6 | 161.2 |
| 1.5.–25.6. | 136 | 209 | 134.4 | 189.5 |
| 1.5.–2.7. | 142 | 214 | 152.5 | 219.2 |
| 1.5.–9.7. | 162 | 222 | 169.8 | 250.2 |
| 1.5.–16.7. | 202 | 245 | 186.3 | 282.5 |
| 1.5.–23.7. | 215 | 253 | 202.1 | 316.0 |
| 1.5.–30.7. | 216 | 425 | 217.2 | 350.9 |
| 1.5.–6.8. | 233 | 451 | 231.4 | 387.1 |
| 1.5.–13.8. | 246 | 452 | 244.9 | 424.6 |
| 1.5.–20.8. | 256 | 462 | 257.7 | 463.4 |
| 1.5.–27.8. | 262 | 461 | 269.6 | 503.5 |

Practical use (the paper's conclusions): start on 14 May with the rain of 1–14 May and plot weekly; stop on 30 July, the average start of berry softening of early varieties, after which the forecast is no longer relevant. Below A only the basic sprays (before flowering, after flowering, before berry softening of early varieties); between A and B the basic sprays plus further ones at discretion; when the sums reach the calamitous zone start protection immediately and keep spraying regularly as the vine grows so that new shoots, leaves, inflorescences and bunches are covered. The app draws the season's cumulative rain against both curves on the Overview, names the zone and its regime, and the daily check alerts once a week while the curve is in the calamitous zone during the forecast period. The curves come from Bratislava's climate; for Vlčnov they are a guide, not a law, and the app keeps its own 3-10 primary-infection and secondary-window indicators next to them.

## Kast / OiDiag (powdery mildew, Geisenheim)

Three consecutive days with at least six hours between 21 and 30 °C start the index at 60; each further such day adds 20, a day without them subtracts 10, temperatures above 32–35 °C knock it down. Version 2.2 also weights the ontogenic resistance of the bunches (Kast & Bleyer 2010). **This is the index the app already uses** (`DiseaseRisk`, "oidium index"), computed from the hourly aggregates of the Open-Meteo fetch; the risk card now says so.

## Botrytis

BS rate botrytis pressure from rain and dew; the app counts wet, mild days (≥2 mm or ≥6 wet hours at 15–25 °C) over the last three days. Both are only weather indicators – bunch damage, canopy and variety matter more.

## Sources

- BS vinařské potřeby, *Situační zpráva* week 35/2026: https://www.vinarskepotreby.cz/clanky/detail/ochrana-revy-vinne-35-tyden-0.htm
- amet.cz, *Signalizace chorob na révě*: http://amet.cz/steberla.htm
- Spolek Ekovín bulletins quoting the Šteberla procedure: https://ekovin.cz/2022/07/17/zprava-c-4-11/
- Kast, W.K. (1997) OiDiag; Kast & Bleyer (2010) OiDiag-2.2 – summarised at https://metos.global/en/disease-models-grapevine/
