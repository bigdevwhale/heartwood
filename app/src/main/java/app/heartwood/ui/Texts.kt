package app.heartwood.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import app.heartwood.data.Kind
import app.heartwood.data.Visit
import app.heartwood.domain.Plural
import app.heartwood.domain.Sky
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Dates, weather and counts in the app's language. */
object T {
    private fun locale(ru: Boolean) = if (ru) Locale("ru") else Locale.ENGLISH
    private fun zone() = ZoneId.systemDefault()

    fun date(d: LocalDate, ru: Boolean, today: LocalDate = LocalDate.now()): String =
        d.format(DateTimeFormatter.ofPattern(if (d.year == today.year) "d MMMM" else "d MMMM yyyy", locale(ru)))

    fun date(at: Long, ru: Boolean): String = date(Instant.ofEpochMilli(at).atZone(zone()).toLocalDate(), ru)

    /** "22 Oct 2026", always with the year: for herbarium labels and notes. */
    fun stamp(d: LocalDate, ru: Boolean): String = d.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale(ru))).replace(".", "")

    fun stamp(at: Long, ru: Boolean) = stamp(Instant.ofEpochMilli(at).atZone(zone()).toLocalDate(), ru)

    fun ago(d: LocalDate, ru: Boolean, today: LocalDate = LocalDate.now()): String {
        val days = ChronoUnit.DAYS.between(d, today).toInt()
        return when {
            days <= 0 -> if (ru) "сегодня" else "today"
            days == 1 -> if (ru) "вчера" else "yesterday"
            ru -> Plural.ru(days, "день", "дня", "дней") + " назад"
            else -> "$days days ago"
        }
    }

    fun sky(s: Sky, ru: Boolean) = when (s) {
        Sky.CLEAR -> if (ru) "ясно" else "clear"
        Sky.CLOUDY -> if (ru) "облачно" else "cloudy"
        Sky.OVERCAST -> if (ru) "пасмурно" else "overcast"
        Sky.FOG -> if (ru) "туман" else "fog"
        Sky.DRIZZLE -> if (ru) "морось" else "drizzle"
        Sky.RAIN -> if (ru) "дождь" else "rain"
        Sky.STORM -> if (ru) "гроза" else "thunder"
        Sky.SNOW -> if (ru) "снег" else "snow"
        Sky.SLEET -> if (ru) "мокрый снег" else "sleet"
    }

    /** "rain, +4°", or empty when the weather wasn't known. */
    fun weather(v: Visit?, ru: Boolean): String {
        if (v == null) return ""
        val s = Sky.of(v.sky)?.let { sky(it, ru) }
        val t = if (v.temp == Visit.NO_TEMP) null else (if (v.temp > 0) "+${v.temp}°" else "${v.temp}°")
        return listOfNotNull(s, t).joinToString(", ")
    }

    fun kind(k: Kind, ru: Boolean) = when (k) {
        Kind.MEET -> if (ru) "знакомство" else "first meeting"
        Kind.VISIT -> if (ru) "встреча" else "meeting"
        Kind.HUG -> if (ru) "объятие" else "hug"
        Kind.LISTEN -> if (ru) "голос" else "voice"
        Kind.SIT -> if (ru) "посидели" else "sat together"
        Kind.LEAF -> if (ru) "лист" else "leaf"
        Kind.RUB -> if (ru) "оттиск" else "rubbing"
        Kind.PHOTO -> if (ru) "снимок" else "photo"
    }

    fun meters(m: Double, ru: Boolean): String = when {
        m < 1000 -> "${m.toInt()} ${if (ru) "м" else "m"}"
        else -> "%.1f %s".format(locale(ru), m / 1000, if (ru) "км" else "km")
    }

    /** "about 83 years": in Russian «около» takes «лет», except after a final one («около 21 года»). */
    fun yearsAbout(n: Int, ru: Boolean) = if (!ru) years(n, false) else "$n ${if (n % 10 == 1 && n % 100 != 11) "года" else "лет"}"

    fun years(n: Int, ru: Boolean) = if (ru) Plural.ru(n, "год", "года", "лет") else "$n ${if (n == 1) "year" else "years"}"
}

object Language {
    const val SYSTEM = ""
    const val EN = "en"
    const val RU = "ru"

    fun current(): String = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')

    fun set(tag: String) {
        AppCompatDelegate.setApplicationLocales(if (tag == SYSTEM) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag))
    }
}
