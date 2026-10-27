package com.zemanim.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import com.zemanim.app.data.LocationHelper
import com.zemanim.app.engine.Location
import com.zemanim.app.ui.screens.CalendarScreen
import com.zemanim.app.ui.screens.HolidaysScreen
import com.zemanim.app.ui.screens.SettingsScreen
import com.zemanim.app.ui.screens.ShabbosScreen
import com.zemanim.app.ui.screens.TodayScreen
import com.zemanim.app.ui.theme.ZemanimTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private enum class Tab(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Filled.WbSunny),
    CALENDAR("Calendar", Icons.Filled.DateRange),
    SHABBOS("Shabbos", Icons.Filled.Star),
    HOLIDAYS("Holidays", Icons.Filled.Event),
    SETTINGS("Settings", Icons.Filled.Settings)
}

/**
 * Single-activity app: top bar with location shortcut, bottom navigation
 * across the five tabs, and a 60-second ticker so the "now" hero stays live.
 */
@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val viewModel: ZemanimViewModel by viewModels()

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val ok = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) applyGpsLocation() else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsState()
            val selectedDate by viewModel.selectedDate.collectAsState()
            var tab by remember { mutableStateOf(Tab.TODAY) }
            var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }

            // 60-second ticker for the "now" hero + countdown.
            LaunchedEffect(Unit) {
                while (isActive) {
                    delay(60_000L)
                    nowMs = System.currentTimeMillis()
                }
            }

            ZemanimTheme(themePref = settings.theme) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Zemanim") },
                            actions = {
                                TextButton(onClick = { tab = Tab.SETTINGS }) {
                                    Text(settings.location.name)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors()
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            Tab.values().forEach { t ->
                                NavigationBarItem(
                                    selected = tab == t,
                                    onClick = { tab = t },
                                    icon = { Icon(t.icon, contentDescription = t.label) },
                                    label = { Text(t.label) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            (fadeIn() + scaleIn(initialScale = 0.99f)) togetherWith
                                (fadeOut() + scaleOut(targetScale = 0.99f))
                        },
                        label = "tab-switch",
                        modifier = Modifier.padding(innerPadding)
                    ) { currentTab ->
                        when (currentTab) {
                            Tab.TODAY -> {
                                val info = viewModel.dayInfo(selectedDate)
                                val today = viewModel.today()
                                TodayScreen(
                                    dayInfo = info,
                                    selectedDate = selectedDate,
                                    isToday = selectedDate == today,
                                    settings = settings,
                                    nowMs = nowMs,
                                    onPrevDay = { viewModel.stepDay(-1) },
                                    onNextDay = { viewModel.stepDay(1) },
                                    onGoToday = { viewModel.goToday() }
                                )
                            }
                            Tab.CALENDAR -> CalendarScreen(
                                settings = settings,
                                today = viewModel.today(),
                                onOpenFullDay = { date ->
                                    viewModel.selectDate(date)
                                    tab = Tab.TODAY
                                }
                            )
                            Tab.SHABBOS -> ShabbosScreen(
                                settings = settings,
                                today = viewModel.today()
                            )
                            Tab.HOLIDAYS -> HolidaysScreen(
                                settings = settings,
                                today = viewModel.today()
                            )
                            Tab.SETTINGS -> SettingsScreen(
                                settings = settings,
                                onSettingsChange = { viewModel.updateSettings(it) },
                                onUseGps = { requestGpsLocation() },
                                onLocationChange = { viewModel.updateLocation(it) }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestGpsLocation() {
        if (LocationHelper.hasLocationPermission(this)) {
            applyGpsLocation()
        } else {
            locationPermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun applyGpsLocation() {
        val ll = LocationHelper.getLastKnownLatLng(this)
        if (ll == null) {
            Toast.makeText(this, "No last-known location available", Toast.LENGTH_SHORT).show()
            return
        }
        val current = viewModel.settings.value.location
        viewModel.updateLocation(
            Location(
                lat = ll.first,
                lng = ll.second,
                tz = LocationHelper.deviceTimeZoneId(),
                elevM = current.elevM,
                name = "GPS (%.4f, %.4f)".format(ll.first, ll.second)
            )
        )
        Toast.makeText(this, "Location updated", Toast.LENGTH_SHORT).show()
    }
}
