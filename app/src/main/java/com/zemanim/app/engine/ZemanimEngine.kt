package com.zemanim.app.engine

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Faithful Kotlin port of the verified JavaScript engine
 * (~/workspace/zemanim-app/engine.js). Same algorithms, same constants,
 * same edge cases. JS doubles are mirrored with Kotlin Double; JS
 * Math.floor on possibly-negative values uses Math.floorDiv.
 *
 * Gregorian day numbers use Howard Hinnant's days-from-civil algorithm:
 * abs = daysFromCivil(y, m, d) + 719163 (Rata Die: 1 = Mon Jan 1, 1 CE).
 */
object ZemanimEngine {

    const val DAY_MS: Long = 86400000L

    // ---------- Gregorian day arithmetic (Hinnant) ----------

    /** Days since 1970-01-01 for a civil date. */
    fun daysFromCivil(y: Int, m: Int, d: Int): Long {
        var yy = y
        if (m <= 2) yy--
        val era = Math.floorDiv(yy, 400)
        val yoe = yy - era * 400 // [0, 399]
        val mp = (m + 9) % 12 // [0, 11]
        val doy = (153 * mp + 2) / 5 + d - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era.toLong() * 146097L + doe.toLong() - 719468L
    }

    /** Civil date for days since 1970-01-01. */
    fun civilFromDays(zIn: Long): CivilDate {
        val z = zIn + 719468L
        val era = Math.floorDiv(z, 146097L)
        val doe = z - era * 146097L // [0, 146096]
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365 // [0, 399]
        var y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
        val m = if (mp < 10) mp + 3 else mp - 9
        if (m <= 2) y++
        return CivilDate(y.toInt(), m.toInt(), d)
    }

    fun gregToAbs(y: Int, m: Int, d: Int): Long = daysFromCivil(y, m, d) + 719163L

    fun absToGreg(abs: Long): CivilDate = civilFromDays(abs - 719163L)

    /** 0 = Sunday .. 6 = Saturday */
    fun absWeekday(abs: Long): Int = Math.floorMod(abs, 7L).toInt()

    /** Greatest date <= abs with weekday dow (0 = Sun). */
    fun dayOnOrBefore(dow: Int, abs: Long): Long = abs - Math.floorMod(abs - dow.toLong(), 7L)

    /** Smallest date >= abs with weekday dow (0 = Sun). */
    fun dayOnOrAfter(dow: Int, abs: Long): Long = abs + Math.floorMod(dow.toLong() - abs, 7L)

    fun nextCivil(y: Int, m: Int, d: Int): CivilDate = absToGreg(gregToAbs(y, m, d) + 1)

    fun prevCivil(y: Int, m: Int, d: Int): CivilDate = absToGreg(gregToAbs(y, m, d) - 1)

    fun addDaysCivil(y: Int, m: Int, d: Int, n: Long): CivilDate =
        absToGreg(gregToAbs(y, m, d) + n)

    // ---------- Hebrew calendar ----------
    // Months: 1=Nisan 2=Iyar 3=Sivan 4=Tamuz 5=Av 6=Elul 7=Tishri 8=Cheshvan
    //         9=Kislev 10=Teves 11=Shevat 12=Adar(I) 13=Adar II (leap only)

    val HEB_MONTHS = arrayOf(
        "", "Nisan", "Iyar", "Sivan", "Tamuz", "Av", "Elul",
        "Tishri", "Cheshvan", "Kislev", "Teves", "Shevat", "Adar", "Adar II"
    )

    fun isHebLeap(y: Int): Boolean = ((7 * y + 1) % 19) < 7

    fun monthsElapsedHY(y: Int): Long {
        val y1 = (y - 1).toLong()
        return 235 * (y1 / 19) + 12 * (y1 % 19) + (7 * (y1 % 19) + 1) / 19
    }

    /** Days from epoch to 1 Tishri of year y (with dechiyos). */
    fun hebrewElapsedDays(y: Int): Long {
        val m = monthsElapsedHY(y)
        val pElapsed = 204 + 793 * (m % 1080)
        val hElapsed = 5 + 12 * m + 793 * (m / 1080) + pElapsed / 1080
        val parts = (pElapsed % 1080) + 1080 * (hElapsed % 24)
        val day = 1 + 29 * m + hElapsed / 24
        var alt = day
        if (parts >= 19440 ||
            (day % 7 == 2L && parts >= 9924 && !isHebLeap(y)) ||
            (day % 7 == 1L && parts >= 16789 && isHebLeap(y - 1))
        ) {
            alt++
        }
        if (alt % 7 == 0L || alt % 7 == 3L || alt % 7 == 5L) alt++
        return alt
    }

    const val HEB_ABS_EPOCH: Long = -1373428L

    fun hebNewYearAbs(y: Int): Long = HEB_ABS_EPOCH + hebrewElapsedDays(y)

    fun daysInHebYear(y: Int): Long = hebNewYearAbs(y + 1) - hebNewYearAbs(y)

    fun hebMonthDays(y: Int, m: Int): Int {
        return when (m) {
            1 -> 30 // Nisan
            2 -> 29 // Iyar
            3 -> 30 // Sivan
            4 -> 29 // Tamuz
            5 -> 30 // Av
            6 -> 29 // Elul
            7 -> 30 // Tishri
            8 -> if (daysInHebYear(y) % 10 == 5L) 30 else 29 // Cheshvan
            9 -> if (daysInHebYear(y) % 10 == 3L) 29 else 30 // Kislev
            10 -> 29 // Teves
            11 -> 30 // Shevat
            12 -> if (isHebLeap(y)) 30 else 29 // Adar I / Adar
            13 -> 29 // Adar II
            else -> 0
        }
    }

    fun nextHebMonth(y: Int, m: Int): Int {
        if (m == 12) return if (isHebLeap(y)) 13 else 1
        if (m == 13) return 1
        return m + 1
    }

    fun hebToAbs(y: Int, m: Int, d: Int): Long {
        var abs = hebNewYearAbs(y) + d.toLong() - 1
        var mm = 7
        while (mm != m) {
            abs += hebMonthDays(y, mm).toLong()
            mm = nextHebMonth(y, mm)
        }
        return abs
    }

    fun absToHeb(abs: Long): HebDate {
        var y = Math.floorDiv(abs - (HEB_ABS_EPOCH + 1), 366L).toInt()
        while (hebNewYearAbs(y + 1) <= abs) y++
        while (hebNewYearAbs(y) > abs) y--
        var m = 7
        var d = abs - hebNewYearAbs(y) + 1
        var guard = 0
        while (d > hebMonthDays(y, m).toLong() && guard++ < 20) {
            d -= hebMonthDays(y, m).toLong()
            m = nextHebMonth(y, m)
        }
        return HebDate(y, m, d.toInt())
    }

    /** Year type: 0 = incomplete, 1 = regular, 2 = complete. */
    fun hebYearType(y: Int): Int {
        val l = daysInHebYear(y) % 10
        if (l == 3L) return 0
        if (l == 5L) return 2
        return 1
    }

    fun gregToHeb(y: Int, m: Int, d: Int): HebDate = absToHeb(gregToAbs(y, m, d))

    // ---------- Molad (chalakim arithmetic, anchored to Molad Tohu) ----------

    const val CHALAKIM_PER_HOUR: Long = 1080
    const val CHALAKIM_PER_MIN: Long = 18
    const val CHALAKIM_PER_DAY: Long = 25920
    const val CHALAKIM_PER_MONTH: Long = 765433 // 29d 12h 793p
    const val CHALAKIM_MOLAD_TOHU: Long = 31524 // 1d 5h 204p

    fun moladChalakim(y: Int, m: Int): Long {
        val leap = isHebLeap(y)
        val monthOfYear = ((m + (if (leap) 6 else 5)) % (if (leap) 13 else 12)) + 1 // Tishri=1
        val monthsElapsed = (235 * ((y - 1) / 19) +
            12 * ((y - 1) % 19) +
            (7 * ((y - 1) % 19) + 1) / 19 +
            (monthOfYear - 1)).toLong()
        return CHALAKIM_MOLAD_TOHU + CHALAKIM_PER_MONTH * monthsElapsed
    }

    fun calcMolad(y: Int, m: Int): Molad {
        val chalakim = moladChalakim(y, m)
        val dayNum = Math.floorDiv(chalakim, CHALAKIM_PER_DAY)
        var parts = chalakim - dayNum * CHALAKIM_PER_DAY
        val hour = (parts / CHALAKIM_PER_HOUR).toInt()
        parts -= hour.toLong() * CHALAKIM_PER_HOUR
        val minutes = (parts / CHALAKIM_PER_MIN).toInt()
        val chal = (parts - minutes.toLong() * CHALAKIM_PER_MIN).toInt()
        var abs = dayNum + (-1373429L)
        if (hour >= 6) abs += 1
        val displayHour = (hour + 18) % 24
        // UTC instant: molad defined at Jerusalem longitude, expressed in fixed
        // UTC+2 ("Jerusalem standard time"), minus 20.94 min longitude offset.
        val moladSeconds = chal * 10.0 / 3.0
        val utcMs = (abs - 719163L) * DAY_MS.toDouble() +
            (displayHour * 3600 + minutes * 60) * 1000.0 +
            moladSeconds * 1000.0 -
            (2 * 60 + 20.94) * 60000.0
        return Molad(abs, displayHour, minutes, chal, absWeekday(abs), utcMs)
    }

    // ---------- Solar calculations (NOAA algorithm) ----------

    const val RAD: Double = PI / 180.0
    const val DEG: Double = 180.0 / PI

    fun norm360(x: Double): Double {
        var r = x % 360.0
        if (r < 0) r += 360.0
        return r
    }

    fun norm24(x: Double): Double {
        var r = x % 24.0
        if (r < 0) r += 24.0
        return r
    }

    /**
     * UTC milliseconds of sunrise/sunset on a given UTC civil date for a
     * solar depression angle. zenithDeg: 90 = geometric sunrise/sunset,
     * 90+angle for degrees below horizon. Returns null if no event.
     */
    fun sunTimeUTC(y: Int, m: Int, d: Int, lat: Double, lng: Double, zenithDeg: Double, rising: Boolean): Long? {
        val dateMs = daysFromCivil(y, m, d) * DAY_MS
        val jan1 = daysFromCivil(y, 1, 1) * DAY_MS
        val n = Math.round((dateMs - jan1).toDouble() / DAY_MS.toDouble()) + 1 // day of year
        val lngHour = lng / 15.0
        val t = if (rising) n + (6 - lngHour) / 24.0 else n + (18 - lngHour) / 24.0
        val mm = 0.9856 * t - 3.289
        val l = norm360(mm + 1.916 * sin(mm * RAD) + 0.020 * sin(2 * mm * RAD) + 282.634)
        var ra = norm360(atan(0.91764 * tan(l * RAD)) * DEG)
        ra += floor(l / 90.0) * 90.0 - floor(ra / 90.0) * 90.0
        ra /= 15.0
        val sinDec = 0.39782 * sin(l * RAD)
        val cosDec = cos(asin(sinDec))
        val cosH = (cos(zenithDeg * RAD) - sinDec * sin(lat * RAD)) /
            (cosDec * cos(lat * RAD))
        if (cosH > 1 || cosH < -1) return null // polar day/night
        var h = acos(cosH) * DEG
        if (rising) h = 360.0 - h
        h /= 15.0
        val tt = h + ra - 0.06571 * t - 6.622
        val ut = norm24(tt - lngHour)
        return dateMs + (ut * 3600000.0).toLong()
    }

    // ---------- Timezone helpers (java.time) ----------

    /** Offset ms (local - UTC) at a UTC instant, whole seconds like the JS Intl version. */
    fun tzOffsetMs(zone: ZoneId, utcMs: Long): Long {
        return zone.rules.getOffset(Instant.ofEpochMilli(utcMs)).totalSeconds.toLong() * 1000L
    }

    /** Civil date in tz for a UTC instant. */
    fun civilInTz(utcMs: Long, zone: ZoneId): CivilInTz {
        val z = ZonedDateTime.ofInstant(Instant.ofEpochMilli(utcMs), zone)
        return CivilInTz(z.year, z.monthValue, z.dayOfMonth, z.hour, z.minute)
    }

    /** UTC ms for a wall time in tz (same 3-iteration loop as the JS engine). */
    fun wallToUtcMs(y: Int, m: Int, d: Int, hh: Int, mi: Int, zone: ZoneId): Long {
        val base = daysFromCivil(y, m, d) * DAY_MS + hh.toLong() * 3600000L + mi.toLong() * 60000L
        var guess = base
        repeat(3) { guess = base - tzOffsetMs(zone, guess) }
        return guess
    }

    /**
     * Solar event for a location civil date, with anchor bounds.
     * Candidates are computed for adjacent UTC dates; the first candidate
     * within [notBefore, notAfter] (inclusive) is returned.
     */
    fun sunEventForDate(
        y: Int, m: Int, d: Int, lat: Double, lng: Double, zenithDeg: Double,
        rising: Boolean, zone: ZoneId, notBefore: Long?, notAfter: Long?
    ): Long? {
        val noonUtc = wallToUtcMs(y, m, d, 12, 0, zone)
        val baseDays = Math.floorDiv(noonUtc, DAY_MS)
        val cands = ArrayList<Long>()
        for (k in -1..1) {
            val c = civilFromDays(baseDays + k.toLong())
            val t = sunTimeUTC(c.y, c.m, c.d, lat, lng, zenithDeg, rising)
            if (t != null) cands.add(t)
        }
        cands.sort()
        for (c in cands) {
            if (notBefore != null && c < notBefore) continue
            if (notAfter != null && c > notAfter) continue
            return c
        }
        return null
    }

    // ---------- Zmanim ----------
    // loc: Location; opts: ZmanimOpts. All times are UTC ms (null when not computable).

    fun computeZmanim(y: Int, m: Int, d: Int, loc: Location, opts: ZmanimOpts): Zmanim {
        val lat = loc.lat
        val lng = loc.lng
        val zone = ZoneId.of(loc.tz)
        val elevM = maxOf(0.0, loc.elevM)
        val useElev = opts.useElevation
        // elevation dip for sunrise/sunset (degrees), matching reference implementation
        val dip = if (useElev) acos(6356.9 / (6356.9 + elevM / 1000.0)) * DEG else 0.0
        // netz/shkia: sun's center 90deg + solar radius (16') + refraction (34') below horizon
        val zNetz = 90.0 + 16.0 / 60.0 + 34.0 / 60.0 + dip

        val midnightStart = wallToUtcMs(y, m, d, 0, 0, zone)
        val noon = wallToUtcMs(y, m, d, 12, 0, zone)

        fun ev(zen: Double, rising: Boolean, notBefore: Long?, notAfter: Long?): Long? {
            return sunEventForDate(y, m, d, lat, lng, zen, rising, zone, notBefore, notAfter)
        }
        // latest rising-candidate at or before anchor (for dawn times)
        fun evBefore(zen: Double, anchor: Long): Long? {
            val baseDays = Math.floorDiv(noon, DAY_MS)
            var best: Long? = null
            for (k in -1..1) {
                val c = civilFromDays(baseDays + k.toLong())
                val t = sunTimeUTC(c.y, c.m, c.d, lat, lng, zen, true)
                if (t != null && t <= anchor) {
                    if (best == null || t > best) best = t
                }
            }
            return best
        }
        // earliest setting-candidate at or after anchor (for nightfall times)
        fun evAfter(zen: Double, anchor: Long): Long? = ev(zen, false, anchor, null)

        val netz = ev(zNetz, true, midnightStart, null)
        val shkia = ev(zNetz, false, noon, null)
        val alos161 = evBefore(106.1, netz ?: noon)
        val alos18 = evBefore(108.0, netz ?: noon)
        val alos198 = evBefore(109.8, netz ?: noon)
        val misheyakir115 = evBefore(101.5, netz ?: noon)
        val misheyakir102 = evBefore(100.2, netz ?: noon)
        val tzeis85 = evAfter(98.5, shkia ?: noon)
        val tzeis7083 = evAfter(97.083, shkia ?: noon)
        val tzeis161 = evAfter(106.1, shkia ?: noon)
        val alos72 = if (netz != null) netz - 72 * 60000L else null
        val tzeis72 = if (shkia != null) shkia + 72 * 60000L else null
        val tzeis50 = if (shkia != null) shkia + 50 * 60000L else null
        val tzeis42 = if (shkia != null) shkia + 42 * 60000L else null

        // shaah zmanis GRA (netz-shkia), floored to ms like reference
        fun shaahGRA(): Long? {
            if (netz == null || shkia == null) return null
            return floor((shkia - netz).toDouble() / 12.0).toLong()
        }
        // shaah zmanis MGA 16.1 (alos161-tzeis161)
        fun shaahMGA161(): Long? {
            if (alos161 == null || tzeis161 == null) return null
            return floor((tzeis161 - alos161).toDouble() / 12.0).toLong()
        }
        fun shaahMGA198(): Long? {
            val a = alos198
            val t = evAfter(109.8, shkia ?: noon)
            if (a == null || t == null) return null
            return floor((t - a).toDouble() / 12.0).toLong()
        }
        val sG = shaahGRA()
        val sM = shaahMGA161()
        val sM198 = shaahMGA198()

        fun gBased(h: Double): Long? {
            if (netz == null || sG == null) return null
            return netz + floor(h * sG.toDouble()).toLong()
        }
        fun mgaBased(h: Double, s: Long?, alos: Long?): Long? {
            if (alos == null || s == null) return null
            return alos + floor(h * s.toDouble()).toLong()
        }

        // chatzos halaila: midpoint shkia -> netz (next day)
        var chatzosLaila: Long? = null
        var beinHaShmashosRT: Long? = null
        if (shkia != null) {
            val nm = nextCivil(y, m, d)
            val nmMidnight = wallToUtcMs(nm.y, nm.m, nm.d, 0, 0, zone)
            val ntTmr = sunEventForDate(nm.y, nm.m, nm.d, lat, lng, zNetz, true, zone, nmMidnight, null)
            if (ntTmr != null) chatzosLaila = shkia + (ntTmr - shkia) / 2
            // Rabbeinu Tam: shkia + 72 zmaniyos minutes = 1.2 proportional hours
            if (sG != null) beinHaShmashosRT = (shkia + 1.2 * sG.toDouble()).toLong()
        }
        // bein hashmashos (geonim): 13.5 min before tzeis 7.083
        val beinHaShmashos = if (tzeis7083 == null) null else tzeis7083 - (13.5 * 60000).toLong()

        // havdalah per method (accept 'deg8.5' or '8.5' style keys)
        val hm = opts.havdalah.removePrefix("deg").removePrefix("min")
        val havdalah = when (hm) {
            "7.083" -> tzeis7083
            "72" -> tzeis72
            "50" -> tzeis50
            "42" -> tzeis42
            else -> tzeis85
        }

        return Zmanim(
            netz = netz,
            shkia = shkia,
            alos161 = alos161,
            alos18 = alos18,
            alos198 = alos198,
            alos72 = alos72,
            misheyakir115 = misheyakir115,
            misheyakir102 = misheyakir102,
            tzeis85 = tzeis85,
            tzeis7083 = tzeis7083,
            tzeis161 = tzeis161,
            tzeis72 = tzeis72,
            tzeis50 = tzeis50,
            tzeis42 = tzeis42,
            beinHaShmashosRT = beinHaShmashosRT,
            sofShemaGRA = gBased(3.0),
            sofTefillaGRA = gBased(4.0),
            chatzos = gBased(6.0),
            minchaGedola = gBased(6.5),
            minchaKetana = gBased(9.5),
            plagHaMincha = gBased(10.75),
            sofAchilasChametz = gBased(4.0),
            sofBiurChametz = gBased(5.0),
            sofShemaMGA = mgaBased(3.0, sM, alos161),
            sofTefillaMGA = mgaBased(4.0, sM, alos161),
            sofShemaMGA198 = mgaBased(3.0, sM198, alos198),
            sofTefillaMGA198 = mgaBased(4.0, sM198, alos198),
            minchaGedolaMGA = mgaBased(6.5, sM, alos161),
            minchaKetanaMGA = mgaBased(9.5, sM, alos161),
            plagHaMinchaMGA = mgaBased(10.75, sM, alos161),
            chatzosLaila = chatzosLaila,
            beinHaShmashos = beinHaShmashos,
            shaahZmanisGRA = sG,
            shaahZmanisMGA = sM,
            havdalah = havdalah,
            tzeis = havdalah, // selected nightfall
            candleLightingMins = opts.candleMin,
            candleLighting = if (shkia == null) null else shkia - opts.candleMin.toLong() * 60000L
        )
    }

    // ---------- Sedra (weekly Torah reading) ----------
    // Schedule keyed by: leap(0/1), RH weekday (2=Mon,3=Tue,5=Thu,7=Sat),
    // year type (0=incomplete,1=regular,2=complete), Israel(0/1).
    // Array entries: parsha index 0-53, negative = doubled pair
    // (-21 = Vayakhel+Pekudei), strings = holiday reading displacing the parsha.

    val PARSHA_NAMES = arrayOf(
        "Bereshit", "Noach", "Lech Lecha", "Vayeira", "Chayei Sara", "Toldot",
        "Vayetzei", "Vayishlach", "Vayeshev", "Mikeitz", "Vayigash", "Vayechi",
        "Shemos", "Va'eira", "Bo", "Beshalach", "Yisro", "Mishpatim", "Terumah",
        "Tetzaveh", "Ki Sisa", "Vayakhel", "Pekudei", "Vayikra", "Tzav", "Shemini",
        "Tazria", "Metzora", "Acharei Mos", "Kedoshim", "Emor", "Behar",
        "Bechukosai", "Bamidbar", "Naso", "Beha'aloscha", "Shlach", "Korach",
        "Chukas", "Balak", "Pinchas", "Matos", "Masei", "Devarim", "Va'eschanan",
        "Eikev", "Re'eh", "Shoftim", "Ki Seitzei", "Ki Savo", "Nitzavim",
        "Vayeilech", "Haazinu", "Vezos Habracha"
    )

    val CHAG_READINGS = mapOf(
        "RH" to "Rosh Hashana",
        "YK" to "Yom Kippur",
        "SUKKOT1" to "Sukkos",
        "SHMINI" to "Shemini Atzeres",
        "CHM_SUKKOT" to "Shabbos Chol HaMoed Sukkos",
        "PESACH" to "Pesach",
        "PESACH1" to "Pesach",
        "PESACH7" to "Pesach",
        "PESACH8" to "Pesach",
        "CHM_PESACH" to "Shabbos Chol HaMoed Pesach",
        "SHAVUOT" to "Shavuos"
    )

    private fun rng(a: Int, b: Int): List<Any> {
        val r = ArrayList<Any>()
        for (i in a..b) r.add(i)
        return r
    }

    private fun sedra(vararg parts: List<Any>): List<Any> {
        val r = ArrayList<Any>()
        for (p in parts) r.addAll(p)
        return r
    }

    private val ysVayeilech: List<Any> = listOf(51, 52, "CHM_SUKKOT")
    private val ysHaazinu: List<Any> = listOf(52, "YK", "CHM_SUKKOT")
    private val ysRh: List<Any> = listOf("RH", 52, "SUKKOT1", "SHMINI")

    private val sedraTypes: Map<String, List<Any>> = run {
        val m = HashMap<String, List<Any>>()
        m["020"] = sedra(ysVayeilech, rng(0, 20), listOf(-21, 23, 24, "CHM_PESACH", 25, -26, -28, 30, -31), rng(33, 40), listOf(-41), rng(43, 49), listOf(-50))
        m["0220"] = sedra(ysVayeilech, rng(0, 20), listOf(-21, 23, 24, "CHM_PESACH", 25, -26, -28, 30, -31, 33, "SHAVUOT"), rng(34, 37), listOf(-38, 40, -41), rng(43, 49), listOf(-50))
        m["0510"] = sedra(ysHaazinu, rng(0, 20), listOf(-21, 23, 24, "PESACH1", "PESACH8", 25, -26, -28, 30, -31), rng(33, 40), listOf(-41), rng(43, 50))
        m["0511"] = sedra(ysHaazinu, rng(0, 20), listOf(-21, 23, 24, "PESACH", 25, -26, -28), rng(30, 40), listOf(-41), rng(43, 50))
        m["052"] = sedra(ysHaazinu, rng(0, 24), listOf("PESACH7", 25, -26, -28, 30, -31), rng(33, 40), listOf(-41), rng(43, 50))
        m["070"] = sedra(ysRh, rng(0, 20), listOf(-21, 23, 24, "PESACH7", 25, -26, -28, 30, -31), rng(33, 40), listOf(-41), rng(43, 50))
        m["072"] = sedra(ysRh, rng(0, 20), listOf(-21, 23, 24, "CHM_PESACH", 25, -26, -28, 30, -31), rng(33, 40), listOf(-41), rng(43, 49), listOf(-50))
        m["1200"] = sedra(ysVayeilech, rng(0, 27), listOf("CHM_PESACH"), rng(28, 33), listOf("SHAVUOT"), rng(34, 37), listOf(-38, 40, -41), rng(43, 49), listOf(-50))
        m["1201"] = sedra(ysVayeilech, rng(0, 27), listOf("CHM_PESACH"), rng(28, 40), listOf(-41), rng(43, 49), listOf(-50))
        m["1220"] = sedra(ysVayeilech, rng(0, 27), listOf("PESACH1", "PESACH8"), rng(28, 40), listOf(-41), rng(43, 50))
        m["1221"] = sedra(ysVayeilech, rng(0, 27), listOf("PESACH"), rng(28, 50))
        m["150"] = sedra(ysHaazinu, rng(0, 28), listOf("PESACH7"), rng(29, 50))
        m["152"] = sedra(ysHaazinu, rng(0, 28), listOf("CHM_PESACH"), rng(29, 49), listOf(-50))
        m["170"] = sedra(ysRh, rng(0, 27), listOf("CHM_PESACH"), rng(28, 40), listOf(-41), rng(43, 49), listOf(-50))
        m["1720"] = sedra(ysRh, rng(0, 27), listOf("CHM_PESACH"), rng(28, 33), listOf("SHAVUOT"), rng(34, 37), listOf(-38, 40, -41), rng(43, 49), listOf(-50))
        m["0221"] = m["020"]!!
        m["0310"] = m["0220"]!!
        m["0311"] = m["020"]!!
        m["1310"] = m["1220"]!!
        m["1311"] = m["1221"]!!
        m["1721"] = m["170"]!!
        m
    }

    private data class SedraArray(val arr: List<Any>, val firstSat: Long, val rhAbs: Long)

    private val sedraCache = HashMap<String, SedraArray>()

    private fun getSedraArray(hyear: Int, il: Boolean): SedraArray {
        val ck = "$hyear${if (il) "i" else "d"}"
        val cached = sedraCache[ck]
        if (cached != null) return cached
        val rhAbs = hebToAbs(hyear, 7, 1)
        val rhDay = absWeekday(rhAbs) + 1 // Sun=1..Sat=7
        val leap = if (isHebLeap(hyear)) 1 else 0
        val type = hebYearType(hyear)
        val key = "$leap$rhDay$type"
        val arr = sedraTypes[key] ?: sedraTypes[key + (if (il) "1" else "0")]
            ?: throw IllegalStateException("No sedra table for year $hyear key $key")
        val rec = SedraArray(arr, dayOnOrBefore(6, rhAbs + 6), rhAbs)
        sedraCache[ck] = rec
        return rec
    }

    /** Parsha read on the Shabbos of the given absolute date (or upcoming Shabbos). */
    fun getParsha(abs: Long, il: Boolean): Parsha {
        var hy = absToHeb(abs).y
        for (attempt in 0 until 2) {
            val s = getSedraArray(hy, il)
            val sat = dayOnOrBefore(6, abs + 6)
            val weekNum = ((sat - s.firstSat) / 7).toInt()
            if (weekNum >= 0 && weekNum < s.arr.size) {
                val e = s.arr[weekNum]
                if (e is String) {
                    return Parsha(listOf(CHAG_READINGS[e] ?: e), true, sat)
                }
                val ei = e as Int
                if (ei >= 0) return Parsha(listOf(PARSHA_NAMES[ei]), false, sat)
                val p1 = -ei
                return Parsha(listOf(PARSHA_NAMES[p1], PARSHA_NAMES[p1 + 1]), false, sat)
            }
            hy++
        }
        return Parsha(emptyList(), false, dayOnOrBefore(6, abs + 6))
    }

    // ---------- Jewish holidays ----------

    fun holidaysForYear(y: Int, il: Boolean, modern: Boolean): List<HolidayEvent> {
        val evs = ArrayList<HolidayEvent>()
        fun add(m: Int, d: Int, name: String, type: String) {
            evs.add(HolidayEvent(hebToAbs(y, m, d), name, type))
        }
        fun addAbs(abs: Long, name: String, type: String) {
            evs.add(HolidayEvent(abs, name, type))
        }
        val leap = isHebLeap(y)
        val adar = if (leap) 13 else 12

        // Tishri
        add(7, 1, "Rosh Hashana", "yt")
        add(7, 2, "Rosh Hashana", "yt")
        addAbs(hebToAbs(y, 7, 3) + (if (absWeekday(hebToAbs(y, 7, 3)) == 6) 1L else 0L), "Tzom Gedaliah", "fast")
        add(7, 9, "Erev Yom Kippur", "erev")
        add(7, 10, "Yom Kippur", "fast-yt")
        add(7, 14, "Erev Sukkos", "erev")
        add(7, 15, "Sukkos", "yt")
        if (!il) add(7, 16, "Sukkos", "yt") else add(7, 16, "Chol HaMoed Sukkos", "chm")
        add(7, 17, "Chol HaMoed Sukkos", "chm")
        add(7, 18, "Chol HaMoed Sukkos", "chm")
        add(7, 19, "Chol HaMoed Sukkos", "chm")
        add(7, 20, "Chol HaMoed Sukkos", "chm")
        add(7, 21, "Hoshana Rabba", "chm")
        add(7, 22, "Shemini Atzeres", "yt")
        if (!il) add(7, 23, "Simchas Torah", "yt")
        // Chanukah: 25 Kislev + 8 days
        for (i in 0 until 8) {
            val ha = absToHeb(hebToAbs(y, 9, 25) + i.toLong())
            add(ha.m, ha.d, "Chanukah", "minor")
        }
        // Asara BTeves
        addAbs(hebToAbs(y, 10, 10) + (if (absWeekday(hebToAbs(y, 10, 10)) == 6) 1L else 0L), "Asara B'Teves", "fast")
        add(11, 15, "Tu B'Shvat", "minor")
        // Purim
        val purim14 = hebToAbs(y, adar, 14)
        val purimObs = if (absWeekday(purim14) == 6) purim14 - 1 else purim14 // 14th on Shabbos -> Fri 13th
        addAbs(purimObs, "Purim", "minor")
        val shushan15 = hebToAbs(y, adar, 15)
        addAbs(shushan15 + (if (absWeekday(shushan15) == 6) 1 else 0), "Shushan Purim", "minor")
        addAbs(hebToAbs(y, adar, 13) - (if (absWeekday(hebToAbs(y, adar, 13)) == 6) 2L else 0L), "Taanis Esther", "fast")
        // Pesach
        add(1, 14, "Erev Pesach", "erev")
        add(1, 15, "Pesach", "yt")
        if (!il) add(1, 16, "Pesach", "yt") else add(1, 16, "Chol HaMoed Pesach", "chm")
        add(1, 17, "Chol HaMoed Pesach", "chm")
        add(1, 18, "Chol HaMoed Pesach", "chm")
        add(1, 19, "Chol HaMoed Pesach", "chm")
        add(1, 20, "Chol HaMoed Pesach", "chm")
        add(1, 21, "Pesach", "yt")
        if (!il) add(1, 22, "Pesach", "yt")
        add(2, 18, "Lag BaOmer", "minor")
        // Shavuos
        add(3, 5, "Erev Shavuos", "erev")
        add(3, 6, "Shavuos", "yt")
        if (!il) add(3, 7, "Shavuos", "yt")
        // Summer fasts
        addAbs(hebToAbs(y, 4, 17) + (if (absWeekday(hebToAbs(y, 4, 17)) == 6) 1L else 0L), "Shiva Asar B'Tamuz", "fast")
        addAbs(hebToAbs(y, 5, 9) + (if (absWeekday(hebToAbs(y, 5, 9)) == 6) 1L else 0L), "Tisha B'Av", "fast")
        add(5, 15, "Tu B'Av", "minor")
        add(6, 29, "Erev Rosh Hashana", "erev")

        // Rosh Chodesh
        val ml = if (leap) 13 else 12
        for (mm in 1..ml) {
            val prev = if (mm == 1) ml else mm - 1
            if (hebMonthDays(y, prev) == 30) {
                addAbs(hebToAbs(y, prev, 30), "Rosh Chodesh " + HEB_MONTHS[mm], "rc")
            }
            add(mm, 1, "Rosh Chodesh " + HEB_MONTHS[mm], "rc")
        }
        // Shabbos Mevarchim (not before Tishri)
        for (m2 in 1..ml) {
            if (m2 == 7) continue
            val rcAbs = hebToAbs(y, m2, 1)
            val mev = if (absWeekday(rcAbs) == 6) rcAbs else dayOnOrBefore(6, rcAbs - 1)
            addAbs(mev, "Shabbos Mevarchim " + HEB_MONTHS[m2], "mevarchim")
        }
        // Arba Parshiyos
        val rcAdar = hebToAbs(y, adar, 1)
        addAbs(dayOnOrBefore(6, rcAdar), "Shabbos Shekalim", "special-shabbos")
        addAbs(dayOnOrBefore(6, purim14 - 1), "Shabbos Zachor", "special-shabbos")
        val rcNisan = hebToAbs(y, 1, 1)
        val hachodesh = dayOnOrBefore(6, rcNisan)
        addAbs(hachodesh, "Shabbos HaChodesh", "special-shabbos")
        addAbs(dayOnOrBefore(6, hachodesh - 1), "Shabbos Parah", "special-shabbos")
        // Shabbos Shuvah / HaGadol / Chazon / Nachamu
        val rhAbs = hebToAbs(y, 7, 1)
        val ykAbs = hebToAbs(y, 7, 10)
        var s = rhAbs + 1
        while (s < ykAbs) {
            if (absWeekday(s) == 6) addAbs(s, "Shabbos Shuvah", "special-shabbos")
            s++
        }
        val pesach15 = hebToAbs(y, 1, 15)
        if (absWeekday(pesach15) != 6) addAbs(dayOnOrBefore(6, pesach15 - 1), "Shabbos HaGadol", "special-shabbos")
        val av9 = hebToAbs(y, 5, 9)
        addAbs(if (absWeekday(av9) == 6) av9 else dayOnOrBefore(6, av9 - 1), "Shabbos Chazon", "special-shabbos")
        addAbs(dayOnOrAfter(6, av9 + 1), "Shabbos Nachamu", "special-shabbos")

        if (modern) {
            // Yom HaShoah: 27 Nisan (Fri->26, Sun->28)
            val shoah = hebToAbs(y, 1, 27)
            val shd = absWeekday(shoah)
            addAbs(shoah + (if (shd == 5) -1L else if (shd == 0) 1L else 0L), "Yom HaShoah", "modern")
            // Yom HaZikaron / HaAtzmaut
            val az5 = hebToAbs(y, 2, 5)
            val azd = absWeekday(az5)
            val zik: Long
            val azm: Long
            if (azd == 5) { azm = az5 - 1; zik = az5 - 2 } // Fri -> Thu/Wed
            else if (azd == 6) { azm = az5 - 2; zik = az5 - 3 } // Sat -> Thu/Wed
            else if (azd == 1) { azm = az5 + 1; zik = az5 } // Mon -> Tue/Mon
            else { azm = az5; zik = az5 - 1 }
            addAbs(zik, "Yom HaZikaron", "modern")
            addAbs(azm, "Yom HaAtzma'ut", "modern")
            add(2, 28, "Yom Yerushalayim", "modern")
        }
        // Birkat HaChama: years (Y-1) % 28 == 0
        if ((y - 1) % 28 == 0) {
            // JS: Date.UTC(2009, 3, 8) + (Y - 5769) * 365.25 * DAY_MS
            // (365.25 * 86400000 = 31557600000 exactly)
            val tChama = daysFromCivil(2009, 4, 8) * DAY_MS + (y - 5769).toLong() * 31557600000L
            val cChama = civilInTz(tChama, ZoneId.of("Asia/Jerusalem"))
            addAbs(gregToAbs(cChama.y, cChama.m, cChama.d), "Birkat HaChama", "minor")
        }
        evs.sortBy { it.abs }
        return evs
    }

    /** Melacha-prohibited day (Shabbos handled separately). */
    fun isYomTovAbs(abs: Long, il: Boolean): Boolean {
        val h = absToHeb(abs)
        val m = h.m
        val d = h.d
        if (m == 7) {
            if (d == 1 || d == 2 || d == 10 || d == 15 || d == 22) return true
            if (d == 16 || d == 23) return !il
        } else if (m == 1) {
            if (d == 15 || d == 21) return true
            if (d == 16 || d == 22) return !il
        } else if (m == 3) {
            if (d == 6) return true
            if (d == 7) return !il
        }
        return false
    }

    // ---------- Daf Yomi ----------
    // 2711-day cycle; Berachos 2 = Jan 5, 2020. Current cycle system valid from Jun 24, 1975.

    private val dafTractates = listOf(
        Triple("Berachos", 2, 64), Triple("Shabbos", 2, 157), Triple("Eruvin", 2, 105),
        Triple("Pesachim", 2, 121), Triple("Shekalim", 2, 22), Triple("Yoma", 2, 88),
        Triple("Sukkah", 2, 56), Triple("Beitzah", 2, 40), Triple("Rosh Hashana", 2, 35),
        Triple("Taanis", 2, 31), Triple("Megillah", 2, 32), Triple("Moed Katan", 2, 29),
        Triple("Chagigah", 2, 27), Triple("Yevamos", 2, 122), Triple("Kesubos", 2, 112),
        Triple("Nedarim", 2, 91), Triple("Nazir", 2, 66), Triple("Sotah", 2, 49),
        Triple("Gittin", 2, 90), Triple("Kiddushin", 2, 82), Triple("Bava Kamma", 2, 119),
        Triple("Bava Metzia", 2, 119), Triple("Bava Basra", 2, 176), Triple("Sanhedrin", 2, 113),
        Triple("Makkos", 2, 24), Triple("Shevuos", 2, 49), Triple("Avodah Zarah", 2, 76),
        Triple("Horayos", 2, 14), Triple("Zevachim", 2, 120), Triple("Menachos", 2, 110),
        Triple("Chullin", 2, 142), Triple("Bechoros", 2, 61), Triple("Arachin", 2, 34),
        Triple("Temurah", 2, 34), Triple("Kerisos", 2, 28), Triple("Meilah", 2, 22),
        Triple("Kinnim", 23, 25), Triple("Tamid", 26, 33), Triple("Midos", 34, 37),
        Triple("Niddah", 2, 73)
    )
    private val dafAnchorAbs: Long = gregToAbs(2020, 1, 5) // Berachos 2
    private val dafMinAbs: Long = gregToAbs(1975, 6, 24)
    private const val DAF_CYCLE: Long = 2711

    fun dafYomi(abs: Long): Daf? {
        if (abs < dafMinAbs) return null
        var idx = Math.floorMod(abs - dafAnchorAbs, DAF_CYCLE)
        for (t in dafTractates) {
            val days = t.third - t.second + 1
            if (idx < days.toLong()) return Daf(t.first, t.second + idx.toInt(), idx)
            idx -= days.toLong()
        }
        return null
    }

    // ---------- Sefiras HaOmer ----------

    fun omerCount(abs: Long): Int {
        val h = absToHeb(abs)
        val start = hebToAbs(h.y, 1, 16)
        val end = hebToAbs(h.y, 3, 5)
        if (abs >= start && abs <= end) return (abs - start + 1).toInt()
        return 0
    }

    // ---------- Kiddush Levana window ----------

    fun kiddushLevana(abs: Long): Levana? {
        val h = absToHeb(abs)
        val molad = calcMolad(h.y, h.m)
        val from = molad.utcMs + 72 * 3600000.0
        val to = molad.utcMs + (14 * 24 + 18) * 3600000.0 + 22 * 60000.0 + 666.0
        val nowMs = (abs - 719163L) * DAY_MS.toDouble() + 12 * 3600000.0
        if (nowMs >= from && nowMs <= to) {
            return Levana(from, to, molad)
        }
        return null
    }

    // ---------- Day info: everything for one civil date ----------

    /** Events for a civil day given as an absolute day number. */
    fun eventsForAbs(abs: Long, il: Boolean, modern: Boolean): List<HolidayEvent> {
        val heb = absToHeb(abs)
        val out = ArrayList<HolidayEvent>()
        for (yy in listOf(heb.y - 1, heb.y, heb.y + 1)) {
            for (e in holidaysForYear(yy, il, modern)) {
                if (e.abs == abs) out.add(e)
            }
        }
        return out
    }

    fun getDayInfo(y: Int, m: Int, d: Int, loc: Location, opts: ZmanimOpts, il: Boolean): DayInfo {
        val abs = gregToAbs(y, m, d)
        val heb = absToHeb(abs)
        val wd = absWeekday(abs)
        val z = computeZmanim(y, m, d, loc, opts)

        val evs = eventsForAbs(abs, il, true)

        val ytToday = isYomTovAbs(abs, il)
        val shabbosToday = (wd == 6)
        val ytTomorrow = isYomTovAbs(abs + 1, il)
        val shabbosTomorrow = (absWeekday(abs + 1) == 6)

        // candle lighting
        val candle: CandleInfo? =
            if ((shabbosTomorrow || ytTomorrow) && !shabbosToday) {
                CandleInfo(z.candleLighting, if (ytToday) "light from an existing flame" else "")
            } else null
        // havdalah
        val havdalah: CandleInfo? =
            if ((shabbosToday || ytToday) && !(shabbosTomorrow || ytTomorrow)) {
                CandleInfo(z.havdalah, "")
            } else null
        // fasts
        var fast: HolidayEvent? = null
        for (e in evs) {
            if (e.type == "fast" || e.type == "fast-yt") fast = e
        }
        var fastBegin: Long? = null
        var fastEnd: Long? = null
        if (fast != null) {
            val isMajor = (fast.name == "Yom Kippur" || fast.name == "Tisha B'Av")
            if (isMajor) {
                // begins at shkia/candle-lighting of erev
                val prev = prevCivil(y, m, d)
                val zp = computeZmanim(prev.y, prev.m, prev.d, loc, opts)
                fastBegin = if (fast.name == "Yom Kippur") zp.candleLighting else zp.shkia
            } else {
                fastBegin = z.alos161
            }
            fastEnd = z.havdalah
        }
        // chametz (Erev Pesach)
        val chametz: Chametz? =
            if (heb.m == 1 && heb.d == 14) Chametz(z.sofAchilasChametz, z.sofBiurChametz) else null
        // parsha (of this Shabbos / upcoming Shabbos)
        val parsha = getParsha(abs, il)
        var shabbosLabel: String? = null
        if (wd == 6) {
            for (e in evs) {
                if (e.type == "special-shabbos") shabbosLabel = e.name
            }
            if (shabbosLabel == null && parsha.names.firstOrNull() == "Beshalach") {
                shabbosLabel = "Shabbos Shirah"
            }
        }

        var molad: MoladInfo? = null
        if (wd == 6) {
            var isMevarchim = false
            for (e in evs) {
                if (e.type == "mevarchim") { isMevarchim = true; break }
            }
            if (isMevarchim) molad = upcomingMolad(abs)
        }

        return DayInfo(
            abs = abs, heb = heb, wd = wd, zmanim = z,
            events = evs, parsha = parsha, shabbosLabel = shabbosLabel,
            isShabbos = shabbosToday, isYomTov = ytToday,
            candle = candle, havdalah = havdalah,
            fast = fast, fastBegin = fastBegin, fastEnd = fastEnd,
            chametz = chametz, omer = omerCount(abs), daf = dafYomi(abs),
            levana = kiddushLevana(abs),
            molad = molad
        )
    }

    /** Molad announced on Shabbos Mevarchim: molad of the upcoming month. */
    fun upcomingMolad(abs: Long): MoladInfo {
        val h = absToHeb(abs)
        val nm = nextHebMonth(h.y, h.m)
        var my = h.y
        if ((h.m == 6 && nm == 7) || ((h.m == 12 || h.m == 13) && nm == 1)) my = h.y + 1
        val mol = calcMolad(my, nm)
        return MoladInfo(HEB_MONTHS[nm], mol)
    }
}
