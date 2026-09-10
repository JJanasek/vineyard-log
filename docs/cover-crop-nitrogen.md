# Nitrogen from a green cover in the inter-rows

Research notes for the nitrogen credit the app adds to the season balance (September 2026).
The question: how much of the nitrogen a legume cover fixes actually reaches the vine, and what
number is defensible to put in a hobby grower's fertiliser plan.

## Two steps, not one

A legume cover feeds the vine only if both happen:

1. **Fixation** – the legume fixes atmospheric nitrogen into its biomass. This follows biomass, so
   it depends on sowing date, winter survival and how long the cover grows before termination.
2. **Release** – the residue mineralises where and when the vine can take it up. A narrow C:N ratio
   (young legume) releases fast; a grass-heavy or mature stand releases late or locks nitrogen up.

Most of the nitrogen never arrives. That is why the app tracks a *credit*, not the fixed amount.

## What the literature measures

| Source | Setting | Biomass | Nitrogen |
| --- | --- | --- | --- |
| Frontiers in Agronomy 2025, [doi 10.3389/fagro.2025.1604142](https://doi.org/10.3389/fagro.2025.1604142) | Douro vineyard, Portugal, 28 % slope, legume mixes rolled in May | 5.9–7.8 t/ha dry matter | 124–177 kg N/ha in the biomass; most released within 45 days of rolling, and the authors note that not all of it is available to the vine |
| Perennia fact sheet, [Nitrogen Credits from Legume Cover Crops](https://ofcaf.perennia.ca/wp-content/uploads/sites/19/2023/07/NItrogen-Credits-from-Legume-Cover-Crops-FINAL-July18.pdf) (Reid, K., 2023) | Nova Scotia, pure stands sown 14 July, sampled 25 October | hairy vetch 2.6 t/ha, red clover 2.2 t/ha, forage peas 8.8 t/ha | 131, 86 and 271 kg N/ha respectively |
| Same fact sheet, citing OMAFRA 2017 | Ontario field crops | – | flat credit of **45 kg N/ha** to the crop after a legume cover (80 kg N/ha before corn) |
| Same fact sheet, from corn yield response | – | – | the following crop takes up **40–50 %** of the nitrogen in the cover-crop biomass |
| [BS vinařské potřeby](https://www.vinarskepotreby.cz/clanky), Czech vineyard practice | winter green manure | about 14 t/ha fresh matter | roughly 53 kg N, 22 kg P₂O₅, 76 kg K₂O per hectare |
| [Screening cover crops for irrigated vineyards](https://pmc.ncbi.nlm.nih.gov/articles/PMC11280555/) (greenhouse) | 120-day screening | – | nitrogen input potential 12.3–114 kg/ha depending on species; clovers best |

Two more rules from the Perennia sheet worth keeping:

- Below about **2.2 t/ha of dry matter** at termination there is not enough nitrogen to change a
  fertiliser plan.
- A cover killed in **early autumn** mineralises while nothing is growing and the nitrogen is lost
  over winter; killed **late autumn or in spring** it stays organic until the soil warms.

## What the app does with it

`util/CoverCrop.kt`:

```
credit = 45 kg N/ha × legume share × share of the block sown        (no biomass weighed)
credit = dry matter × N % × 0.45 × share of the block sown          (biomass weighed)
```

The nitrogen concentration used when biomass is weighed but not analysed is 2.5 %, between the
Douro mixes (2.1–2.3 %) and pure young legumes (red clover 3.9 %, hairy vetch 5.1 %).

The share of the block matters for a hobby vineyard: sowing every second inter-row on a vineyard
whose inter-rows are about 70 % of the ground is roughly 0.35 of the area, so a half-legume mix
sown that way is worth around 8 kg N/ha, not 45.

Log it as a **Green cover** entry with the legume percentage and the covered percentage; the block
page then shows the credit as its own line, separate from what went on through the soil and through
the leaf. It is an estimate for planning – the soil analysis every four years is what settles it.
