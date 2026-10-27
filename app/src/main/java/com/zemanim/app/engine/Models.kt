package com.zemanim.app.engine

/**
 * Data model for the Zemanim engine (a faithful port of the verified
 * JavaScript engine in ~/workspace/zemanim-app/engine.js).
 *
 * Day numbers ("abs") are Rata Die numbers: 1 = Monday, January 1, 1 CE.
 * All instants are UTC epoch milliseconds, except molad/levana bounds which
 * are Doubles to match the JavaScript engine exactly (fractional ms from
 * chalakim conversion). Weekdays: 0 = Sunday .. 6 = Saturday.
 * Hebrew months: 1 = Nisan .. 7 = Tishri .. 12 = Adar(I) .. 13 = Adar II.
 */

data class CivilDate(val y: Int, val m: Int, val d: Int)

data class HebDate(val y: Int, val m: Int, val d: Int)

data class CivilInTz(val y: Int, val m: Int, val d: Int, val hh: Int, val mi: Int)

data class Location(
    val lat: Double,
    val lng: Double,
    val tz: String,
    val elevM: Double = 0.0,
    val name: String = ""
)

data class ZmanimOpts(
    val havdalah: String = "8.5",
    val candleMin: Int = 18,
    val useElevation: Boolean = false
)

data class Zmanim(
    val netz: Long?,
    val shkia: Long?,
    val alos161: Long?,
    val alos18: Long?,
    val alos198: Long?,
    val alos72: Long?,
    val misheyakir115: Long?,
    val misheyakir102: Long?,
    val tzeis85: Long?,
    val tzeis7083: Long?,
    val tzeis161: Long?,
    val tzeis72: Long?,
    val tzeis50: Long?,
    val tzeis42: Long?,
    val beinHaShmashosRT: Long?,
    val sofShemaGRA: Long?,
    val sofTefillaGRA: Long?,
    val chatzos: Long?,
    val minchaGedola: Long?,
    val minchaKetana: Long?,
    val plagHaMincha: Long?,
    val sofAchilasChametz: Long?,
    val sofBiurChametz: Long?,
    val sofShemaMGA: Long?,
    val sofTefillaMGA: Long?,
    val sofShemaMGA198: Long?,
    val sofTefillaMGA198: Long?,
    val minchaGedolaMGA: Long?,
    val minchaKetanaMGA: Long?,
    val plagHaMinchaMGA: Long?,
    val chatzosLaila: Long?,
    val beinHaShmashos: Long?,
    val shaahZmanisGRA: Long?,
    val shaahZmanisMGA: Long?,
    val havdalah: Long?,
    val tzeis: Long?,
    val candleLightingMins: Int,
    val candleLighting: Long?
)

data class HolidayEvent(val abs: Long, val name: String, val type: String)

data class Parsha(val names: List<String>, val chag: Boolean, val shabbosAbs: Long)

data class Daf(val name: String, val daf: Int, val index: Long)

data class Molad(
    val abs: Long,
    val hour: Int,
    val minutes: Int,
    val chalakim: Int,
    val dow: Int,
    /** UTC instant as Double (fractional ms from chalakim), exactly like the JS engine. */
    val utcMs: Double
)

data class MoladInfo(val month: String, val molad: Molad)

data class Levana(val fromMs: Double, val toMs: Double, val molad: Molad)

data class CandleInfo(val ms: Long?, val note: String)

data class Chametz(val achila: Long?, val biur: Long?)

data class DayInfo(
    val abs: Long,
    val heb: HebDate,
    val wd: Int,
    val zmanim: Zmanim,
    val events: List<HolidayEvent>,
    val parsha: Parsha,
    val shabbosLabel: String?,
    val isShabbos: Boolean,
    val isYomTov: Boolean,
    val candle: CandleInfo?,
    val havdalah: CandleInfo?,
    val fast: HolidayEvent?,
    val fastBegin: Long?,
    val fastEnd: Long?,
    val chametz: Chametz?,
    /** Sefira day, 0 when not during the Omer. */
    val omer: Int,
    val daf: Daf?,
    val levana: Levana?,
    val molad: MoladInfo?
)
