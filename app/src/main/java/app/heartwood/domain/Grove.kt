package app.heartwood.domain

import app.heartwood.data.Kind
import app.heartwood.data.Note
import app.heartwood.data.Tree
import app.heartwood.data.Visit
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

enum class Season {
    WINTER, SPRING, SUMMER, AUTUMN;

    companion object {
        fun of(month: Int) = when (month) {
            12, 1, 2 -> WINTER
            in 3..5 -> SPRING
            in 6..8 -> SUMMER
            else -> AUTUMN
        }

        fun of(d: LocalDate) = of(d.monthValue)
    }
}

fun day(at: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()

object Geo {
    /** How close counts as "at the tree", in metres. */
    const val NEAR = 50.0

    fun meters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).let { it * it } + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).let { it * it }
        return 2 * r * asin(sqrt(a))
    }

    /** At the tree, giving a fix the benefit of its doubt up to 40 m. */
    fun near(distance: Double, accuracy: Float): Boolean = distance - min(accuracy, 40f) <= NEAR
}

/** One ring of the cross-section: one day you met. [width] is how much happened, [absence] marks a long gap before it. */
class Ring(val day: LocalDate, val visits: List<Visit>, val width: Float, val absence: Boolean) {
    val season: Season = Season.of(day)
    val kinds: Set<Kind> = visits.map { it.kindOf }.toSet()
    val hug: Boolean = Kind.HUG in kinds
    val first: Boolean = Kind.MEET in kinds
    val weather: Visit? = visits.firstOrNull { it.sky.isNotEmpty() }
}

object Rings {
    /** A gap this long leaves a dark line in the wood, like a dry year. */
    const val ABSENCE_DAYS = 30

    private fun weight(k: Kind) = when (k) {
        Kind.MEET -> 0.4f
        Kind.HUG -> 0.7f
        Kind.SIT -> 0.5f
        Kind.LISTEN, Kind.LEAF, Kind.RUB, Kind.PHOTO -> 0.2f
        Kind.VISIT -> 0f
    }

    /** A tree's visits as rings, the first meeting innermost. */
    fun of(visits: List<Visit>, zone: ZoneId = ZoneId.systemDefault()): List<Ring> {
        val days = visits.sortedBy { it.at }.groupBy { day(it.at, zone) }
        var prev: LocalDate? = null
        return days.map { (d, vs) ->
            val gap = prev?.let { ChronoUnit.DAYS.between(it, d) > ABSENCE_DAYS } ?: false
            prev = d
            Ring(d, vs, 1f + vs.map { it.kindOf }.toSet().sumOf { weight(it).toDouble() }.toFloat(), gap)
        }
    }
}

enum class Level(val days: Int, val seasons: Int, val title: Words) {
    STRANGER(0, 0, w("Stranger", "Незнакомец")),
    ACQUAINTANCE(3, 0, w("Acquaintance", "Знакомый")),
    PAL(10, 2, w("Pal", "Приятель")),
    FRIEND(25, 3, w("Friend", "Друг")),
    KIN(50, 4, w("Kindred", "Побратим")),
}

/** Where a friendship stands and what the next step still needs. */
class Friendship(val level: Level, val next: Level?, val progress: Float, val needs: String?)

object Friend {
    fun of(rings: List<Ring>, today: LocalDate, ru: Boolean): Friendship {
        val days = rings.size
        val seasons = rings.map { it.season }.toSet()
        val since = rings.firstOrNull()?.day
        val years = since?.let { ChronoUnit.DAYS.between(it, today) } ?: 0
        fun reached(l: Level) = days >= l.days && seasons.size >= l.seasons && (l != Level.KIN || years >= 365)
        val level = Level.entries.last { reached(it) }
        val next = Level.entries.getOrNull(level.ordinal + 1) ?: return Friendship(level, null, 1f, null)
        val progress = ((days - level.days).toFloat() / (next.days - level.days)).coerceIn(0f, 1f)
        val needs = when {
            days < next.days -> {
                val n = next.days - days
                if (ru) "до «${next.title.ru}» — ещё ${Plural.ru(n, "встреча", "встречи", "встреч")}" else "$n more ${if (n == 1) "meeting" else "meetings"} to ${next.title.en}"
            }
            seasons.size < next.seasons -> {
                val missing = Season.entries.filter { it !in seasons }.joinToString(", ") { seasonName(it, ru) }
                if (ru) "для «${next.title.ru}» встретьтесь ещё в другое время года: $missing" else "to become ${next.title.en}, meet in another season too: $missing"
            }
            next == Level.KIN -> if (ru) "побратимами становятся после года дружбы" else "kindred takes a full year of friendship"
            else -> null
        }
        return Friendship(level, next, progress, needs)
    }

    fun seasonName(s: Season, ru: Boolean) = when (s) {
        Season.WINTER -> if (ru) "зима" else "winter"
        Season.SPRING -> if (ru) "весна" else "spring"
        Season.SUMMER -> if (ru) "лето" else "summer"
        Season.AUTUMN -> if (ru) "осень" else "autumn"
    }
}

object Plural {
    fun ru(n: Int, one: String, few: String, many: String): String {
        val m = n % 100
        val d = n % 10
        val word = if (m in 11..14) many else if (d == 1) one else if (d in 2..4) few else many
        return "$n $word"
    }
}

/** What the tree says about a day you met, in its own voice. */
object Memory {
    fun of(tree: Tree, ring: Ring, ru: Boolean): String {
        val sp = Species.of(tree.species)
        fun g(m: String, f: String) = if (sp.male) m else f
        val out = mutableListOf<String>()
        if (ring.first) out += if (ru) "День знакомства. Обхват — ${tree.girth} см. С этого кольца всё началось." else "The day we met. Girth ${tree.girth} cm. Everything started with this ring."
        ring.weather?.let { v -> weather(v, ring.season, sp, ru)?.let { out += it } }
        for (v in ring.visits) {
            out += when (v.kindOf) {
                Kind.HUG -> if (ru) "Обнимались ${v.seconds} секунд. Твоё сердце замедлилось до ${v.bpm} ударов — почти до моего." else "We hugged for ${v.seconds} seconds. Your heart slowed to ${v.bpm} beats — almost to mine."
                Kind.LISTEN -> if (ru) "Записали мой голос. Послушай его зимой." else "You recorded my voice. Listen to it in winter."
                Kind.SIT -> {
                    val m = (v.seconds / 60).coerceAtLeast(1)
                    if (ru) "Посидели рядом ${m} мин. Ничего не случилось — и это было хорошо." else "We sat together for $m min. Nothing happened, and that was good."
                }
                Kind.LEAF -> if (ru) "Мой лист теперь в гербарии. Я отдал${g("", "а")} лучший." else "My leaf is in the herbarium now. I gave you my best one."
                Kind.RUB -> if (ru) "Сняли оттиск моей коры. Теперь у тебя есть отпечаток моей кожи." else "You took a rubbing of my bark. Now you have a print of my skin."
                Kind.PHOTO -> if (ru) "Ещё кадр в мою хронику — с того же места." else "One more frame for my chronicle, from the same spot."
                else -> continue
            }
        }
        if (out.isEmpty()) out += if (ru) "Просто встреча. Без повода — самые лучшие." else "Just a meeting. The ones without a reason are the best."
        return out.distinct().joinToString(" ")
    }

    private fun weather(v: Visit, season: Season, sp: Species, ru: Boolean): String? {
        fun g(m: String, f: String) = if (sp.male) m else f
        val sky = Sky.of(v.sky) ?: return null
        val t = v.temp
        return when {
            sky == Sky.STORM -> if (ru) "Гремела гроза. Хорошо, что под кроной было сухо." else "There was thunder. Good thing it was dry under my crown."
            sky == Sky.SLEET -> if (ru) "Мокрый снег — а встреча всё равно состоялась." else "Sleet, and still we met."
            sky == Sky.SNOW -> if (ru) "Шёл снег. К стволу вела одна цепочка следов." else "It was snowing. One line of footprints led to my trunk."
            sky.rain -> if (ru) "Шёл дождь. Мы мокли вместе." else "It was raining. We got wet together."
            sky == Sky.FOG -> if (ru) "Был туман. Я видел${g("", "а")} только тебя." else "There was fog. I could only see you."
            t != Visit.NO_TEMP && t >= 25 -> if (ru) "Была жара. Моя тень пригодилась." else "It was hot. My shade came in handy."
            t != Visit.NO_TEMP && t <= -10 -> if (ru) "Мороз звенел в ветках." else "The frost rang in my branches."
            sky == Sky.CLEAR -> if (ru) "Было ясно. Свет шёл сквозь крону." else "It was clear. Light came through my crown."
            season == Season.AUTUMN && !sp.evergreen -> if (ru) "Серый день. Я ронял${g("", "а")} листья тебе на плечи." else "A grey day. I dropped leaves on your shoulders."
            else -> if (ru) "Тихий серый день." else "A quiet grey day."
        }
    }

    /** The tree's line today: its own, or a word after a long absence, or before the first meeting. */
    fun line(tree: Tree, rings: List<Ring>, today: LocalDate, ru: Boolean): String {
        val sp = Species.of(tree.species)
        val last = rings.lastOrNull()?.day
        return when {
            last == null -> if (ru) "Мы ещё не встречались. Я здесь." else "We haven't met yet. I'm here."
            ChronoUnit.DAYS.between(last, today) > Rings.ABSENCE_DAYS -> if (ru) (if (sp.male) "Я знал, что придёшь." else "Я знала, что придёшь.") else "I knew you'd come."
            else -> sp.lines[((today.toEpochDay() + tree.id) % sp.lines.size).toInt()](ru)
        }
    }
}

/** News along the roots: the friend met most recently tells the others about it. */
object Mycelium {
    class News(val from: Tree, val text: String)

    fun news(tree: Tree, trees: List<Tree>, lastMet: Map<Long, LocalDate>, today: LocalDate, ru: Boolean): News? {
        val mine = lastMet[tree.id]
        val from = trees.filter { it.id != tree.id }
            .mapNotNull { t -> lastMet[t.id]?.let { t to it } }
            .filter { (_, d) -> (mine == null || d.isAfter(mine)) && ChronoUnit.DAYS.between(d, today) <= 30 }
            .maxByOrNull { it.second } ?: return null
        val sp = Species.of(from.first.species)
        val line = sp.news[(from.second.toEpochDay() % sp.news.size).toInt()](ru)
        return News(from.first, if (ru) "«${from.first.name}» передаёт по грибнице: $line" else "“${from.first.name}” passes on through the roots: $line")
    }
}

object Hollow {
    const val YEAR = 365L * 86_400_000L

    fun sealed(notes: List<Note>, treeId: Long): Note? = notes.firstOrNull { it.treeId == treeId && it.openedAt == 0L }
    fun ripe(n: Note, now: Long) = n.openedAt == 0L && now >= n.openAt
}
