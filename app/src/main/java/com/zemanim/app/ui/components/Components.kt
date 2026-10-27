package com.zemanim.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zemanim.app.engine.Format
import com.zemanim.app.engine.HolidayEvent
import com.zemanim.app.ui.ZmanRow

/** Event dot color by type (readable on both light and dark cards). */
fun eventColor(type: String): Color = when (type) {
    "yt" -> Color(0xFF1E3A8A)
    "fast", "fast-yt" -> Color(0xFFB91C1C)
    "rc" -> Color(0xFF15803D)
    "minor" -> Color(0xFF7C5C10)
    "chm" -> Color(0xFF3B6EA5)
    "modern" -> Color(0xFF0F766E)
    "mevarchim", "special-shabbos" -> Color(0xFF6D28D9)
    else -> Color(0xFFB45309)
}

/** Card with an uppercase section title. */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
            }
            content()
        }
    }
}

/** One event row: colored dot + name + optional type description. */
@Composable
fun EventRow(event: HolidayEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(eventColor(event.type))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = event.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            val desc = Format.eventDesc(event.type)
            if (desc.isNotEmpty()) {
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** One zman row: name (+sub) on the left, time on the right. */
@Composable
fun ZmanRowItem(
    row: ZmanRow,
    zoneId: String,
    hour12: Boolean,
    highlighted: Boolean,
    passed: Boolean
) {
    val bg = if (highlighted) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .alpha(if (passed && !highlighted) 0.55f else 1f)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
                color = if (highlighted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (row.sub.isNotEmpty()) {
                Text(
                    text = row.sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp
                )
            }
        }
        Text(
            text = Format.fmtTime(row.ms, zoneId, hour12),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Small pill chip (parsha / omer / daf yomi). */
@Composable
fun InfoChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
        )
    }
}

/** Uppercase group header inside the zmanim list. */
@Composable
fun ZmanGroupHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    androidx.compose.material3.TextButton(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (expanded) "▴" else "▾",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
