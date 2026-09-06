// Ports of the app's util/ package (Gdd, DiseaseRisk, Steberla, WineMath, Phi, Copper, Nutrients). Kept in step with the Kotlin unit tests.
export const DAY = 86400000;
export const epochDay = (d) => Math.floor(d.getTime() / DAY);
export const toDate = (ed) => new Date(ed * DAY);
export const ymd = (ed) => { const d = toDate(ed); return d.toISOString().slice(0, 10); };
export const fmtDate = (ed, lang) => { const d = toDate(ed); return lang === 'cs' ? `${d.getUTCDate()}. ${d.getUTCMonth() + 1}. ${d.getUTCFullYear()}` : d.toISOString().slice(0, 10); };
export const yearOf = (ed) => toDate(ed).getUTCFullYear();
export const dayOfYear = (ed) => { const d = toDate(ed); return Math.floor((Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate()) - Date.UTC(d.getUTCFullYear(), 0, 1)) / DAY) + 1; };
export const ed = (y, m, d) => Math.floor(Date.UTC(y, m - 1, d) / DAY);
export const fmt = (v, n = 1) => v == null ? '–' : Number(v).toFixed(n).replace(/\.0+$/, '').replace(/(\.\d*?)0+$/, '$1');

export const settings = { gddBase: 10, seasonStart: [4, 1], seasonEnd: [10, 31], targetNm: 21 };

// ---- GDD ----
export function gddDaily(day, base = settings.gddBase) { if (day.tMin == null || day.tMax == null) return null; return Math.max(0, (day.tMin + day.tMax) / 2 - base); }
export function seasonRange(year) { return [ed(year, settings.seasonStart[0], settings.seasonStart[1]), ed(year, settings.seasonEnd[0], settings.seasonEnd[1])]; }
export function gddCumulative(days, year) { const [a, b] = seasonRange(year); let s = 0; return days.filter(d => d.date >= a && d.date <= b).sort((x, y) => x.date - y.date).flatMap(d => { const g = gddDaily(d); if (g == null) return []; s += g; return [[d.date, s]]; }); }
export function seasonSummary(days, year) { const [a, b] = seasonRange(year); const inS = days.filter(d => d.date >= a && d.date <= b); return { gdd: inS.reduce((s, d) => s + (gddDaily(d) ?? 0), 0), rain: inS.reduce((s, d) => s + (d.rainMm ?? 0), 0), frost: days.filter(d => yearOf(d.date) === year && d.frost).length, days: inS.filter(d => gddDaily(d) != null).length }; }

// ---- sugar ----
export function toNm(value, kind) { switch (kind) { case 'NM': return value; case 'BRIX': return value / 1.025; case 'OECHSLE': return value / 4.25; case 'SG': return ((value - 1) * 1000) / 4.25; default: return null; } }
export const potentialAlcohol = (nm) => nm * 0.6;
export function sugarTrend(ms, maxPoints = 4) { const sugar = ms.filter(m => ['NM', 'BRIX', 'OECHSLE', 'SG'].includes(m.kind)).sort((a, b) => a.date - b.date); if (!sugar.length) return null; const kind = sugar[sugar.length - 1].kind; const same = sugar.filter(m => m.kind === kind).slice(-maxPoints); if (new Set(same.map(m => m.date)).size < 2) return null; const xs = same.map(m => m.date), ys = same.map(m => m.value); const mx = xs.reduce((a, b) => a + b) / xs.length, my = ys.reduce((a, b) => a + b) / ys.length; const sxx = xs.reduce((s, x) => s + (x - mx) ** 2, 0); const slope = sxx === 0 ? 0 : xs.reduce((s, x, i) => s + (x - mx) * (ys[i] - my), 0) / sxx; return { kind, lastDate: same[same.length - 1].date, lastValue: same[same.length - 1].value, perDay: slope, points: same.length }; }
export function daysTo(trend, target) { const diff = target - trend.lastValue; if (Math.abs(trend.perDay) < 1e-6) return null; const d = diff / trend.perDay; return d < 0 ? null : Math.floor(d) + 1; }

// ---- PHI ----
export function earliestHarvest(entries, productById) { let max = null; for (const e of entries) { if (e.type !== 'SPRAY' && e.type !== 'FERTILIZATION') continue; for (const u of e.usages) { const p = productById.get(u.productId); if (p && p.phiDays != null) { const t = e.date + p.phiDays; if (max == null || t > max) max = t; } } } return max; }

// ---- disease risk (weather only; same rules as DiseaseRisk.kt, no forecast) ----
const warmDay = (d) => d.warmHours != null ? d.warmHours >= 6 : (d.tMin != null && d.tMax != null && d.tMax >= 24 && d.tMax <= 33 && d.tMin >= 12);
const secondaryDay = (d) => (d.tMin ?? -99) >= 12 && (d.wetHours != null ? d.wetHours >= 4 : ((d.rainMm ?? 0) >= 1 || (d.humidityPct ?? 0) >= 85));
const wetDay = (d) => { const mean = ((d.tMin ?? 0) + (d.tMax ?? 0)) / 2; return ((d.rainMm ?? 0) >= 2 || (d.wetHours ?? 0) >= 6) && mean >= 15 && mean <= 25; };
export function diseaseRisk(days, shootsOut) {
  const sorted = days.filter(d => d.tMin != null && d.tMax != null).sort((a, b) => a.date - b.date);
  if (!sorted.length) return null;
  const byDate = new Map(sorted.map(d => [d.date, d]));
  let idx = 0, started = false, streak = 0; const index = new Map();
  for (const d of sorted) { const warm = warmDay(d); if (!started) { streak = warm ? streak + 1 : 0; if (streak >= 3) { started = true; idx = 60; } } else { idx += warm ? 20 : -10; if ((d.hotHours ?? 0) >= 1 || (d.tMax ?? 0) > 35) idx -= 10; idx = Math.max(0, Math.min(100, idx)); if (idx === 0) { started = false; streak = 0; } } index.set(d.date, started ? idx : 0); }
  let primary = null; for (const d of sorted) { if (d.date < shootsOut) continue; const prev = byDate.get(d.date - 1); const rain2 = (d.rainMm ?? 0) + (prev?.rainMm ?? 0); if ((d.tMin ?? -99) >= 10 && rain2 >= 10) primary = d.date; }
  const last = sorted[sorted.length - 1].date;
  const recent = [0, 1].map(i => byDate.get(last - i)).filter(Boolean); const secondary = recent.some(secondaryDay);
  const primaryRecent = primary != null && last - primary >= 0 && last - primary <= 10;
  const per = secondary && (byDate.get(last)?.rainMm ?? 0) > 0.5 ? 'HIGH' : (secondary || primaryRecent) ? 'MODERATE' : 'LOW';
  const oi = index.get(last) ?? 0; const oid = oi >= 60 ? 'HIGH' : oi >= 40 ? 'MODERATE' : 'LOW';
  const wet = [0, 1, 2].map(i => byDate.get(last - i)).filter(Boolean).filter(wetDay).length; const bot = wet === 0 ? 'LOW' : wet === 1 ? 'MODERATE' : 'HIGH';
  return { date: last, peronospora: per, primaryInfectionDate: primary, oidium: oid, oidiumIndex: oi, botrytis: bot, wetDaysLast3: wet };
}

// ---- Šteberla (Meteorologické zprávy 35/1982) ----
export const steberla = {
  FIRST_DAY: 13, LAST_DAY: 90,
  week: (d) => (d + 1) / 7,
  a(d) { const x = this.week(d); return Math.max(0, -37.529410 + 24.536249 * x - 0.380418 * x * x); },
  b(d) { const x = this.week(d); return Math.max(0, -1.073529 + 18.625645 * x + 0.650154 * x * x); },
  zone(mm, d) { return mm > this.b(d) ? 'CALAMITOUS' : mm > this.a(d) ? 'SPORADIC' : 'NON_CALAMITOUS'; },
  evaluate(days, year, at) {
    const may1 = ed(year, 5, 1); const end = Math.min(at, may1 + this.LAST_DAY); const d = end - may1; if (d < this.FIRST_DAY) return null;
    const byDate = new Map(days.filter(x => x.date >= may1 && x.date <= end).map(x => [x.date, x])); if (![...byDate.values()].some(x => x.rainMm != null)) return null;
    let sum = 0; const series = []; for (let i = 0; i <= d; i++) { sum += byDate.get(may1 + i)?.rainMm ?? 0; series.push([may1 + i, sum]); }
    return { date: end, daysSinceMay1: d, cumulativeMm: sum, a: this.a(d), b: this.b(d), zone: this.zone(sum, d), series, periodOver: at > may1 + this.LAST_DAY };
  },
};

// ---- copper and NPK ----
const norm = (s) => (s || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
const known = [['kuprikol 250', 250], ['kuprikol', 500], ['champion', 500], ['funguran', 500], ['kocide', 350], ['cuprozin', 250], ['flowbrix', 380], ['cuproxat', 190], ['airone', 272], ['cuprocaffaro', 375], ['bordeaux', 200], ['bordosk', 200]];
export function copperGPerKg(p) { if (p.copperGPerKg != null) return p.copperGPerKg; const text = norm(`${p.name} ${p.activeIngredient || ''}`); let m = text.match(/(\d+(?:[.,]\d+)?)\s*g\s*cu\s*\/\s*(?:kg|l)/); if (m) return parseFloat(m[1].replace(',', '.')); const cw = ['cu', 'med', 'copper', 'cupr', 'kupr'].some(w => text.includes(w)); if (cw) { m = text.match(/(\d+(?:[.,]\d+)?)\s*%/); if (m) return parseFloat(m[1].replace(',', '.')) * 10; } const name = norm(p.name); const k = known.find(([n]) => name.includes(n)); if (k) return k[1]; if (cw) { m = text.match(/(\d+(?:[.,]\d+)?)\s*g\s*\/\s*(?:kg|l)/); if (m) return parseFloat(m[1].replace(',', '.')); } return null; }
function perHa(u, block) { const unit = (u.doseUnit || '').trim().toLowerCase().replace(/\s/g, ''); if (u.dose != null && (unit === 'kg/ha' || unit === 'l/ha')) return u.dose; if (u.dose != null && (unit === 'g/ha' || unit === 'ml/ha')) return u.dose / 1000; if (u.totalAmount != null && block?.areaHa > 0) { const tu = (u.totalUnit || '').trim().toLowerCase(); if (tu === 'kg' || tu === 'l') return u.totalAmount / block.areaHa; if (tu === 'g' || tu === 'ml') return u.totalAmount / 1000 / block.areaHa; } return null; }
export function seasonCopper(entries, year, block, productById) { let s = 0; for (const e of entries) { if (e.type !== 'SPRAY' || yearOf(e.date) !== year) continue; for (const u of e.usages) { const p = productById.get(u.productId); if (!p) continue; const g = copperGPerKg(p); if (g == null) continue; const kg = perHa(u, block); if (kg != null) s += kg * g / 1000; } } return s; }
export function npkPercent(p) { const m = (p.npk || '').match(/(\d+(?:[.,]\d+)?)\s*[-–:/]\s*(\d+(?:[.,]\d+)?)\s*[-–:/]\s*(\d+(?:[.,]\d+)?)/) || (p.name || '').match(/(\d+(?:[.,]\d+)?)\s*[-–:/]\s*(\d+(?:[.,]\d+)?)\s*[-–:/]\s*(\d+(?:[.,]\d+)?)/); if (!m) return null; const f = (x) => parseFloat(x.replace(',', '.')); return { n: f(m[1]), p: f(m[2]), k: f(m[3]) }; }
export function seasonNpk(entries, year, block, productById) { const s = { n: 0, p: 0, k: 0 }; for (const e of entries) { if (e.type !== 'FERTILIZATION' || yearOf(e.date) !== year) continue; for (const u of e.usages) { const p = productById.get(u.productId); const pct = p && npkPercent(p); if (!pct) continue; const kg = perHa(u, block); if (kg == null) continue; s.n += kg * pct.n / 100; s.p += kg * pct.p / 100; s.k += kg * pct.k / 100; } } return s; }
