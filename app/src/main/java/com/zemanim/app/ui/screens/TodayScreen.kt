package com.zemanim.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zemanim.app.data.AppSettings
import com.zemanim.app.engine.CivilDate
import com.zemanim.app.engine.DayInfo
import com.zemanim.app.engine.Format
import com.zemanim.app.ui.ZmanRow
import com.zemanim.app.ui.buildZmanRows
import com.zemanim.app.ui.components.EventRow
import com.zemanim.app.ui.components.InfoChip
import com.zemanim.app.ui.components.SectionCard
import com.zemanim.app.ui.components.ZmanGroupHeader
import com.zemanim.app.ui.components.ZmanRowItem
import com.zemanim.app.ui.havdalahLabel
import com.zemanim.app.ui.parshaLabel

/**
 * Today tab: date header, "now" hero card with animated progress ring,
 * parsha/omer/daf chips, events, special times, grouped zmanim, shaah
 * zmanis, and a Kiddush Levana card when inside the window.
 */
@Composable
fun TodayScreen(
    dayInfo: DayInfo,
    selectedDate: CivilDate,
    isToday: Boolean,
    settings: AppSettings,
    nowMs: Long,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onGoToday: () -> Unit
) {
    val zoneId = settings.location.tz
    val hour12 = settings.hour12
    AnimatedContent(
        targetState = selectedDate,
        transitionSpec = {
            (fadeIn() + scaleIn(initialScale = 0.985f)) togetherWith
                (fadeOut() + scaleOut(targetScale = 0.985f))
        },
        label = "today-date"
    ) { _ ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ---- Header ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevDay) { Text("‹", fontSize = 28.sp) }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = Format.hebDateStr(dayInfo.heb),
                        style = MaterialTheme.typography.displayLarge
                    )
                    Text(
                        text = Format.gregStr(selectedDate.y, selectedDate.m, selectedDate.d),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Format.WEEKDAYS[dayInfo.wd],
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onNextDay) { Text("›", fontSize = 28.sp) }
            }
            if (!isToday) {
                TextButton(
                    onClick = onGoToday,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) { Text("Back to today") }
            }

            // ---- Now hero ----
            NowHero(dayInfo, settings, nowMs)

            // ---- Chips ----
            val chips = ArrayList<String>()
            val pl = parshaLabel(dayInfo.parsha.names, dayInfo.parsha.chag)
            if (pl.isNotEmpty()) chips.add(pl)
            if (dayInfo.omer > 0) chips.add("Omer ${dayInfo.omer}")
            dayInfo.daf?.let { chips.add("Daf Yomi: ${it.name} ${it.daf}") }
            if (chips.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) { chips.forEach { InfoChip(it) } }
            }

            // ---- Events ----
            if (dayInfo.events.isNotEmpty()) {
                SectionCard(title = "Events") {
                    dayInfo.events.forEach { EventRow(it) }
                }
            }

            // ---- Special times ----
            SpecialTimesCard(dayInfo, settings)

            // ---- Grouped zmanim ----
            ZmanimList(dayInfo, settings, nowMs)

            // ---- Shaah Zmanis footer ----
            dayInfo.zmanim.shaahZmanisGRA?.let { sG ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Shaah Zmanis (GRA): ${Format.fmtDur(sG)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        dayInfo.zmanim.shaahZmanisMGA?.let { sM ->
                            if (settings.showMga) {
                                Text(
                                    text = "Shaah Zmanis (MGA): ${Format.fmtDur(sM)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // ---- Kiddush Levana ----
            dayInfo.levana?.let { lv ->
                SectionCard(title = "Kiddush Levana") {
                    Text(
                        text = "Earliest: ${Format.fmtTime(lv.fromMs.toLong(), zoneId, hour12)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Ideal: ${Format.fmtTime((lv.molad.utcMs + 7.0 * 86400000.0).toLong(), zoneId, hour12)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Latest: ${Format.fmtTime(lv.toMs.toLong(), zoneId, hour12)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Preferably recited outdoors, under the moon, on Motzei Shabbos if possible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/** Hero card: current zman period, animated progress ring, countdown to next zman. */
@Composable
private fun NowHero(dayInfo: DayInfo, settings: AppSettings, nowMs: Long) {
    val rows = buildZmanRows(dayInfo.zmanim, settings.showMga, settings.showExtraDawn, settings.havdalah)
        .filter { it.ms != null }
    val nextIdx = rows.indexOfFirst { it.ms!! > nowMs }
    val prev = if (nextIdx > 0) rows[nextIdx - 1] else null
    val next = if (nextIdx >= 0) rows[nextIdx] else null
    val progressTarget = when {
        prev != null && next != null ->
            ((nowMs - prev.ms!!).toFloat() / (next.ms!! - prev.ms!!).toFloat()).coerceIn(0f, 1f)
        next == null -> 1f
        else -> 0f
    }
    val progress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "now-progress"
    )
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Now",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                )
                Text(
                    text = prev?.name ?: "Before first zman",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                if (next != null) {
                    Text(
                        text = "Next: ${next.name} at ${Format.fmtTime(next.ms, settings.location.tz, settings.hour12)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = "in ${Format.fmtDur(next.ms!! - nowMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                } else {
                    Text(
                        text = "Day complete",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Box(
                modifier = Modifier.size(92.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(92.dp)) {
                    drawArc(
                        color = track,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        size = Size(size.width, size.height),
                        style = Stroke(width = 9f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = primary,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        size = Size(size.width, size.height),
                        style = Stroke(width = 9f, cap = StrokeCap.Round)
                    )
                }
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/** Candle lighting / havdalah / fast / chametz / molad card. */
@Composable
private fun SpecialTimesCard(dayInfo: DayInfo, settings: AppSettings) {
    val zoneId = settings.location.tz
    val hour12 = settings.hour12
    val lines = ArrayList<Pair<String, String>>()
    dayInfo.candle?.let {
        lines.add("Candle lighting" to Format.fmtTime(it.ms, zoneId, hour12))
    }
    dayInfo.havdalah?.let {
        lines.add("Havdalah (${havdalahLabel(settings.havdalah)})" to Format.fmtTime(it.ms, zoneId, hour12))
    }
    if (dayInfo.havdalah == null && (dayInfo.isShabbos || dayInfo.isYomTov)) {
        lines.add("Havdalah" to "— Yom Tov")
    }
    dayInfo.fast?.let { fast ->
        dayInfo.fastBegin?.let { lines.add("Fast begins (${fast.name})" to Format.fmtTime(it, zoneId, hour12)) }
        dayInfo.fastEnd?.let { lines.add("Fast ends (${fast.name})" to Format.fmtTime(it, zoneId, hour12)) }
    }
    dayInfo.chametz?.let {
        lines.add("Sof Achilas Chametz" to Format.fmtTime(it.achila, zoneId, hour12))
        lines.add("Sof Biur Chametz" to Format.fmtTime(it.biur, zoneId, hour12))
    }
    dayInfo.molad?.let { m ->
        val mol = m.molad
        lines.add(
            "Molad ${m.month}" to
                "${Format.WEEKDAYS[mol.dow]} ${mol.hour}:${mol.minutes.toString().padStart(2, '0')} + ${mol.chalakim} chalakim"
        )
    }
    if (lines.isEmpty()) return
    SectionCard(title = "Special Times") {
        lines.forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** Grouped, expandable zmanim list with next zman highlighted. */
@Composable
private fun ZmanimList(dayInfo: DayInfo, settings: AppSettings, nowMs: Long) {
    val rows = buildZmanRows(dayInfo.zmanim, settings.showMga, settings.showExtraDawn, settings.havdalah)
    val groups = rows.groupBy { it.group }
    val nextMs = rows.filter { it.ms != null && it.ms > nowMs }.minOfOrNull { it.ms!! }
    val zoneId = settings.location.tz
    val hour12 = settings.hour12
    var expanded by remember { mutableStateOf(mapOf<String, Boolean>()) }
    SectionCard(title = "Zmanim") {
        groups.forEach { (group, groupRows) ->
            val isOpen = expanded[group] ?: true
            ZmanGroupHeader(
                title = group,
                expanded = isOpen,
                onToggle = { expanded = expanded + (group to !isOpen) }
            )
            AnimatedVisibility(visible = isOpen) {
                Column {
                    groupRows.forEach { row ->
                        ZmanRowItem(
                            row = row,
                            zoneId = zoneId,
                            hour12 = hour12,
                            highlighted = row.ms != null && row.ms == nextMs,
                            passed = row.ms != null && row.ms <= nowMs
                        )
                    }
                }
            }
        }
    }
}
