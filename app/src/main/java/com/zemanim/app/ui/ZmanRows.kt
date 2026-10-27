package com.zemanim.app.ui

import com.zemanim.app.engine.Zmanim

/**
 * The ordered, grouped zmanim list shown on the Today / Shabbos screens,
 * mirroring the web app's zmanRows (app1.js). Groups: Dawn, Morning,
 * Afternoon, Evening. [ms] is UTC epoch-ms, null when not computable.
 */
data class ZmanRow(val group: String, val name: String, val sub: String, val ms: Long?)

fun havdalahLabel(method: String): String = when (method) {
    "7.083" -> "7.083°"
    "72" -> "72 minutes"
    "50" -> "50 minutes"
    "42" -> "42 minutes"
    else -> "8.5°"
}

fun buildZmanRows(
    z: Zmanim,
    showMga: Boolean,
    showExtraDawn: Boolean,
    havdalahMethod: String
): List<ZmanRow> {
    val rows = ArrayList<ZmanRow>()
    fun add(group: String, name: String, sub: String, ms: Long?) {
        rows.add(ZmanRow(group, name, sub, ms))
    }
    if (showExtraDawn) {
        add("Dawn", "Alos HaShachar", "16.1° below horizon", z.alos161)
        add("Dawn", "Alos HaShachar", "18° below horizon", z.alos18)
        add("Dawn", "Alos HaShachar", "19.8° below horizon", z.alos198)
        add("Dawn", "Misheyakir", "11.5° below horizon", z.misheyakir115)
        add("Dawn", "Misheyakir", "10.2° below horizon", z.misheyakir102)
    } else {
        add("Dawn", "Alos HaShachar", "dawn (16.1°)", z.alos161)
    }
    add("Dawn", "Netz HaChama", "sunrise", z.netz)
    add("Morning", "Sof Zman Kriyas Shema", "GRA", z.sofShemaGRA)
    if (showMga) add("Morning", "Sof Zman Kriyas Shema", "MGA", z.sofShemaMGA)
    add("Morning", "Sof Zman Tefillah", "GRA", z.sofTefillaGRA)
    if (showMga) add("Morning", "Sof Zman Tefillah", "MGA", z.sofTefillaMGA)
    add("Morning", "Chatzos", "midday", z.chatzos)
    add("Afternoon", "Mincha Gedola", "", z.minchaGedola)
    add("Afternoon", "Mincha Ketana", "", z.minchaKetana)
    add("Afternoon", "Plag HaMincha", "", z.plagHaMincha)
    add("Evening", "Shkia", "sunset", z.shkia)
    if (z.beinHaShmashos != null) add("Evening", "Bein HaShmashos", "dusk (Geonim)", z.beinHaShmashos)
    add("Evening", "Tzeis HaKochavim", "nightfall (${havdalahLabel(havdalahMethod)})", z.tzeis)
    if (z.beinHaShmashosRT != null) add("Evening", "Bein HaShmashos", "Rabbeinu Tam", z.beinHaShmashosRT)
    add("Evening", "Chatzos HaLaila", "midnight", z.chatzosLaila)
    return rows
}

/** Parsha label for chips/heroes: "Parshas Haazinu", "Parshas Acharei Mos-Kedoshim", or a chag name. */
fun parshaLabel(names: List<String>, chag: Boolean): String {
    if (names.isEmpty()) return ""
    return if (chag) names.joinToString(" / ") else "Parshas " + names.joinToString("-")
}
