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
S výslovným upozorněním, že jde o kostru, ne o doporučení. Odhad: půl dne (rozšíření plánu sezóny).

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

## Navržené pořadí
1. Jednotky pro malovinaře (bez toho je zadávání postřiků nepřirozené).
2. Odrůdový katalog + cíl sklizně podle odrůdy.
3. Grafy 1 a 2 (počasí + riziko + postřiky; srovnání ročníků).
4. Šablony protokolů ve sklepě a kostra postřiků.
5. Přílohy PDF, GDD do sklizně po první sezóně.
