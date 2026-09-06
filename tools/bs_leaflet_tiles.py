#!/usr/bin/env python3
"""Cut the photo sheet (page 1) of the BS vinařské potřeby "Postřikový plán" A3 leaflet into per-topic
photos for the app's device-only extra gallery (assets/guide-extra, git-ignored).

The leaflet is copyrighted by BS vinařské potřeby s.r.o.; this keeps a private copy for your own phone
and shows the source in the app. Do not commit or redistribute the output. Ask BS for permission before
publishing an APK that contains it.

usage: tools/bs_leaflet_tiles.py A3_letak_postrikvy_plan_2025_bez_orezovych_znacek.pdf [out_dir]
needs: pdftoppm (poppler-utils) and Pillow.
"""
import json, os, subprocess, sys, tempfile
from PIL import Image

PDF = sys.argv[1]
OUT = sys.argv[2] if len(sys.argv) > 2 else os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'assets', 'guide-extra')
SOURCE = 'https://www.vinarskepotreby.cz/clanky/detail/postrikovy-plan-vinice-2025.htm'
CREDIT = '© BS vinařské potřeby – Postřikový plán 2025, str. 1 (soukromá kopie, nešířit)'

# Grid of photo pairs on page 1 (left half of the A3 spread), as fractions of the full page width / height.
COLS = [(0.0242, 0.1715), (0.1759, 0.3233), (0.3282, 0.4761)]
ROWS = [(0.2199, 0.2813), (0.3131, 0.3745), (0.4064, 0.4686), (0.5307, 0.5928), (0.6240, 0.6861)]
# (guide key, Czech caption) per cell, row by row
CELLS = [
    [('peronospora', 'plíseň révová'), ('oidium', 'padlí révové'), ('botrytis', 'plíseň šedá')],
    [('erineum_mite', 'kadeřavost (hálčivec révový)'), ('lobesia', 'obaleči'), ('erineum_mite', 'plstnatost (vlnovník révový)')],
    [('esca', 'esca'), ('scaphoideus', 'fytoplazmové žloutnutí révy'), ('scaphoideus', 'stolbur')],
    [('n_deficiency', 'nedostatek dusíku'), ('p_deficiency', 'nedostatek fosforu'), ('k_deficiency', 'nedostatek draslíku')],
    [('mg_deficiency', 'nedostatek hořčíku'), ('s_deficiency', 'nedostatek síry'), ('chlorosis', 'nedostatek železa')],
]

os.makedirs(OUT, exist_ok=True)
with tempfile.TemporaryDirectory() as tmp:
    subprocess.run(['pdftoppm', '-f', '1', '-l', '1', '-r', '220', '-png', PDF, os.path.join(tmp, 'p')], check=True)
    png = [f for f in os.listdir(tmp) if f.endswith('.png')][0]
    page = Image.open(os.path.join(tmp, png)).convert('RGB')
W, H = page.size
credits, n = [], 0
for r, (y0, y1) in enumerate(ROWS):
    for c, (x0, x1) in enumerate(COLS):
        key, caption = CELLS[r][c]
        left, right, top, bottom = int(x0 * W), int(x1 * W), int(y0 * H), int(y1 * H)
        mid = (left + right) // 2
        for i, box in enumerate([(left, top, mid, bottom), (mid, top, right, bottom)]):
            n += 1
            name = f'bs2025_{key}_{r + 1}{c + 1}{"ab"[i]}.jpg'
            tile = page.crop(box)
            tile.save(os.path.join(OUT, name), quality=88)
            credits.append(dict(file=name, key=key, caption=caption, credit=CREDIT, source=SOURCE))
with open(os.path.join(OUT, 'credits-extra.json'), 'w', encoding='utf-8') as f:
    json.dump(credits, f, ensure_ascii=False, indent=1)
print(f'{n} tiles written to {os.path.abspath(OUT)} (page {W}x{H}); this folder is git-ignored')
