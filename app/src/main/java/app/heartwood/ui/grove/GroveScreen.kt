package app.heartwood.ui.grove

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.heartwood.container
import app.heartwood.data.Tree
import app.heartwood.domain.Rings
import app.heartwood.ui.Go
import app.heartwood.ui.Home
import app.heartwood.ui.T
import app.heartwood.ui.Tab
import app.heartwood.ui.TabBar
import app.heartwood.ui.art.TreeArt
import app.heartwood.ui.components.Gap
import app.heartwood.ui.components.InkButton
import app.heartwood.ui.components.Label
import app.heartwood.ui.components.LineButton
import app.heartwood.ui.components.NativeCanvas
import app.heartwood.ui.components.Said
import app.heartwood.ui.components.Sheet
import app.heartwood.ui.theme.S
import app.heartwood.ui.theme.SType
import app.heartwood.ui.theme.kraft
import java.time.temporal.ChronoUnit

const val MAX_TREES = 7

@Composable
fun GroveScreen(h: Home, go: Go) {
    val ru = h.ru
    var under by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().kraft()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(if (ru) "Моя роща" else "My grove", style = SType.Display)
                    Text("HEARTWOOD · ${if (ru) "СЕРДЦЕВИНА" else "A FORESTER'S NOTEBOOK"}", style = SType.Typed.copy(fontSize = 11.sp, letterSpacing = 1.sp))
                }
                IconButton(onClick = go::settings) { Icon(Icons.Outlined.Settings, null, tint = S.c.ink) }
            }
            Gap(6)
            if (h.trees.isEmpty()) {
                Empty(h, go)
                return@Column
            }
            Text(
                if (ru) "Деревья, с которыми вы дружите. Под землёй их связывает грибница: навестите одно — остальные узнают."
                else "The trees you're friends with. Underground the mycelium links them: visit one and the others hear of it.",
                style = SType.Muted,
            )
            Gap(10)
            Box(Modifier.fillMaxWidth().height(320.dp).shadow(6.dp, S.RSm).clip(S.RSm)) {
                GroveMap(h.trees, under, h.latest?.id, null, h.today.monthValue, onTap = { go.tree(it.id) }, modifier = Modifier.fillMaxSize())
                if (!under) Text("© OpenStreetMap", style = SType.Typed.copy(fontSize = 9.sp), modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp))
            }
            Gap(8)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.border(1.5.dp, S.c.ink, S.RSm)) {
                    for ((i, label) in listOf(if (ru) "На земле" else "Above", if (ru) "Под землёй" else "Underground").withIndex()) {
                        val on = (i == 1) == under
                        Text(
                            label,
                            style = SType.Typed.copy(color = if (on) S.c.paper else S.c.ink),
                            modifier = Modifier.then(if (on) Modifier.background(S.c.ink) else Modifier).clickable { under = i == 1 }.padding(horizontal = 11.dp, vertical = 7.dp),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(if (ru) "${h.trees.size} из $MAX_TREES" else "${h.trees.size} of $MAX_TREES", style = SType.Typed)
            }
            if (under) {
                Gap(6)
                Text(
                    h.latest?.let { if (ru) "Весточка от «${it.name}» идёт по грибнице к остальным." else "News from “${it.name}” is travelling through the mycelium to the others." }
                        ?: (if (ru) "Грибница ждёт первой встречи." else "The mycelium is waiting for a first meeting."),
                    style = SType.Muted,
                )
            }
            h.call?.let { call ->
                Gap(20)
                Sheet(tape = true, onClick = { go.tree(call.tree.id) }) {
                    Row {
                        Envelope()
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Label(if (ru) "Зов" else "A call")
                            Said(call.text, size = 23)
                            Text("— «${call.tree.name}», ${h.species(call.tree).title(ru).lowercase()}", style = SType.Muted)
                        }
                    }
                }
            }
            Gap(18)
            Sheet(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                h.trees.forEachIndexed { i, t ->
                    if (i > 0) HorizontalDivider(color = S.c.line.copy(alpha = .6f))
                    TreeRow(h, t) { go.tree(t.id) }
                }
            }
            Gap(14)
            if (h.trees.size < MAX_TREES) LineButton(if (ru) "+ подружиться с новым деревом" else "+ befriend a new tree", go::newTree, Modifier.fillMaxWidth())
            else Text(if (ru) "В роще семь деревьев — больше и не нужно: за ними не успеть." else "Seven trees in the grove: more would be more than you could visit.", style = SType.Muted)
            Gap(20)
        }
        TabBar(Tab.GROVE, go)
    }
}

@Composable
private fun TreeRow(h: Home, t: Tree, onClick: () -> Unit) {
    val ru = h.ru
    val sp = h.species(t)
    val last = h.lastMet[t.id]
    val f = h.friendship(t.id)
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        NativeCanvas(Modifier.size(52.dp, 60.dp)) { c, w, hh ->
            c.drawBitmap(TreeArt.bitmap(sp, h.today.monthValue, t.seed, w.toInt(), hh.toInt()), 0f, 0f, null)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(t.name, style = SType.Title.copy(fontSize = 17.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(sp.title(ru), f.level.title(ru).lowercase(), last?.let { T.ago(it, ru, h.today) } ?: (if (ru) "ещё не встречались" else "not met yet")).joinToString(" · "),
                style = SType.Muted.copy(fontSize = 13.sp),
            )
            if (last != null && ChronoUnit.DAYS.between(last, h.today) > Rings.ABSENCE_DAYS) {
                Text(if (ru) "На карточке нарос мох. Дерево ждёт." else "Moss has grown on the card. The tree is waiting.", style = SType.Muted.copy(fontSize = 12.sp, color = S.c.moss))
            }
        }
        Text("${h.rings(t.id).size}", style = SType.Typed)
    }
}

@Composable
private fun Envelope() {
    NativeCanvas(Modifier.size(44.dp, 32.dp)) { c, w, hh ->
        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { style = android.graphics.Paint.Style.STROKE; strokeWidth = 3f; color = 0xFF2F2A24.toInt() }
        val f = android.graphics.Paint().apply { color = 0xFFEFE2C4.toInt() }
        c.drawRect(2f, 2f, w - 2, hh - 2, f)
        c.drawRect(2f, 2f, w - 2, hh - 2, p)
        c.drawLine(2f, 2f, w / 2, hh * .55f, p)
        c.drawLine(w - 2, 2f, w / 2, hh * .55f, p)
    }
}

@Composable
private fun Empty(h: Home, go: Go) {
    val ru = h.ru
    val context = androidx.compose.ui.platform.LocalContext.current
    Gap(10)
    Sheet(tape = true) {
        NativeCanvas(Modifier.fillMaxWidth().height(200.dp)) { c, w, hh -> TreeArt.draw(c, w, hh, app.heartwood.domain.Species.OAK, h.today.monthValue, 11) }
        Gap(8)
        Said(if (ru) "Выберите настоящее дерево — у дома, у школы, в парке. Дружба растёт только от встреч у него." else "Choose a real tree — by your house, your school, in the park. The friendship grows only from meeting it there.", size = 23)
        Gap(10)
        Text(
            if (ru) "Нужно будет: сфотографировать его, определить породу, обнять и отметить на карте. Потом — приходить."
            else "You'll photograph it, work out what it is, hug it and mark it on the map. Then — keep coming back.",
            style = SType.Muted,
        )
    }
    Gap(16)
    InkButton(if (ru) "Подружиться с деревом" else "Befriend a tree", go::newTree, Modifier.fillMaxWidth(), moss = true)
    Gap(10)
    // A friend's QR code opens the app by itself; this is for a link sent as text.
    val paste = androidx.compose.ui.platform.LocalClipboardManager.current
    LineButton(if (ru) "Принять дерево от друга (ссылка в буфере)" else "Take a friend's tree (link in clipboard)", {
        val g = app.heartwood.domain.Gift.parse(paste.getText()?.text.orEmpty())
        if (g != null) context.container.gift.value = g
        else android.widget.Toast.makeText(context, if (ru) "В буфере нет ссылки на дерево" else "No tree link in the clipboard", android.widget.Toast.LENGTH_SHORT).show()
    }, Modifier.fillMaxWidth())
}
