package app.heartwood.domain

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** The weather at a meeting, as the tree remembers it. */
enum class Sky {
    CLEAR, CLOUDY, OVERCAST, FOG, DRIZZLE, RAIN, STORM, SNOW, SLEET;

    val rain get() = this == DRIZZLE || this == RAIN || this == STORM || this == SLEET
    val snow get() = this == SNOW || this == SLEET

    companion object {
        /** The WMO weather code Open-Meteo reports. */
        fun wmo(code: Int): Sky = when (code) {
            0, 1 -> CLEAR
            2 -> CLOUDY
            3 -> OVERCAST
            45, 48 -> FOG
            51, 53, 55, 56, 57 -> DRIZZLE
            61, 63, 65, 80, 81, 82 -> RAIN
            66, 67 -> SLEET
            71, 73, 75, 77, 85, 86 -> SNOW
            95, 96, 99 -> STORM
            else -> CLOUDY
        }

        fun of(s: String?) = entries.firstOrNull { it.name == s }
    }
}

data class WeatherNow(val sky: Sky, val temp: Double)

object Weather {
    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 6000
        c.readTimeout = 8000
        c.setRequestProperty("User-Agent", "Heartwood/1.0 github.com/bigdevwhale/heartwood")
        try {
            if (c.responseCode !in 200..299) error("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    /**
     * The weather at the tree now: from Open-Meteo, or from MET Norway when Open-Meteo can't be
     * reached (some networks block it). No key; nothing but the tree's rounded coordinates is sent.
     */
    fun fetch(lat: Double, lon: Double): WeatherNow =
        runCatching { openMeteo(lat, lon) }.getOrElse { metNo(lat, lon) }

    private fun openMeteo(lat: Double, lon: Double): WeatherNow {
        val j = JSONObject(get("https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f&current=temperature_2m,weather_code".format(Locale.US, lat, lon)))
        val cur = j.getJSONObject("current")
        return WeatherNow(Sky.wmo(cur.getInt("weather_code")), cur.getDouble("temperature_2m"))
    }

    private fun metNo(lat: Double, lon: Double): WeatherNow {
        val j = JSONObject(get("https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.3f&lon=%.3f".format(Locale.US, lat, lon)))
        val first = j.getJSONObject("properties").getJSONArray("timeseries").getJSONObject(0).getJSONObject("data")
        val d = first.getJSONObject("instant").getJSONObject("details")
        val symbol = first.optJSONObject("next_1_hours")?.optJSONObject("summary")?.optString("symbol_code")
            ?: first.optJSONObject("next_6_hours")?.optJSONObject("summary")?.optString("symbol_code") ?: "cloudy"
        return WeatherNow(symbol(symbol), d.getDouble("air_temperature"))
    }

    /** MET Norway's symbol codes ("lightrainshowers_day", "fog"…) as skies. */
    fun symbol(code: String): Sky {
        val c = code.substringBefore('_')
        return when {
            "thunder" in c -> Sky.STORM
            "sleet" in c -> Sky.SLEET
            "snow" in c -> Sky.SNOW
            c.startsWith("light") && "rain" in c -> Sky.DRIZZLE
            "rain" in c -> Sky.RAIN
            c == "fog" -> Sky.FOG
            c == "cloudy" -> Sky.OVERCAST
            c == "partlycloudy" -> Sky.CLOUDY
            else -> Sky.CLEAR
        }
    }
}
