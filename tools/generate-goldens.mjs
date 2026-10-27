/* Generates app/src/test/resources/goldens.json from the verified JS engine.
 * Run: node tools/generate-goldens.mjs  (from ~/workspace/Zemanim)
 * The Kotlin unit test (EngineGoldenTest) compares the Kotlin port against
 * these values: Hebrew date exact, parsha/events exact, zmanim within 2 min.
 */
import { createRequire } from 'module';
import { writeFileSync } from 'fs';
const require = createRequire(import.meta.url);
const ZE = require('../../zemanim-app/engine.js');

const NY = { lat: 40.7128, lng: -74.006, tz: 'America/New_York', elevM: 10, name: 'New York' };
const JLM = { lat: 31.7683, lng: 35.2137, tz: 'Asia/Jerusalem', elevM: 750, name: 'Jerusalem' };
const REY = { lat: 64.1466, lng: -21.9426, tz: 'Atlantic/Reykjavik', elevM: 0, name: 'Reykjavik' };
const SYD = { lat: -33.8688, lng: 151.2093, tz: 'Australia/Sydney', elevM: 40, name: 'Sydney' };
const TRO = { lat: 69.6496, lng: 18.9560, tz: 'Europe/Oslo', elevM: 0, name: 'Tromso' };
const LA = { lat: 34.0522, lng: -118.2437, tz: 'America/Los_Angeles', elevM: 90, name: 'Los Angeles' };

const CASES = [
  // [y, m, d, loc, il, label]
  [2026, 10, 2, NY, false, 'Hoshana Rabba Fri NY'],
  [2026, 10, 3, NY, false, 'Shabbos Shemini Atzeres NY'],
  [2026, 10, 4, NY, false, 'Simchas Torah diaspora NY'],
  [2026, 9, 21, NY, false, 'Yom Kippur 5787 NY'],
  [2026, 9, 12, NY, false, 'Rosh Hashana day 1 NY'],
  [2026, 12, 5, NY, false, 'Chanukah 25 Kislev Shabbos NY'],
  [2026, 12, 20, NY, false, 'Asara BTeves NY'],
  [2026, 4, 1, NY, false, 'Erev Pesach chametz NY'],
  [2026, 4, 2, NY, false, 'Pesach day 1 NY'],
  [2026, 4, 20, NY, false, 'Omer day NY'],
  [2026, 10, 10, NY, false, 'Shabbos Mevarchim Cheshvan molad NY'],
  [2026, 9, 20, NY, false, 'Kiddush Levana window NY'],
  [2026, 1, 15, NY, false, 'Winter NY'],
  [2026, 6, 21, NY, false, 'Summer NY'],
  [2025, 10, 2, NY, false, 'Yom Kippur 5786 NY'],
  [2024, 10, 6, NY, false, 'Tzom Gedaliah shifted Sun NY'],
  [2024, 3, 25, NY, false, 'Purim leap year NY'],
  [2024, 3, 24, NY, false, 'Taanis Esther leap year NY'],
  [2026, 7, 30, NY, false, 'Tisha BAv 5786 NY'],
  [2026, 7, 23, NY, false, 'Tisha BAv observed 5786 NY'],
  [2026, 5, 14, NY, false, 'Lag BaOmer 5786 NY'],
  [2026, 5, 5, NY, false, 'Lag BaOmer actual 5786 NY'],
  [2026, 2, 28, NY, false, 'Regular Shabbos NY'],
  [2027, 9, 22, NY, false, 'RH 5788 NY'],
  [2026, 10, 2, JLM, true, 'Hoshana Rabba Jerusalem IL'],
  [2026, 10, 3, JLM, true, 'Shabbos Shemini Atzeres Jerusalem IL'],
  [2026, 4, 2, JLM, true, 'Pesach day 1 Jerusalem IL'],
  [2026, 6, 21, JLM, true, 'Summer Jerusalem IL'],
  [2026, 1, 15, JLM, true, 'Winter Jerusalem IL'],
  [2026, 10, 4, JLM, false, 'Simchas Torah diaspora-schedule in Jerusalem tz'],
  [2026, 6, 21, REY, false, 'Reykjavik near-midnight sun'],
  [2026, 12, 21, REY, false, 'Reykjavik short day'],
  [2026, 6, 21, SYD, false, 'Sydney southern winter'],
  [2026, 12, 21, SYD, false, 'Sydney southern summer'],
  [2026, 12, 21, TRO, false, 'Tromso polar night netz null'],
  [2026, 10, 2, LA, false, 'Los Angeles Oct 2'],
];

const ZMANIM_FIELDS = [
  'netz', 'shkia', 'alos161', 'alos18', 'alos198', 'alos72',
  'misheyakir115', 'misheyakir102', 'tzeis85', 'tzeis7083', 'tzeis161',
  'tzeis72', 'tzeis50', 'tzeis42', 'beinHaShmashosRT',
  'sofShemaGRA', 'sofTefillaGRA', 'chatzos', 'minchaGedola', 'minchaKetana',
  'plagHaMincha', 'sofAchilasChametz', 'sofBiurChametz',
  'sofShemaMGA', 'sofTefillaMGA', 'sofShemaMGA198', 'sofTefillaMGA198',
  'minchaGedolaMGA', 'minchaKetanaMGA', 'plagHaMinchaMGA',
  'chatzosLaila', 'beinHaShmashos', 'shaahZmanisGRA', 'shaahZmanisMGA',
  'havdalah', 'tzeis', 'candleLighting',
];

const out = [];
for (const [y, m, d, loc, il, label] of CASES) {
  const info = ZE.getDayInfo(y, m, d, loc, { il, candleMin: 18, havdalah: '8.5' });
  const z = {};
  for (const f of ZMANIM_FIELDS) z[f] = info.zmanim[f] === undefined ? null : info.zmanim[f];
  out.push({
    label, y, m, d,
    loc: { name: loc.name, lat: loc.lat, lng: loc.lng, tz: loc.tz, elevM: loc.elevM },
    il,
    heb: info.heb, wd: info.wd,
    parsha: { names: info.parsha.names, chag: info.parsha.chag },
    events: info.events.map(e => ({ name: e.name, type: e.type })),
    zmanim: z,
    candleLightingMins: info.zmanim.candleLightingMins,
    omer: info.omer,
    daf: info.daf ? { name: info.daf.name, daf: info.daf.daf } : null,
    molad: info.molad ? {
      month: info.molad.month, hour: info.molad.molad.hour,
      minutes: info.molad.molad.minutes, chalakim: info.molad.molad.chalakim,
    } : null,
    levana: info.levana ? { fromMs: info.levana.fromMs, toMs: info.levana.toMs } : null,
    candle: info.candle ? info.candle.ms : null,
    havdalah: info.havdalah ? info.havdalah.ms : null,
    fastBegin: info.fastBegin, fastEnd: info.fastEnd,
    chametz: info.chametz ? { achila: info.chametz.achila, biur: info.chametz.biur } : null,
  });
}

writeFileSync(new URL('../app/src/test/resources/goldens.json', import.meta.url), JSON.stringify(out, null, 1));
console.log(`wrote ${out.length} golden cases`);
