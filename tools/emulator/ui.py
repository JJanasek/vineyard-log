#!/usr/bin/env python3
"""Emulator UI helper. Commands:
  tap TEXT [N]     tap N-th (1-based; negative = from end) node whose text/content-desc contains TEXT
  tapedit N        tap N-th EditText node
  type TEXT        type into focused field
  shot NAME        screenshot to NAME.png
  dump             print labelled nodes with bounds
  edits            print EditText nodes (text + bounds)
  back | swipe | tapxy X Y | key CODE
"""
import os, re, subprocess, sys, time
ADB = ['/home/janek/Android/Sdk/platform-tools/adb', '-s', 'emulator-5554']
D = os.environ.get('EMU_DIR', '/tmp/vineyard-emu')

def sh(*a, capture=False):
    return subprocess.run(list(ADB) + list(a), capture_output=capture, text=capture)

def dump():
    time.sleep(1.0)
    r = subprocess.run(list(ADB) + ['exec-out', 'uiautomator', 'dump', '/dev/tty'], capture_output=True)
    xml = r.stdout.decode('utf-8', 'ignore').replace('UI hierchary dumped to: /dev/tty', '')
    os.makedirs(D, exist_ok=True); open(f'{D}/ui.xml', 'w').write(xml)
    nodes = []
    for n in re.findall(r'<node [^>]*>', xml):
        t = re.search(r'text="([^"]*)"', n); c = re.search(r'content-desc="([^"]*)"', n)
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        cls = re.search(r'class="([^"]*)"', n)
        if not b: continue
        x1, y1, x2, y2 = map(int, b.groups())
        nodes.append(dict(text=t.group(1) if t else '', desc=c.group(1) if c else '',
                          cls=cls.group(1) if cls else '', x=(x1 + x2) // 2, y=(y1 + y2) // 2, bounds=b.group(0)))
    return nodes

def tap(x, y):
    sh('shell', 'input', 'tap', str(x), str(y)); time.sleep(0.8)

cmd = sys.argv[1]
if cmd == 'tap':
    q = sys.argv[2].lower(); which = int(sys.argv[3]) if len(sys.argv) > 3 else 1
    hits = [n for n in dump() if q in n['text'].lower() or q in n['desc'].lower()]
    if not hits: print(f"NOT FOUND: {sys.argv[2]}"); sys.exit(1)
    n = hits[which - 1 if which > 0 else len(hits) + which]
    tap(n['x'], n['y']); print(f"tapped '{sys.argv[2]}' ({hits.index(n)+1}/{len(hits)}) at {n['x']},{n['y']}")
elif cmd == 'tapedit':
    k = int(sys.argv[2]); eds = [n for n in dump() if 'EditText' in n['cls']]
    if k > len(eds): print(f"only {len(eds)} EditTexts"); sys.exit(1)
    tap(eds[k - 1]['x'], eds[k - 1]['y']); time.sleep(1.2); print(f"tapped EditText #{k}/{len(eds)} at {eds[k-1]['x']},{eds[k-1]['y']}")
elif cmd == 'tapfield':
    # tap the EditText that contains the label text (Compose puts the label inside the field bounds)
    q = sys.argv[2].lower(); nodes = dump()
    labels = [n for n in nodes if q == n['text'].lower() or q == n['desc'].lower()] or [n for n in nodes if q in n['text'].lower()]
    if not labels: print(f"NOT FOUND label: {sys.argv[2]}"); sys.exit(1)
    lab = labels[0]
    def contains(n, x, y):
        m = re.search(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', n['bounds']); x1,y1,x2,y2 = map(int, m.groups()); return x1 <= x <= x2 and y1 <= y <= y2
    eds = [n for n in nodes if 'EditText' in n['cls'] and contains(n, lab['x'], lab['y'])]
    if not eds: print(f"no EditText around label {sys.argv[2]}"); sys.exit(1)
    tap(eds[0]['x'], eds[0]['y']); time.sleep(1.2); print(f"tapped field '{sys.argv[2]}' at {eds[0]['x']},{eds[0]['y']}")
elif cmd == 'type':
    sh('shell', 'input', 'text', sys.argv[2].replace(' ', '%s')); time.sleep(1.0)
elif cmd == 'shot':
    time.sleep(0.7)
    with open(f'{D}/{sys.argv[2]}.png', 'wb') as f:
        f.write(subprocess.run(list(ADB) + ['exec-out', 'screencap', '-p'], capture_output=True).stdout)
    print(f"shot {sys.argv[2]}.png")
elif cmd == 'dump':
    for n in dump():
        lab = n['text'] or n['desc']
        if lab: print(f"{lab[:70]!r} {n['bounds']}")
elif cmd == 'edits':
    for i, n in enumerate([n for n in dump() if 'EditText' in n['cls']], 1):
        print(f"#{i} text={n['text']!r} {n['bounds']}")
elif cmd == 'back': sh('shell', 'input', 'keyevent', '4'); time.sleep(0.6)
elif cmd == 'key': sh('shell', 'input', 'keyevent', *sys.argv[2:]); time.sleep(0.6)
elif cmd == 'clear':
    sh('shell', 'input', 'keyevent', '123'); time.sleep(0.3)
    sh('shell', 'input', 'keyevent', *(['67'] * 25)); time.sleep(0.8)
elif cmd == 'hidekb':
    r = sh('shell', 'dumpsys', 'input_method', capture=True)
    if 'mInputShown=true' in r.stdout:
        sh('shell', 'input', 'keyevent', '4'); time.sleep(0.8); print('keyboard hidden')
    else: print('keyboard was not shown')
elif cmd == 'tapxy': tap(int(sys.argv[2]), int(sys.argv[3]))
elif cmd == 'swipe': sh('shell', 'input', 'swipe', '540', '1800', '540', '700', '400'); time.sleep(0.6)
