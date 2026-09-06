import * as M from './models.js';

const I18N = {
  en: { open: 'Open backup', download: 'Download backup', appTitle: 'Backup viewer', wTitle: 'Your vineyard log, in the browser', oFile: 'Choose a file', oFileHint: 'The backup the app saved to your folder or exported from Settings.', oUrl: 'Load from a link', oUrlHint: 'A public link to the JSON (e.g. a Nextcloud / Drive share); the server must allow cross-site reads.', oPaste: 'Paste JSON', oPasteHint: 'Open the backup in any text app, copy everything and paste it here.', choose: 'Choose file', loadBtn: 'Load', badJson: 'That is not a Vineyard Log backup (JSON with schemaVersion and entries).', urlFail: 'Could not load the link (not reachable or the server blocks cross-site reads).', wText: 'Open the JSON backup the app writes to your synced folder (vineyard-log-latest.json) or exports from Settings. Everything runs in this browser; nothing is uploaded anywhere.', drop: 'Drop the backup file here', tabs: { overview: 'Overview', log: 'Log', blocks: 'Blocks', cellar: 'Cellar', weather: 'Weather', products: 'Products', add: 'New entry' },
    risk: 'Disease risk', peronospora: 'Downy mildew', oidium: 'Powdery mildew (Kast-style index)', botrytis: 'Botrytis', level: { LOW: 'low', MODERATE: 'moderate', HIGH: 'high' }, noRisk: 'No weather days with temperatures in the last 40 days.', primary: 'Primary infection conditions on', index: 'Index', wetDays: 'wet days in the last 3',
    steberla: 'Downy mildew by Šteberla', zone: { NON_CALAMITOUS: 'Non-calamitous zone: basic sprays only (before flowering, after flowering, before berry softening of early varieties).', SPORADIC: 'Sporadic-calamitous zone: basic sprays plus further ones at discretion.', CALAMITOUS: 'Calamitous zone: protect regularly as the vine grows.' }, steberlaBefore: 'Evaluation starts on 14 May.', since: 'Since 1 May', curveA: 'Curve A', curveB: 'Curve B', cumRain: 'Cumulative rain',
    seasonWeather: 'Season weather', tmax: 'T max', tmin: 'T min', rain: 'Rain', sprays: 'sprays', gddByVintage: 'Cumulative GDD by vintage', gdd: 'GDD', harvestByYear: 'Harvest by year', block: 'Block', harvest: 'Harvest', kgPerVine: 'kg/vine', ripening: 'Ripening – vineyard readings (°NM)', fermentation: 'Fermentation – cellar readings (°NM)', daysSince: 'days since the start of the batch', year: 'Year', all: 'all', domain: { VINEYARD: 'Vineyard', CELLAR: 'Cellar' }, type: 'Type', filter: 'Filter', entries: 'entries', noEntries: 'No entries match.',
    season: 'Season', earliest: 'Earliest harvest (PHI)', cu: 'Cu this season', of: 'of', npk: 'Fertilised: N %n · P₂O₅ %p · K₂O %k kg/ha', vines: 'vines', planted: 'planted', latest: 'Latest readings', volume: 'Volume', grapes: 'Grapes', yeast: 'Yeast', start: 'Start', status: 'Status', date: 'Date', source: 'Source', typed: 'typed', products: 'Products', category: 'Category', phi: 'PHI', supplier: 'Supplier', dose: 'Dose',
    newEntry: 'New entry (added to this backup; download it and use Import backup → merge in the app)', save: 'Add entry', title: 'Title', notes: 'Notes', batch: 'Batch', none: '—', measurement: 'Reading', value: 'Value', added: 'Entry added. Download the backup to move it into the app.', loaded: 'Backup loaded', exported: 'exported', stored: 'restored from this browser' },
  cs: { open: 'Otevřít zálohu', download: 'Stáhnout zálohu', appTitle: 'Prohlížeč zálohy', wTitle: 'Tvůj vinařský deník v prohlížeči', oFile: 'Vybrat soubor', oFileHint: 'Záloha, kterou aplikace uložila do složky nebo exportovala v Nastavení.', oUrl: 'Načíst z adresy', oUrlHint: 'Veřejný odkaz na JSON (např. sdílení z Nextcloudu / Disku); server musí povolit čtení z jiného webu.', oPaste: 'Vložit JSON', oPasteHint: 'Otevři zálohu v libovolném textovém editoru, zkopíruj všechno a vlož sem.', choose: 'Vybrat soubor', loadBtn: 'Načíst', badJson: 'Tohle není záloha Vineyard Logu (JSON se schemaVersion a entries).', urlFail: 'Odkaz se nepodařilo načíst (nedostupný, nebo server nepovoluje čtení z jiného webu).', wText: 'Otevři JSON zálohu, kterou aplikace píše do synchronizované složky (vineyard-log-latest.json) nebo exportuje v Nastavení. Všechno běží v tomto prohlížeči, nic se nikam neposílá.', drop: 'Sem přetáhni soubor zálohy', tabs: { overview: 'Přehled', log: 'Deník', blocks: 'Tratě', cellar: 'Sklep', weather: 'Počasí', products: 'Produkty', add: 'Nový záznam' },
    risk: 'Riziko chorob', peronospora: 'Plíseň révová', oidium: 'Padlí (index podle Kasta)', botrytis: 'Botrytida', level: { LOW: 'nízké', MODERATE: 'střední', HIGH: 'vysoké' }, noRisk: 'V posledních 40 dnech nejsou dny s teplotami.', primary: 'Podmínky primární infekce', index: 'Index', wetDays: 'vlhké dny z posledních 3',
    steberla: 'Plíseň révová podle Šteberly', zone: { NON_CALAMITOUS: 'Nekalamitní pásmo: jen základní postřiky (před květem, po odkvětu, před zaměkáním raných odrůd).', SPORADIC: 'Sporadicko-kalamitní pásmo: základní postřiky a podle uvážení další.', CALAMITOUS: 'Kalamitní pásmo: stříkat pravidelně podle přírůstků.' }, steberlaBefore: 'Hodnocení začíná 14. 5.', since: 'Od 1. 5.', curveA: 'Křivka A', curveB: 'Křivka B', cumRain: 'Kumulativní srážky',
    seasonWeather: 'Počasí sezóny', tmax: 'T max', tmin: 'T min', rain: 'Srážky', sprays: 'postřiky', gddByVintage: 'Kumulativní GDD po ročnících', gdd: 'GDD', harvestByYear: 'Sklizeň po letech', block: 'Trať', harvest: 'Sklizeň', kgPerVine: 'kg/keř', ripening: 'Zrání – měření z vinice (°NM)', fermentation: 'Kvašení – měření ze sklepa (°NM)', daysSince: 'dnů od začátku šarže', year: 'Rok', all: 'vše', domain: { VINEYARD: 'Vinice', CELLAR: 'Sklep' }, type: 'Typ', filter: 'Filtr', entries: 'záznamů', noEntries: 'Filtru neodpovídá žádný záznam.',
    season: 'Sezóna', earliest: 'Nejdřívější sklizeň (OL)', cu: 'Cu letos', of: 'z', npk: 'Pohnojeno: N %n · P₂O₅ %p · K₂O %k kg/ha', vines: 'keřů', planted: 'výsadba', latest: 'Poslední měření', volume: 'Objem', grapes: 'Hrozny', yeast: 'Kvasinky', start: 'Začátek', status: 'Stav', date: 'Datum', source: 'Zdroj', typed: 'ručně', products: 'Produkty', category: 'Kategorie', phi: 'OL', supplier: 'Dodavatel', dose: 'Dávka',
    newEntry: 'Nový záznam (přidá se do této zálohy; stáhni ji a v aplikaci použij Import zálohy → sloučit)', save: 'Přidat záznam', title: 'Název', notes: 'Poznámky', batch: 'Šarže', none: '—', measurement: 'Měření', value: 'Hodnota', added: 'Záznam přidán. Stáhni zálohu a načti ji v aplikaci.', loaded: 'Záloha načtena', exported: 'export', stored: 'obnoveno z tohoto prohlížeče' },
};
const params = new URLSearchParams(location.search);
let stored = null; try { stored = localStorage.getItem('vineyard-log-lang'); } catch (e) { /* ignore */ }
let lang = ['cs', 'en'].includes(params.get('lang')) ? params.get('lang') : ['cs', 'en'].includes(stored) ? stored : 'cs';
let T = I18N[lang];
let labels = { en: {}, cs: {} };
let data = null;
let tab = 'overview';
let year = new Date().getFullYear();
const charts = [];
const $ = (id) => document.getElementById(id);
const el = (tag, attrs = {}, ...children) => { const e = document.createElement(tag); for (const [k, v] of Object.entries(attrs)) { if (k === 'class') e.className = v; else if (k === 'html') e.innerHTML = v; else if (k.startsWith('on')) e.addEventListener(k.slice(2), v); else e.setAttribute(k, v); } for (const c of children.flat()) if (c != null) e.append(c.nodeType ? c : document.createTextNode(String(c))); return e; };
const L = (group, key) => (labels[lang]?.[group]?.[key]) ?? key;
const kindLabel = (key) => labels[lang]?.measurementKinds?.[key]?.label ?? key;
const kindUnit = (key) => labels[lang]?.measurementKinds?.[key]?.unit ?? '';

window.addEventListener('error', (e) => { const b = $('error'); b.textContent = `${e.message}`; b.hidden = false; });
window.addEventListener('unhandledrejection', (e) => { const b = $('error'); b.textContent = `${e.reason?.message || e.reason}`; b.hidden = false; });

async function init() {
  try { labels = await (await fetch('labels.json')).json(); } catch (e) { /* labels optional */ }
  applyLang();
  $('file').addEventListener('change', (e) => e.target.files[0] && readFile(e.target.files[0]));
  const drop = $('drop');
  drop.addEventListener('dragover', (e) => { e.preventDefault(); drop.classList.add('over'); });
  drop.addEventListener('dragleave', () => drop.classList.remove('over'));
  drop.addEventListener('drop', (e) => { e.preventDefault(); drop.classList.remove('over'); const f = e.dataTransfer.files[0]; if (f) readFile(f); });
  $('btn-lang').addEventListener('click', () => { lang = lang === 'cs' ? 'en' : 'cs'; T = I18N[lang]; try { localStorage.setItem('vineyard-log-lang', lang); } catch (e) { /* ignore */ } applyLang(); if (data) render(); });
  $('btn-download').addEventListener('click', download);
  $('btn-pick').addEventListener('click', () => $('file').click());
  $('btn-url').addEventListener('click', async () => { const u = $('url-input').value.trim(); if (!u) return; try { const r = await fetch(u); if (!r.ok) throw new Error(r.status); load(await r.text(), 'url'); } catch (e) { showError(new Error(T.urlFail)); } });
  $('btn-paste').addEventListener('click', () => { const t = $('paste-input').value.trim(); if (t) load(t, 'file'); });
  const url = params.get('backup');
  if (url) { try { load(await (await fetch(url)).text(), 'url'); } catch (e) { showError(e); } }
  else { try { const s = localStorage.getItem('vineyard-log-backup'); if (s) load(s, 'stored'); } catch (e) { /* ignore */ } }
}
function applyLang() { for (const [id, key] of [['o-file', 'oFile'], ['o-file-hint', 'oFileHint'], ['o-url', 'oUrl'], ['o-url-hint', 'oUrlHint'], ['o-paste', 'oPaste'], ['o-paste-hint', 'oPasteHint'], ['btn-pick', 'choose'], ['btn-url', 'loadBtn'], ['btn-paste', 'loadBtn']]) { const e = $(id); if (e) e.textContent = T[key]; }
  $('lbl-open').textContent = T.open; $('btn-download').textContent = T.download; $('btn-lang').textContent = lang === 'cs' ? 'EN' : 'CS'; $('title').textContent = T.appTitle; $('w-title').textContent = T.wTitle; $('w-text').textContent = T.wText; $('w-drop').textContent = T.drop; document.documentElement.lang = lang; }
function showError(e) { const b = $('error'); b.textContent = e.message || String(e); b.hidden = false; }
function readFile(file) { const r = new FileReader(); r.onload = () => load(r.result, 'file'); r.readAsText(file); }

function load(text, origin) {
  let raw; try { raw = JSON.parse(text); } catch (e) { showError(new Error(T.badJson)); return; }
  if (!raw || typeof raw !== 'object' || !Array.isArray(raw.entries)) { showError(new Error(T.badJson)); return; }
  const usagesByEntry = new Map(); for (const u of raw.usages || []) { (usagesByEntry.get(u.entryId) || usagesByEntry.set(u.entryId, []).get(u.entryId)).push(u); }
  const measByEntry = new Map(); for (const m of raw.measurements || []) { (measByEntry.get(m.entryId) || measByEntry.set(m.entryId, []).get(m.entryId)).push(m); }
  const photosByEntry = new Map(); for (const p of raw.photos || []) { (photosByEntry.get(p.entryId) || photosByEntry.set(p.entryId, []).get(p.entryId)).push(p); }
  data = {
    raw, blocks: raw.blocks || [], products: raw.products || [], batches: raw.batches || [], weather: (raw.weather || []).slice().sort((a, b) => a.date - b.date),
    entries: (raw.entries || []).map(e => ({ ...e, usages: usagesByEntry.get(e.id) || [], measurements: measByEntry.get(e.id) || [], photos: photosByEntry.get(e.id) || [] })).sort((a, b) => b.date - a.date),
    productById: new Map((raw.products || []).map(p => [p.id, p])), blockById: new Map((raw.blocks || []).map(b => [b.id, b])), batchById: new Map((raw.batches || []).map(b => [b.id, b])),
  };
  try { if (origin !== 'stored') localStorage.setItem('vineyard-log-backup', text); } catch (e) { /* too large or blocked */ }
  $('error').hidden = true;
  // default to the current year when it has data, else the latest year with data
  const dataYears = new Set([...data.entries.map(e => M.yearOf(e.date)), ...data.weather.map(w => M.yearOf(w.date)), ...data.batches.map(b => b.vintage)]);
  if (origin !== 'edit' && !dataYears.has(year)) year = dataYears.size ? Math.max(...dataYears) : new Date().getFullYear();
  $('welcome').hidden = true; $('tabs').hidden = false; $('btn-download').hidden = false;
  render();
}
function download() { const out = { ...data.raw, exportedAt: Date.now() }; const blob = new Blob([JSON.stringify(out, null, 1)], { type: 'application/json' }); const a = el('a', { href: URL.createObjectURL(blob), download: 'vineyard-log-web.json' }); document.body.append(a); a.click(); a.remove(); }

function render() {
  const tabs = $('tabs'); tabs.innerHTML = '';
  for (const key of Object.keys(T.tabs)) tabs.append(el('button', { class: key === tab ? 'active' : '', onclick: () => { tab = key; render(); } }, T.tabs[key]));
  charts.splice(0).forEach(c => c.destroy());
  const v = $('view'); v.innerHTML = '';
  const years = [...new Set([...data.entries.map(e => M.yearOf(e.date)), ...data.weather.map(w => M.yearOf(w.date)), ...data.batches.map(b => b.vintage), new Date().getFullYear()])].sort((a, b) => b - a);
  const yearSel = el('select', { onchange: (e) => { year = Number(e.target.value); render(); } }, years.map(y => el('option', { value: y, ...(y === year ? { selected: '' } : {}) }, y)));
  if (['overview', 'blocks', 'cellar', 'weather', 'log'].includes(tab)) v.append(el('div', { class: 'row' }, el('b', {}, T.year + ': '), yearSel));
  ({ overview, log, blocks, cellar, weather, products, add })[tab](v);
}

function chart(parent, config) { const c = el('canvas'); parent.append(c); charts.push(new Chart(c, config)); }
const dayLabel = (ed) => M.fmtDate(ed, lang).replace(/\s?\d{4}$/, '');

function overview(v) {
  const today = M.epochDay(new Date()); const thisYear = new Date().getFullYear();
  if (year === thisYear) {
    const recent = data.weather.filter(d => d.date >= today - 40);
    const budBreak = data.entries.filter(e => e.type === 'PHENOLOGY' && e.phenologyStage === 'BUD_BREAK' && M.yearOf(e.date) === thisYear).map(e => e.date);
    const shootsOut = budBreak.length ? Math.min(...budBreak) + 15 : M.ed(thisYear, 5, 1);
    const r = M.diseaseRisk(recent, shootsOut);
    const card = el('div', { class: 'card' }, el('h2', {}, T.risk + (r ? ' · ' + M.fmtDate(r.date, lang) : '')));
    if (!r) card.append(el('p', { class: 'muted' }, T.noRisk));
    else { const pill = (lv) => el('span', { class: 'pill ' + (lv === 'HIGH' ? 'high' : lv === 'MODERATE' ? 'mod' : 'low') }, T.level[lv]);
      card.append(el('p', {}, el('b', {}, T.peronospora + ' '), pill(r.peronospora), r.primaryInfectionDate ? el('span', { class: 'muted' }, ` · ${T.primary} ${M.fmtDate(r.primaryInfectionDate, lang)}`) : null));
      card.append(el('p', {}, el('b', {}, T.oidium + ' '), pill(r.oidium), el('span', { class: 'muted' }, ` · ${T.index} ${r.oidiumIndex}/100`)));
      card.append(el('p', {}, el('b', {}, T.botrytis + ' '), pill(r.botrytis), el('span', { class: 'muted' }, ` · ${r.wetDaysLast3} ${T.wetDays}`))); }
    v.append(card);
  }
  const yw = data.weather.filter(d => M.yearOf(d.date) === year);
  const ye = data.entries.filter(e => M.yearOf(e.date) === year);
  // Šteberla
  const st = M.steberla.evaluate(yw, year, year === thisYear ? today : M.ed(year, 7, 30));
  const sc = el('div', { class: 'card' }, el('h2', {}, `${T.steberla} ${year}`));
  if (!st) sc.append(el('p', { class: 'muted' }, T.steberlaBefore));
  else { const may1 = M.ed(year, 5, 1); const days = [...Array(M.steberla.LAST_DAY + 1).keys()];
    chart(sc, { type: 'line', data: { labels: days.map(i => dayLabel(may1 + i)), datasets: [
      { label: T.cumRain, data: days.map(i => st.series[i]?.[1] ?? null), borderColor: '#212121', pointRadius: 0, borderWidth: 2 },
      { label: T.curveA, data: days.map(i => M.steberla.a(i)), borderColor: '#f9a825', pointRadius: 0, borderWidth: 2 },
      { label: T.curveB, data: days.map(i => M.steberla.b(i)), borderColor: '#c62828', pointRadius: 0, borderWidth: 2 }] }, options: { animation: false, scales: { x: { ticks: { maxTicksLimit: 6 } } } } });
    sc.append(el('p', { class: 'muted' }, `${T.since} ${M.fmt(st.cumulativeMm, 0)} mm · ${T.curveA} ${M.fmt(st.a, 0)} · ${T.curveB} ${M.fmt(st.b, 0)} mm`));
    sc.append(el('p', { style: st.zone === 'CALAMITOUS' ? 'color:#b3261e' : st.zone === 'SPORADIC' ? 'color:#b26a00' : 'color:#3e5a2b' }, T.zone[st.zone])); }
  v.append(sc);
  // season weather
  const wc = el('div', { class: 'card' }, el('h2', {}, `${T.seasonWeather} ${year}`));
  if (yw.length) { const sprayDays = new Set(ye.filter(e => e.type === 'SPRAY').map(e => e.date)); const sum = M.seasonSummary(yw, year);
    chart(wc, { data: { labels: yw.map(d => dayLabel(d.date)), datasets: [
      { type: 'line', label: T.tmax, data: yw.map(d => d.tMax), borderColor: '#d84315', pointRadius: 0, borderWidth: 1.5, yAxisID: 'y' },
      { type: 'line', label: T.tmin, data: yw.map(d => d.tMin), borderColor: '#1565c0', pointRadius: 0, borderWidth: 1.5, yAxisID: 'y' },
      { type: 'bar', label: T.rain, data: yw.map(d => d.rainMm ?? 0), backgroundColor: 'rgba(66,165,245,0.6)', yAxisID: 'y1' },
      { type: 'bar', label: T.sprays, data: yw.map(d => sprayDays.has(d.date) ? 40 : null), backgroundColor: '#3e5a2b', yAxisID: 'y', barThickness: 2 }] },
      options: { animation: false, scales: { x: { ticks: { maxTicksLimit: 8 } }, y: { position: 'left' }, y1: { position: 'right', grid: { drawOnChartArea: false } } } } });
    wc.append(el('p', { class: 'muted' }, `${T.gdd} ${M.fmt(sum.gdd, 0)} · ${T.rain} ${M.fmt(sum.rain, 0)} mm · ${sprayDays.size} ${T.sprays}`)); }
  else wc.append(el('p', { class: 'muted' }, '–'));
  v.append(wc);
  // GDD by vintage
  const gy = [...new Set(data.weather.map(d => M.yearOf(d.date)))].sort();
  if (gy.length > 1) { const gc = el('div', { class: 'card' }, el('h2', {}, T.gddByVintage)); const palette = ['#3e5a2b', '#6a3e8c', '#b26a00', '#1565c0', '#8e24aa', '#00897b'];
    chart(gc, { type: 'line', data: { datasets: gy.map((y, i) => ({ label: String(y), data: M.gddCumulative(data.weather, y).map(([d, g]) => ({ x: M.dayOfYear(d), y: Math.round(g) })), borderColor: palette[i % palette.length], pointRadius: 0, borderWidth: 2, parsing: false })) }, options: { animation: false, scales: { x: { type: 'linear', min: 90, max: 305, ticks: { callback: (val) => dayLabel(M.ed(year, 1, 1) + val - 1), maxTicksLimit: 6 } } } } });
    v.append(gc); }
  // ripening (vineyard) and fermentation (cellar)
  const vy = ye.filter(e => e.domain === 'VINEYARD'); const sugar = vy.flatMap(e => e.measurements.map(m => ({ m, e })).filter(x => M.toNm(x.m.value, x.m.kind) != null));
  if (sugar.length) { const rc = el('div', { class: 'card' }, el('h2', {}, `${T.ripening} ${year}`)); const byBlock = new Map(); for (const { m, e } of sugar) (byBlock.get(e.blockId) || byBlock.set(e.blockId, []).get(e.blockId)).push({ x: M.dayOfYear(m.date), y: M.toNm(m.value, m.kind) }); const palette = ['#3e5a2b', '#6a3e8c', '#b26a00', '#1565c0'];
    chart(rc, { type: 'line', data: { datasets: [...byBlock.entries()].map(([bid, pts], i) => ({ label: data.blockById.get(bid)?.name ?? '?', data: pts.sort((a, b) => a.x - b.x), borderColor: palette[i % palette.length], parsing: false })) }, options: { animation: false, scales: { x: { type: 'linear', ticks: { callback: (val) => dayLabel(M.ed(year, 1, 1) + val - 1), maxTicksLimit: 6 } } } } }); v.append(rc); }
  const batches = data.batches.filter(b => b.vintage === year); const ferm = batches.map(b => { const rs = data.entries.filter(e => e.batchId === b.id).flatMap(e => e.measurements); const start = b.startDate ?? (rs.length ? Math.min(...rs.map(m => m.date)) : null); if (start == null) return null; const pts = rs.filter(m => M.toNm(m.value, m.kind) != null).map(m => ({ x: m.date - start, y: M.toNm(m.value, m.kind) })).sort((a, b) => a.x - b.x); return pts.length ? { b, pts } : null; }).filter(Boolean);
  if (ferm.length) { const fc = el('div', { class: 'card' }, el('h2', {}, `${T.fermentation} ${year}`)); const palette = ['#3e5a2b', '#6a3e8c', '#b26a00', '#1565c0'];
    chart(fc, { type: 'line', data: { datasets: ferm.map(({ b, pts }, i) => ({ label: b.name, data: pts, borderColor: palette[i % palette.length], parsing: false })) }, options: { animation: false, scales: { x: { type: 'linear', title: { display: true, text: T.daysSince } } } } }); v.append(fc); }
  // harvest table
  const harvests = data.entries.filter(e => e.type === 'HARVEST' && (e.quantity ?? 0) > 0);
  if (harvests.length) { const hc = el('div', { class: 'card' }, el('h2', {}, T.harvestByYear)); const groups = new Map(); for (const e of harvests) { const k = `${M.yearOf(e.date)}|${e.blockId}`; (groups.get(k) || groups.set(k, []).get(k)).push(e); }
    const tbl = el('table', {}, el('tr', {}, el('th', {}, T.block), el('th', {}, T.harvest), el('th', {}, T.kgPerVine), el('th', {}, '°NM')));
    for (const [k, list] of [...groups.entries()].sort((a, b) => b[0].localeCompare(a[0]))) { const [y, bid] = k.split('|'); const b = data.blockById.get(Number(bid)); const kg = list.reduce((s, e) => s + (e.quantity ?? 0), 0); const nm = list.flatMap(e => e.measurements.map(m => M.toNm(m.value, m.kind)).filter(x => x != null)); tbl.append(el('tr', {}, el('td', {}, `${y} · ${b?.name ?? '–'}`), el('td', {}, `${M.fmt(kg, 0)} kg`), el('td', {}, b?.vineCount ? M.fmt(kg / b.vineCount, 2) : '–'), el('td', {}, nm.length ? M.fmt(nm.reduce((a, c) => a + c) / nm.length, 1) : '–'))); }
    hc.append(tbl); v.append(hc); }
}

let logFilter = { domain: '', type: '' };
function log(v) {
  const list = data.entries.filter(e => M.yearOf(e.date) === year && (!logFilter.domain || e.domain === logFilter.domain) && (!logFilter.type || e.type === logFilter.type));
  const types = [...new Set(data.entries.map(e => e.type))];
  v.append(el('div', { class: 'row' }, el('select', { onchange: (e) => { logFilter.domain = e.target.value; render(); } }, el('option', { value: '' }, T.all), ['VINEYARD', 'CELLAR'].map(d => el('option', { value: d, ...(logFilter.domain === d ? { selected: '' } : {}) }, T.domain[d]))),
    el('select', { onchange: (e) => { logFilter.type = e.target.value; render(); } }, el('option', { value: '' }, T.type + ': ' + T.all), types.map(t => el('option', { value: t, ...(logFilter.type === t ? { selected: '' } : {}) }, L('entryTypes', t)))), el('span', { class: 'muted' }, `${list.length} ${T.entries}`)));
  if (!list.length) v.append(el('p', { class: 'muted' }, T.noEntries));
  for (const e of list) v.append(entryCard(e));
}
function entryCard(e) {
  const target = e.blockId != null ? data.blockById.get(e.blockId)?.name : e.batchId != null ? data.batchById.get(e.batchId)?.name : null;
  const c = el('div', { class: 'entry' }, el('div', { class: 'head' }, el('span', { class: 'date' }, M.fmtDate(e.date, lang)), el('b', {}, e.title || L('entryTypes', e.type)), target ? el('span', { class: 'muted' }, '· ' + target) : null, e.title ? el('span', { class: 'pill' }, L('entryTypes', e.type)) : null));
  if (e.phenologyStage) c.append(el('div', { class: 'small' }, L('stages', e.phenologyStage)));
  if (e.quantity != null) c.append(el('div', { class: 'small' }, `${M.fmt(e.quantity)} ${e.quantityUnit || ''}`));
  if (e.usages.length) c.append(el('div', { class: 'small' }, e.usages.map(u => `${data.productById.get(u.productId)?.name ?? '?'} ${u.dose != null ? M.fmt(u.dose) + ' ' + (u.doseUnit || '') : ''}`.trim()).join(' · ')));
  if (e.measurements.length) c.append(el('div', { class: 'small' }, e.measurements.map(m => `${kindLabel(m.kind)} ${M.fmt(m.value)} ${kindUnit(m.kind)}`.trim()).join(' · ')));
  if (e.notes) c.append(el('div', { class: 'muted small' }, e.notes));
  return c;
}

function blocks(v) {
  const grid = el('div', { class: 'grid' });
  for (const b of data.blocks.filter(b => !b.archived)) {
    const be = data.entries.filter(e => e.blockId === b.id); const ye = be.filter(e => M.yearOf(e.date) === year);
    const sprays = ye.filter(e => e.type === 'SPRAY').length; const phi = M.earliestHarvest(ye, data.productById); const cu = M.seasonCopper(ye, year, b, data.productById); const npk = M.seasonNpk(ye, year, b, data.productById);
    const harvestKg = ye.filter(e => e.type === 'HARVEST').reduce((s, e) => s + (e.quantity ?? 0), 0);
    const card = el('div', { class: 'card' }, el('h2', {}, b.name), el('div', { class: 'muted' }, [b.variety, b.vineCount ? `${b.vineCount} ${T.vines}` : null, b.plantedYear ? `${T.planted} ${b.plantedYear}` : null].filter(Boolean).join(' · ')));
    card.append(el('p', {}, `${T.season} ${year}: ${sprays} ${T.sprays}` + (harvestKg ? ` · ${T.harvest} ${M.fmt(harvestKg, 0)} kg` : '')));
    if (phi != null) card.append(el('div', { class: 'small' }, `${T.earliest}: ${M.fmtDate(phi, lang)}`));
    if (cu > 0) card.append(el('div', { class: 'small', style: cu >= 3 ? 'color:#b3261e' : '' }, `${T.cu}: ${M.fmt(cu, 2)} ${T.of} 4 kg/ha`));
    if (npk.n + npk.p + npk.k > 0) card.append(el('div', { class: 'small' }, T.npk.replace('%n', M.fmt(npk.n, 0)).replace('%p', M.fmt(npk.p, 0)).replace('%k', M.fmt(npk.k, 0))));
    const latest = new Map(); for (const e of ye.filter(e => e.domain === 'VINEYARD')) for (const m of e.measurements) { const cur = latest.get(m.kind); if (!cur || m.date > cur.date) latest.set(m.kind, m); }
    if (latest.size) card.append(el('div', { class: 'small muted' }, `${T.latest}: ` + [...latest.values()].map(m => `${kindLabel(m.kind)} ${M.fmt(m.value)} ${kindUnit(m.kind)} (${M.fmtDate(m.date, lang)})`).join(' · ')));
    grid.append(card);
  }
  v.append(grid);
}

function cellar(v) {
  const grid = el('div', { class: 'grid' });
  for (const b of data.batches.filter(b => b.vintage === year)) {
    const be = data.entries.filter(e => e.batchId === b.id); const ms = be.flatMap(e => e.measurements);
    const card = el('div', { class: 'card' }, el('h2', {}, `${b.name} (${b.vintage})`), el('div', { class: 'muted' }, [L('wineStyles', b.style), b.variety, b.status].filter(Boolean).join(' · ')));
    card.append(el('div', { class: 'small' }, [b.volumeL ? `${T.volume} ${M.fmt(b.volumeL)} l` : null, b.grapesKg ? `${T.grapes} ${M.fmt(b.grapesKg)} kg` : null, b.yeast ? `${T.yeast} ${b.yeast}` : null, b.startDate ? `${T.start} ${M.fmtDate(b.startDate, lang)}` : null].filter(Boolean).join(' · ')));
    const latest = new Map(); for (const m of ms) { const cur = latest.get(m.kind); if (!cur || m.date > cur.date) latest.set(m.kind, m); }
    if (latest.size) card.append(el('div', { class: 'small muted' }, `${T.latest}: ` + [...latest.values()].map(m => `${kindLabel(m.kind)} ${M.fmt(m.value)} ${kindUnit(m.kind)}`).join(' · ')));
    for (const e of be.slice(0, 6)) card.append(entryCard(e));
    grid.append(card);
  }
  v.append(grid);
}

function weather(v) {
  const yw = data.weather.filter(d => M.yearOf(d.date) === year).sort((a, b) => b.date - a.date);
  const tbl = el('table', {}, el('tr', {}, el('th', {}, T.date), el('th', {}, T.tmin), el('th', {}, T.tmax), el('th', {}, T.rain), el('th', {}, 'RH'), el('th', {}, T.gdd), el('th', {}, T.source)));
  for (const d of yw) tbl.append(el('tr', {}, el('td', {}, M.fmtDate(d.date, lang)), el('td', {}, M.fmt(d.tMin)), el('td', {}, M.fmt(d.tMax)), el('td', {}, M.fmt(d.rainMm)), el('td', {}, M.fmt(d.humidityPct, 0)), el('td', {}, M.fmt(M.gddDaily(d), 1)), el('td', { class: 'muted' }, d.source || T.typed)));
  v.append(tbl);
}

function products(v) {
  const tbl = el('table', {}, el('tr', {}, el('th', {}, T.products), el('th', {}, T.category), el('th', {}, T.supplier), el('th', {}, T.dose), el('th', {}, T.phi)));
  for (const p of data.products.filter(p => !p.archived).sort((a, b) => a.name.localeCompare(b.name))) tbl.append(el('tr', {}, el('td', {}, el('b', {}, p.name), el('div', { class: 'muted small' }, [p.activeIngredient, p.purpose].filter(Boolean).join(' · '))), el('td', {}, L('productCategories', p.category)), el('td', {}, p.supplier), el('td', {}, p.doseMin != null ? `${M.fmt(p.doseMin)}${p.doseMax != null && p.doseMax !== p.doseMin ? '–' + M.fmt(p.doseMax) : ''} ${p.doseUnit || ''}` : '–'), el('td', {}, p.phiDays != null ? `${p.phiDays} d` : '–')));
  v.append(tbl);
}

function add(v) {
  v.append(el('p', { class: 'muted' }, T.newEntry));
  const types = Object.entries(labels[lang]?.entryDomains ?? {});
  const f = el('form', { class: 'new' });
  const domain = el('select', {}, ['VINEYARD', 'CELLAR'].map(d => el('option', { value: d }, T.domain[d])));
  const type = el('select', {});
  const fillTypes = () => { type.innerHTML = ''; types.filter(([, d]) => d === domain.value).forEach(([k]) => type.append(el('option', { value: k }, L('entryTypes', k)))); };
  domain.addEventListener('change', fillTypes); fillTypes();
  const date = el('input', { type: 'date', value: new Date().toISOString().slice(0, 10) });
  const block = el('select', {}, el('option', { value: '' }, T.block + ': ' + T.none), data.blocks.map(b => el('option', { value: b.id }, b.name)));
  const batch = el('select', {}, el('option', { value: '' }, T.batch + ': ' + T.none), data.batches.map(b => el('option', { value: b.id }, `${b.name} (${b.vintage})`)));
  const title = el('input', { type: 'text', placeholder: T.title });
  const kind = el('select', {}, el('option', { value: '' }, T.measurement + ': ' + T.none), Object.keys(labels[lang]?.measurementKinds ?? {}).map(k => el('option', { value: k }, kindLabel(k))));
  const value = el('input', { type: 'number', step: 'any', placeholder: T.value });
  const notes = el('textarea', { rows: 3, placeholder: T.notes, class: 'wide' });
  const msg = el('div', { class: 'muted wide' });
  f.append(domain, type, date, block, batch, title, kind, value, notes, el('button', { class: 'primary wide', type: 'submit' }, T.save), msg);
  f.addEventListener('submit', (ev) => {
    ev.preventDefault();
    const raw = data.raw; const id = Math.max(0, ...(raw.entries || []).map(e => e.id)) + 1; const d = M.epochDay(new Date(date.value + 'T00:00:00Z'));
    const entry = { id, date: d, domain: domain.value, type: type.value, blockId: domain.value === 'VINEYARD' && block.value ? Number(block.value) : null, batchId: domain.value === 'CELLAR' && batch.value ? Number(batch.value) : null, title: title.value.trim(), notes: notes.value.trim(), phenologyStage: null, waterLPerHa: null, sprayVolumeL: null, quantity: null, quantityUnit: '', tempC: null, windKmh: null, humidityPct: null, weatherNote: '', laborHours: null, cost: null, createdAt: Date.now(), guideKey: '' };
    raw.entries = [...(raw.entries || []), entry];
    if (kind.value && value.value !== '') { const mid = Math.max(0, ...(raw.measurements || []).map(m => m.id)) + 1; raw.measurements = [...(raw.measurements || []), { id: mid, entryId: id, date: d, blockId: entry.blockId, batchId: entry.batchId, kind: kind.value, value: Number(value.value), note: '' }]; }
    load(JSON.stringify(raw), 'edit'); tab = 'add'; render(); $('view').querySelector('.muted.wide') && ($('view').querySelector('.muted.wide').textContent = T.added);
  });
  v.append(f);
}

init();
