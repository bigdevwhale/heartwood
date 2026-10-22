package app.heartwood.ui.book

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.heartwood.container
import app.heartwood.data.Visit
import app.heartwood.domain.day
import app.heartwood.sense.Voices
import app.heartwood.sense.Waves
import app.heartwood.ui.Go
import app.heartwood.ui.Home
import app.heartwood.ui.T
import app.heartwood.ui.Tab
import app.heartwood.ui.TabBar
import app.heartwood.ui.components.Gap
import app.heartwood.ui.components.Sheet
import app.heartwood.ui.components.Stamp
import app.heartwood.ui.components.Tape
import app.heartwood.ui.components.rememberPhoto
import app.heartwood.ui.meet.LeafCard
import app.heartwood.ui.theme.S
import app.heartwood.ui.theme.SType
import app.heartwood.ui.theme.kraft

/** The forester's notebook: everything the trees gave back. */
@Composable
fun BookScreen(h: Home, go: Go) {
    val ru = h.ru
    var tab by rememberSaveable { mutableIntStateOf(0) }
    DisposableEffect(Unit) { onDispose { Voices.stop() } }
    Column(Modifier.fillMaxSize().kraft()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
            Gap(10)
            Text(if (ru) "Тетрадь лесника" else "The forester's notebook", style = SType.Display)
            Text(if (ru) "Всё, что деревья вам отдали." else "Everything the trees gave you.", style = SType.Muted)
            Gap(12)
            val tabs = listOf(if (ru) "Гербарий" else "Herbarium", if (ru) "Оттиски" else "Rubbings", if (ru) "Голоса" else "Voices", if (ru) "Тайники" else "Hollows")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tabs.forEachIndexed { i, name ->
                    val on = i == tab
                    Text(
                        name, style = SType.Typed.copy(color = if (on) S.c.paper else S.c.ink),
                        modifier = Modifier.clip(S.RSm).border(1.5.dp, S.c.ink, S.RSm).background(if (on) S.c.ink else S.c.kraft).clickable { tab = i }.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            Gap(16)
            when (tab) {
                0 -> Herbarium(h)
                1 -> Rubbings(h)
                2 -> VoicesList(h)
                else -> Hollows(h)
            }
            Gap(20)
        }
        TabBar(Tab.BOOK, go)
    }
}

@Composable
private fun EmptyPage(text: String) {
    Text(text, style = SType.Hand.copy(fontSize = 22.sp, color = S.c.muted), modifier = Modifier.padding(vertical = 30.dp))
}

@Composable
private fun Herbarium(h: Home) {
    val ru = h.ru
    if (h.leaves.isEmpty()) return EmptyPage(if (ru) "Пока пусто. Один упавший лист в сезон от каждого дерева — и за год соберётся гербарий." else "Empty for now. One fallen leaf a season from each tree, and in a year there's a herbarium.")
    for (row in h.leaves.chunked(2)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            for (v in row) {
                val t = h.tree(v.treeId) ?: continue
                val d = day(v.at)
                LeafCard(
                    t, v.file, d.monthValue,
                    "«${t.name}» · ${T.stamp(d, ru)}" + T.weather(v, ru).takeIf { it.isNotEmpty() }?.let { "\n$it" }.orEmpty(),
                    ((v.id * 37) % 40 - 20).toFloat(), Modifier.weight(1f),
                )
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
        Gap(16)
    }
}

@Composable
private fun Rubbings(h: Home) {
    val ru = h.ru
    if (h.rubbings.isEmpty()) return EmptyPage(if (ru) "Оттисков пока нет. У дерева: «Фроттаж» — сфотографируйте кору и потрите." else "No rubbings yet. At a tree: “Bark rubbing” — photograph the bark and rub.")
    for (row in h.rubbings.chunked(2)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            for (v in row) {
                val t = h.tree(v.treeId) ?: continue
                Box(Modifier.weight(1f)) {
                    Sheet(padding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
                        val img by rememberPhoto(v.file, 600)
                        Box(Modifier.fillMaxWidth().aspectRatio(1f).background(S.c.paper)) {
                            img?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                        }
                        Gap(4)
                        Text("«${t.name}» · ${T.stamp(day(v.at), ru)}", style = SType.Hand.copy(fontSize = 17.sp), modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                    Tape(Modifier.align(Alignment.TopCenter), width = 54.dp)
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
        Gap(14)
    }
}

@Composable
private fun VoicesList(h: Home) {
    val ru = h.ru
    val c = LocalContext.current.container
    if (h.voices.isEmpty()) return EmptyPage(if (ru) "Голосов пока нет. У дерева: «Послушать» — 10 секунд коры." else "No voices yet. At a tree: “Listen” — 10 seconds of bark.")
    Sheet(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
        h.voices.forEachIndexed { i, v ->
            if (i > 0) HorizontalDivider(color = S.c.line.copy(alpha = .6f))
            val t = h.tree(v.treeId) ?: return@forEachIndexed
            val key = "v${v.id}"
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).clip(CircleShape).border(1.5.dp, S.c.ink, CircleShape).clickable { Voices.toggle(c.file(v.file), key) },
                    contentAlignment = Alignment.Center,
                ) { Text(if (Voices.playing == key) "■" else "▶", style = SType.Body) }
                Spacer(Modifier.width(10.dp))
                Wave(v, Modifier.weight(1f).height(30.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.width(112.dp)) {
                    Text("«${t.name}»", style = SType.Title.copy(fontSize = 14.sp))
                    Text("${T.stamp(day(v.at), ru)}\n${T.weather(v, ru)}", style = SType.Typed.copy(fontSize = 11.sp, lineHeight = 14.sp))
                }
            }
        }
    }
}

@Composable
private fun Wave(v: Visit, modifier: Modifier) {
    val levels = Waves.decode(v.wave)
    val ink = S.c.ink
    Canvas(modifier) {
        if (levels.isEmpty()) return@Canvas
        val n = 48
        val step = size.width / n
        for (i in 0 until n) {
            val a = levels.slice((i * levels.size / n) until ((i + 1) * levels.size / n).coerceAtLeast(i * levels.size / n + 1).coerceAtMost(levels.size)).maxOrNull() ?: 0f
            val hh = (a * size.height * .48f).coerceAtLeast(1f)
            drawLine(ink, Offset(i * step + step / 2, size.height / 2 - hh), Offset(i * step + step / 2, size.height / 2 + hh), step * .55f, StrokeCap.Round)
        }
    }
}

@Composable
private fun Hollows(h: Home) {
    val ru = h.ru
    if (h.notes.isEmpty()) return EmptyPage(if (ru) "Тайников пока нет. У дерева: «Тайник в дупле» — записка себе на год вперёд." else "No hollows yet. At a tree: “The hollow” — a note to yourself a year ahead.")
    for (n in h.notes.sortedByDescending { it.leftAt }) {
        val t = h.tree(n.treeId) ?: continue
        Sheet(tape = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("«${t.name}» · ${if (ru) "оставлена" else "left"} ${T.stamp(n.leftAt, ru)}", style = SType.Typed, modifier = Modifier.weight(1f))
                if (n.openedAt == 0L) Stamp(if (ru) "запечатана" else "sealed", color = S.c.rust) else Stamp(if (ru) "открыта" else "opened", color = S.c.moss)
            }
            Gap(6)
            if (n.openedAt != 0L) Text(n.text, style = SType.Hand.copy(fontSize = 22.sp))
            else Text(if (ru) "Откроется ${T.stamp(n.openAt, ru)}, только у дерева." else "Opens on ${T.stamp(n.openAt, ru)}, only at the tree.", style = SType.Body)
        }
        Gap(16)
    }
}
