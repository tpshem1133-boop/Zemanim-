package com.zemanim.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.zemanim.app.data.AppSettings
import com.zemanim.app.data.SettingsStore
import com.zemanim.app.engine.CivilDate
import com.zemanim.app.engine.DayInfo
import com.zemanim.app.engine.HolidayEvent
import com.zemanim.app.engine.Location
import com.zemanim.app.engine.ZemanimEngine
import com.zemanim.app.engine.ZmanimOpts
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One calendar day that carries holiday/fast/minor events (for the Holidays tab). */
data class HolidayDay(val abs: Long, val events: List<HolidayEvent>)

/**
 * Holds settings + the selected civil date. DayInfo is computed on demand
 * from (settings, date) so every setting change recomputes instantly.
 */
class ZemanimViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SettingsStore(application)

    private val _settings = MutableStateFlow(store.load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _selectedDate = MutableStateFlow(todayInTz(_settings.value.location.tz))
    val selectedDate: StateFlow<CivilDate> = _selectedDate.asStateFlow()

    /** Today in the location timezone. */
    fun todayInTz(tz: String): CivilDate {
        val now = ZonedDateTime.now(ZoneId.of(tz))
        return CivilDate(now.year, now.monthValue, now.dayOfMonth)
    }

    fun today(): CivilDate = todayInTz(_settings.value.location.tz)

    fun dayInfo(date: CivilDate): DayInfo {
        val s = _settings.value
        return ZemanimEngine.getDayInfo(
            date.y, date.m, date.d, s.location,
            ZmanimOpts(havdalah = s.havdalah, candleMin = s.candleMin, useElevation = false),
            s.il
        )
    }

    fun selectDate(d: CivilDate) {
        _selectedDate.value = d
    }

    fun stepDay(n: Long) {
        val cur = _selectedDate.value
        val abs = ZemanimEngine.gregToAbs(cur.y, cur.m, cur.d) + n
        _selectedDate.value = ZemanimEngine.absToGreg(abs)
    }

    fun goToday() {
        _selectedDate.value = today()
    }

    fun updateSettings(s: AppSettings) {
        store.save(s)
        _settings.value = s
    }

    /** Switch location (GPS / city / manual): persist and reset to today in the new tz. */
    fun updateLocation(loc: Location) {
        updateSettings(_settings.value.copy(location = loc))
        _selectedDate.value = todayInTz(loc.tz)
    }

    /** Next Friday + Saturday on/after [from] (civil dates). */
    fun nextFridaySaturday(from: CivilDate): Pair<CivilDate, CivilDate> {
        var fri = ZemanimEngine.gregToAbs(from.y, from.m, from.d)
        while (ZemanimEngine.absWeekday(fri) != 5) fri++
        return Pair(ZemanimEngine.absToGreg(fri), ZemanimEngine.absToGreg(fri + 1))
    }

    /** Upcoming holiday/fast days for the next [days] days, capped at 80 entries. */
    fun upcomingHolidayDays(fromAbs: Long, days: Long = 370): List<HolidayDay> {
        val il = _settings.value.il
        val out = ArrayList<HolidayDay>()
        var a = fromAbs
        val end = fromAbs + days
        while (a < end && out.size < 80) {
            val evs = ZemanimEngine.eventsForAbs(a, il, true).filter {
                it.type == "yt" || it.type == "fast" || it.type == "fast-yt" || it.type == "minor"
            }
            if (evs.isNotEmpty()) out.add(HolidayDay(a, evs))
            a++
        }
        return out
    }
}
