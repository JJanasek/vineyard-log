#!/usr/bin/env python3
"""Reduce a real Vineyard Log backup to a test fixture.

The real backup is personal and stays out of the repository (.gitignore). This keeps the parts the
calculations need - blocks, the entries and their products, measurements and the weather - drops
free text and every product nobody used, and writes the result where the unit tests read it.

    python3 tools/make_test_fixture.py ~/vineyard-log-2026-sloucene.json
"""
import json
import os
import sys

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "app", "src", "test", "resources", "season-real.json")

# free text can hold anything the grower jotted down; the tests never read it
STRIP_ENTRY = ("notes", "weatherNote")
KEEP_PRODUCT = ("id", "name", "supplier", "category", "activeIngredient", "doseMin", "doseMax",
                "doseUnit", "phiDays", "copperGPerKg", "intervalYears", "npk")


def main(path: str) -> None:
    with open(path, encoding="utf-8") as f:
        src = json.load(f)

    entries = src.get("entries", [])
    entry_ids = {e["id"] for e in entries}
    usages = [u for u in src.get("usages", []) if u.get("entryId") in entry_ids]
    used_products = {u["productId"] for u in usages}

    out = {
        "schemaVersion": src.get("schemaVersion", 1),
        "exportedAt": src.get("exportedAt", 0),
        "blocks": src.get("blocks", []),
        "products": [{k: v for k, v in p.items() if k in KEEP_PRODUCT}
                     for p in src.get("products", []) if p["id"] in used_products],
        "batches": src.get("batches", []),
        "batchSources": src.get("batchSources", []),
        "entries": [{k: v for k, v in e.items() if k not in STRIP_ENTRY} for e in entries],
        "usages": [{k: v for k, v in u.items() if k != "note"} for u in usages],
        "measurements": [{k: v for k, v in m.items() if k != "note"} for m in src.get("measurements", [])],
        "weather": [{k: v for k, v in w.items() if k != "note"} for w in src.get("weather", [])],
    }

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(out, f, ensure_ascii=False, separators=(",", ":"), sort_keys=True)
    counts = {k: len(v) for k, v in out.items() if isinstance(v, list)}
    print(f"{OUT}: {os.path.getsize(OUT) // 1024} kB {counts}")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
