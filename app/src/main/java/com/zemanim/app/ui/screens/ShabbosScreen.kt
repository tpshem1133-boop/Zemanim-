package com.zemanim.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zemanim.app.data.AppSettings
import com.zemanim.app.engine.CivilDate
import com.zemanim.app.engine.Format
import com.zemanim.app.engine.ZemanimEngine
import com.zemanim.app.engine.ZmanimOpts
import com.zemanim.app.ui.components.EventRow
import com.zemanim.app.ui.components.SectionCard
import com.zemanim.app.ui.components.ZmanRowItem
import com.zemanim.app.ui.buildZmanRows
import com.zemanim.app.ui.havdalahLabel
import com.zemanim.app.ui.parshaLabel

/**
 * Shabbos tab: upcoming Shabbos hero (parsha + Hebrew date), candle
 * lighting + havdalah card, full Shabbos-day zmanim list, Shabbos events.
 */
@Composable
fun ShabbosScreen(settings: AppSettings, today: CivilDate) {
    val zoneId = settings.location.tz
    val hour12 = settings.hour12
    val (friday, shabbos) = remember(today, settings.location.tz) {
        var fri = ZemanimEngine.gregToAbs(today.y, today.m, today.d)
        while (ZemanimEngine.absWeekday(fri) != 5) fri++
        Pair(ZemanimEngine.absToGreg(fri), ZemanimEngine.absToGreg(fri + 1))
    }
    val opts = ZmanimOpts(
        havdalah = settings.havdalah,
        candleMin = settings.candleMin,
        useElevation = false
    )
    val friInfo = remember(friday, settings) {
        ZemanimEngine.getDayInfo(friday.y, friday.m, friday.d, settings.location, opts, settings.il)
    }
    val shabInfo = remember(shabbos, settings) {
        ZemanimEngine.getDayInfo(shabbos.y, shabbos.m, shabbos.d, settings.location, opts, settings.il)
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ---- Hero ----
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Shabbos",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                )
                val label = parshaLabel(shabInfo.parsha.names, shabInfo.parsha.chag)
                Text(
                    text = if (label.isNotEmpty()) label else "Shabbos",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = Format.hebDateStr(shabInfo.heb),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = Format.gregStr(shabbos.y, shabbos.m, shabbos.d),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                )
            }
        }

        // ---- Candle lighting + havdalah ----
        SectionCard(title = "Candle Lighting & Havdalah") {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Candle lighting",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = friInfo.candle?.let { Format.fmtTime(it.ms, zoneId, hour12) } ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = Format.gregStr(friday.y, friday.m, friday.d),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Havdalah (${havdalahLabel(settings.havdalah)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = shabInfo.havdalah?.let { Format.fmtTime(it.ms, zoneId, hour12) } ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = Format.gregStr(shabbos.y, shabbos.m, shabbos.d),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (shabInfo.havdalah == null) {
                Text(
                    text = "No havdalah time: the next day is Yom Tov.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ---- Shabbos events ----
        if (shabInfo.events.isNotEmpty()) {
            SectionCard(title = "Shabbos Events") {
                shabInfo.events.forEach { EventRow(it) }
            }
        }
        if (friInfo.events.isNotEmpty()) {
            SectionCard(title = "Erev Shabbos") {
                friInfo.events.forEach { EventRow(it) }
            }
        }

        // ---- Full Shabbos-day zmanim ----
        SectionCard(title = "Shabbos Zmanim") {
            buildZmanRows(shabInfo.zmanim, settings.showMga, settings.showExtraDawn, settings.havdalah)
                .groupBy { it.group }
                .forEach { (group, groupRows) ->
                    Text(
                        text = group,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                    groupRows.forEach { row ->
                        ZmanRowItem(
                            row = row,
                            zoneId = zoneId,
                            hour12 = hour12,
                            highlighted = false,
                            passed = false
                        )
                    }
                }
        }
    }
}
