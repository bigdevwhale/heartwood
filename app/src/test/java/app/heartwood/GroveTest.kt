package app.heartwood

import app.heartwood.data.Kind
import app.heartwood.data.Note
import app.heartwood.data.Tree
import app.heartwood.data.Visit
import app.heartwood.domain.Calls
import app.heartwood.domain.Friend
import app.heartwood.domain.Geo
import app.heartwood.domain.Gift
import app.heartwood.domain.Level
import app.heartwood.domain.Memory
import app.heartwood.domain.Rings
import app.heartwood.domain.Species
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GroveTest {
    private val zone = ZoneOffset.UTC
    private fun at(d: LocalDate, hour: Int = 12) = d.atStartOfDay(zone).toInstant().toEpochMilli() + hour * 3_600_000L
    private val oak = Tree(id = 1, name = "Old Man", species = Species.OAK.name, girth = 310, lat = 50.1, lon = 14.4, seed = 11)

    @Test fun oneRingPerDay() {
        val d = LocalDate.of(2026, 5, 2)
        val vs = listOf(
            Visit(treeId = 1, at = at(d, 9), kind = Kind.MEET.name),
            Visit(treeId = 1, at = at(d, 10), kind = Kind.HUG.name, seconds = 60, bpm = 24),
            Visit(treeId = 1, at = at(d.plusDays(1)), kind = Kind.VISIT.name),
            Visit(treeId = 1, at = at(d.plusDays(60)), kind = Kind.SIT.name, seconds = 600),
        )
        val rings = Rings.of(vs, zone)
        assertEquals(3, rings.size)
        assertTrue(rings[0].first && rings[0].hug)
        assertTrue("a hug day is wider than a plain one", rings[0].width > rings[1].width)
        assertFalse(rings[1].absence)
        assertTrue("two months away leave a dark line", rings[2].absence)
    }

    @Test fun friendshipNeedsSeasonsAndAYear() {
        val start = LocalDate.of(2025, 6, 1)
        // Sixty meetings, all in one summer: lots of days, one season.
        val summer = (0 until 60).map { Visit(treeId = 1, at = at(start.plusDays(it.toLong())), kind = Kind.VISIT.name) }
        val f = Friend.of(Rings.of(summer, zone), start.plusDays(400), ru = false)
        assertEquals(Level.ACQUAINTANCE, f.level)
        assertNotNull(f.needs)
        // Every week for a year and a half: kindred.
        val year = (0 until 78).map { Visit(treeId = 1, at = at(start.plusDays(it * 7L)), kind = Kind.VISIT.name) }
        assertEquals(Level.KIN, Friend.of(Rings.of(year, zone), start.plusDays(560), ru = true).level)
    }

    @Test fun nearMeansFiftyMetres() {
        val d = Geo.meters(50.1, 14.4, 50.1004, 14.4)
        assertTrue(d in 40.0..50.0)
        assertTrue(Geo.near(d, 5f))
        assertFalse(Geo.near(Geo.meters(50.1, 14.4, 50.102, 14.4), 10f))
        // A poor fix doesn't buy more than 40 m of doubt.
        assertFalse(Geo.near(120.0, 500f))
    }

    @Test fun ageFromGirth() {
        assertEquals(194, Species.OAK.age(310))
        assertTrue(Species.POPLAR.age(310) < Species.OAK.age(310))
    }

    @Test fun giftRoundTrip() {
        val link = Gift.link(oak.copy(why = "под ним я читал"), "Лена")
        val g = Gift.parse("Дружи с этим деревом: $link")
        assertNotNull(g)
        assertEquals("Old Man", g!!.name)
        assertEquals(Species.OAK, g.species)
        assertEquals(310, g.girth)
        assertEquals(50.1, g.lat, 1e-6)
        assertEquals("под ним я читал", g.why)
        assertEquals("Лена", g.from)
        assertNull(Gift.parse("heartwood://tree?n=x&s=banana&la=1&lo=1"))
    }

    @Test fun callsAreRareAndOncePerYear() {
        val today = LocalDate.of(2026, 10, 22)
        val now = at(today)
        val maple = Tree(id = 2, name = "Ember", species = Species.MAPLE.name, girth = 160, lat = 0.0, lon = 0.0)
        val first = Calls.pick(listOf(maple), emptyMap(), emptyList(), emptySet(), "", today, now, false)
        assertNotNull(first)
        assertEquals("red", first!!.key)
        val log = Calls.append("", Calls.id(maple, first.key, Calls.year(first, today)), now)
        // A week apart at least…
        assertNull(Calls.pick(listOf(maple), emptyMap(), emptyList(), emptySet(), log, today.plusDays(3), now + 3 * 86_400_000L, false))
        // …and the same reason not twice in a year.
        assertNull(Calls.pick(listOf(maple), emptyMap(), emptyList(), emptySet(), log, today.plusDays(9), now + 9 * 86_400_000L, false))
        // A ripe note in the hollow comes first.
        val note = Note(id = 5, treeId = 2, leftAt = now - 400 * 86_400_000L, openAt = now - 1000, text = "hi")
        assertEquals("note5", Calls.pick(listOf(maple), emptyMap(), listOf(note), emptySet(), "", today, now, true)!!.key)
        // Two calls in thirty days is the most.
        val two = Calls.append(Calls.append("", "9:x:2026", now - 20 * 86_400_000L), "9:y:2026", now - 10 * 86_400_000L)
        assertNull(Calls.pick(listOf(maple), emptyMap(), listOf(note), emptySet(), two, today, now, false))
    }

    @Test fun memoryInBothLanguages() {
        val d = LocalDate.of(2026, 1, 10)
        val rings = Rings.of(listOf(Visit(treeId = 1, at = at(d), kind = Kind.MEET.name, sky = "SNOW", temp = -4), Visit(treeId = 1, at = at(d, 13), kind = Kind.HUG.name, seconds = 61, bpm = 24)), zone)
        val en = Memory.of(oak, rings[0], false)
        val ru = Memory.of(oak, rings[0], true)
        assertTrue(en.contains("310") && en.contains("snowing") && en.contains("61 seconds"))
        assertTrue(ru.contains("Шёл снег") && ru.contains("61 секунд"))
    }

    @Test fun everySpeciesIsComplete() {
        for (sp in Species.entries) {
            assertTrue(sp.name, sp.lines.size >= 4 && sp.news.size >= 3 && sp.events.isNotEmpty() && sp.autumn.isNotEmpty())
            assertTrue(sp.name, sp.lines.all { it.en.isNotBlank() && it.ru.isNotBlank() })
            assertTrue(sp.name, sp.events.all { it.months.first in 1..12 && it.months.last in 1..12 })
        }
    }
}
