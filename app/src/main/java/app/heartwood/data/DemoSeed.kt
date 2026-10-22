package app.heartwood.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import app.heartwood.AppContainer
import app.heartwood.domain.Season
import app.heartwood.domain.Sky
import app.heartwood.domain.Species
import app.heartwood.localized
import app.heartwood.ru
import app.heartwood.sense.Waves
import app.heartwood.ui.art.BarkArt
import app.heartwood.ui.art.TreeArt
import java.io.DataOutputStream
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * A grove a year and a half in, for the demo build and the screenshots, in Prague: an old oak met
 * forty-odd times (hugs, voices, rubbings, a leaf every season, a chronicle, a note ripe in its hollow
 * today), a chatty birch not visited for six weeks, a maple met all summer, and a linden from a friend.
 */
object DemoSeed {
    private const val DAY = 86_400_000L

    suspend fun seedIfEmpty(context: Context, c: AppContainer) {
        val dao = c.db.dao()
        if (dao.trees().isNotEmpty()) return
        val ru = context.localized().ru
        fun n(en: String, r: String) = if (ru) r else en
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()

        suspend fun tree(name: String, sp: Species, girth: Int, lat: Double, lon: Double, why: String, seed: Long, daysAgo: Int, from: String = ""): Tree {
            val t = Tree(name = name, species = sp.name, girth = girth, lat = lat, lon = lon, why = why, seed = seed, createdAt = now - daysAgo * DAY, from = from)
            return t.copy(id = dao.insert(t))
        }

        fun weather(d: LocalDate, r: Random): Pair<String, Int> {
            val (skies, lo, hi) = when (Season.of(d)) {
                Season.WINTER -> Triple(listOf(Sky.SNOW, Sky.OVERCAST, Sky.CLEAR, Sky.SNOW, Sky.FOG), -9, 2)
                Season.SPRING -> Triple(listOf(Sky.CLEAR, Sky.RAIN, Sky.CLOUDY, Sky.DRIZZLE, Sky.CLEAR), 4, 17)
                Season.SUMMER -> Triple(listOf(Sky.CLEAR, Sky.CLEAR, Sky.STORM, Sky.CLOUDY, Sky.RAIN), 17, 31)
                Season.AUTUMN -> Triple(listOf(Sky.OVERCAST, Sky.RAIN, Sky.FOG, Sky.CLEAR, Sky.CLOUDY), 2, 15)
            }
            return skies[r.nextInt(skies.size)].name to (lo + r.nextInt(hi - lo + 1))
        }

        /** Meeting days between two dates, with what happened on each. */
        suspend fun meet(t: Tree, from: LocalDate, to: LocalDate, count: Int, seed: Int, extra: (Int, LocalDate) -> List<Kind> = { _, _ -> emptyList() }) {
            val r = Random(seed)
            val span = java.time.temporal.ChronoUnit.DAYS.between(from, to).toInt().coerceAtLeast(1)
            val days = (listOf(0) + (1 until count - 1).map { 1 + r.nextInt(span - 1) } + listOf(span)).distinct().sorted().map { from.plusDays(it.toLong()) }
            days.forEachIndexed { i, d ->
                val (sky, temp) = weather(d, r)
                val at = d.atStartOfDay(zone).toInstant().toEpochMilli() + (8 + r.nextInt(11)) * 3_600_000L + r.nextInt(60) * 60_000L
                val kinds = mutableListOf(
                    if (i == 0) Kind.MEET else listOf(Kind.VISIT, Kind.VISIT, Kind.HUG, Kind.HUG, Kind.SIT, Kind.VISIT, Kind.HUG)[r.nextInt(7)],
                )
                kinds += extra(i, d)
                kinds.distinct().forEachIndexed { k, kind ->
                    var v = Visit(treeId = t.id, at = at + k * 240_000L, kind = kind.name, sky = sky, temp = temp)
                    v = when (kind) {
                        Kind.HUG -> v.copy(seconds = 60 + r.nextInt(40), bpm = 24)
                        Kind.SIT -> v.copy(seconds = (6 + r.nextInt(20)) * 60)
                        Kind.LISTEN -> {
                            val rel = c.newFile("voices", "wav")
                            v.copy(file = rel, wave = wind(c.file(rel), r.nextLong()))
                        }
                        Kind.RUB -> {
                            val rel = c.newFile("rubbings", "png")
                            c.file(rel).outputStream().use { BarkArt.rubbing(Species.of(t.species).bark, 700, r.nextLong()).compress(Bitmap.CompressFormat.PNG, 100, it) }
                            v.copy(file = rel)
                        }
                        Kind.PHOTO -> {
                            val rel = c.newFile("photos", "jpg")
                            picture(c.file(rel), Species.of(t.species), d.monthValue, t.seed, r)
                            v.copy(file = rel, align = "%.4f,%.4f,%.4f,%.2f".format(java.util.Locale.US, 1f, 0f, 0f, 0f))
                        }
                        else -> v
                    }
                    dao.insert(v)
                }
            }
        }

        /** One leaf a season: on the first meeting day that falls in the middle of one. */
        fun leafDays(): (Int, LocalDate) -> Boolean {
            val pressed = HashSet<String>()
            return { _, d ->
                val key = "${d.year}-${Season.of(d)}"
                Season.of(d) != Season.WINTER && d.dayOfMonth in 5..28 && d.monthValue % 3 != 0 && pressed.add(key)
            }
        }

        // The old oak by the Stromovka pond.
        val oak = tree(n("Old Man", "Старик"), Species.OAK, 310, 50.10598, 14.42155, n("I read under it as a child", "под ним я читал в детстве"), 11, 538)
        val oakLeaf = leafDays()
        val photoEvery = 7
        meet(oak, today.minusDays(538), today.minusDays(3), 46, 1) { i, d ->
            buildList {
                if (oakLeaf(i, d)) add(Kind.LEAF)
                if (i in listOf(5, 17, 29, 41)) add(Kind.LISTEN)
                if (i in listOf(9, 33)) add(Kind.RUB)
                if (i % photoEvery == 3) add(Kind.PHOTO)
            }
        }
        dao.insert(Note(treeId = oak.id, leftAt = now - 365 * DAY, openAt = now - 2 * 3_600_000L, text = n(
            "Hello from last October. If you're reading this, you still come. Tell him there are a lot of acorns this year.",
            "Привет из прошлого октября. Если ты это читаешь — значит, ты всё ещё приходишь. Скажи ему, что жёлудей в этом году много.",
        )))
        // The first photo of the chronicle.
        val first = c.newFile("photos", "jpg")
        picture(c.file(first), Species.OAK, today.minusDays(538).monthValue, oak.seed, Random(2))
        dao.update(oak.copy(photo = first))

        // The birch at the tram stop: chatty, and not visited for six weeks.
        val birch = tree(n("Chatterbox", "Болтушка"), Species.BIRCH, 118, 50.09872, 14.42770, n("by the tram stop, always rustling", "у остановки, всегда шелестит"), 23, 440)
        val birchLeaf = leafDays()
        meet(birch, today.minusDays(440), today.minusDays(41), 18, 2) { i, d ->
            buildList {
                if (birchLeaf(i, d)) add(Kind.LEAF)
                if (i == 8) add(Kind.LISTEN)
                if (i == 12) add(Kind.RUB)
            }
        }
        dao.insert(Note(treeId = birch.id, leftAt = now - 120 * DAY, openAt = now + 245 * DAY, text = n("Did the magpie stay?", "Сорока осталась?")))

        // The maple in the courtyard, met all summer.
        val maple = tree(n("Ember", "Огонёк"), Species.MAPLE, 165, 50.10182, 14.43010, n("the reddest in the yard", "самый красный во дворе"), 37, 130)
        val mapleLeaf = leafDays()
        meet(maple, today.minusDays(130), today.minusDays(6), 11, 3) { i, d -> buildList { if (mapleLeaf(i, d) || i == 9) add(Kind.LEAF) } }

        // A linden passed on by a friend, met twice.
        val linden = tree(n("Honey", "Медуница"), Species.LINDEN, 240, 50.10390, 14.42460, n("Lena's linden: smells of honey in July", "Ленина липа: в июле пахнет мёдом"), 53, 95, from = n("Lena", "Лена"))
        meet(linden, today.minusDays(92), today.minusDays(20), 3, 4)

        c.settings.update { it.copy(onboarded = true) }
    }

    /** A painted stand-in for a photo: sky, grass and the tree, for the demo chronicle. */
    private fun picture(f: File, sp: Species, month: Int, seed: Long, r: Random) {
        val w = 900
        val h = 1200
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val season = Season.of(month)
        val sky = when (season) {
            Season.WINTER -> intArrayOf(0xFFC9D3DC.toInt(), 0xFFE9ECEE.toInt())
            Season.AUTUMN -> intArrayOf(0xFFB9C6CF.toInt(), 0xFFE8E1D2.toInt())
            else -> intArrayOf(0xFF9CC3E0.toInt(), 0xFFE4EEF2.toInt())
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, 0f, h * .85f, sky[0], sky[1], Shader.TileMode.CLAMP) })
        val ground = when (season) {
            Season.WINTER -> 0xFFEEF0F0.toInt()
            Season.AUTUMN -> 0xFF9C9A62.toInt()
            Season.SPRING -> 0xFF9DBB6C.toInt()
            Season.SUMMER -> 0xFF7FA457.toInt()
        }
        c.drawRect(0f, h * .86f, w.toFloat(), h.toFloat(), Paint().apply { color = ground })
        TreeArt.draw(c, w.toFloat(), h * .96f, sp, month, seed, dx = (r.nextFloat() - .5f) * 30, dy = (r.nextFloat() - .5f) * 16)
        f.parentFile?.mkdirs()
        f.outputStream().use { b.compress(Bitmap.CompressFormat.JPEG, 86, it) }
    }

    /** Six seconds of wind in a crown as a WAV, and its loudness for the waveform. */
    private fun wind(f: File, seed: Long): String {
        val rate = 16_000
        val n = rate * 6
        val r = Random(seed)
        val pcm = ShortArray(n)
        var brown = 0.0
        var gust = .5
        var target = .5
        val levels = ArrayList<Int>()
        var peak = 0
        for (i in 0 until n) {
            if (i % (rate / 2) == 0) target = .2 + r.nextDouble() * .8
            gust += (target - gust) * .0004
            brown = (brown + .02 * (r.nextDouble() * 2 - 1)) / 1.02
            val rustle = (r.nextDouble() * 2 - 1) * (if (r.nextDouble() < .3) .08 else .02) * gust * gust
            val v = ((brown * 3.5 + rustle) * gust * (1 + .2 * sin(i / 900.0))).coerceIn(-1.0, 1.0)
            pcm[i] = (v * 22_000).toInt().toShort()
            peak = maxOf(peak, abs(pcm[i].toInt()))
            if (i % (rate / 20) == rate / 20 - 1) {
                levels += (kotlin.math.sqrt(peak / 32767.0) * 255).toInt().coerceIn(0, 255)
                peak = 0
            }
        }
        f.parentFile?.mkdirs()
        DataOutputStream(f.outputStream().buffered()).use { o ->
            fun le32(x: Int) { o.write(x and 255); o.write(x shr 8 and 255); o.write(x shr 16 and 255); o.write(x shr 24 and 255) }
            fun le16(x: Int) { o.write(x and 255); o.write(x shr 8 and 255) }
            o.writeBytes("RIFF"); le32(36 + n * 2); o.writeBytes("WAVEfmt "); le32(16); le16(1); le16(1); le32(rate); le32(rate * 2); le16(2); le16(16)
            o.writeBytes("data"); le32(n * 2)
            for (s in pcm) le16(s.toInt())
        }
        return Waves.encode(levels)
    }
}
