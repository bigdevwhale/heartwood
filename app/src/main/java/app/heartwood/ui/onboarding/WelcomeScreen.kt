package app.heartwood.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.heartwood.data.Kind
import app.heartwood.data.Visit
import app.heartwood.domain.Rings
import app.heartwood.domain.Species
import app.heartwood.ui.art.RingArt
import app.heartwood.ui.art.TreeArt
import app.heartwood.ui.components.Gap
import app.heartwood.ui.components.InkButton
import app.heartwood.ui.components.LineButton
import app.heartwood.ui.components.NativeCanvas
import app.heartwood.ui.components.Said
import app.heartwood.ui.components.Sheet
import app.heartwood.ui.components.ru
import app.heartwood.ui.theme.S
import app.heartwood.ui.theme.SType
import app.heartwood.ui.theme.kraft
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

private class PageText(val title: String, val body: String, val said: String)

/** Four pages: what a tree friendship is, the rings, what happens at the tree, the grove. */
@Composable
fun WelcomeScreen(again: Boolean = false, startPage: Int = 0, onDone: (plantNow: Boolean) -> Unit) {
    val ru = ru()
    val pages = listOf(
        PageText(
            if (ru) "Подружитесь с деревом" else "Befriend a tree",
            if (ru) "С настоящим — у дома, у школы, в парке. Сфотографируйте его, узнайте породу, обнимите и отметьте на карте." else "A real one — by your house, your school, in the park. Photograph it, find out what it is, hug it and mark it on the map.",
            if (ru) "Я никуда не уйду. Приходи." else "I'm not going anywhere. Come.",
        ),
        PageText(
            if (ru) "Каждая встреча — кольцо" else "Every meeting is a ring",
            if (ru) "Дружба растёт только от встреч у самого дерева, в пределах 50 метров. За год складывается спил — по нему видна вся история." else "The friendship grows only from meetings at the tree itself, within 50 metres. Over a year a cross-section builds up that tells the whole story.",
            if (ru) "Сердцевина — день, когда мы познакомились." else "The pith is the day we met.",
        ),
        PageText(
            if (ru) "Только рядом" else "Only up close",
            if (ru) "Обнять — телефон к груди, грудь к стволу. Послушать кору. Снять оттиск. Засушить лист. Снять по кальке для хроники. Посидеть. Оставить записку в дупле на год." else "Hug — phone to chest, chest to the trunk. Listen to the bark. Take a rubbing. Press a leaf. Photograph it over the last photo for the chronicle. Sit. Leave a note in the hollow for a year.",
            if (ru) "Пропадёшь — я не обижусь. Просто нарастёт мох." else "If you vanish I won't sulk. Moss will just grow.",
        ),
        PageText(
            if (ru) "Своя роща" else "Your own grove",
            if (ru) "До семи деревьев. Под землёй их связывает грибница: навестите одно — остальные узнают. Иногда деревья зовут: зацвела липа, лёг первый снег." else "Up to seven trees. Underground the mycelium links them: visit one and the others hear. Now and then a tree calls: the linden is in blossom, the first snow is down.",
            if (ru) "Липа зацвела. Приходи подышать." else "The linden's in blossom. Come and breathe.",
        ),
    )
    val pager = rememberPagerState(startPage) { pages.size }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().kraft().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
        Text("HEARTWOOD", style = SType.Stamp)
        HorizontalPager(pager, Modifier.weight(1f)) { i ->
            val p = pages[i]
            Column(Modifier.fillMaxSize().padding(top = 10.dp)) {
                Sheet(tape = true) {
                    Box(Modifier.fillMaxWidth().aspectRatio(1.1f)) { Picture(i) }
                }
                Gap(18)
                Text(p.title, style = SType.Display)
                Gap(6)
                Text(p.body, style = SType.Body)
                Gap(10)
                Said(p.said, size = 23)
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center) {
            for (i in pages.indices) Box(Modifier.padding(4.dp).size(8.dp).clip(CircleShape).background(if (i == pager.currentPage) S.c.ink else S.c.line))
        }
        if (pager.currentPage < pages.size - 1) {
            InkButton(if (ru) "Дальше" else "Next", { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }, Modifier.fillMaxWidth())
        } else if (again) {
            InkButton(if (ru) "Понятно" else "Got it", { onDone(false) }, Modifier.fillMaxWidth())
        } else {
            InkButton(if (ru) "Подружиться с деревом" else "Befriend a tree", { onDone(true) }, Modifier.fillMaxWidth(), moss = true)
            Gap(8)
            LineButton(if (ru) "Сначала осмотреться" else "Look around first", { onDone(false) }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Picture(page: Int) {
    val month = LocalDate.now().monthValue
    when (page) {
        0 -> NativeCanvas(Modifier.fillMaxSize()) { c, w, h -> TreeArt.draw(c, w, h, Species.OAK, month, 11) }
        1 -> {
            val rings = remember {
                val r = Random(4)
                val start = LocalDate.now().minusDays(400)
                val vs = (0 until 38).map { i ->
                    val d = start.plusDays((i * 10.5 + r.nextInt(5)).toLong())
                    val at = d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 3_600_000L * 12
                    Visit(treeId = 1, at = at, kind = (if (i == 0) Kind.MEET else listOf(Kind.VISIT, Kind.HUG, Kind.SIT, Kind.LISTEN)[r.nextInt(4)]).name)
                }
                Rings.of(vs)
            }
            NativeCanvas(Modifier.fillMaxSize().padding(10.dp)) { c, w, h ->
                val s = minOf(w, h)
                c.save()
                c.translate((w - s) / 2, (h - s) / 2)
                RingArt.draw(c, rings, RingArt.layout(rings, s, 11), null)
                c.restore()
            }
        }
        2 -> NativeCanvas(Modifier.fillMaxSize()) { c, w, h ->
            val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            val r = minOf(w, h) * .3f
            p.color = 0xFFE9DCC0.toInt(); c.drawCircle(w / 2, h / 2, r, p)
            p.color = 0xFFF1E7D1.toInt(); c.drawCircle(w / 2, h / 2, r * .78f, p)
            p.style = android.graphics.Paint.Style.STROKE
            p.strokeWidth = r * .03f; p.color = 0xFF2F2A24.toInt(); c.drawCircle(w / 2, h / 2, r, p)
            p.color = 0xFFA9533F.toInt(); p.strokeWidth = r * .06f
            c.drawArc(w / 2 - r, h / 2 - r, w / 2 + r, h / 2 + r, -90f, 250f, false, p)
            p.strokeWidth = r * .02f; p.alpha = 120
            c.drawCircle(w / 2, h / 2, r * 1.18f, p)
            p.alpha = 60
            c.drawCircle(w / 2, h / 2, r * 1.34f, p)
            val t = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2F2A24.toInt(); textSize = r * .55f; textAlign = android.graphics.Paint.Align.CENTER }
            c.drawText("31", w / 2, h / 2 + r * .2f, t)
        }
        else -> NativeCanvas(Modifier.fillMaxSize()) { c, w, h ->
            c.drawColor(0xFF231B14.toInt())
            val pts = listOf(.25f to .35f, .72f to .3f, .55f to .72f, .2f to .78f)
            val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.STROKE; color = 0x66E9D58A; strokeWidth = w * .004f; setShadowLayer(w * .015f, 0f, 0f, 0xFFE9D58A.toInt())
            }
            val r = Random(9)
            for (i in pts.indices) for (j in i + 1 until pts.size) repeat(4) {
                val a = pts[i]; val b = pts[j]
                val path = android.graphics.Path()
                path.moveTo(a.first * w, a.second * h)
                path.cubicTo((a.first + (b.first - a.first) * .33f + (r.nextFloat() - .5f) * .2f) * w, (a.second + (b.second - a.second) * .33f + (r.nextFloat() - .5f) * .2f) * h,
                    (a.first + (b.first - a.first) * .66f + (r.nextFloat() - .5f) * .2f) * w, (a.second + (b.second - a.second) * .66f + (r.nextFloat() - .5f) * .2f) * h, b.first * w, b.second * h)
                c.drawPath(path, p)
            }
            val g = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE9D58A.toInt(); setShadowLayer(w * .03f, 0f, 0f, 0xFFE9D58A.toInt()) }
            for ((x, y) in pts) c.drawCircle(x * w, y * h, w * .014f, g)
        }
    }
}
