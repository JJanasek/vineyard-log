# Návrh: Vineyard Log pro malovinaře (září 2026)

Podklady: tvůj deník 2025 (RB, RŠ, MP ve Vlčnově, 57–80 l vína na odrůdu), rozbor půdy 2026,
zkušenost s fomopsisem 2026, počasí Open-Meteo 2025–2026 a nabídka Vinařského domu.

## 1. Jednotky pro malovinaře

Dnes aplikace počítá s hektary a l/ha, což pro 100–300 keřů nedává smysl. Návrh:

| Kde | Dnes | Nově |
| --- | --- | --- |
| Výměra tratě | ha | přepínač **m² / ary / ha** v nastavení, k tomu **počet keřů** (už je) |
| Postřik | voda l/ha, dávka kg/ha | **objem jíchy v litrech** na trať a **koncentrace** (g nebo ml na 10 l, případně %); aplikace z dávky na etiketě (kg/ha při 400–1000 l/ha) přepočítá g/10 l a naopak |
| Můj postřikovač | – | v nastavení **objem nádrže** (např. 15 l); u každého přípravku v záznamu se ukáže „na jednu náplň: 45 g“ |
| Hnojení | kg/ha | **g na keř** nebo **kg na ar**; doporučení z rozboru (75 kg/ha P₂O₅) přepočte na tvoje m² |
| Výnos | kg | kg celkem + **kg na keř** (z počtu keřů) |
| Sklep | l, hl | zůstává l; kalkulačky už berou litry |

Přepočet postřiku: koncentrace [g/10 l] = dávka [kg/ha] × 1000 / voda [l/ha] × 10. Přípravek si v
katalogu ponechá dávku z etikety (kg/ha nebo %), záznam si vybere režim. Odhad práce: 1–2 dny.

## 2. Šablony

### 2a. Odrůdový katalog ČR
Vestavěný seznam moštových odrůd pěstovaných u nás (bílé: MT, Veltlínské zelené, RB, RŠ, Ryzlink
rýnský, Sauvignon, Pálava, Tramín, Chardonnay, Neuburské, Aurelius, Hibernal; modré: Modrý Portugal,
Frankovka, Svatovavřinecké, Zweigeltrebe, Rulandské modré, André, Cabernet Moravia, Dornfelder, Merlot;
PIWI: Solaris, Johanniter, Hibernal, Cabernet Cortis…). U každé:
- barva, ranost (raná / střední / pozdní), obvyklé okno sklizně na Moravě,
- typická cílová cukernatost (°NM) pro suché víno,
- náchylnost: peronospora / padlí / botrytida / fomopsis (např. MP: raný, náchylný na botrytidu;
  MT: náchylný na padlí a peronosporu; RB/RŠ: husté hrozny → botrytida),
- typ vína, poznámka.

Využití: trať si odrůdu vybere ze seznamu → cíl pro odhad sklizně, upozornění „blíží se okno sklizně“,
plán sezóny posune kontroly zralosti u raných odrůd, riziková karta zvýrazní choroby, na které je
odrůda citlivá. Odhad práce: 1 den (data + výběr v editaci tratě).

### 2b. Šablony protokolu ve sklepě
„Aromatické bílé bez JMF“ (Go-Ferm → zakvášení → Active PRO → Pure při 1/3 prokvašení → síření →
stáčení), „Rosé z MP“, „Červené s macerací“. Založení šarže ze šablony vytvoří naplánované kroky
s odstupem dní od zakvášení (v plánu sezóny nebo přímo jako „čekající“ záznamy). Odhad: 1 den.

### 2c. Kostra postřikového programu
Dvě kostry podle fenofáze (ekologická měď/síra; konvenční střídání účinných látek), jen jako
předvyplněné úkoly „postřik proti X v období Y“ bez konkrétních přípravků – ty si dosadíš z katalogu.
S výslovným upozorněním, že jde o kostru, ne o doporučení. Zdrojem pro dávky v hobby jednotkách mohou být
rádcovské články Vinařského domu (např. „první jarní postřik“: Sulka 4–6 %, Kumulus 150–200 g/10 l, Rock Effect 3 %,
stříkat po třech dnech s odpoledními 15 °C) a tabulky „Dávkování na 10 l / OL“ přímo u přípravků, které stahovač už čte.
Odhad: půl dne (rozšíření plánu sezóny).

## 3. Odhad sklizně a další modely z měření

Hotové: trend cukru → datum dosažení cílové °NM; kvašení → dny do dokvašení a varování při
zastavení. Návrh rozšíření:
- cíl podle odrůdy (viz 2a) místo jedné hodnoty v nastavení,
- druhý ukazatel z **poklesu kyselin** (TA klesá zhruba 0,1–0,2 g/l za den v září) a poměr cukr/kyseliny,
- **GDD do sklizně**: po prvním ročníku aplikace zjistí, při jaké sumě teplot jsi sklízel, a další rok
  odhaduje sklizeň i z počasí, ne jen z odběrů,
- **ochranná lhůta vs. odhad sklizně**: varování, když by poslední postřik odhad sklizně nestihl,
- kvašení: očekávaná délka podle YAN a teploty, upozornění na rychlé kvašení (>2 °NM/den → teplota).

## 4. Grafy navíc

1. **Počasí sezóny**: pás min/max teploty + sloupce srážek po dnech, do toho barevně dny s vysokým
   rizikem a svislé čáry postřiků. Tohle by ti u fomopsisu 2026 rovnou ukázalo, jestli mezi postřikem
   a deštěm nebyla mezera.
2. **Srovnání ročníků GDD**: křivky 2025 a 2026 přes sebe (den v roce), stejně srážky kumulativně.
3. **Zrání po odrůdách**: cukr a kyseliny v čase, více ročníků přes sebe.
4. **Roční přehled**: sloupce sklizeň kg, °NM, TA po odrůdách a letech.
5. **Kvašení**: k cukru a teplotě přidat rychlost poklesu za den.

Odhad: 1–2 dny (LineChart už umí více řad; přidat sloupce a svislé značky).

## 5. Katalog Vinařského domu
Skript `tools/vinarskydum_catalog_to_json.py` stáhne fungicidy, insekticidy, herbicidy, hnojiva na révu
a biologickou ochranu (1 požadavek za sekundu) do JSON pro *Import přípravků (sloučit)*. Výsledek zůstává
u tebe, do repozitáře nepatří. Dávky a ochranné lhůty z popisků jsou orientační – etiketa má přednost.

## 6. Rozbor půdy
Rozbor z 26. 5. 2026 jsem převedl do importu jako záznam „Rozbor půdy (rozšířený)“ u každé tratě
(pH 7,3; P 121; K 297; Mg 382; humus 0,83 %) s celým komentářem laboratoře a čtyřmi úkoly v plánu
sezóny (prokypřovací směs, hloubkové kypření, organika / HUMAC Agro, trvalé ozelenění od 2027).
Do budoucna: **přílohy** (PDF) k záznamům, aby protokol zůstal u záznamu.

## 7. Fomopsis 2026 – co zalogovat
Postřiky s datem, přípravkem, dávkou, objemem jíchy a počasím při aplikaci; první příznaky s fotkou
(atlas → „Zapsat pozorování“); déšť po postřiku. Riziková karta ukáže mokrá období; graf 1 pak
mezery v krytí. Fomopsis se infikuje hned po rašení za chladného vlhka – klíčový je první postřik
při 3–10 cm letorostů a odstranění napadeného réví v zimě.

## Navržené pořadí (aktualizované)
1. ~~Jednotky pro malovinaře~~ – hotovo.
2. ~~ČHMÚ jako zdroj počasí (Hluk 4 km)~~ – hotovo (5. 9.).
3. ~~Odrůdový katalog + předvyplnění tratí, šarží, plánu a cílů podle odrůd~~ – hotovo (5. 9.).
4. ~~Záložka Přehled s grafy~~ – hotovo (6. 9.): riziko + připomínky + úkoly nahoře, grafy rozdělené na Vinice (počasí sezóny s postřiky, GDD po ročnících, zrání a kyseliny z měření na vinici, sklizeň po letech) a Sklep (kvašení a teplota po šaržích, poslední rozbory) a Půda. Zbývají automatické připomínky (úkoly plánu, riziko na pozadí, kvašení, odhad odběru).
5. Šablony protokolů ve sklepě a kostra postřiků.
6. Vlastní fotky k tématům atlasu; přílohy PDF; GDD do sklizně po první sezóně.
7. Diagnostika napříč ročníky (GDD vs. cukr při sklizni, postřiky vs. výskyt chorob, YAN vs. délka kvašení, roční Cu/S, náklady na trať).
8. Počasí z vlastní stanice (CSV import, později čidla hustoty), rozpoznávání fotek.

Hotovo mimo pořadí (6. 9.): malovinařské jednotky i v deníku a detailu záznamu; typ záznamu Obnova vinice (dosadba, přeštěpování, zmlazení, vyklučení, nová výsadba) se souhrnem na trati.

## 8. Doplnění plánu (5. 9. 2026 večer)

### 8a. Předvyplnění podle toho, co máš vysazené
**Hotovo (5. 9. 2026):** katalog 31 odrůd (`data/varieties/Varieties.kt`) s barvou, raností, oknem sklizně,
cílovou °NM a citlivostí na choroby; výběr odrůdy v editaci tratě, řádek s oknem sklizně v detailu tratě,
cílová °NM pro odhad sklizně podle odrůdy, nová šarže z tratě (odrůda, barva, název), citlivé odrůdy
u vysokého rizika, odrůdové úkoly kontrol zralosti při „Obnovit výchozí“ v plánu sezóny.
Odrůdový katalog (bod 2a) se použije na víc míst: trať si vybere odrůdu ze seznamu a aplikace
předvyplní barvu, ranost, okno sklizně a cílovou cukernatost; plán sezóny dostane odrůdové
položky (kontroly zralosti od zaměkání u raných dřív, botrytida u hustých hroznů RB/RŠ, obaleči);
riziková karta zvýrazní choroby, na které jsou tvoje odrůdy citlivé; nová šarže z tratě zdědí
odrůdu, barvu a šablonu sklepního protokolu (bílé bez JMF / rosé z MP / červené). Kalkulačky
dostanou předvyplněný objem z výlisnosti tratě a cílovou °NM z odrůdy.

### 8b. Připomínky a záložka „Přehled“
**Hotovo (5. 9. 2026) – připomínky:** Počasí → zvoneček. Vlastní připomínky (jednou / denně / týdně v daný den /
každých N dní, čas, od–do, typ záznamu, trať nebo šarže), šablony „Kontrolní odběr každé pondělí 7:00 do 31. 10.“,
„Kontrola vinice každé 3 dny“ a „Kontrola kvašení denně po 3 týdny“. AlarmManager (přesný, pokud to systém dovolí),
přežije restart i aktualizaci, klepnutí na notifikaci otevře předvyplněný záznam. Automaticky: připomínka konce
ochranné lhůty po uložení postřiku (vypínatelné v Nastavení). Karta „Nejbližší připomínky“ v záložce Počasí.
Zbývá: záložka Přehled s grafy, další automatické připomínky (úkoly plánu, riziko, kvašení, odhad sklizně).
- **Vlastní připomínky**: název, opakování (jednou / denně / týdně v daný den), od–do (např. od
  zaměkání do sklizně), volitelně typ záznamu, který se má připomínkou založit („Kontrolní odběr
  cukernatosti – každé pondělí 8:00“). Notifikace Androidu (oprávnění POST_NOTIFICATIONS,
  AlarmManager/WorkManager), klepnutí otevře rovnou formulář záznamu.
- **Automatické připomínky** (zapínatelné): konec ochranné lhůty posledního postřiku; úkoly plánu
  sezóny na začátku jejich okna; vysoké riziko chorob po denním stažení počasí na pozadí; kvašení
  bez poklesu cukru 3 dny; další odběr podle odhadu sklizně („za 5 dní bys měl být na 21 °NM“).
- **Záložka Přehled** nahradí záložku Počasí: nahoře riziko a dnešní/nejbližší připomínky a úkoly,
  pod tím grafy (počasí sezóny s postřiky a rizikem, srovnání ročníků GDD, zrání po tratích, roční
  přehled sklizně) a nakonec sekce Počasí (stažení, seznam dnů). Deník zůstává první záložkou.
Odhad: připomínky 1–1,5 dne, záložka Přehled s grafy 1,5–2 dny.

### 8c. ČHMÚ jako druhý zdroj počasí
Ověřeno na opendata.chmi.cz (otevřená data od června 2024, JSON): stanice v `meta1-YYYYMMDD.json`
(WSI, název, souřadnice, výška), denní data `recent/data/daily/MM/dly-{WSI}-{RRRRMM}.json`
s prvky TMI, TMA, TPM, SRA, H…; starší roky v `historical`. Nejblíž k Vlčnovu: **Hluk 4 km** a
**Nivnice 5,6 km** (jen srážkoměry: SRA), **Staré Město 14 km** a **Strání 15 km** (automatické:
teploty, vlhkost, srážky). Porovnání 27.–30. 8. 2026: Hluk 28. 8. 10,4 mm, Staré Město 28. 8. 5,6 mm,
Nivnice 29. 8. 4,7 mm; Open-Meteo 29. 8. 21 mm. Rozdíl má dvě příčiny: ČHMÚ počítá srážkový den
07:00–07:00 (noční déšť z 28./29. 8. patří do 28. 8.) a model přeceňuje lokální přeháňku.
Návrh: v nastavení „Najít stanice ČHMÚ“ (vybere se srážkoměr a teplotní stanice zvlášť), tlačítko
„Stáhnout z ČHMÚ“ vedle Open-Meteo; naměřené hodnoty (zdroj „chmi“) přepíší modelové, ručně zadané
dny nikdy; hodinové ukazatele pro riziko (ovlhčení, teplé hodiny) zůstanou z Open-Meteo, protože
denní soubory ČHMÚ je nemají (10minutová data by to uměla, ale jsou jen pro automatické stanice).
Odhad: 1 den.

### 8d. Fotky v atlasu
Hotovo: 24 fotek z Commons, u peronospory, padlí, botrytidy, vlnovníka, obalečů (oba druhy), úpalu,
krup a mrazu 2–3 fotky k tématu. Commons nemá licencované fotky révy pro nedostatky živin (Mg, K, N, B),
červenou spálu, sprchávání ani další fotky escy a fomopsisu. Návrh: (1) **vlastní fotky k tématu** –
u každé položky atlasu tlačítko „Přidat svou fotku“, které uloží fotku jako pozorování s odkazem na
téma a zobrazí ji v atlasu; časem tak vznikne tvoje vlastní galerie (a základ pro pozdější
rozpoznávání); (2) u nedostatků živin jednoduché schéma listu místo fotky; (3) zdroje s licencí
CC BY-NC (např. Bugwood/IPM Images) jdou použít v aplikaci pro osobní použití, ale ne do
veřejného repozitáře.
