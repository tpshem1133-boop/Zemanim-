package com.zemanim.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.zemanim.app.data.AppSettings
import com.zemanim.app.data.CITY_LIST
import com.zemanim.app.data.TIMEZONE_LIST
import com.zemanim.app.engine.Location
import com.zemanim.app.ui.components.SectionCard

/**
 * Settings tab: location (GPS, city presets, manual fields), candle
 * minutes, havdalah method, Israel/Diaspora, clock format, MGA and
 * extra dawn/dusk toggles, theme, and an About card.
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onUseGps: () -> Unit,
    onLocationChange: (Location) -> Unit
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ---- Location ----
        SectionCard(title = "Location") {
            Text(
                text = settings.location.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "%.4f, %.4f · %s · elev %.0f m".format(
                    settings.location.lat, settings.location.lng,
                    settings.location.tz, settings.location.elevM
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onUseGps,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) { Text("Use my current location (GPS)") }

            Text(
                text = "City",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 14.dp)
            )
            SelectorRow(
                current = settings.location.name,
                options = CITY_LIST.map { it.name },
                onSelect = { name ->
                    CITY_LIST.firstOrNull { it.name == name }?.let { onLocationChange(it) }
                }
            )

            ManualLocationFields(settings, onLocationChange)
        }

        // ---- Candle lighting ----
        SectionCard(title = "Candle Lighting") {
            Text(
                text = "Minutes before sunset",
                style = MaterialTheme.typography.bodyMedium
            )
            SelectorRow(
                current = settings.candleMin.toString(),
                options = listOf("10", "15", "18", "20", "22", "30", "40"),
                onSelect = { onSettingsChange(settings.copy(candleMin = it.toInt())) }
            )
        }

        // ---- Havdalah ----
        SectionCard(title = "Havdalah Method") {
            SelectorRow(
                current = havdalahName(settings.havdalah),
                options = listOf("8.5", "7.083", "72", "50", "42").map { havdalahName(it) },
                onSelect = { label ->
                    val key = mapOf(
                        "8.5° below horizon" to "8.5",
                        "7.083° below horizon" to "7.083",
                        "72 minutes after sunset" to "72",
                        "50 minutes after sunset" to "50",
                        "42 minutes after sunset" to "42"
                    )[label] ?: "8.5"
                    onSettingsChange(settings.copy(havdalah = key))
                }
            )
        }

        // ---- Toggles ----
        SectionCard(title = "Calendar & Display") {
            ToggleRow(
                label = "Israel schedule",
                sub = "Shorter Yom Tov, Shushan Purim rules",
                checked = settings.il,
                onChange = { onSettingsChange(settings.copy(il = it)) }
            )
            ToggleRow(
                label = "12-hour clock",
                sub = "Off = 24-hour clock",
                checked = settings.hour12,
                onChange = { onSettingsChange(settings.copy(hour12 = it)) }
            )
            ToggleRow(
                label = "Show Magen Avraham zmanim",
                sub = "Alos 16.1° based shaah zmanis and sof times",
                checked = settings.showMga,
                onChange = { onSettingsChange(settings.copy(showMga = it)) }
            )
            ToggleRow(
                label = "Show extra dawn/dusk times",
                sub = "Alos 18° / 19.8°, misheyakir, tzeis variants",
                checked = settings.showExtraDawn,
                onChange = { onSettingsChange(settings.copy(showExtraDawn = it)) }
            )
        }

        // ---- Theme ----
        SectionCard(title = "Theme") {
            SelectorRow(
                current = when (settings.theme) {
                    "light" -> "Light"
                    "dark" -> "Dark"
                    else -> "Follow system"
                },
                options = listOf("Follow system", "Light", "Dark"),
                onSelect = { label ->
                    val key = when (label) {
                        "Light" -> "light"
                        "Dark" -> "dark"
                        else -> "auto"
                    }
                    onSettingsChange(settings.copy(theme = key))
                }
            )
        }

        // ---- About ----
        SectionCard(title = "About") {
            Text(
                text = "Zemanim computes daily zmanim and the Hebrew calendar " +
                    "entirely on your device — no network, no account, no tracking.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Methods: sunrise/sunset at 90.833° with refraction and " +
                    "elevation; shaah zmanis (GRA) from netz to shkia; " +
                    "Rabbeinu Tam = shkia + 1.2 shaah zmanis; " +
                    "molad by chalakim arithmetic.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Times are solid approximations for everyday use. " +
                    "For halachic questions, consult your rabbi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Version 1.0 · com.zemanim.app",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

private fun havdalahName(key: String): String = when (key) {
    "8.5" -> "8.5° below horizon"
    "7.083" -> "7.083° below horizon"
    "72" -> "72 minutes after sunset"
    "50" -> "50 minutes after sunset"
    "42" -> "42 minutes after sunset"
    else -> key
}

@Composable
private fun ToggleRow(
    label: String,
    sub: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(text = sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SelectorRow(
    current: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = { expanded = true }) {
            Text(text = "$current  ▾", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        expanded = false
                        onSelect(opt)
                    }
                )
            }
        }
    }
}

@Composable
private fun ManualLocationFields(
    settings: AppSettings,
    onLocationChange: (Location) -> Unit
) {
    var lat by remember(settings.location) { mutableStateOf(settings.location.lat.toString()) }
    var lng by remember(settings.location) { mutableStateOf(settings.location.lng.toString()) }
    var elev by remember(settings.location) { mutableStateOf(settings.location.elevM.toString()) }
    var tzExpanded by remember { mutableStateOf(false) }
    var tz by remember(settings.location) { mutableStateOf(settings.location.tz) }

    Text(
        text = "Manual coordinates",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 14.dp)
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = lat,
            onValueChange = { lat = it },
            label = { Text("Latitude") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = lng,
            onValueChange = { lng = it },
            label = { Text("Longitude") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = elev,
            onValueChange = { elev = it },
            label = { Text("Elevation (m)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        Box(modifier = Modifier.weight(1f)) {
            TextButton(onClick = { tzExpanded = true }) {
                Text(text = "$tz  ▾", style = MaterialTheme.typography.bodySmall)
            }
            DropdownMenu(expanded = tzExpanded, onDismissRequest = { tzExpanded = false }) {
                TIMEZONE_LIST.forEach { zone ->
                    DropdownMenuItem(
                        text = { Text(zone) },
                        onClick = {
                            tzExpanded = false
                            tz = zone
                        }
                    )
                }
            }
        }
    }
    Button(
        onClick = {
            val la = lat.toDoubleOrNull()
            val lo = lng.toDoubleOrNull()
            val el = elev.toDoubleOrNull()
            if (la != null && lo != null && el != null && la in -90.0..90.0 && lo in -180.0..180.0) {
                onLocationChange(Location(la, lo, tz, el, "Custom (%.4f, %.4f)".format(la, lo)))
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) { Text("Save manual location") }
}
