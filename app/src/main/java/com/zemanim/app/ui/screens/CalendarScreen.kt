package com.zemanim.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zemanim.app.data.AppSettings
import com.zemanim.app.engine.CivilDate
import com.zemanim.app.engine.DayInfo
import com.zemanim.app.engine.Format
import com.zemanim.app.engine.ZemanimEngine
import com.zemanim.app.ui.components.EventRow
import com.zemanim.app.ui.components.eventColor

/**
 * Calendar tab: Hebrew month grid with event dots, horizontal month
 * transitions, and a detail bottom sheet for the tapped day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    settings: AppSettings,
    today: CivilDate,
    onOpenFullDay: (CivilDate) -> Unit
) {
    val todayHeb = remember(settings.location.tz) {
        ZemanimEngine.gregToHeb(today.y, today.m, today.d)
    }
    var hebY by remember { mutableStateOf(todayHeb.y) }
    var hebM by remember { mutableStateOf(todayHeb.m) }
    var sheetAbs by remember { mutableStateOf<Long?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val todayAbs = ZemanimEngine.gregToAbs(today.y, today.m, today.d)

    fun stepMonth(dir: Int) {
        var y = hebY
        var m = hebM
        if (dir > 0) {
            m = ZemanimEngine.nextHebMonth(y, m)
            if (m == 7) y++
        } else {
            m--
            if (m < 1) {
                m = if (ZemanimEngine.daysInHebYear(y - 1) > 360) 13 else 12
                y--
            }
        }
        hebY = y
        hebM = m
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        // ---- Month header ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { stepMonth(-1) }) { Text("‹", fontSize = 28.sp) }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = hebMonthLabelHe(hebY, hebM),
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    text = hebMonthLabelEn(hebY, hebM),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { stepMonth(1) }) { Text("›", fontSize = 28.sp) }
        }
        TextButton(
            onClick = { hebY = todayHeb.y; hebM = todayHeb.m },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) { Text("This month") }

        // ---- Weekday header ----
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                Text(
                    text = d,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        // ---- Month grid (slide direction follows navigation) ----
        AnimatedContent(
            targetState = Pair(hebY, hebM),
            transitionSpec = {
                val forward = targetState.first > initialState.first ||
                    (targetState.first == initialState.first && targetState.second > initialState.second)
                if (forward) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith
                        (slideOutHorizontally { it } + fadeOut())
                }
            },
            label = "calendar-month"
        ) { (y, m) ->
            val firstAbs = ZemanimEngine.hebToAbs(y, m, 1)
            val firstDow = ZemanimEngine.absWeekday(firstAbs)
            val daysInMonth = ZemanimEngine.hebMonthDays(y, m)
            val cells = ArrayList<Long?>()
            repeat(firstDow) { cells.add(null) }
            for (d in 1..daysInMonth) cells.add(firstAbs + (d - 1).toLong())
            while (cells.size % 7 != 0) cells.add(null)

            Column {
                cells.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { abs ->
                            if (abs == null) {
                                Spacer(modifier = Modifier.weight(1f))
                            } else {
                                DayCell(
                                    abs = abs,
                                    isToday = abs == todayAbs,
                                    settings = settings,
                                    onClick = { sheetAbs = abs }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- Detail bottom sheet ----
    sheetAbs?.let { abs ->
        val greg = ZemanimEngine.absToGreg(abs)
        val info = remember(abs, settings) {
            ZemanimEngine.getDayInfo(
                greg.y, greg.m, greg.d, settings.location,
                com.zemanim.app.engine.ZmanimOpts(
                    havdalah = settings.havdalah,
                    candleMin = settings.candleMin,
                    useElevation = false
                ),
                settings.il
            )
        }
        ModalBottomSheet(
            onDismissRequest = { sheetAbs = null },
            sheetState = sheetState
        ) {
            DaySheetContent(
                info = info,
                greg = greg,
                settings = settings,
                onOpenFullDay = {
                    sheetAbs = null
                    onOpenFullDay(greg)
                }
            )
        }
    }
}

@Composable
private fun DayCell(
    abs: Long,
    isToday: Boolean,
    settings: AppSettings,
    onClick: () -> Unit
) {
    val heb = ZemanimEngine.absToHeb(abs)
    val evs = ZemanimEngine.eventsForAbs(abs, settings.il, true)
    val dotTypes = evs.map { it.type }.distinct().take(3)
    val isShabbos = ZemanimEngine.absWeekday(abs) == 6
    val bg = when {
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface
    }
    val fg = when {
        isToday -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .weight(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = heb.d.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isShabbos || isToday) FontWeight.Bold else FontWeight.Normal,
                color = fg
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.padding(top = 3.dp)
            ) {
                if (isShabbos) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(eventColor("special-shabbos"))
                    )
                }
                dotTypes.forEach { t ->
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(eventColor(t))
                    )
                }
            }
        }
    }
}

@Composable
private fun DaySheetContent(
    info: DayInfo,
    greg: CivilDate,
    settings: AppSettings,
    onOpenFullDay: () -> Unit
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = Format.hebDateStr(info.heb),
            style = MaterialTheme.typography.displayMedium
        )
        Text(
            text = Format.gregStr(greg.y, greg.m, greg.d),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        info.parsha.let { p ->
            val label = com.zemanim.app.ui.parshaLabel(p.names, p.chag)
            if (label.isNotEmpty()) {
                Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        if (info.events.isNotEmpty()) {
            info.events.forEach { EventRow(it) }
        }
        info.candle?.let {
            Text(
                text = "Candle lighting: ${Format.fmtTime(it.ms, settings.location.tz, settings.hour12)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        info.havdalah?.let {
            Text(
                text = "Havdalah: ${Format.fmtTime(it.ms, settings.location.tz, settings.hour12)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        val z = info.zmanim
        val keyRows = listOf(
            "Netz" to z.netz, "Sof Shema (GRA)" to z.sofShemaGRA,
            "Chatzos" to z.chatzos, "Mincha Ketana" to z.minchaKetana,
            "Shkia" to z.shkia, "Tzeis" to z.tzeis
        )
        keyRows.forEach { (label, ms) ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(
                    text = Format.fmtTime(ms, settings.location.tz, settings.hour12),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Button(onClick = onOpenFullDay, modifier = Modifier.fillMaxWidth()) {
            Text("Open full day")
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/** "תשרי תשפ״ז" — Hebrew month label from the 1st of the month. */
private fun hebMonthLabelHe(y: Int, m: Int): String {
    val full = Format.hebDateStr(com.zemanim.app.engine.HebDate(y, m, 1))
    return full.split(" ").drop(1).joinToString(" ")
}

/** "Tishri 5787" — English month label from the 1st of the month. */
private fun hebMonthLabelEn(y: Int, m: Int): String {
    val full = Format.enHebDateStr(com.zemanim.app.engine.HebDate(y, m, 1))
    return full.split(" ").drop(1).joinToString(" ")
}
