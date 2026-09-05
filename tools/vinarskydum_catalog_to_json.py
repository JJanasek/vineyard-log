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
            body, _ = get_cached(url)
        except Exception as e:
            print(f"  {url}: {e}", file=sys.stderr); break
        found = []
        for block in re.split(r'<div class="product"', body)[1:]:
            m = re.search(r'href="((?:https://www\.vinarskydum\.cz)?/[a-z0-9][a-z0-9-]*/)"', block)
            if not m: continue
            url = m.group(1) if m.group(1).startswith("http") else BASE + m.group(1)
            if not re.search(r"/(strana-|kategorie/)", url) and url not in found:
                found.append(url)
        new = [u for u in found if u not in urls]
        urls += new
        print(f"  {slug} page {page}: {len(new)} products", file=sys.stderr)
        if not new or f"strana-{page + 1}" not in body:
            break
        page += 1; time.sleep(1.0)
    return urls

import os, hashlib
CACHE = os.path.expanduser("~/.cache/vineyard-log/vinarskydum")

def get_cached(url):
    os.makedirs(CACHE, exist_ok=True)
    f = os.path.join(CACHE, hashlib.sha1(url.encode()).hexdigest() + ".html")
    if os.path.exists(f):
        return open(f, encoding="utf-8").read(), True
    body = get(url)
    open(f, "w", encoding="utf-8").write(body)
    return body, False

BREADCRUMB_CAT = [
    (r"fungicid|houbov", "FUNGICIDE"), (r"insekticid|škůdc|skudc|akaricid|biologick", "INSECTICIDE"), (r"herbicid|plevel", "HERBICIDE"),
    (r"listov", "FOLIAR_FERTILIZER"), (r"hnojiv|substrát|substrat", "SOIL_FERTILIZER"), (r"smáč|smac|adjuv", "ADJUVANT"), (r"stimul|humin", "BIOSTIMULANT"),
]

def vine_dose_from_table(body):
    """Returns (dose_min, dose_max, unit, phi_days) from the crop table row for grapevine, if present."""
    for table in re.findall(r"<table.*?</table>", body, re.S):
        head = [text(h) for h in re.findall(r"<th[^>]*>(.*?)</th>", table, re.S)]
        if not head: continue
        dose_col = next((i for i, h in enumerate(head) if re.search(r"dávk", h, re.I)), None)
        phi_col = next((i for i, h in enumerate(head) if re.search(r"^OL|ochrann", h, re.I)), None)
        if dose_col is None: continue
        hdr = head[dose_col].lower()
        per = "10l" if "10 l" in hdr or "10l" in hdr else ("hl" if "100 l" in hdr else ("ha" if "ha" in hdr else "10l"))
        for row in re.findall(r"<tr[^>]*>(.*?)</tr>", table, re.S):
            cells = [text(c) for c in re.findall(r"<td[^>]*>(.*?)</td>", row, re.S)]
            if len(cells) <= dose_col or not re.search(r"r[ée]va|vinn", cells[0], re.I): continue
            m = re.search(r"(\d+(?:[.,]\d+)?)\s*(?:[-–]\s*(\d+(?:[.,]\d+)?))?\s*(g|ml|kg|l)?", cells[dose_col])
            if not m: continue
            lo = float(m.group(1).replace(",", ".")); hi = float(m.group(2).replace(",", ".")) if m.group(2) else lo
            unit_base = (m.group(3) or "g").lower()
            phi = None
            if phi_col is not None and len(cells) > phi_col:
                nums = [int(x) for x in re.findall(r"\d+", cells[phi_col])]
                if nums: phi = max(nums)
            return lo, hi, f"{unit_base}/{per}", phi
    return None

def parse_product(url, default_cat):
    body, _ = get_cached(url)
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
    bc = re.search(r'class="breadcrumbs[^"]*"(.*?)</(?:nav|div|ul|ol)>', body, re.S)
    crumbs = [text(x) for x in re.findall(r'<a[^>]*>(.*?)</a>', bc.group(1), re.S)] if bc else []
    crumbs = [c for c in crumbs if c and c.lower() not in ("domů", "vinohrad a zahrada")]
    subcat = crumbs[-1] if crumbs else ""
    if subcat.lower() in name.lower() or name.lower() in subcat.lower(): subcat = crumbs[-2] if len(crumbs) >= 2 else ""
    dose_min = dose_max = None; unit = ""; table_phi = None
    tbl = vine_dose_from_table(body)
    if tbl:
        dose_min, dose_max, unit, table_phi = tbl
    anchor = re.search(r"dávk", desc, re.I)
    for chunk in ([] if tbl else ([desc[anchor.start():]] if anchor else []) + [desc]):
        m = DOSE.search(chunk)
        if m and m.group(4).lower().replace(" ", "") in ("l", "kg"):
            m = None  # "400 g/l" is the active-ingredient concentration, not a dose
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
    crumb_text = " ".join(crumbs).lower()
    for pat, c in BREADCRUMB_CAT:
        if re.search(pat, crumb_text):
            cat = c; break
    else:
        for pat, c in CAT_WORDS:
            if re.search(pat, (name + " " + params.get("kategorie", "")).lower()):
                cat = c; break
    if not package:
        mm = re.search(r"-(\d+(?:-\d+)?)-?(kg|g|l|ml)/?$", url)
        if mm: package = f"{mm.group(1).replace('-', '.')} {mm.group(2)}"
    return {
        "name": name, "supplier": "VINARSKY_DUM", "category": cat, "activeIngredient": active.group(1).strip() if active else "",
        "doseMin": dose_min, "doseMax": dose_max, "doseUnit": unit, "phiDays": table_phi if table_phi is not None else (int(phi.group(1)) if phi else None),
        "purpose": ((subcat + " – ") if subcat and subcat.lower() not in name.lower() else "") + desc[:400], "url": url, "packageSize": package, "price": price,
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
            if not os.path.exists(os.path.join(CACHE, hashlib.sha1(url.encode()).hexdigest() + ".html")): time.sleep(1.0)
    json.dump({"source": "vinarskydum.cz product pages, personal import", "products": products}, open(a.output, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    by = {}
    for p in products: by[p["category"]] = by.get(p["category"], 0) + 1
    with_dose = sum(1 for p in products if p["doseMin"] is not None); with_phi = sum(1 for p in products if p["phiDays"])
    print(f"{len(products)} products -> {a.output} ({with_dose} with dose, {with_phi} with PHI); {by}")

if __name__ == "__main__":
    sys.exit(main())
