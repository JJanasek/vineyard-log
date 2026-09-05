#!/usr/bin/env python3
"""
Convert the markdown wine diary (one file per variety and vintage, see templates/vino-template.md
in the vinohrad-log repo) into a Vineyard Log backup JSON.

    python3 tools/import_markdown_log.py /path/to/vinohrad-log -o old-log.json

Import it on the phone with Settings -> "Import backup (merge)" (adds to what is there) or
"Import backup (replace all)" on a fresh install.
"""
import argparse, json, re, sys, time
from pathlib import Path

PLANTED = {"rulandske sede": 1980, "rulandske bile": 1980, "modry portugal": 2003}
TRAINING = "Rýnsko-hessenské"
LOCATION_NOTE = "Vlčnov"
CAT_BY_FIELD = {"kvasinky": "YEAST", "výživa": "YEAST_NUTRIENT", "vyziva": "YEAST_NUTRIENT", "enzymy": "ENZYME"}

def strip_accents(s):
    import unicodedata
    return "".join(c for c in unicodedata.normalize("NFD", s) if unicodedata.category(c) != "Mn")

def parse_date(s):
    m = re.search(r"(\d{1,2})\.\s*(\d{1,2})\.\s*(\d{4})", s)
    if not m: return None
    d, mo, y = map(int, m.groups())
    import datetime
    return datetime.date(y, mo, d).toordinal() - datetime.date(1970, 1, 1).toordinal()

def num(s):
    m = re.search(r"-?\d+(?:[.,]\d+)?", s or "")
    return float(m.group(0).replace(",", ".")) if m else None

def eval_formula(s):
    """'1*1.2*0.8' or '3\\*1.2\\*0.6' -> 0.96 / 2.16"""
    m = re.search(r"(\d+(?:[.,]\d+)?)\s*\\?\*\s*(\d+(?:[.,]\d+)?)\s*\\?\*\s*(\d+(?:[.,]\d+)?)", s)
    if not m: return None
    a, b, c = (float(x.replace(",", ".")) for x in m.groups())
    return round(a * b * c, 2)

class Out:
    def __init__(self):
        self.blocks, self.products, self.batches, self.sources, self.entries, self.usages, self.measurements = [], [], [], [], [], [], []
        self.ids = {k: 0 for k in ("block", "product", "batch", "entry", "usage", "measurement")}
    def nid(self, k): self.ids[k] += 1; return self.ids[k]
    def block(self, name, variety, planted):
        for b in self.blocks:
            if b["name"].lower() == name.lower(): return b["id"]
        b = {"id": self.nid("block"), "name": name, "variety": variety, "plantedYear": planted, "trainingSystem": TRAINING, "notes": LOCATION_NOTE}
        self.blocks.append(b); return b["id"]
    def product(self, name, url, category, purpose=""):
        for p in self.products:
            if p["name"].lower() == name.lower(): return p["id"]
        supplier = "VINARSKY_DUM" if "vinarskydum" in url else ("LIPERA" if "lipera" in url else "OTHER")
        p = {"id": self.nid("product"), "name": name, "supplier": supplier, "category": category, "url": url, "purpose": purpose}
        self.products.append(p); return p["id"]
    def entry(self, **kw):
        e = {"id": self.nid("entry"), "createdAt": int(time.time() * 1000)}
        e.update(kw); self.entries.append(e); return e["id"]
    def usage(self, entry_id, product_id, dose=None, unit="", total=None, total_unit="", note=""):
        self.usages.append({"id": self.nid("usage"), "entryId": entry_id, "productId": product_id, "dose": dose, "doseUnit": unit,
                            "totalAmount": total, "totalUnit": total_unit, "note": note})
    def measurement(self, entry_id, date, kind, value, block_id=None, batch_id=None, note=""):
        self.measurements.append({"id": self.nid("measurement"), "entryId": entry_id, "date": date, "blockId": block_id,
                                  "batchId": batch_id, "kind": kind, "value": value, "note": note})

def parse_file(path: Path, out: Out):
    text = path.read_text(encoding="utf-8")
    m = re.search(r"^#\s*(.+?)\s*-\s*Ročník\s*(\d{4})", text, re.M)
    if not m: print("skip (no title):", path); return
    title, vintage = m.group(1).strip(), int(m.group(2))
    style = "ROSE" if "ROSE" in title.upper() else "WHITE"
    variety = re.sub(r"\s*\(.*?\)", "", title).strip()
    key = strip_accents(variety).lower()
    block_id = out.block(variety, variety, PLANTED.get(key))
    # parameter table
    params = {}
    for row in re.finditer(r"^\|\s*\*\*(.+?)\*\*\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|", text, re.M):
        params[strip_accents(row.group(1)).lower()] = (row.group(2).strip(), row.group(3).strip())
    harvest = parse_date(params.get("datum sberu", ("", ""))[0])
    kg = num(params.get("vaha hroznu", ("", ""))[0])
    nm = num(params.get("cukernatost", ("", ""))[0]); nm_note = params.get("cukernatost", ("", ""))[1]
    ph = num(params.get("ph mostu", ("", ""))[0]); ph_note = params.get("ph mostu", ("", ""))[1]
    ta = num(params.get("kyseliny", ("", ""))[0]); ta_note = params.get("kyseliny", ("", ""))[1]
    juice = num(params.get("vylisnost", ("", ""))[0])
    alc = num(params.get("alkohol", ("", ""))[0]); alc_note = params.get("alkohol", ("", ""))[1]
    # process bullets
    proc = {}
    for b in re.finditer(r"^\*\s*\*\*(.+?):\*\*\s*(.*)$", text, re.M):
        proc[strip_accents(b.group(1)).lower()] = b.group(2).strip()
    def links(s):
        return [(n.strip(), u) for n, u in re.findall(r"\[([^\[\]]+)\]\((https?://[^)]+)\)", s)]
    notes = []
    for k, label in (("macerace", "Macerace"), ("doslazeni", "Doslazení"), ("teplota kvaseni", "Teplota kvašení")):
        v = proc.get(k, "").strip("[] ")
        if v and not v.startswith("°C") and v != "Množství cukru v kg": notes.append(f"{label}: {v}")
    yeast_links = links(proc.get("kvasinky", ""))
    batch_id = out.nid("batch")
    out.batches.append({
        "id": batch_id, "name": variety, "vintage": vintage, "variety": variety, "style": style,
        "status": "AGING", "volumeL": juice, "grapesKg": kg, "yeast": ", ".join(n for n, _ in yeast_links),
        "startDate": harvest, "notes": "\n".join(notes),
    })
    out.sources.append({"batchId": batch_id, "blockId": block_id, "kg": kg})
    # harvest entry (vineyard)
    if harvest is not None:
        e = out.entry(date=harvest, domain="VINEYARD", type="HARVEST", blockId=block_id, quantity=kg, quantityUnit="kg",
                      title=f"Sklizeň {variety}", notes=nm_note if nm_note and "prselo" in nm_note else "")
        if nm: out.measurement(e, harvest, "NM", nm, block_id=block_id, note=nm_note)
        if ta: out.measurement(e, harvest, "TA", ta, block_id=block_id, note=ta_note)
        if ph: out.measurement(e, harvest, "PH", ph, block_id=block_id, note=ph_note)
        # must prep (cellar): maceration + enzymes + juice volume
        mp = out.entry(date=harvest, domain="CELLAR", type="MUST_PREP", batchId=batch_id, title="Zpracování a lisování",
                       notes=proc.get("macerace", "").strip("[] "), quantity=juice, quantityUnit="L")
        for name, url in links(proc.get("enzymy", "")):
            purpose = ""
            mm = re.search(re.escape(name) + r"\]\([^)]+\)\s*-\s*([^,\]]+)", proc.get("enzymy", ""))
            if mm: purpose = mm.group(1).strip()
            out.usage(mp, out.product(name, url, "ENZYME", purpose), note=purpose)
        if juice: out.measurement(mp, harvest, "VOLUME", juice, batch_id=batch_id)
        if nm: out.measurement(mp, harvest, "NM", nm, batch_id=batch_id)
        if ta: out.measurement(mp, harvest, "TA", ta, batch_id=batch_id)
    # log sections
    for sec in re.finditer(r"^###\s*\[([^\]]+)\]\s*-\s*(.+?)\s*$\n((?:(?!^###).*\n?)*)", text, re.M):
        date = parse_date(sec.group(1)); head = sec.group(2).strip()
        bullets = [l.strip("- ").strip() for l in sec.group(3).splitlines() if l.strip().startswith("-")]
        body = "\n".join(bullets)
        if date is None: continue
        h = strip_accents(head).lower()
        if "sber" in h or "lisov" in h:
            continue  # covered by the harvest / must-prep entries
        if "kvas" in h:
            e = out.entry(date=date, domain="CELLAR", type="YEAST_PITCH", batchId=batch_id, title=head, notes=body)
            for name, url in yeast_links: out.usage(e, out.product(name, url, "YEAST"))
            for name, url in links(proc.get("vyziva", "")): out.usage(e, out.product(name, url, "YEAST_NUTRIENT"), note="dávkování dle návodu")
            dosl = proc.get("doslazeni", "").strip("[] ")
            if dosl:
                kgsugar = eval_formula(dosl)
                a = out.entry(date=date, domain="CELLAR", type="ADDITION", batchId=batch_id, title="Doslazení", notes=dosl,
                              quantity=kgsugar, quantityUnit="kg")
                out.usage(a, out.product("Cukr (sacharóza)", "", "OTHER_CELLAR", "Doslazení moštu"), total=kgsugar, total_unit="kg")
            continue
        if "skolen" in h or "stac" in h:
            e = out.entry(date=date, domain="CELLAR", type="RACKING", batchId=batch_id, title=head, notes=body)
            if alc: out.measurement(e, date, "ALCOHOL", alc, batch_id=batch_id, note=alc_note)
            continue
        out.entry(date=date, domain="CELLAR", type="CELLAR_OTHER", batchId=batch_id, title=head, notes=body)

def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("repo", type=Path); ap.add_argument("-o", "--output", type=Path, default=Path("old-log.json"))
    a = ap.parse_args()
    out = Out()
    for f in sorted(a.repo.glob("[12][0-9][0-9][0-9]/*.md")):
        parse_file(f, out)
    data = {"schemaVersion": 1, "exportedAt": int(time.time() * 1000), "blocks": out.blocks, "products": out.products,
            "batches": out.batches, "batchSources": out.sources, "entries": out.entries, "usages": out.usages,
            "measurements": out.measurements, "weather": []}
    a.output.write_text(json.dumps(data, ensure_ascii=False, indent=1), encoding="utf-8")
    print(f"{len(out.blocks)} blocks, {len(out.products)} products, {len(out.batches)} batches, {len(out.entries)} entries, "
          f"{len(out.usages)} product usages, {len(out.measurements)} measurements -> {a.output}")

if __name__ == "__main__":
    sys.exit(main())
