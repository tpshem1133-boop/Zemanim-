# Zemanim — Jewish Prayer Times & Hebrew Calendar (Android)

A native Android app (Kotlin + Jetpack Compose, Material 3) that computes
daily zmanim and the Hebrew calendar entirely on-device. No backend, no
network, no ads, no trackers.

## Features

- **Today** — full grouped zmanim list (dawn, morning, afternoon, evening),
  a live "now / next zman" hero card with an animated progress ring,
  parsha / Omer / Daf Yomi chips, events, candle lighting, havdalah,
  fast times, chametz times, molad, and Kiddush Levana.
- **Calendar** — Hebrew month grid with event dots; tap a day for a detail
  sheet with key zmanim and an "Open full day" action.
- **Shabbos** — upcoming Shabbos hero, candle lighting, havdalah, and the
  full Shabbos-day zmanim list.
- **Holidays** — upcoming Yomim Tovim and fasts for the next 12 months,
  grouped by Hebrew month, with candle/fast times.
- **Settings** — GPS location (no Play Services), 22 city presets, manual
  coordinates/timezone/elevation, candle-lighting minutes, havdalah method,
  Israel/Diaspora schedule, 12/24-hour clock, MGA/extra-dawn toggles,
  light/dark/auto theme. Everything persists in SharedPreferences.

## Calculation engine

`app/src/main/java/com/zemanim/app/engine/ZemanimEngine.kt` is a faithful
Kotlin port of a verified JavaScript engine: Hebrew calendar with dechiyos,
molad (chalakim arithmetic), NOAA solar calculations (90.833° zenith with
refraction and elevation dip), shaah zmanis (GRA and MGA), the full sedra
schedule for Israel and Diaspora, holidays and fasts with nidche shifts,
Daf Yomi (2711-day cycle), Sefiras HaOmer, and Kiddush Levana.

## Verification

`app/src/test/resources/goldens.json` holds 36 reference cases generated
from the JavaScript engine (`tools/generate-goldens.mjs`); the JVM unit test
`EngineGoldenTest` compares the Kotlin port against them (Hebrew date,
parsha, and events exact; zmanim within 2 minutes). The CI workflow runs
`gradle :app:testDebugUnitTest` before building.

## Building

Local Gradle builds are not possible in the author's sandbox, so releases
are built with GitHub Actions (see `.github/workflows/build-apk.yml`):
push to `main` and download the `Zemanim-debug-apk` and
`Zemanim-debug-aab` artifacts from the run.

- Package: `com.zemanim.app`
- minSdk 26, targetSdk 34, Kotlin 2.0.20, AGP 8.5.0, Compose BOM 2024.06.00

Times are solid approximations for everyday use. For halachic questions,
consult your rabbi.
