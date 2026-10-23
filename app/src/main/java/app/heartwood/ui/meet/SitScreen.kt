package app.heartwood.ui.meet

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.heartwood.container
import app.heartwood.data.Kind
import app.heartwood.domain.Species
import app.heartwood.sense.FaceDown
import app.heartwood.ui.Go
import app.heartwood.ui.Home
import app.heartwood.ui.art.TreeArt
import app.heartwood.ui.components.Art
import app.heartwood.ui.components.KeepScreenOn
import app.heartwood.ui.components.LineButton
import app.heartwood.ui.theme.Mono
import app.heartwood.ui.theme.Serif
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sitting beside the tree: the phone face down, leaves drifting on a dark screen, a clock that only
 * runs while the phone lies still on its face. Nothing else happens; that is the point.
 */
@Composable
fun SitScreen(id: Long, h: Home, go: Go) {
    val t = h.tree(id) ?: return
    val ru = h.ru
    val context = LocalContext.current
    val c = context.container
    val face = remember { FaceDown(context) }
    var sat by remember { mutableLongStateOf(if (Art.still) 754_000L else 0L) }
    var frame by remember { mutableLongStateOf(0L) }
    var up by remember { mutableStateOf(false) }
    KeepScreenOn()
    DisposableEffect(Unit) {
        face.start()
        onDispose { face.stop() }
    }
    LaunchedEffect(Unit) {
        if (Art.still) return@LaunchedEffect
        var last = withFrameMillis { it }
        while (true) {
            val now = withFrameMillis { it }
            if (face.down || !face.available) sat += now - last
            frame = now
            last = now
        }
    }
    val cols = TreeArt.crownColours(Species.of(t.species), h.today.monthValue)
    val leaves = remember { List(14) { Random(it).let { r -> floatArrayOf(r.nextFloat(), r.nextFloat(), .15f + r.nextFloat() * .25f, r.nextFloat() * 6f) } } }
    Box(Modifier.fillMaxSize().background(Color(0xFF15110D))) {
        Canvas(Modifier.fillMaxSize()) {
            val tm = frame / 1000f
            leaves.forEachIndexed { i, l ->
                val y = ((l[1] * size.height + tm * l[2] * 60f) % (size.height + 40f)) - 20f
                val x = l[0] * size.width + sin(tm * .6f + l[3]) * 16.dp.toPx()
                rotate(sin(tm + l[3]) * 57f, Offset(x, y)) {
                    drawOval(Color(cols[i % cols.size]).copy(alpha = .55f), Offset(x - 6.dp.toPx(), y - 3.dp.toPx()), androidx.compose.ui.geometry.Size(12.dp.toPx(), 6.dp.toPx()))
                }
            }
        }
        Column(Modifier.align(Alignment.Center).systemBarsPadding().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val s = sat / 1000
            Text("%02d:%02d".format(s / 60, s % 60), style = TextStyle(fontFamily = Mono, fontSize = 56.sp, color = Color(0xFFE6D6B8), letterSpacing = 2.sp))
            Text(
                when {
                    !face.available -> if (ru) "Мы просто посидим." else "We'll just sit."
                    face.down -> if (ru) "Тихо. Мы сидим." else "Quiet. We're sitting."
                    else -> if (ru) "Положите телефон экраном вниз.\nМы просто посидим." else "Lay the phone face down.\nWe'll just sit."
                },
                style = TextStyle(fontFamily = Serif, fontSize = 16.sp, lineHeight = 23.sp, color = Color(0xB3E6D6B8)),
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp, bottom = 28.dp),
            )
            LineButtonLight(if (ru) "Встать" else "Get up") {
                val secs = (sat / 1000).toInt()
                if (secs < 60) {
                    Toast.makeText(context, if (ru) "Так быстро? Посидите хотя бы минуту." else "So soon? Sit at least a minute.", Toast.LENGTH_SHORT).show()
                    if (secs < 5) go.back()
                } else if (!up) {
                    up = true
                    c.write { record(t, Kind.SIT, seconds = secs) }
                    go.back()
                }
            }
        }
    }
}

/** A pale button on the dark: the paper of the notebook showing through. */
@Composable
private fun LineButtonLight(text: String, onClick: () -> Unit) {
    LineButton(text, onClick, Modifier.width(180.dp).background(Color(0xFFE6D6B8), app.heartwood.ui.theme.S.RSm))
}
