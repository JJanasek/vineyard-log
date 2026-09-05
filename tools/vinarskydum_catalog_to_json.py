#!/usr/bin/env python3
"""
Download the plant-protection and fertiliser range of vinarskydum.cz (a Shoptet shop) into a
products JSON for Vineyard Log (Settings -> Import products (merge)).

    python3 tools/vinarskydum_catalog_to_json.py -o vinarskydum-products.json

Polite by design: one request per second, a clear User-Agent, only the categories listed below.
Their robots.txt allows product pages; their terms of use carry no database clause (checked
September 2026), but keep the output for yourself and re-run at most a few times a year.
"""
import argparse, html, json, re, sys, time, urllib.request, urllib.parse

BASE = "https://www.vinarskydum.cz"
UA = "VineyardLog/0.1 (personal catalog import; contact: see github.com/JJanasek/vineyard-log)"
CATEGORIES = {  # slug -> category for products found there
    "fungicidy-postriky-proti-plisni": "FUNGICIDE",
    "insekticidy-postriky-proti-skudcum": "INSECTICIDE",
    "biologicka-ochrana-proti-skudcum": "INSECTICIDE",
    "postriky-proti-skudcum-rostlin": "INSECTICIDE",
    "herbicidy-postriky-na-plevel": "HERBICIDE",
    "hnojivo-na-vinnou-revu": "SOIL_FERTILIZER",
    "kapalna-hnojiva": "FOLIAR_FERTILIZER",
    "organicka-hnojiva": "SOIL_FERTILIZER",
    "vysadba-a-ochrana-vinohradu": "OTHER_VINEYARD",
    "postriky-a-pripravky-na-ochranu-rostlin": "OTHER_VINEYARD",
}
DOSE = re.compile(r"(\d+(?:[.,]\d+)?)\s*(?:(?:až|do|-|–|—)\s*(\d+(?:[.,]\d+)?))?\s*(g|ml|kg|l)\s*/\s*(hl|l|ha|100\s*l|10\s*l|m2|m²)", re.I)
PCT = re.compile(r"(\d+(?:[.,]\d+)?)\s*(?:(?:až|-|–)\s*(\d+(?:[.,]\d+)?))?\s*%", re.I)
PHI = re.compile(r"ochrann[áa]\s+lh[ůu]t[ay]?[^0-9]{0,40}?(\d{1,3})\s*(dn|den)", re.I)
ACTIVE = re.compile(r"[úu][čc]inn[áa]\s+l[áa]tk[ay][:\s]+([^.;\n]{3,80})", re.I)
CAT_WORDS = [  # refine the category from the page text
    (r"fungicid|peronospor|padl|plísn|plisn|botryt|oidium|síra|sira|měď|med[ěe]n", "FUNGICIDE"),
    (r"insekticid|akaricid|obaleč|obalec|škůdc|skudc|svilušk|mšic|msic", "INSECTICIDE"),
    (r"herbicid|plevel", "HERBICIDE"),
    (r"listov[áé] hnoj|foliar|na list", "FOLIAR_FERTILIZER"),
    (r"hnojiv", "SOIL_FERTILIZER"),
    (r"stimul|aminokys|huminov|humac", "BIOSTIMULANT"),
    (r"smáč|smac|adjuv", "ADJUVANT"),
]

def get(url):
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.read().decode("utf-8", "ignore")

def text(s):
    s = re.sub(r"<script.*?</script>|<style.*?</style>", "", s, flags=re.S)
    return re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", " ", s))).strip()

def category_products(slug):
    urls, page = [], 1
    while True:
        url = f"{BASE}/{slug}/" + (f"strana-{page}/" if page > 1 else "")
        try:
            body = get(url)
        except Exception as e:
            print(f"  {url}: {e}", file=sys.stderr); break
        found = []
        for block in re.split(r'<div class="product"', body)[1:]:
            m = re.search(r'href="(https://www\.vinarskydum\.cz/[^"#?]+/)"', block)
            if m and not re.search(r"/(strana-|kategorie/)", m.group(1)) and m.group(1) not in found:
                found.append(m.group(1))
        new = [u for u in found if u not in urls]
        urls += new
        print(f"  {slug} page {page}: {len(new)} products", file=sys.stderr)
        if not new or f"strana-{page + 1}" not in body:
            break
        page += 1; time.sleep(1.0)
    return urls

def parse_product(url, default_cat):
    body = get(url)
    name = re.search(r"<h1[^>]*>(.*?)</h1>", body, re.S)
    name = text(name.group(1)) if name else url.rstrip("/").rsplit("/", 1)[-1]
    price = None
    m = re.search(r'itemprop="price"[^>]*content="([\d.,]+)"', body) or re.search(r'"price"\s*:\s*"?([\d.]+)', body)
    if m: price = float(m.group(1).replace(",", "."))
    params = {}
    for th, td in re.findall(r"<tr[^>]*>\s*<th[^>]*>(.*?)</th>\s*<td[^>]*>(.*?)</td>", body, re.S):
        params[text(th).rstrip(":").lower()] = text(td)
    package = params.get("hmotnost") or params.get("objem") or params.get("balení") or ""
    if not package:
        mm = re.search(r"(\d+(?:[.,]\d+)?)\s*(kg|g|l|ml)\b", name, re.I)
        package = f"{mm.group(1)} {mm.group(2).lower()}" if mm else ""
    desc = re.search(r'class="description-inner"(.*?)(?=<div class="p-detail|<section|<footer)', body, re.S)
    desc = text(desc.group(1)) if desc else ""
    dose_min = dose_max = None; unit = ""
    anchor = re.search(r"dávk", desc, re.I)
    for chunk in ([desc[anchor.start():]] if anchor else []) + [desc]:
        m = DOSE.search(chunk)
        if m:
            dose_min = float(m.group(1).replace(",", ".")); dose_max = float(m.group(2).replace(",", ".")) if m.group(2) else dose_min
            per = m.group(4).lower().replace(" ", "")
            if per == "100l": per = "hl"
            unit = f"{m.group(3).lower()}/{per}"
            break
    else:
        m = PCT.search(desc[anchor.start():] if anchor else desc)
        if m and anchor:
            dose_min = float(m.group(1).replace(",", ".")); dose_max = float(m.group(2).replace(",", ".")) if m.group(2) else dose_min; unit = "%"
    phi = PHI.search(desc); active = ACTIVE.search(desc)
    cat = default_cat
    for pat, c in CAT_WORDS:
        if re.search(pat, (name + " " + params.get("kategorie", "")).lower()):
            cat = c; break
    return {
        "name": name, "supplier": "VINARSKY_DUM", "category": cat, "activeIngredient": active.group(1).strip() if active else "",
        "doseMin": dose_min, "doseMax": dose_max, "doseUnit": unit, "phiDays": int(phi.group(1)) if phi else None,
        "purpose": desc[:400], "url": url, "packageSize": package, "price": price,
        "notes": "Z nabídky vinarskydum.cz; dávku a ochrannou lhůtu ověřte na etiketě.",
    }

def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("-o", "--output", default="vinarskydum-products.json")
    ap.add_argument("--categories", nargs="*", default=list(CATEGORIES), help="category slugs to crawl")
    a = ap.parse_args()
    seen, products = {}, []
    for slug in a.categories:
        cat = CATEGORIES.get(slug, "OTHER_VINEYARD")
        for url in category_products(slug):
            if url in seen: continue
            seen[url] = True
            try:
                products.append(parse_product(url, cat))
            except Exception as e:
                print(f"  {url}: {e}", file=sys.stderr)
            time.sleep(1.0)
    json.dump({"source": "vinarskydum.cz product pages, personal import", "products": products}, open(a.output, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    by = {}
    for p in products: by[p["category"]] = by.get(p["category"], 0) + 1
    with_dose = sum(1 for p in products if p["doseMin"] is not None); with_phi = sum(1 for p in products if p["phiDays"])
    print(f"{len(products)} products -> {a.output} ({with_dose} with dose, {with_phi} with PHI); {by}")

if __name__ == "__main__":
    sys.exit(main())
