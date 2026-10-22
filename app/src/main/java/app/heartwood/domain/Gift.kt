package app.heartwood.domain

import android.net.Uri
import app.heartwood.data.Tree
import java.util.Locale

/** A tree passed on to a friend: everything but your rings, as a link a phone camera can read from a QR code. */
data class TreeGift(val name: String, val species: Species, val girth: Int, val lat: Double, val lon: Double, val why: String, val from: String)

object Gift {
    const val SCHEME = "heartwood"

    fun link(t: Tree, from: String): String = Uri.Builder().scheme(SCHEME).authority("tree")
        .appendQueryParameter("n", t.name)
        .appendQueryParameter("s", t.species.lowercase(Locale.ROOT))
        .appendQueryParameter("g", t.girth.toString())
        .appendQueryParameter("la", "%.6f".format(Locale.US, t.lat))
        .appendQueryParameter("lo", "%.6f".format(Locale.US, t.lon))
        .apply { if (t.why.isNotBlank()) appendQueryParameter("w", t.why) }
        .apply { if (from.isNotBlank()) appendQueryParameter("f", from) }
        .build().toString()

    fun parse(text: String): TreeGift? {
        val start = text.indexOf("$SCHEME://tree")
        if (start < 0) return null
        val u = runCatching { Uri.parse(text.substring(start).trim().substringBefore(' ').substringBefore('\n')) }.getOrNull() ?: return null
        val name = u.getQueryParameter("n")?.trim()?.take(40)?.takeIf { it.isNotEmpty() } ?: return null
        val sp = Species.entries.firstOrNull { it.name.equals(u.getQueryParameter("s"), ignoreCase = true) } ?: return null
        val lat = u.getQueryParameter("la")?.toDoubleOrNull()?.takeIf { it in -90.0..90.0 } ?: return null
        val lon = u.getQueryParameter("lo")?.toDoubleOrNull()?.takeIf { it in -180.0..180.0 } ?: return null
        val girth = u.getQueryParameter("g")?.toIntOrNull()?.coerceIn(10, 2000) ?: 100
        return TreeGift(name, sp, girth, lat, lon, u.getQueryParameter("w").orEmpty().take(120), u.getQueryParameter("f").orEmpty().take(40))
    }
}
