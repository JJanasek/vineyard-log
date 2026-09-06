#!/usr/bin/env python3
"""Generate the static website (web/dist) from the app's exported content.

Inputs: app/build/site-content/*.json (written by the ContentExportTest unit test), the app's string
resources (labels for enums, en + cs), docs/*.md, guide photos from app/src/main/assets/guide and the
screenshots in web/site/img. Output: web/dist with /cs and /en page sets plus the backup viewer.

usage: python3 web/build_site.py [--content app/build/site-content] [--out web/dist]
"""
import argparse, hashlib, json, os, re, shutil, sys, time, xml.etree.ElementTree as ET
from jinja2 import Environment, FileSystemLoader, select_autoescape

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ap = argparse.ArgumentParser()
ap.add_argument('--content', default=os.path.join(ROOT, 'app', 'build', 'site-content'))
ap.add_argument('--out', default=os.path.join(ROOT, 'web', 'dist'))
args = ap.parse_args()

if not os.path.isdir(args.content) or not os.listdir(args.content):
    sys.exit(f'{args.content} is missing or empty – run ./gradlew :app:testDebugUnitTest --tests "*ContentExportTest" --rerun first')

def load(name):
    with open(os.path.join(args.content, name), encoding='utf-8') as f:
        return json.load(f)

def strings(lang_dir):
    out = {}
    d = os.path.join(ROOT, 'app', 'src', 'main', 'res', lang_dir)
    for fn in ('strings.xml', 'strings_enums.xml'):
        p = os.path.join(d, fn)
        if not os.path.exists(p):
            continue
        for s in ET.parse(p).getroot().findall('string'):
            out[s.get('name')] = (s.text or '').replace('\\n', '\n').replace("\\'", "'")
    return out

STR = {'en': strings('values'), 'cs': strings('values-cs')}
def label(name, lang):
    return STR[lang].get(name) or STR['en'].get(name) or name

guide, phenology, varieties = load('guide.json'), load('phenology.json'), load('varieties.json')
spray, cellar, enums, steberla = load('spray_program.json'), load('cellar_templates.json'), load('enums.json'), load('steberla.json')
with open(os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'guide', 'credits.json'), encoding='utf-8') as f:
    credits = {c['key']: c for c in json.load(f)}

UI = {
 'en': dict(home='Home', menu='Menu', guide='Field guide', phenology='Growth stages', varieties='Varieties', spray='Spray programme', cellar='Cellar protocols', docs='Docs', viewer='Backup viewer', download='Download', switch='Česky',
    footer='Vineyard Log – an offline vineyard and cellar notebook for small growers. Code MIT; guide photos keep their Creative Commons licences.', built='built',
    tagline='A notebook for a small vineyard and cellar', intro='Vineyard Log is a free, offline Android app for hobby growers: sprays with doses per 10 l and PHI, phenology by photos, ripeness readings, harvest, cellar batches with protocols, weather from Open-Meteo and ČHMÚ, disease-risk models (3-10 rule, Kast/OiDiag index, Šteberla curves), reminders and daily alerts. Everything stays on your phone; backups are plain JSON you control.',
    get_app='Get the app', shots_note='Screenshots show the Czech interface; the app switches to English in Settings.', open_viewer='Open a backup in the browser', what='What it does', how_it_works='How it works', how_text='Add your blocks, log what you do from the block page, check the Overview tab. The field guide, spray programme, variety catalogue and cellar protocols on this site are generated from the same data the app ships with.',
    features=[
        ('overview.png', 'Overview', 'Season weather with sprays marked, cumulative GDD by vintage, harvest and ripening per block, fermentation per batch, soil analyses – one tab for the whole year.'),
        ('steberla.png', 'Disease risk models', 'Downy mildew by the 3-10 rule and the Šteberla rain curves (exact regressions from the 1982 paper), Kast/OiDiag-style powdery mildew index, botrytis wet days – from your own weather table.'),
        ('spray-entry.png', 'Spray entry that thinks along', 'Label dose turned into g per 10 l and per tank, PHI with the earliest harvest date, tank-mix rules from the BS leaflet, active-ingredient rotation, copper budget, and warnings when it is too hot or too windy to spray.'),
        ('conditions.png', 'Too hot or too windy?', 'Type the temperature and wind (or let the app take the day\'s maximum from the weather table): sulphur and oils above 28 / 25 °C scorch, sulphur is weak in the cold, wind over 5 m/s means drift and the Czech spraying rules say no.'),
        ('stage-picker.png', 'Growth stages by photo', 'Twelve BBCH stages with photos and a picker: tap the one that matches the vine, the spray programme and the risk models follow.'),
        ('guide.png', 'Field guide', 'Diseases, pests, deficiencies and disorders with Commons photos, symptoms, conditions, what to do, links to BS vinařské potřeby articles – plus your own photos logged against each topic.'),
        ('weather.png', 'Weather from three sources', 'Open-Meteo forecast and history, measured ČHMÚ stations near your vineyard, and your own rain gauge as weekly totals spread over the rainy days.'),
        ('reminders.png', 'Reminders and daily alerts', 'Sampling, PHI end, protocol steps, yeast nutrition – plus automatic alerts: high risk, frost in spring, stuck fermentation, sampling before the estimated harvest.'),
        ('cellar.png', 'Cellar batches with protocols', 'Batches from harvest to bottle: protocol templates become dated steps, readings chart the fermentation, calculators for chaptalisation, SO₂ and yeast nutrition sit next to the log.'),
        ('block.png', 'Blocks in hobby units', 'Ares and m², doses per 10 l, PHI and copper per block, NPK totals, fertiliser intervals, vineyard renewal (replanting, regrafting) with the share renewed.'),
        ('spray.png', 'Spray programme and season plan', 'Eight windows by growth stage with the BS 2025 hobby plan matched to your products; a yearly checklist of vineyard tasks with reminders.'),
        ('viewer.png', 'Backup viewer on the web', 'Open the JSON backup in any browser: the same overview, log, blocks, cellar and weather; add entries and download the backup for a merge in the app. Nothing is uploaded.'),
        ('quick-start.png', 'Offline, yours', 'No account, no server: everything lives in the phone. Backups to a synced folder, PDF and CSV exports, photos and PDF attachments on entries, Czech and English.'),
    ],
    dl_text='The app is an Android APK (Android 8 and newer). Install it from the latest GitHub release; your phone will ask to allow installing from unknown sources.', dl_release='Latest release (APK)', dl_ci='CI builds', dl_note='The release APK is signed with a debug key for now; updates install over each other as long as the key stays the same. When the app lands on F-Droid, switch to that build.', fdroid='Publishing on F-Droid is planned. The app already meets the basics (open source, no trackers, no proprietary services except the optional weather fetches); a reproducible release signing is the remaining step.', build_yourself='Build it yourself',
    guide_intro='Diseases, pests, deficiencies and disorders of the vine in Central Europe – the same reference the app carries, with Wikimedia Commons photos and links to BS vinařské potřeby articles.', photo='Photo', symptoms='What you see', conditions='When it happens', action='What to do', bs_articles='BS vinařské potřeby articles',
    phenology_intro='The BBCH stages the app uses, in season order, with what to look for and what it means for the work. Log a stage when about half of the vines show it.', what_you_see='What you see', what_to_do='What it means for the work',
    varieties_intro='Varieties grown in Moravia with the app\'s defaults: colour, ripening class, usual harvest window, target sugar and disease sensitivity (L/M/H).', variety='Variety', style='Colour', ripening='Ripening', harvest_window='Harvest (MM-dd)',
    spray_intro='Skeleton of the season by growth stage: what to aim at and how the BS 2025 hobby plan handles it (products per litre of water). Adjust to the risk card and the weather.', bs_plan='BS spray plan 2025 (per litre of water):', spray_sources='Plan rows and general rules from the 2025 leaflet of',
    cellar_intro='Protocol templates the app turns into dated reminders with instructions. Doses are hobby-sized (g/hl and per 10 l); the label and the lab win.', day='Day', step='Step', notes='Notes',
    kinds={'DISEASE': 'Diseases', 'PEST': 'Pests', 'DEFICIENCY': 'Deficiencies', 'DISORDER': 'Disorders and damage'}, levels={'LOW': 'L', 'MEDIUM': 'M', 'HIGH': 'H'}),
 'cs': dict(home='Úvod', menu='Menu', guide='Atlas', phenology='Fenofáze', varieties='Odrůdy', spray='Postřikový program', cellar='Sklepní protokoly', docs='Dokumentace', viewer='Prohlížeč zálohy', download='Stažení', switch='English',
    footer='Vineyard Log – offline zápisník vinice a sklepa pro malé vinaře. Kód MIT; fotky atlasu mají své licence Creative Commons.', built='sestaveno',
    tagline='Zápisník pro malou vinici a sklep', intro='Vineyard Log je bezplatná offline aplikace pro Android pro hobby vinaře: postřiky s dávkami na 10 l a ochrannou lhůtou, fenofáze podle fotek, měření zralosti, sklizeň, šarže ve sklepě s protokoly, počasí z Open-Meteo a ČHMÚ, modely rizika chorob (pravidlo 3-10, index podle Kasta, Šteberlovy křivky), připomínky a denní upozornění. Všechno zůstává v telefonu; záloha je obyčejný JSON pod tvou kontrolou.',
    get_app='Stáhnout aplikaci', shots_note='', open_viewer='Otevřít zálohu v prohlížeči', what='Co umí', how_it_works='Jak to funguje', how_text='Založ tratě, zapisuj z detailu tratě, koukej na Přehled. Atlas, postřikový program, katalog odrůd i sklepní protokoly na tomto webu se generují ze stejných dat, která nese aplikace.',
    features=[
        ('overview.png', 'Přehled', 'Počasí sezóny s vyznačenými postřiky, kumulativní GDD po ročnících, sklizeň a zralost po tratích, kvašení po šaržích, rozbory půdy – celý rok na jedné záložce.'),
        ('steberla.png', 'Modely rizika chorob', 'Plíseň révová podle pravidla 3-10 a Šteberlových křivek (přesné regrese z práce z roku 1982), index padlí podle Kasta/OiDiag, vlhké dny pro botrytidu – z vlastní tabulky počasí.'),
        ('spray-entry.png', 'Zápis postřiku, který myslí s tebou', 'Dávka z etikety přepočtená na g na 10 l a na nádrž, ochranná lhůta s nejbližším termínem sklizně, pravidla tank mixu z letáku BS, střídání účinných látek, bilance mědi a varování, když je na postřik moc horko nebo fouká.'),
        ('conditions.png', 'Moc horko nebo fouká?', 'Zadej teplotu a vítr (nebo si aplikace vezme denní maximum z tabulky počasí): síra a oleje nad 28 / 25 °C popálí, síra v chladu nefunguje, vítr nad 5 m/s znamená úlet a podle českých pravidel se nestříká.'),
        ('stage-picker.png', 'Fenofáze podle fotek', 'Dvanáct stádií BBCH s fotkami a výběrem: ťukni na to, které keř ukazuje, postřikový program a modely rizika se podle toho řídí.'),
        ('guide.png', 'Atlas', 'Choroby, škůdci, nedostatky živin a poruchy s fotkami z Commons, příznaky, podmínky, co dělat, odkazy na články BS vinařské potřeby – a vlastní fotky zapsané k tématu.'),
        ('weather.png', 'Počasí ze tří zdrojů', 'Předpověď a historie z Open-Meteo, měřené stanice ČHMÚ u vinice a vlastní srážkoměr jako týdenní úhrny rozpočítané na deštivé dny.'),
        ('reminders.png', 'Připomínky a denní upozornění', 'Odběry, konec ochranné lhůty, kroky protokolu, výživa kvasinek – a automatická upozornění: vysoké riziko, jarní mráz, zastavené kvašení, odběr před odhadovanou sklizní.'),
        ('cellar.png', 'Šarže ve sklepě s protokoly', 'Šarže od sklizně po lahev: šablony protokolu se stanou datovanými kroky, měření kreslí průběh kvašení, vedle deníku kalkulačky na doslazení, SO₂ a výživu kvasinek.'),
        ('block.png', 'Tratě v hobby jednotkách', 'Ary a m², dávky na 10 l, ochranná lhůta a měď po tratích, součty NPK, intervaly hnojiv, omlazování vinice (dosadba, přeštěpování) s podílem obnovených keřů.'),
        ('spray.png', 'Postřikový program a plán sezóny', 'Osm oken podle fenofáze s hobby plánem BS 2025 napárovaným na tvoje přípravky; roční seznam prací na vinici s připomínkami.'),
        ('viewer.png', 'Prohlížeč zálohy na webu', 'Otevři JSON zálohu v libovolném prohlížeči: stejný přehled, deník, tratě, sklep a počasí; přidej záznamy a stáhni zálohu ke sloučení v aplikaci. Nic se nikam neposílá.'),
        ('quick-start.png', 'Offline a tvoje', 'Bez účtu a bez serveru: všechno je v telefonu. Zálohy do synchronizované složky, export do PDF a CSV, fotky a PDF přílohy u záznamů, čeština i angličtina.'),
    ],
    dl_text='Aplikace je APK pro Android (8 a novější). Nainstaluj ji z posledního vydání na GitHubu; telefon se zeptá na povolení instalace z neznámých zdrojů.', dl_release='Poslední vydání (APK)', dl_ci='Sestavení z CI', dl_note='APK je zatím podepsané ladicím klíčem; aktualizace se instalují přes sebe, dokud klíč zůstane stejný. Až bude aplikace na F-Droidu, přejdi na tamní sestavení.', fdroid='Zveřejnění na F-Droidu je v plánu. Základ aplikace splňuje (otevřený kód, žádné sledování, žádné proprietární služby kromě volitelného stahování počasí); zbývá reprodukovatelné podepisování vydání.', build_yourself='Sestav si ji sám',
    guide_intro='Choroby, škůdci, nedostatky živin a poruchy révy ve střední Evropě – stejná příručka, jakou nese aplikace, s fotkami z Wikimedia Commons a odkazy na články BS vinařské potřeby.', photo='Foto', symptoms='Co vidíš', conditions='Kdy se to děje', action='Co dělat', bs_articles='Články BS vinařské potřeby',
    phenology_intro='Stádia BBCH, se kterými aplikace pracuje, v pořadí sezóny a s tím, co na keři hledat a co to znamená pro práci. Fenofázi zapiš, když ji ukazuje zhruba polovina keřů.', what_you_see='Co vidíš', what_to_do='Co to znamená pro práci',
    varieties_intro='Odrůdy pěstované na Moravě s výchozími hodnotami aplikace: barva, ranost, obvyklé okno sklizně, cílová cukernatost a citlivost na choroby (N/S/V).', variety='Odrůda', style='Barva', ripening='Ranost', harvest_window='Sklizeň (MM-dd)',
    spray_intro='Kostra sezóny podle fenofáze: na co mířit a jak to řeší hobby plán BS 2025 (přípravky na litr vody). Přizpůsob kartě rizika a počasí.', bs_plan='Postřikový plán BS 2025 (na litr vody):', spray_sources='Řádky plánu a obecné zásady z letáku 2025 od',
    cellar_intro='Šablony protokolu, ze kterých aplikace dělá datované připomínky s návodem. Dávky jsou malovinařské (g/hl a na 10 l); etiketa a laboratoř mají přednost.', day='Den', step='Krok', notes='Poznámky',
    kinds={'DISEASE': 'Choroby', 'PEST': 'Škůdci', 'DEFICIENCY': 'Nedostatky živin', 'DISORDER': 'Poruchy a poškození'}, levels={'LOW': 'N', 'MEDIUM': 'S', 'HIGH': 'V'}),
}

env = Environment(loader=FileSystemLoader(os.path.join(ROOT, 'web', 'site', 'templates')), autoescape=select_autoescape(['html']))
out = args.out
if os.path.exists(out):
    shutil.rmtree(out)
os.makedirs(out)
shutil.copytree(os.path.join(ROOT, 'web', 'site', 'static'), os.path.join(out, 'static'))
shutil.copytree(os.path.join(ROOT, 'web', 'site', 'img'), os.path.join(out, 'img'))
os.makedirs(os.path.join(out, 'img', 'guide'))
for fn in os.listdir(os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'guide')):
    if fn.endswith('.jpg'):
        shutil.copy(os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'guide', fn), os.path.join(out, 'img', 'guide', fn))
shutil.copytree(os.path.join(ROOT, 'web', 'viewer'), os.path.join(out, 'viewer'))
os.makedirs(os.path.join(out, 'data'))
for fn in os.listdir(args.content):
    shutil.copy(os.path.join(args.content, fn), os.path.join(out, 'data', fn))
# resolved labels for the viewer
labels = {}
for lang in ('en', 'cs'):
    labels[lang] = {
        'entryTypes': {e['key']: label(e['label'], lang) for e in enums['entryTypes']},
        'stages': {e['key']: label(e['label'], lang) for e in enums['stages']},
        'measurementKinds': {e['key']: {'label': label(e['label'], lang), 'unit': e['unit']} for e in enums['measurementKinds']},
        'productCategories': {e['key']: label(e['label'], lang) for e in enums['productCategories']},
        'wineStyles': {e['key']: label(e['label'], lang) for e in enums['wineStyles']},
        'entryDomains': {e['key']: e['domain'] for e in enums['entryTypes']},
    }
with open(os.path.join(out, 'viewer', 'labels.json'), 'w', encoding='utf-8') as f:
    json.dump(labels, f, ensure_ascii=False)
with open(os.path.join(out, 'viewer', 'steberla.json'), 'w', encoding='utf-8') as f:
    json.dump(steberla, f)
# cache-busting: GitHub Pages serves files with max-age=600, so stamp a content hash onto the viewer's
# own references; otherwise a browser could pair a fresh index.html with a stale app.js for ten minutes
vdir = os.path.join(out, 'viewer')
stamp = hashlib.sha1(b''.join(open(os.path.join(vdir, fn), 'rb').read() for fn in
                              ('app.js', 'models.js', 'style.css', 'labels.json', 'steberla.json'))).hexdigest()[:8]
for fn, pairs in (('index.html', (('href="style.css"', f'href="style.css?v={stamp}"'), ('src="app.js"', f'src="app.js?v={stamp}"'))),
                  ('app.js', (("'./models.js'", f"'./models.js?v={stamp}'"), ("'labels.json'", f"'labels.json?v={stamp}'"), ("'steberla.json'", f"'steberla.json?v={stamp}'"))),
                  ('models.js', (("'steberla.json'", f"'steberla.json?v={stamp}'"),))):
    path = os.path.join(vdir, fn)
    with open(path, encoding='utf-8') as f: text = f.read()
    for a, b in pairs: text = text.replace(a, b)
    with open(path, 'w', encoding='utf-8') as f: f.write(text)
with open(os.path.join(out, 'index.html'), 'w', encoding='utf-8') as f:
    f.write(env.get_template('root_index.html').render())
open(os.path.join(out, '.nojekyll'), 'w').close()

DOCS = [('quick-start', {'en': 'Quick start', 'cs': 'Rychlý start'}), ('disease-models', {'en': 'Disease models (Šteberla, Kast)', 'cs': 'Modely chorob (Šteberla, Kast)'}),
        ('bs-articles', {'en': 'BS vinařské potřeby reading list', 'cs': 'Články BS vinařské potřeby'}), ('weather-stations-and-probes', {'en': 'Weather stations and probes', 'cs': 'Meteostanice a sondy'}),
        ('navrh-hobby-vinar', {'en': 'Design proposal for hobby growers (Czech)', 'cs': 'Návrh pro hobby vinaře'})]
built = time.strftime('%Y-%m-%d')
for lang in ('en', 'cs'):
    other = 'cs' if lang == 'en' else 'en'
    t = UI[lang]
    base = os.path.join(out, lang)
    os.makedirs(os.path.join(base, 'guide')); os.makedirs(os.path.join(base, 'docs'))
    def render(template, path, depth, **ctx):
        root = '../' * depth
        self_path = path
        html = env.get_template(template).render(lang=lang, other_lang=other, t=t, root=root, built=built, self_path=self_path, **ctx)
        with open(os.path.join(base, path), 'w', encoding='utf-8') as f:
            f.write(html)
    render('index.html', 'index.html', 1, page='index', title=t['tagline'])
    render('download.html', 'download.html', 1, page='download', title=t['download'])
    groups = [(t['kinds'][k], [e for e in guide if e['kind'] == k]) for k in ('DISEASE', 'PEST', 'DEFICIENCY', 'DISORDER')]
    render('guide_index.html', 'guide/index.html', 2, page='guide', title=t['guide'], groups=groups)
    for e in guide:
        render('guide_entry.html', f'guide/{e["key"]}.html', 2, page='guide', title=e['name'][lang], e=e, credits=credits)
    stages = [dict(s, labelText=label(s['label'], lang)) for s in phenology]
    render('phenology.html', 'phenology.html', 1, page='phenology', title=t['phenology'], stages=stages, credits=credits)
    styles = {e['key']: label(e['label'], lang) for e in enums['wineStyles']}
    render('varieties.html', 'varieties.html', 1, page='varieties', title=t['varieties'], varieties=varieties, styles=styles, lv=t['levels'])
    targets = {x['key']: x['label'] for x in spray['targets']}
    render('spray.html', 'spray.html', 1, page='spray', title=t['spray'], windows=spray['windows'], targets=targets)
    tpls = [dict(tp, steps=[dict(s, typeText=label(s['typeLabel'], lang)) for s in tp['steps']]) for tp in cellar]
    render('cellar.html', 'cellar.html', 1, page='cellar', title=t['cellar'], templates=tpls, styles=styles)
    docs = [dict(slug=slug, title=title[lang]) for slug, title in DOCS if os.path.exists(os.path.join(ROOT, 'docs', slug + '.md'))]
    render('docs_index.html', 'docs/index.html', 2, page='docs', title=t['docs'], docs=docs)
    for d in docs:
        with open(os.path.join(ROOT, 'docs', d['slug'] + '.md'), encoding='utf-8') as f:
            body = f.read().replace('</script', '<\\/script')
        render('doc.html', f'docs/{d["slug"]}.html', 2, page='docs', title=d['title'], body=body)
print('site written to', out)
