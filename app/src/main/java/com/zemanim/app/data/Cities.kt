package com.zemanim.app.data

import com.zemanim.app.engine.Location

/** City presets, mirroring the web app's list (name, lat, lng, timezone, elevation m). */
val CITY_LIST: List<Location> = listOf(
    Location(31.7683, 35.2137, "Asia/Jerusalem", 750.0, "Jerusalem, Israel"),
    Location(32.0853, 34.7818, "Asia/Jerusalem", 10.0, "Tel Aviv, Israel"),
    Location(32.8191, 34.9995, "Asia/Jerusalem", 50.0, "Haifa, Israel"),
    Location(40.7128, -74.006, "America/New_York", 10.0, "New York, USA"),
    Location(40.0821, -74.2097, "America/New_York", 20.0, "Lakewood, NJ"),
    Location(39.2904, -76.6122, "America/New_York", 30.0, "Baltimore, MD"),
    Location(25.7617, -80.1918, "America/New_York", 2.0, "Miami, FL"),
    Location(41.8781, -87.6298, "America/Chicago", 180.0, "Chicago, IL"),
    Location(34.0522, -118.2437, "America/Los_Angeles", 90.0, "Los Angeles, CA"),
    Location(43.6532, -79.3832, "America/Toronto", 76.0, "Toronto, Canada"),
    Location(45.5017, -73.5673, "America/Toronto", 30.0, "Montreal, Canada"),
    Location(51.5074, -0.1278, "Europe/London", 35.0, "London, UK"),
    Location(53.4808, -2.2426, "Europe/London", 50.0, "Manchester, UK"),
    Location(48.8566, 2.3522, "Europe/Paris", 35.0, "Paris, France"),
    Location(51.2194, 4.4025, "Europe/Brussels", 10.0, "Antwerp, Belgium"),
    Location(52.52, 13.405, "Europe/Berlin", 34.0, "Berlin, Germany"),
    Location(55.7558, 37.6173, "Europe/Moscow", 150.0, "Moscow, Russia"),
    Location(-26.2041, 28.0473, "Africa/Johannesburg", 1750.0, "Johannesburg, South Africa"),
    Location(-23.5558, -46.6396, "America/Sao_Paulo", 760.0, "São Paulo, Brazil"),
    Location(-34.6037, -58.3816, "America/Argentina/Buenos_Aires", 25.0, "Buenos Aires, Argentina"),
    Location(-33.8688, 151.2093, "Australia/Sydney", 40.0, "Sydney, Australia"),
    Location(-37.8136, 144.9631, "Australia/Melbourne", 30.0, "Melbourne, Australia")
)

/** Timezones offered for manual location entry. */
val TIMEZONE_LIST: List<String> = listOf(
    "America/New_York", "America/Chicago", "America/Denver", "America/Los_Angeles",
    "America/Anchorage", "Pacific/Honolulu", "America/Toronto", "America/Sao_Paulo",
    "America/Argentina/Buenos_Aires", "Atlantic/Azores", "Europe/London", "Europe/Paris",
    "Europe/Brussels", "Europe/Berlin", "Europe/Moscow", "Asia/Jerusalem", "Asia/Dubai",
    "Asia/Kolkata", "Asia/Singapore", "Asia/Tokyo", "Australia/Sydney",
    "Australia/Melbourne", "Africa/Johannesburg", "Africa/Cairo", "Pacific/Auckland", "UTC"
)
