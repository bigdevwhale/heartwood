package app.heartwood.domain

import app.heartwood.data.Note
import app.heartwood.data.Tree
import java.time.LocalDate

/** A tree calling you over: "I'm in blossom, come". */
class Call(val tree: Tree, val key: String, val text: String)

/**
 * When trees call. Rarely, and only with a reason: a note in the hollow is ready, a year since you
 * met, the first snow, or the tree's own moment of the year (blossom, sap, red leaves…).
 * No more than [PER_MONTH] in thirty days, a week apart, and each reason once a year.
 */
object Calls {
    const val PER_MONTH = 2
    private const val DAY = 86_400_000L

    /** A logged call: "treeId:key:year@millis". */
    class Logged(val id: String, val at: Long)

    fun parse(log: String): List<Logged> = log.split(';').mapNotNull { e ->
        val i = e.lastIndexOf('@')
        if (i <= 0) null else e.substring(i + 1).toLongOrNull()?.let { Logged(e.substring(0, i), it) }
    }

    fun append(log: String, id: String, at: Long): String =
        (parse(log).takeLast(60) + Logged(id, at)).joinToString(";") { "${it.id}@${it.at}" }

    fun id(tree: Tree, key: String, year: Int) = "${tree.id}:$key:$year"

    /**
     * The one call to make today, if any. [firstMet] is each tree's first meeting; [snowAt] is
     * the trees where it is snowing now (fetched weather), for the first snow of the winter.
     */
    fun pick(
        trees: List<Tree>,
        firstMet: Map<Long, LocalDate>,
        notes: List<Note>,
        snowAt: Set<Long>,
        log: String,
        today: LocalDate,
        now: Long,
        ru: Boolean,
    ): Call? {
        val logged = parse(log)
        if (logged.any { now - it.at < 7 * DAY }) return null
        if (logged.count { now - it.at < 30 * DAY } >= PER_MONTH) return null
        val done = logged.map { it.id }.toSet()
        // The winter that a December or a March belongs to, so one first snow per winter.
        val winter = if (today.monthValue >= 7) today.year else today.year - 1
        val candidates = mutableListOf<Call>()

        for (n in notes) {
            val t = trees.firstOrNull { it.id == n.treeId } ?: continue
            if (Hollow.ripe(n, now) && id(t, "note${n.id}", 0) !in done) {
                candidates += Call(t, "note${n.id}", if (ru) "В моём дупле ждёт записка. Сегодня её можно открыть — но только здесь." else "A note is waiting in my hollow. Today it can be opened — but only here.")
            }
        }
        for (t in trees) {
            val met = firstMet[t.id] ?: continue
            if (met.year < today.year && met.monthValue == today.monthValue && met.dayOfMonth == today.dayOfMonth && id(t, "anniversary", today.year) !in done) {
                val years = today.year - met.year
                candidates += Call(
                    t, "anniversary",
                    if (ru) (if (years == 1) "Год назад в этот день мы познакомились. Приходи — покажу, как выросли кольца." else "Сегодня ${Plural.ru(years, "год", "года", "лет")}, как мы знакомы. Приходи.")
                    else (if (years == 1) "A year ago today we met. Come — I'll show you how the rings have grown." else "$years years since we met, today. Come."),
                )
            }
        }
        for (t in trees.filter { it.id in snowAt }.take(1)) {
            if (id(t, "snow", winter) !in done && today.monthValue in listOf(10, 11, 12, 1, 2, 3, 4)) {
                candidates += Call(t, "snow", if (ru) "Первый снег. Приходи посмотреть на меня в снегу." else "First snow. Come and see me in the snow.")
            }
        }
        // A tree's own moment, from the tenth of the month so it isn't the 1st for everyone.
        if (today.dayOfMonth >= 10) {
            val turn = (today.toEpochDay() % (trees.size.coerceAtLeast(1))).toInt()
            for (t in trees.drop(turn) + trees.take(turn)) {
                val sp = Species.of(t.species)
                for (e in sp.events) {
                    if (today.monthValue in e.months && id(t, e.key, today.year) !in done) candidates += Call(t, e.key, e.call(ru))
                }
            }
        }
        return candidates.firstOrNull()
    }

    /** The year a call is filed under. */
    fun year(call: Call, today: LocalDate) = when {
        call.key.startsWith("note") -> 0
        call.key == "snow" -> if (today.monthValue >= 7) today.year else today.year - 1
        else -> today.year
    }
}
