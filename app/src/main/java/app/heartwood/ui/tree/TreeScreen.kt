package app.heartwood.ui.tree

import android.content.Intent
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.heartwood.container
import app.heartwood.data.Kind
import app.heartwood.data.Tree
import app.heartwood.domain.Gift
import app.heartwood.domain.Memory
import app.heartwood.domain.Rings
import app.heartwood.domain.Species
import app.heartwood.ui.Go
import app.heartwood.ui.Home
import app.heartwood.ui.T
import app.heartwood.ui.art.Engrave
import app.heartwood.ui.art.RingArt
import app.heartwood.ui.art.TreeArt
import app.heartwood.ui.components.Bar
import app.heartwood.ui.components.Gap
import app.heartwood.ui.components.InkButton
import app.heartwood.ui.components.Label
import app.heartwood.ui.components.LineButton
import app.heartwood.ui.components.NativeCanvas
import app.heartwood.ui.components.Page
import app.heartwood.ui.components.Photos
import app.heartwood.ui.components.Said
import app.heartwood.ui.components.Sheet
import app.heartwood.ui.components.Stamp
import app.heartwood.ui.theme.S
import app.heartwood.ui.theme.SType
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.temporal.ChronoUnit

private val engravings = LruCache<String, Bitmap>(8)

/** The tree's portrait: an engraving of its first photo, or the drawn tree if there's none. */
@Composable
fun Portrait(t: Tree, month: Int, modifier: Modifier) {
    val c = LocalContext.current.container
    val px = with(LocalDensity.current) { 300.dp.roundToPx() }
    val engraved by produceState<Bitmap?>(null, t.photo) {
        if (t.photo.isEmpty()) return@produceState
        value = engravings.get(t.photo) ?: withContext(Dispatchers.Default) {
            Photos.load(c.file(t.photo), 1000)?.let { Engrave.of(it, px, px * 7 / 6) }?.also { engravings.put(t.photo, it) }
        }
    }
    Box(modifier) {
        val e = engraved
        if (e != null) Image(e.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else NativeCanvas(Modifier.fillMaxSize()) { cv, w, h -> TreeArt.draw(cv, w, h, Species.of(t.species), month, t.seed) }
    }
}

@Composable
fun TreeScreen(id: Long, h: Home, go: Go) {
    val t = h.tree(id) ?: return
    val ru = h.ru
    val sp = h.species(t)
    val rings = h.rings(id)
    val f = h.friendship(id)
    val context = LocalContext.current
    val c = context.container
    var menu by remember { mutableStateOf(false) }
    var give by remember { mutableStateOf(false) }
    var forget by remember { mutableStateOf(false) }
    var picked by rememberSaveable(id, rings.size) { mutableStateOf(if (rings.isEmpty()) null else rings.size - 1) }
    val last = rings.lastOrNull()?.day
    val mossy = last != null && ChronoUnit.DAYS.between(last, h.today) > Rings.ABSENCE_DAYS

    Page("", go::back, actions = {
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, null, tint = S.c.ink) }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem({ Text(if (ru) "Передать другу" else "Pass it on to a friend") }, { menu = false; give = true })
                DropdownMenuItem({ Text(if (ru) "Поправить точку на карте" else "Move its point on the map") }, { menu = false; go.move(id) })
                DropdownMenuItem({ Text(if (ru) "Забыть это дерево" else "Forget this tree") }, { menu = false; forget = true })
            }
        }
    }) {
        Row(verticalAlignment = Alignment.Bottom) {
            Box(Modifier.rotate(-1.5f).shadow(6.dp, S.RSm).background(S.c.paper).padding(6.dp)) {
                Portrait(t, h.today.monthValue, Modifier.size(128.dp, 150.dp).then(if (mossy) Modifier.mossy() else Modifier))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Stamp(f.level.title(ru))
                Gap(4)
                Text("«${t.name}»", style = SType.Display)
                val age = sp.age(t.girth)
                Text(
                    "${sp.title(ru).uppercase()} · ${sp.character(ru)}\n" +
                        (if (ru) "обхват ${t.girth} см · ~${T.years(age, ru)}" else "girth ${t.girth} cm · ~${T.years(age, ru)}") +
                        (h.firstMet[id]?.let { "\n" + (if (ru) "дружим с " else "friends since ") + T.date(it, ru, h.today) } ?: ""),
                    style = SType.Typed,
                )
                Gap(6)
                Bar(f.progress, Modifier.fillMaxWidth())
            }
        }
        f.needs?.let { Text(it, style = SType.Typed.copy(fontSize = 11.5.sp), modifier = Modifier.padding(top = 6.dp)) }
        Gap(14)
        Sheet { Said(h.line(t), size = 24) }
        h.news(t)?.let { n ->
            Gap(12)
            Sheet {
                Label(if (ru) "По грибнице" else "Through the roots")
                Gap(2)
                Text(n.text, style = SType.Body)
            }
        }
        h.sealed(id)?.let { n ->
            Gap(12)
            Sheet(tape = true) {
                Label(if (ru) "В дупле" else "In the hollow")
                Text(
                    if (h.now >= n.openAt) (if (ru) "Записка от ${T.stamp(n.leftAt, ru)} готова. Откройте её у дерева." else "The note from ${T.stamp(n.leftAt, ru)} is ready. Open it at the tree.")
                    else (if (ru) "Записка запечатана до ${T.stamp(n.openAt, ru)}. Откроется только здесь." else "A note sealed until ${T.stamp(n.openAt, ru)}. It opens only here."),
                    style = SType.Body,
                )
            }
        }
        Gap(14)
        InkButton(if (ru) "Я у дерева — встреча" else "I'm at the tree — meet", { go.meet(id) }, Modifier.fillMaxWidth(), moss = true)
        Gap(18)
        Sheet {
            Text(if (ru) "Спил дружбы" else "The friendship, sawn through", style = SType.Title)
            Text(
                if (rings.isEmpty()) (if (ru) "Колец пока нет. Первое появится, когда вы встретитесь." else "No rings yet. The first appears when you meet.")
                else (if (ru) "Каждый день встречи — кольцо. Сердцевина — день знакомства. Нажмите на кольцо." else "Every day you meet is a ring. The pith is the day you met. Tap a ring."),
                style = SType.Muted,
            )
            Gap(8)
            RingsView(t, rings, picked) { picked = it }
            Gap(8)
            Legend(ru)
        }
        picked?.let { i ->
            rings.getOrNull(if (i < 0) 0 else i)?.let { ring ->
                Gap(12)
                Sheet {
                    Text(
                        (if (i < 0) (if (ru) "СЕРДЦЕВИНА" else "THE PITH") else (if (ru) "КОЛЬЦО ${i + 1}" else "RING ${i + 1}")) + " · " +
                            listOf(T.stamp(ring.day, ru), T.weather(ring.weather, ru), ring.kinds.joinToString(", ") { T.kind(it, ru) }).filter { it.isNotEmpty() }.joinToString(" · "),
                        style = SType.Typed,
                    )
                    Gap(4)
                    Said(Memory.of(t, ring, ru), size = 23)
                    if (ring.first && t.why.isNotBlank()) Text((if (ru) "Почему именно ты: «" else "Why you: “") + t.why + (if (ru) "»" else "”"), style = SType.Muted, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        Gap(12)
        Sheet {
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Stat(rings.size, if (ru) "встреч" else "meetings")
                Stat(h.count(id, Kind.HUG), if (ru) "объятий" else "hugs")
                Stat(h.count(id, Kind.LISTEN), if (ru) "голосов" else "voices")
                Stat(h.count(id, Kind.LEAF), if (ru) "листьев" else "leaves")
            }
        }
        val frames = h.frames(t)
        if (frames.size >= 2) {
            Gap(12)
            LineButton(if (ru) "Хроника: ${frames.size} кадров" else "Chronicle: ${frames.size} frames", { go.chronicle(id) }, Modifier.fillMaxWidth())
        }
        if (t.from.isNotBlank()) Text(if (ru) "Передано: ${t.from}" else "Passed on by ${t.from}", style = SType.Typed, modifier = Modifier.padding(top = 10.dp))
        Gap(24)
    }

    if (give) GiveDialog(t, ru) { give = false }
    if (forget) AlertDialog(
        onDismissRequest = { forget = false },
        title = { Text(if (ru) "Забыть «${t.name}»?" else "Forget “${t.name}”?") },
        text = { Text(if (ru) "Кольца, голоса, листья и оттиски этого дерева сотрутся. Само дерево, конечно, останется стоять." else "Its rings, voices, leaves and rubbings will be erased. The tree itself will go on standing, of course.") },
        confirmButton = { TextButton(onClick = { forget = false; c.write { forget(t) }; go.grove() }) { Text(if (ru) "Забыть" else "Forget", color = S.c.rust) } },
        dismissButton = { TextButton(onClick = { forget = false }) { Text(if (ru) "Оставить" else "Keep") } },
    )
}

private fun Modifier.mossy() = drawWithContent {
    drawContent()
    drawCircle(Brush.radialGradient(listOf(Color(0xAA5F7A3C), Color.Transparent), Offset(0f, size.height), size.minDimension * .35f), size.minDimension * .35f, Offset(0f, size.height))
    drawCircle(Brush.radialGradient(listOf(Color(0x885F7A3C), Color.Transparent), Offset(size.width, 0f), size.minDimension * .25f), size.minDimension * .25f, Offset(size.width, 0f))
}

@Composable
private fun RingsView(t: Tree, rings: List<app.heartwood.domain.Ring>, picked: Int?, onPick: (Int?) -> Unit) {
    // Kept outside of state: drawing writes it, taps read it.
    val layout = remember { arrayOfNulls<RingArt.Layout>(1) }
    NativeCanvas(
        Modifier.fillMaxWidth().aspectRatio(1f).pointerInput(rings) {
            detectTapGestures { o -> layout[0]?.let { l -> RingArt.hit(l, o.x, o.y)?.let(onPick) } }
        },
    ) { cv, w, _ ->
        val l = RingArt.layout(rings, w, t.seed)
        layout[0] = l
        RingArt.draw(cv, rings, l, picked)
    }
}

@Composable
private fun Legend(ru: Boolean) {
    val items = listOf(
        RingArt.seasonColour(app.heartwood.domain.Season.SPRING) to (if (ru) "весна" else "spring"),
        RingArt.seasonColour(app.heartwood.domain.Season.SUMMER) to (if (ru) "лето" else "summer"),
        RingArt.seasonColour(app.heartwood.domain.Season.AUTUMN) to (if (ru) "осень" else "autumn"),
        RingArt.seasonColour(app.heartwood.domain.Season.WINTER) to (if (ru) "зима" else "winter"),
        0xFFE7B9A0.toInt() to (if (ru) "объятие" else "a hug"),
        0xFF2F2A24.toInt() to (if (ru) "долгая разлука" else "a long absence"),
    )
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for ((col, name) in items) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
            Box(Modifier.size(14.dp, 4.dp).background(Color(col)))
            Spacer(Modifier.width(4.dp))
            Text(name, style = SType.Typed.copy(fontSize = 11.sp))
        }
    }
}

@Composable
private fun Stat(n: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$n", style = SType.Display.copy(fontSize = 24.sp))
        Text(label, style = SType.Typed.copy(fontSize = 10.5.sp))
    }
}

fun qr(text: String, size: Int): Bitmap {
    val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.CHARACTER_SET to "UTF-8"))
    val px = IntArray(size * size) { i -> if (m[i % size, i / size]) 0xFF2F2A24.toInt() else 0xFFF1E7D1.toInt() }
    return Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888)
}

@Composable
private fun GiveDialog(t: Tree, ru: Boolean, onDone: () -> Unit) {
    val context = LocalContext.current
    val link = remember(t) { Gift.link(t, "") }
    val img = remember(link) { qr(link, 600).asImageBitmap() }
    AlertDialog(
        onDismissRequest = onDone,
        title = { Text(if (ru) "Передать «${t.name}»" else "Pass on “${t.name}”") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (ru) "Пусть друг наведёт камеру телефона на код — дерево станет и его. Кольца у каждого свои."
                    else "Let a friend point their phone camera at the code and the tree becomes theirs too. Each of you keeps your own rings.",
                    style = SType.Muted,
                )
                Gap(10)
                Image(img, null, Modifier.size(220.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val text = (if (ru) "Дружи с этим деревом: ${Species.of(t.species).title.ru} «${t.name}». Открой в Heartwood: " else "Befriend this tree: ${Species.of(t.species).title.en} “${t.name}”. Open in Heartwood: ") + link
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
                onDone()
            }) { Text(if (ru) "Отправить ссылкой" else "Send as a link") }
        },
        dismissButton = { TextButton(onClick = onDone) { Text(if (ru) "Готово" else "Done") } },
    )
}
