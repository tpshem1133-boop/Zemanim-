package com.zemanim.app.engine

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Display formatting helpers, ported from the web app (app1.js):
 * Hebrew numerals with gershayim, Hebrew/English month names, dates,
 * times in the location timezone, and durations.
 */
object Format {

    private val ONES = arrayOf("", "א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט")
    private val TENS = arrayOf("", "י", "כ", "ל", "מ", "נ", "ס", "ע", "פ", "צ")
    private val HUND = arrayOf("", "ק", "ר", "ש", "ת", "תק", "תר", "תש", "תת", "תתק")

    /** Hebrew numerals with gershayim (drops thousands, e.g. 5787 -> תשפ"ז). */
    fun hebNum(nIn: Int): String {
        if (nIn <= 0) return ""
        var n = nIn % 1000
        if (n == 0) return "ת״ת" // 1000 exactly (rare)
        if (n == 15) return "ט״ו"
        if (n == 16) return "ט״ז"
        var s = ""
        if (n >= 100) {
            val hd = n / 100
            s += HUND[hd]
            n -= hd * 100
        }
        if (n == 15) {
            s += "טו"
        } else if (n == 16) {
            s += "טז"
        } else {
            val t = n / 10
            val o = n % 10
            if (t > 0) s += TENS[t]
            if (o > 0) s += ONES[o]
        }
        if (s.length <= 1) return s + "׳"
        return s.substring(0, s.length - 1) + "״" + s.substring(s.length - 1)
    }

    private val HEB_MONTHS_HE = arrayOf(
        "", "ניסן", "אייר", "סיון", "תמוז", "אב", "אלול", "תשרי", "חשון",
        "כסלו", "טבת", "שבט", "אדר", "אדר ב׳"
    )
    private val HEB_MONTHS_HE_LEAP = arrayOf(
        "", "ניסן", "אייר", "סיון", "תמוז", "אב", "אלול", "תשרי", "חשון",
        "כסלו", "טבת", "שבט", "אדר א׳", "אדר ב׳"
    )
    private val EN_MONTHS = arrayOf(
        "", "Nissan", "Iyar", "Sivan", "Tammuz", "Av", "Elul", "Tishri",
        "Cheshvan", "Kislev", "Teves", "Shevat", "Adar", "Adar II"
    )
    private val EN_MONTHS_LEAP = arrayOf(
        "", "Nissan", "Iyar", "Sivan", "Tammuz", "Av", "Elul", "Tishri",
        "Cheshvan", "Kislev", "Teves", "Shevat", "Adar I", "Adar II"
    )

    fun hebMonthNameHe(m: Int, leap: Boolean): String =
        (if (leap) HEB_MONTHS_HE_LEAP else HEB_MONTHS_HE)[m]

    fun enMonthName(m: Int, leap: Boolean): String =
        (if (leap) EN_MONTHS_LEAP else EN_MONTHS)[m]

    /** e.g. כ״א תשרי תשפ״ז */
    fun hebDateStr(h: HebDate): String {
        val leap = ZemanimEngine.isHebLeap(h.y)
        return "${hebNum(h.d)} ${hebMonthNameHe(h.m, leap)} ${hebNum(h.y)}"
    }

    /** e.g. 21 Tishri 5787 */
    fun enHebDateStr(h: HebDate): String {
        val leap = ZemanimEngine.isHebLeap(h.y)
        return "${h.d} ${enMonthName(h.m, leap)} ${h.y}"
    }

    private val gregFmt = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US)

    /** e.g. Friday, October 2, 2026 */
    fun gregStr(y: Int, m: Int, d: Int): String =
        LocalDate.of(y, m, d).format(gregFmt)

    /** e.g. Friday, October 2, 2026 for an absolute day. */
    fun gregStrAbs(abs: Long): String {
        val c = ZemanimEngine.absToGreg(abs)
        return gregStr(c.y, c.m, c.d)
    }

    private val time12Fmt = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val time24Fmt = DateTimeFormatter.ofPattern("HH:mm", Locale.US)

    /** Format an epoch-ms instant in the location timezone; "—" when null. */
    fun fmtTime(ms: Long?, zoneId: String, hour12: Boolean): String {
        if (ms == null) return "—"
        val z = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.of(zoneId))
        return z.format(if (hour12) time12Fmt else time24Fmt)
    }

    /** "1h 44m" or "58 min". */
    fun fmtDur(ms: Long): String {
        val s = (if (ms < 0) 0 else ms) / 1000
        val h = s / 3600
        val m = (s % 3600) / 60
        return if (h > 0) "${h}h ${m}m" else "$m min"
    }

    val WEEKDAYS = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Shabbos")

    /** Weekday name for an absolute day (0 = Sunday). */
    fun weekdayName(abs: Long): String = WEEKDAYS[ZemanimEngine.absWeekday(abs)]

    /** Event-type description used under event names. */
    fun eventDesc(type: String): String = when (type) {
        "yt" -> "Yom Tov"
        "fast", "fast-yt" -> "Fast day"
        "rc" -> "Rosh Chodesh"
        "minor" -> "Minor holiday"
        else -> ""
    }
}
