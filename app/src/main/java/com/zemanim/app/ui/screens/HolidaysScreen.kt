package com.zemanim.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zemanim.app.HolidayDay
import com.zemanim.app.data.AppSettings
import com.zemanim.app.engine.CivilDate
import com.zemanim.app.engine.Format
import com.zemanim.app.engine.ZemanimEngine
import com.zemanim.app.engine.ZmanimOpts
import com.zemanim.app.ui.components.EventRow
import com.zemanim.app.ui.components.SectionCard

/**
 * Holidays tab: upcoming Yomim Tovim and fasts for the next 12 months,
 * grouped by Hebrew month, with candle/fast times where applicable.
 */
@Composable
fun HolidaysScreen(settings: AppSettings, today: CivilDate) {
    val zoneId = settings.location.tz
    val hour12 = settings.hour12
    val days = remember(today, settings.il) {
        val fromAbs = ZemanimEngine.gregToAbs(today.y, today.m, today.d)
        val out = ArrayList<HolidayDay>()
        var a = fromAbs
        val end = fromAbs + 370L
        while (a < end && out.size < 80) {
            val evs = ZemanimEngine.eventsForAbs(a, settings.il, true).filter {
                it.type == "yt" || it.type == "fast" || it.type == "fast-yt" || it.type == "minor"
            }
            if (evs.isNotEmpty()) out.add(HolidayDay(a, evs))
            a++
        }
        out
    }
    val grouped = days.groupBy { abs ->
        val h = ZemanimEngine.absToHeb(abs)
        Pair(h.y, h.m)
    }

    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        grouped.forEach { (ym, monthDays) ->
            item {
                Text(
                    text = hebMonthLabelEn(ym.first, ym.second),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            items(monthDays) { day ->
                val greg = ZemanimEngine.absToGreg(day.abs)
                val heb = ZemanimEngine.absToHeb(day.abs)
                SectionCard(title = "") {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = Format.hebDateStr(heb),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = Format.gregStr(greg.y, greg.m, greg.d),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        day.events.forEach { EventRow(it) }
                        // Candle/fast times for this day
                        val info = ZemanimEngine.getDayInfo(
                            greg.y, greg.m, greg.d, settings.location,
                            ZmanimOpts(
                                havdalah = settings.havdalah,
                                candleMin = settings.candleMin,
                                useElevation = false
                            ),
                            settings.il
                        )
                        info.candle?.let {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Candle lighting",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = Format.fmtTime(it.ms, zoneId, hour12),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        info.havdalah?.let {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Havdalah",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = Format.fmtTime(it.ms, zoneId, hour12),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        if (info.fast != null) {
                            info.fastBegin?.let { fb ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Fast begins",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = Format.fmtTime(fb, zoneId, hour12),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            info.fastEnd?.let { fe ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Fast ends",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = Format.fmtTime(fe, zoneId, hour12),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** "Tishri 5787" — shared with the calendar month label. */
private fun hebMonthLabelEn(y: Int, m: Int): String {
    val full = Format.enHebDateStr(com.zemanim.app.engine.HebDate(y, m, 1))
    return full.split(" ").drop(1).joinToString(" ")
}
