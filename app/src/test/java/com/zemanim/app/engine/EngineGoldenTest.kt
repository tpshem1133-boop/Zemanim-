package com.zemanim.app.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Validates the Kotlin port against golden values generated from the verified
 * JavaScript engine (tools/generate-goldens.mjs -> app/src/test/resources/goldens.json).
 *
 * Assertions: Hebrew date exact, weekday exact, parsha names exact, event
 * name+type lists exact, omer/daf/molad exact, zmanim and special times
 * within 2 minutes, null-ness exact.
 *
 * No org.json here: android.jar stubs throw in local unit tests, so this
 * file carries a minimal JSON parser for the (fully controlled) golden format.
 */
class EngineGoldenTest {

    // ---------- minimal JSON parser ----------

    private sealed interface JVal
    private data class JObj(val m: Map<String, JVal>) : JVal
    private data class JArr(val l: List<JVal>) : JVal
    private data class JStr(val s: String) : JVal
    private data class JNum(val d: Double) : JVal
    private data class JBool(val b: Boolean) : JVal
    private object JNull : JVal

    private class Parser(private val s: String) {
        private var i = 0

        fun parse(): JVal {
            val v = value()
            ws()
            if (i != s.length) fail("trailing data at $i")
            return v
        }

        private fun ws() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        private fun value(): JVal {
            ws()
            if (i >= s.length) fail("unexpected end")
            return when (s[i]) {
                '{' -> {
                    i++
                    val m = LinkedHashMap<String, JVal>()
                    ws()
                    if (i < s.length && s[i] == '}') {
                        i++
                        return JObj(m)
                    }
                    while (true) {
                        ws()
                        val k = str()
                        ws()
                        if (i >= s.length || s[i] != ':') fail("expected : at $i")
                        i++
                        m[k] = value()
                        ws()
                        if (i < s.length && s[i] == ',') {
                            i++
                            continue
                        }
                        if (i < s.length && s[i] == '}') {
                            i++
                            break
                        }
                        fail("expected , or } at $i")
                    }
                    JObj(m)
                }
                '[' -> {
                    i++
                    val l = ArrayList<JVal>()
                    ws()
                    if (i < s.length && s[i] == ']') {
                        i++
                        return JArr(l)
                    }
                    while (true) {
                        l.add(value())
                        ws()
                        if (i < s.length && s[i] == ',') {
                            i++
                            continue
                        }
                        if (i < s.length && s[i] == ']') {
                            i++
                            break
                        }
                        fail("expected , or ] at $i")
                    }
                    JArr(l)
                }
                '"' -> JStr(str())
                't' -> {
                    expect("true")
                    JBool(true)
                }
                'f' -> {
                    expect("false")
                    JBool(false)
                }
                'n' -> {
                    expect("null")
                    JNull
                }
                else -> {
                    val start = i
                    while (i < s.length && (s[i].isDigit() || s[i] == '-' || s[i] == '+' || s[i] == '.' || s[i] == 'e' || s[i] == 'E')) i++
                    if (start == i) fail("unexpected char ${s[i]} at $i")
                    JNum(s.substring(start, i).toDouble())
                }
            }
        }

        private fun expect(w: String) {
            if (!s.startsWith(w, i)) fail("expected $w at $i")
            i += w.length
        }

        private fun str(): String {
            if (i >= s.length || s[i] != '"') fail("expected string at $i")
            i++
            val sb = StringBuilder()
            while (i < s.length) {
                val c = s[i++]
                if (c == '"') return sb.toString()
                if (c == '\\') {
                    if (i >= s.length) fail("bad escape")
                    when (val e = s[i++]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            sb.append(s.substring(i, i + 4).toInt(16).toChar())
                            i += 4
                        }
                        else -> fail("bad escape \\$e")
                    }
                } else {
                    sb.append(c)
                }
            }
            error("unterminated string")
        }
    }

    private fun JVal.obj(): Map<String, JVal> = (this as JObj).m
    private fun JVal.arr(): List<JVal> = (this as JArr).l
    private fun JVal.str(): String = (this as JStr).s
    private fun JVal.num(): Double = (this as JNum).d
    private fun JVal.long(): Long = num().toLong()
    private fun JVal.bool(): Boolean = (this as JBool).b
    private fun JVal.isNull(): Boolean = this === JNull
    private fun Map<String, JVal>.req(k: String): JVal =
        this[k] ?: error("missing key $k")

    // ---------- golden comparison ----------

    private val toleranceMs = 120000L // 2 minutes

    private fun assertTime(label: String, field: String, expected: JVal, actual: Long?) {
        if (expected.isNull()) {
            assertNull("$label: $field should be null", actual)
        } else {
            assertTrue(
                "$label: $field should be non-null (expected ${expected.num()})",
                actual != null
            )
            val diff = kotlin.math.abs(actual!! - expected.num().toLong())
            assertTrue("$label: $field diff ${diff}ms > ${toleranceMs}ms", diff <= toleranceMs)
        }
    }

    @Test
    fun goldensMatch() {
        val stream = javaClass.classLoader!!.getResourceAsStream("goldens.json")
            ?: error("goldens.json not found on test classpath")
        val text = stream.bufferedReader().readText()
        val cases = (Parser(text).parse() as JArr).l
        assertTrue("expected golden cases", cases.isNotEmpty())

        for (caseVal in cases) {
            val c = caseVal.obj()
            val label = c.req("label").str()
            val y = c.req("y").long().toInt()
            val m = c.req("m").long().toInt()
            val d = c.req("d").long().toInt()
            val locVal = c.req("loc").obj()
            val loc = Location(
                lat = locVal.req("lat").num(),
                lng = locVal.req("lng").num(),
                tz = locVal.req("tz").str(),
                elevM = locVal.req("elevM").num(),
                name = locVal.req("name").str()
            )
            val il = c.req("il").bool()
            val info = ZemanimEngine.getDayInfo(
                y, m, d, loc, ZmanimOpts(havdalah = "8.5", candleMin = 18, useElevation = false), il
            )

            // Hebrew date + weekday, exact
            val heb = c.req("heb").obj()
            assertEquals("$label: heb y", heb.req("y").long().toInt(), info.heb.y)
            assertEquals("$label: heb m", heb.req("m").long().toInt(), info.heb.m)
            assertEquals("$label: heb d", heb.req("d").long().toInt(), info.heb.d)
            assertEquals("$label: weekday", c.req("wd").long().toInt(), info.wd)

            // Parsha names + chag flag, exact
            val parsha = c.req("parsha").obj()
            val expNames = parsha.req("names").arr().map { it.str() }
            assertEquals("$label: parsha names", expNames, info.parsha.names)
            assertEquals("$label: parsha chag", parsha.req("chag").bool(), info.parsha.chag)

            // Events (name + type), exact, in order
            val expEvents = c.req("events").arr().map {
                val e = it.obj()
                e.req("name").str() to e.req("type").str()
            }
            val actEvents = info.events.map { it.name to it.type }
            assertEquals("$label: events", expEvents, actEvents)

            // Omer / Daf, exact
            assertEquals("$label: omer", c.req("omer").long().toInt(), info.omer)
            val dafVal = c.req("daf")
            if (dafVal.isNull()) {
                assertNull("$label: daf should be null", info.daf)
            } else {
                val dv = dafVal.obj()
                assertEquals("$label: daf name", dv.req("name").str(), info.daf!!.name)
                assertEquals("$label: daf", dv.req("daf").long().toInt(), info.daf!!.daf)
            }

            // Molad (Shabbos Mevarchim), exact hour/minutes/chalakim/month
            val moladVal = c.req("molad")
            if (moladVal.isNull()) {
                assertNull("$label: molad should be null", info.molad)
            } else {
                val mv = moladVal.obj()
                assertEquals("$label: molad month", mv.req("month").str(), info.molad!!.month)
                assertEquals("$label: molad hour", mv.req("hour").long().toInt(), info.molad!!.molad.hour)
                assertEquals("$label: molad minutes", mv.req("minutes").long().toInt(), info.molad!!.molad.minutes)
                assertEquals("$label: molad chalakim", mv.req("chalakim").long().toInt(), info.molad!!.molad.chalakim)
            }

            // Zmanim, within tolerance, null-ness exact
            val z = c.req("zmanim").obj()
            val zmap = mapOf<String, Long?>(
                "netz" to info.zmanim.netz,
                "shkia" to info.zmanim.shkia,
                "alos161" to info.zmanim.alos161,
                "alos18" to info.zmanim.alos18,
                "alos198" to info.zmanim.alos198,
                "alos72" to info.zmanim.alos72,
                "misheyakir115" to info.zmanim.misheyakir115,
                "misheyakir102" to info.zmanim.misheyakir102,
                "tzeis85" to info.zmanim.tzeis85,
                "tzeis7083" to info.zmanim.tzeis7083,
                "tzeis161" to info.zmanim.tzeis161,
                "tzeis72" to info.zmanim.tzeis72,
                "tzeis50" to info.zmanim.tzeis50,
                "tzeis42" to info.zmanim.tzeis42,
                "beinHaShmashosRT" to info.zmanim.beinHaShmashosRT,
                "sofShemaGRA" to info.zmanim.sofShemaGRA,
                "sofTefillaGRA" to info.zmanim.sofTefillaGRA,
                "chatzos" to info.zmanim.chatzos,
                "minchaGedola" to info.zmanim.minchaGedola,
                "minchaKetana" to info.zmanim.minchaKetana,
                "plagHaMincha" to info.zmanim.plagHaMincha,
                "sofAchilasChametz" to info.zmanim.sofAchilasChametz,
                "sofBiurChametz" to info.zmanim.sofBiurChametz,
                "sofShemaMGA" to info.zmanim.sofShemaMGA,
                "sofTefillaMGA" to info.zmanim.sofTefillaMGA,
                "sofShemaMGA198" to info.zmanim.sofShemaMGA198,
                "sofTefillaMGA198" to info.zmanim.sofTefillaMGA198,
                "minchaGedolaMGA" to info.zmanim.minchaGedolaMGA,
                "minchaKetanaMGA" to info.zmanim.minchaKetanaMGA,
                "plagHaMinchaMGA" to info.zmanim.plagHaMinchaMGA,
                "chatzosLaila" to info.zmanim.chatzosLaila,
                "beinHaShmashos" to info.zmanim.beinHaShmashos,
                "shaahZmanisGRA" to info.zmanim.shaahZmanisGRA,
                "shaahZmanisMGA" to info.zmanim.shaahZmanisMGA,
                "havdalah" to info.zmanim.havdalah,
                "tzeis" to info.zmanim.tzeis,
                "candleLighting" to info.zmanim.candleLighting
            )
            for ((field, actual) in zmap) {
                assertTime(label, field, z.req(field), actual)
            }
            assertEquals(
                "$label: candleLightingMins",
                c.req("candleLightingMins").long().toInt(),
                info.zmanim.candleLightingMins
            )

            // Special times
            assertTime(label, "candle", c.req("candle"), info.candle?.ms)
            assertTime(label, "havdalahInfo", c.req("havdalah"), info.havdalah?.ms)
            assertTime(label, "fastBegin", c.req("fastBegin"), info.fastBegin)
            assertTime(label, "fastEnd", c.req("fastEnd"), info.fastEnd)
            val chametzVal = c.req("chametz")
            if (chametzVal.isNull()) {
                assertNull("$label: chametz should be null", info.chametz)
            } else {
                val cv = chametzVal.obj()
                assertTime(label, "chametz.achila", cv.req("achila"), info.chametz!!.achila)
                assertTime(label, "chametz.biur", cv.req("biur"), info.chametz!!.biur)
            }

            // Kiddush Levana window, within tolerance
            val levanaVal = c.req("levana")
            if (levanaVal.isNull()) {
                assertNull("$label: levana should be null", info.levana)
            } else {
                val lv = levanaVal.obj()
                assertTrue(
                    "$label: levana.fromMs should be non-null",
                    info.levana != null
                )
                val fromDiff = kotlin.math.abs(info.levana!!.fromMs - lv.req("fromMs").num())
                val toDiff = kotlin.math.abs(info.levana!!.toMs - lv.req("toMs").num())
                assertTrue("$label: levana.fromMs diff $fromDiff", fromDiff <= toleranceMs.toDouble())
                assertTrue("$label: levana.toMs diff $toDiff", toDiff <= toleranceMs.toDouble())
            }
        }
    }
}
