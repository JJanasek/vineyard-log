#!/usr/bin/env python3
"""
Convert the official Lipera product catalog PDF (download it yourself from
https://www.lipera.cz/dokumenty-ke-stazeni/) into a products JSON that Vineyard Log can
merge via Settings -> "Import products (merge)".

    python3 tools/lipera_catalog_to_json.py katalog.pdf -o lipera-products.json

Needs `pdftotext` (poppler-utils). The parser is heuristic: it splits every page into the
two printed columns using word coordinates, treats large upper-case lines as product names,
and reads the dose from the "DÁVKOVÁNÍ" box or the yeast table row. Review the result in
the app; doses are only suggestions until you compare them with the label.

The output is your own local file; do not redistribute the vendor's catalog data.
"""
import argparse
import json
import re
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path

NS = {"x": "http://www.w3.org/1999/xhtml"}

SECTIONS = [  # (title regex, category)
    (r"^KVASINKY", "YEAST"),
    (r"^REHYDRATACE", "YEAST_NUTRIENT"),
    (r"^V[ÝY]ŽIV", "YEAST_NUTRIENT"),
    (r"^SPECI[ÁA]LN[ÍI]\s+P[ŘR][ÍI]PRAVKY", "OTHER_CELLAR"),
    (r"^[ČC]I[ŘR][ÍI]C[ÍI]", "FINING"),
    (r"^ENZYMY", "ENZYME"),
    (r"^AKTIVN[ÍI]", "FINING"),
    (r"^BENTONITY", "FINING"),
    (r"^BAKTERIE", "MLF_BACTERIA"),
    (r"^ODKYSELEN[ÍI]", "ACID"),
    (r"^DUBOV[ÉE]", "OTHER_CELLAR"),
    (r"^TANINY", "TANNIN"),
    (r"^STABILIZACE", "STABILIZATION"),
    (r"^SPECI[ÁA]LN[ÍI]\s+PRODUKTY", "OTHER_VINEYARD"),
]
NOISE_HEADINGS = re.compile(
    r"^(D[ÁA]VKOV[ÁA]N[ÍI]|LEGENDA|JAK POU|JAK[ÝY]|ENZYM\?|AROMA|UHL[ÍI]|INDEX|OBSAHU|KOMPLETN|SERVIS|PRO VINA|"
    r"ODBORN|KOMPLEXN|PARTNERSTV|AKREDITOVAN|Produktov|KATALOG|V[ÍI]tejte|NA AROMA|ŠKOLEN|PRO ŠKOLEN|"
    r"[ČC]I[ŘR]EN[ÍI]|FLOTACE|ROS[ÉE]|B[ÍI]L[ÁA]|[ČC]ERVEN[ÁA]|SEKT|ODR[ŮU]DOV|NEUTR|UNIVERZ|KVASINKY PRO)",
    re.I,
)
DOSE_RE = re.compile(
    r"(\d+(?:[.,]\d+)?)\s*(?:(?:až|do|-|–|—)\s*(\d+(?:[.,]\d+)?))?\s*(g|mg|ml|kg|l)\s*/\s*(hl|l|ha|100\s*l|10\s*l|t)",
    re.I,
)
NOISE_NAMES = re.compile(r"^(PRO[ČC]|P[ŘR][ÍI]PRAVKY|KVASINEK|ORGANICKOU|C[ÍI]LEN[ÁA]|NA |DO |JAK |PRO |V[ÝY][ŽZ]IV)", re.I)
# products whose real category differs from the section they are printed in
OVERRIDES = [
    (r"viniflora|bactiless|bactiferm|oenos|bakteri|lactobacillus|oenococcus", "MLF_BACTERIA"),
    (r"^kyselina|sihadex|kalium-bi|weinkalk|odkysel|dokysel", "ACID"),
    (r"go-ferm|gärsalz|garsalz|thiamin|fermaid|nutricell|proferm|omnisal|vinonutri|stimula|aktive power", "YEAST_NUTRIENT"),
    (r"optiwhite|optired|glutastar|mannolees|caudalys|reduless|noblesse|reskue|longevity|optiless", "OTHER_CELLAR"),
    (r"kupfersulf|no brett", "STABILIZATION"),
]
VINEYARD_CATEGORY = [
    (r"list|foliar", "FOLIAR_FERTILIZER"), (r"hnoj", "SOIL_FERTILIZER"), (r"stimul|amino", "BIOSTIMULANT"),
    (r"s[íi]ra|s[íi]rn|m[ěe][ďd]|fungicid|padl|peronosp", "FUNGICIDE"), (r"sm[áa][čc]", "ADJUVANT"),
]


def run_pdftotext(pdf: Path) -> ET.Element:
    out = tempfile.NamedTemporaryFile(suffix=".html", delete=False)
    out.close()
    subprocess.run(["pdftotext", "-bbox-layout", str(pdf), out.name], check=True)
    txt = Path(out.name).read_text(encoding="utf-8", errors="ignore")
    Path(out.name).unlink(missing_ok=True)
    return ET.fromstring(txt)


def page_lines(page):
    """Yield (text, xmin, ymin, height, xmax) for every line on the page."""
    for line in page.iter("{%s}line" % NS["x"]):
        words = [w for w in line.iter("{%s}word" % NS["x"])]
        if not words:
            continue
        text = " ".join((w.text or "").strip() for w in words).strip()
        if not text:
            continue
        xmin = min(float(w.get("xMin")) for w in words)
        xmax = max(float(w.get("xMax")) for w in words)
        ymin = min(float(w.get("yMin")) for w in words)
        ymax = max(float(w.get("yMax")) for w in words)
        yield text, xmin, ymin, ymax - ymin, xmax


def is_upper_heading(text: str) -> bool:
    letters = [c for c in text if c.isalpha()]
    if len(letters) < 3:
        return False
    upper = sum(1 for c in letters if c.isupper())
    return upper / len(letters) >= 0.8 and not NOISE_HEADINGS.search(text)


def parse_dose(text: str):
    m = DOSE_RE.search(text)
    if not m:
        return None
    lo = float(m.group(1).replace(",", "."))
    hi = float(m.group(2).replace(",", ".")) if m.group(2) else None
    unit, per = m.group(3).lower(), m.group(4).lower().replace(" ", "")
    if per == "100l":
        per = "hl"
    elif per == "10l":
        per, lo, hi = "hl", lo * 10, (hi * 10 if hi else None)
    # "20 g/hl, 40 g/hl" -> range 20-40
    second = DOSE_RE.search(text, m.end())
    if second and hi is None:
        v = float(second.group(1).replace(",", "."))
        if v > lo:
            hi = v
    return lo, hi, f"{unit}/{per}"


def parse(pdf: Path):
    root = run_pdftotext(pdf)
    products = []
    section_cat = None
    section_name = ""
    for page in root.iter("{%s}page" % NS["x"]):
        width = float(page.get("width"))
        lines = list(page_lines(page))
        if not lines:
            continue
        heights = sorted(h for _, _, _, h, _ in lines)
        body_h = heights[len(heights) // 2]
        # section title: big upper-case text near the top of the page, possibly split over two lines
        big_top = " ".join(t.strip() for t, x, y, h, xm in sorted(lines, key=lambda l: l[2]) if h > body_h * 1.3 and y < 220)
        for pat, cat in SECTIONS:
            if re.search(pat, big_top):
                section_cat, section_name = cat, big_top[:40]
                break
        left = sorted([l for l in lines if l[1] < width * 0.48], key=lambda l: (round(l[2]), l[1]))
        right = sorted([l for l in lines if l[1] >= width * 0.48], key=lambda l: (round(l[2]), l[1]))
        for column in (left, right):
            current = None
            mode = None  # 'desc' | 'dose' | 'yeast_table'
            for text, xmin, ymin, h, xmax in column:
                t = text.strip()
                if re.match(r"^\d{1,3}$", t):
                    continue  # page number
                if is_upper_heading(t) and h > body_h * 1.05 and len(t) <= 40 and not any(re.search(p, t) for p, _ in SECTIONS):
                    current = {"name": t, "desc": [], "dose": None, "extra": [], "temp": None, "nutri": None}
                    products.append((section_cat, section_name, current))
                    mode = "desc"
                    continue
                if current is None:
                    continue
                if re.match(r"^Teplota kva", t, re.I):
                    mode = "yeast_table"
                    continue
                if mode == "yeast_table":
                    # cells of the yeast table arrive one by one: temp, dose, nutrient demand
                    m = re.match(r"^(.*?°C|dle podm[ií]nek)\s+(.*?)\s+(n[ií]zk[ée].*|st[řr]edn[ií].*|vysok[ée].*)$", t, re.I)
                    if m:
                        current["temp"], current["dose"], current["nutri"] = m.group(1), m.group(2), m.group(3)
                        mode = "desc"
                        continue
                    if re.match(r"^(D[ÁA]VKOV[ÁA]N[ÍI]|N[áa]roky)", t, re.I):
                        continue
                    if re.search(r"°C|dle podm", t, re.I):
                        current["temp"] = t
                        continue
                    if DOSE_RE.search(t):
                        current["dose"] = t
                        continue
                    if re.match(r"^(n[íi]zk|st[řr]edn|vysok)", t, re.I):
                        current["nutri"] = t
                        mode = "desc"
                        continue
                    mode = "desc"  # anything else ends the table; fall through
                if re.match(r"^D[ÁA]VKOV[ÁA]N[ÍI]", t, re.I):
                    mode = "dose"
                    continue
                if mode == "dose":
                    if current["dose"] is None:
                        current["dose"] = t
                    elif len(t) < 30 and not is_upper_heading(t):
                        current["dose"] += " " + t
                    mode = "desc"
                    continue
                if mode == "yeast_table":
                    # e.g. "15–20 °C 20 g/hl, 40 g/hl střední"
                    m = re.match(r"^(.*?°C|dle podm[ií]nek)\s+(.*?)\s+(n[ií]zk[ée].*|st[řr]edn[ií].*|vysok[ée].*)$", t, re.I)
                    if m:
                        current["extra"].append(f"Fermentation {m.group(1)}; nutrient demand: {m.group(3)}")
                        current["dose"] = m.group(2)
                    else:
                        current["dose"] = current["dose"] or t
                    mode = "desc"
                    continue
                if mode == "desc" and not is_upper_heading(t):
                    current["desc"].append(t)
    return products


def clean_name(raw: str) -> str:
    n = re.sub(r"\s+®", "®", raw).replace("™", "").strip()
    n = re.sub(r"\s+", " ", n)
    # Title-case shouting names but keep short codes like "SVG", "R71", "CY 3079"
    words = []
    for w in n.split(" "):
        if len(w) <= 4 or any(ch.isdigit() for ch in w) or not w.isupper():
            words.append(w)
        else:
            words.append(w.capitalize())
    return " ".join(words)


def to_product(section_cat, section_name, p):
    desc = " ".join(p["desc"])
    desc = re.sub(r"\s+", " ", desc).replace("- ", "")
    dose = parse_dose(p["dose"] or "") or parse_dose(desc)
    cat = section_cat or "OTHER_CELLAR"
    for pat, c in OVERRIDES:
        if re.search(pat, p["name"].lower()):
            cat = c
            break
    if cat == "OTHER_VINEYARD":
        for pat, c in VINEYARD_CATEGORY:
            if re.search(pat, (p["name"] + " " + desc).lower()):
                cat = c
                break
    notes = list(p["extra"])
    if p.get("temp") or p.get("nutri"):
        notes.append("Fermentation " + (p.get("temp") or "?") + "; nutrient demand: " + (p.get("nutri") or "?"))
    if p["dose"] and dose is None:
        notes.append(f"Dose text: {p['dose']}")
    notes.append(f"Catalog section: {section_name}")
    return {
        "name": clean_name(p["name"]),
        "supplier": "LIPERA",
        "category": cat,
        "doseMin": dose[0] if dose else None,
        "doseMax": (dose[1] if dose[1] is not None else dose[0]) if dose else None,
        "doseUnit": dose[2] if dose else "",
        "purpose": desc[:400],
        "url": "https://www.lipera.cz/",
        "notes": "\n".join(notes),
    }


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("pdf", type=Path)
    ap.add_argument("-o", "--output", type=Path, default=Path("lipera-products.json"))
    ap.add_argument("--min-desc", type=int, default=20, help="drop entries with shorter descriptions")
    args = ap.parse_args()
    raw = parse(args.pdf)
    seen = set()
    products = []
    for sec_cat, sec_name, p in raw:
        if sec_cat is None:
            continue  # intro pages before the first catalog section
        if NOISE_NAMES.search(p["name"]) or "?" in p["name"]:
            continue
        prod = to_product(sec_cat, sec_name, p)
        key = prod["name"].lower()
        if key in seen or len(prod["purpose"]) < args.min_desc:
            continue
        seen.add(key)
        products.append(prod)
    args.output.write_text(
        json.dumps({"source": f"Lipera product catalog PDF ({args.pdf.name}), parsed locally", "products": products},
                   ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    with_dose = sum(1 for p in products if p["doseMin"] is not None)
    print(f"{len(products)} products written to {args.output} ({with_dose} with a parsed dose)")
    by_cat = {}
    for p in products:
        by_cat[p["category"]] = by_cat.get(p["category"], 0) + 1
    for k, v in sorted(by_cat.items(), key=lambda kv: -kv[1]):
        print(f"  {k}: {v}")


if __name__ == "__main__":
    sys.exit(main())
