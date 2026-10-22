package app.heartwood.ui.meet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.heartwood.container
import app.heartwood.data.Kind
import app.heartwood.domain.Memory
import app.heartwood.domain.Rings
import app.heartwood.sense.Heart
import app.heartwood.sense.HugSense
import app.heartwood.ui.Go
import app.heartwood.ui.Home
import app.heartwood.ui.components.Art
import app.heartwood.ui.components.Bar
import app.heartwood.ui.components.Centered
import app.heartwood.ui.components.Gap
import app.heartwood.ui.components.InkButton
import app.heartwood.ui.components.KeepScreenOn
import app.heartwood.ui.components.Page
import app.heartwood.ui.components.Said
import app.heartwood.ui.theme.Mono
import app.heartwood.ui.theme.S
import app.heartwood.ui.theme.SType
import app.heartwood.ui.theme.Serif
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A full hug, in seconds of holding still against the trunk. */
const val HUG_SECONDS = 60
private const val FROM_BPM = 72
private const val TO_BPM = 24

/**
 * The hug: the phone against your chest, your chest against the trunk. While the proximity sensor
 * is covered and the phone is still, a heartbeat plays in the hand and slows from yours toward the
 * tree's. A minute of it is a hug.
 */
@Composable
fun HugScreen(id: Long, h: Home, go: Go) {
    val t = h.tree(id) ?: return
    val ru = h.ru
    val context = LocalContext.current
    val c = context.container
    val sense = remember { HugSense(context) }
    val heart = remember { Heart(context) }
    var pressing by remember { mutableStateOf(false) }
    var held by remember { mutableFloatStateOf(if (Art.still) 22f else 0f) }
    var done by remember { mutableStateOf<String?>(null) }
    val pulse = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    KeepScreenOn()
    DisposableEffect(Unit) {
        sense.start()
        onDispose { sense.stop() }
    }
    val hugging = done == null && ((sense.available && sense.covered && sense.still) || pressing)
    val p = (held / HUG_SECONDS).coerceIn(0f, 1f)
    val bpm = (FROM_BPM - (FROM_BPM - TO_BPM) * p).toInt()

    LaunchedEffect(hugging) {
        while (hugging && done == null) {
            val now = (held / HUG_SECONDS).coerceIn(0f, 1f)
            heart.beat(1f - now * .5f)
            scope.launch { pulse.snapTo(1f); pulse.animateTo(0f, tween(900)) }
            val wait = 60_000L / (FROM_BPM - (FROM_BPM - TO_BPM) * now).toInt()
            delay(wait)
            held += wait / 1000f
            if (held >= HUG_SECONDS) {
                heart.done()
                val visit = c.record(t, Kind.HUG, seconds = held.toInt(), bpm = TO_BPM)
                val rings = Rings.of(h.visits(id) + app.heartwood.data.Visit(id = visit, treeId = id, kind = Kind.HUG.name, seconds = held.toInt(), bpm = TO_BPM))
                done = rings.lastOrNull()?.let { r -> Memory.of(t, r, ru) } ?: ""
            }
        }
    }

    Page(if (ru) "Объятие" else "A hug", go::back) {
        Text(
            if (sense.available) (if (ru) "Прижмите телефон экраном к груди и обнимите ствол. Стойте спокойно — сердце в ладони будет замедляться к ритму дерева."
            else "Hold the phone screen-in against your chest and hug the trunk. Stand still — the heartbeat in your hand will slow toward the tree's.")
            else (if (ru) "Обнимите ствол и держите палец на круге." else "Hug the trunk and keep a finger on the circle."),
            style = SType.Muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Gap(18)
        Centered {
            Box(
                Modifier.size(230.dp).pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        pressing = true
                        waitForUpOrCancellation()
                        pressing = false
                    }
                },
                contentAlignment = Alignment.Center,
            ) {
                val ink = S.c.ink
                val rust = S.c.rust
                val paper = S.c.paper
                val paper2 = S.c.paper2
                Canvas(Modifier.size(230.dp)) {
                    val r = size.minDimension / 2 * .82f
                    drawCircle(paper2, r)
                    drawCircle(paper, r * .78f)
                    drawCircle(ink, r, style = Stroke(2.dp.toPx()))
                    drawArc(rust, -90f, 360f * p, false, Offset(center.x - r, center.y - r), androidx.compose.ui.geometry.Size(r * 2, r * 2), style = Stroke(5.dp.toPx()))
                    val k = pulse.value
                    if (k > 0f) drawCircle(rust.copy(alpha = k * .8f), r * (1f + (1f - k) * .25f), style = Stroke(2.dp.toPx()))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$bpm", style = androidx.compose.ui.text.TextStyle(fontFamily = Serif, fontSize = (46 + pulse.value * 6).sp, color = S.c.ink))
                    Text(if (ru) "УДАРОВ В МИНУТУ" else "BEATS A MINUTE", style = androidx.compose.ui.text.TextStyle(fontFamily = Mono, fontSize = 11.sp, color = S.c.muted))
                }
            }
        }
        Gap(10)
        Bar(p, Modifier.fillMaxWidth(), color = S.c.rust)
        Gap(6)
        Text(
            when {
                done != null -> if (ru) "В спиле появилось тёплое кольцо." else "A warm ring has appeared in the cross-section."
                hugging -> if (ru) "Ваш ритм → ритм дерева" else "Your rhythm → the tree's"
                held > 0 -> if (ru) (if (p < .3f) "Ещё немного. Дерево только начало слышать." else "Почти. Не отпускай.") else (if (p < .3f) "A little longer. The tree has only begun to hear." else "Almost. Don't let go.")
                sense.available -> if (ru) "Ждём, когда телефон закроется грудью и замрёт." else "Waiting for the phone to be covered and still."
                else -> if (ru) "Нажмите и держите круг." else "Press and hold the circle."
            },
            style = SType.Typed, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        done?.let {
            Gap(14)
            Said(it, size = 24)
            Gap(14)
            InkButton(if (ru) "Отпустить" else "Let go", go::back, Modifier.fillMaxWidth(), moss = true)
        }
        if (done == null && sense.available) {
            Gap(18)
            Text(if (ru) "Без датчика: можно держать палец на круге." else "Or keep a finger on the circle instead.", style = SType.Typed.copy(color = S.c.faint), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        Gap(20)
    }
}
