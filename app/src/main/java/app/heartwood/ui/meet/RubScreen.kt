package app.heartwood.ui.meet

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.heartwood.container
import app.heartwood.data.Kind
import app.heartwood.ui.Go
import app.heartwood.ui.Home
import app.heartwood.ui.art.BarkArt
import app.heartwood.ui.art.PAPER
import app.heartwood.ui.components.Gap
import app.heartwood.ui.components.InkButton
import app.heartwood.ui.components.LineButton
import app.heartwood.ui.components.NativeCanvas
import app.heartwood.ui.components.Page
import app.heartwood.ui.components.Photos
import app.heartwood.ui.components.rememberCamera
import app.heartwood.ui.theme.S
import app.heartwood.ui.theme.SType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.math.hypot

private const val RUB_PX = 900
private const val GRID = 12

/**
 * A bark rubbing: paper laid over the bark and rubbed with a finger, like graphite. The relief comes
 * from a photo of this tree's bark — its raised parts — or, without one, from the species' pattern.
 */
@Composable
fun RubScreen(id: Long, h: Home, go: Go) {
    val t = h.tree(id) ?: return
    val ru = h.ru
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    var mask by remember { mutableStateOf<Bitmap?>(null) }
    var working by remember { mutableStateOf(false) }
    val camera = rememberCamera("tmp") { rel ->
        if (rel == null) return@rememberCamera
        working = true
        scope.launch {
            mask = withContext(Dispatchers.Default) {
                val photo = Photos.load(c.file(rel), 1200)
                c.file(rel).delete()
                photo?.let { BarkArt.fromPhoto(it, RUB_PX, RUB_PX) }
            }
            working = false
        }
    }

    Page(if (ru) "Фроттаж" else "A bark rubbing", go::back) {
        val m = mask
        if (m == null) {
            Text(
                if (ru) "Сфотографируйте кору вплотную, с ладонь шириной, — в ней проступят настоящие бороздки этого дерева. Или потрите бумагу по узору породы."
                else "Photograph the bark up close, a hand's width — the real ridges of this tree will come through. Or rub over the species' pattern instead.",
                style = SType.Muted,
            )
            Gap(16)
            InkButton(if (working) (if (ru) "Снимаю оттиск…" else "Lifting the relief…") else (if (ru) "Сфотографировать кору" else "Photograph the bark"), camera.shoot, Modifier.fillMaxWidth(), enabled = !working)
            Gap(10)
            LineButton(if (ru) "Без фото — узор породы" else "No photo — the species' pattern", {
                mask = BarkArt.pattern(h.species(t).bark, RUB_PX, RUB_PX, System.nanoTime() % 9999)
            }, Modifier.fillMaxWidth(), enabled = !working)
        } else Rubbing(m, ru) { bmp ->
            val rel = c.newFile("rubbings", "png")
            c.write {
                withContext(Dispatchers.IO) { c.file(rel).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
                record(t, Kind.RUB, file = rel)
            }
            go.back()
        }
        Gap(20)
    }
}

@Composable
private fun Rubbing(mask: Bitmap, ru: Boolean, onKeep: (Bitmap) -> Unit) {
    val paper = remember { Bitmap.createBitmap(RUB_PX, RUB_PX, Bitmap.Config.ARGB_8888).apply { eraseColor(PAPER) } }
    val canvas = remember { Canvas(paper) }
    val cells = remember { HashSet<Int>() }
    var tick by remember { mutableIntStateOf(0) }
    val graphite = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { alpha = 46 } }
    val smudge = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BarkArt.GRAPHITE; alpha = 6 } }
    val clip = remember { Path() }
    // Reading the tick recomposes the percentage as the rubbing grows.
    val coverage = (tick.let { cells.size } * 100 / (GRID * GRID * .7f)).toInt().coerceAtMost(100)

    fun rubAt(x: Float, y: Float) {
        val r = RUB_PX * .045f
        clip.reset()
        clip.addCircle(x, y, r, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(clip)
        canvas.drawBitmap(mask, 0f, 0f, graphite)
        canvas.drawCircle(x, y, r, smudge)
        canvas.restore()
        cells += (y / RUB_PX * GRID).toInt().coerceIn(0, GRID - 1) * GRID + (x / RUB_PX * GRID).toInt().coerceIn(0, GRID - 1)
    }

    Text(if (ru) "Водите пальцем по бумаге — как грифелем по коре." else "Rub the paper with a finger, like graphite over bark.", style = SType.Muted)
    Gap(10)
    Box(Modifier.fillMaxWidth().aspectRatio(1f).shadow(6.dp, S.RSm).background(S.c.paper)) {
        NativeCanvas(
            Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    var last: androidx.compose.ui.geometry.Offset? = null
                    detectDragGestures(
                        onDragStart = { o -> val k = RUB_PX / size.width.toFloat(); rubAt(o.x * k, o.y * k); last = o; tick++ },
                        onDragEnd = { last = null },
                    ) { change, _ ->
                        val k = RUB_PX / size.width.toFloat()
                        val p = change.position
                        val from = last ?: p
                        val d = hypot(p.x - from.x, p.y - from.y)
                        var s = 6f
                        while (s < d) {
                            rubAt((from.x + (p.x - from.x) * s / d) * k, (from.y + (p.y - from.y) * s / d) * k)
                            s += 6f
                        }
                        rubAt(p.x * k, p.y * k)
                        last = p
                        tick++
                    }
                }
                .pointerInput(Unit) { detectTapGestures { o -> val k = RUB_PX / size.width.toFloat(); rubAt(o.x * k, o.y * k); tick++ } },
        ) { cv, w, hh ->
            tick.let { }
            cv.drawBitmap(paper, null, RectF(0f, 0f, w, hh), Paint(Paint.FILTER_BITMAP_FLAG))
        }
    }
    Gap(8)
    Text("$coverage%", style = SType.Typed, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    Gap(10)
    InkButton(if (ru) "Вклеить в тетрадь" else "Paste into the notebook", { onKeep(paper.copy(Bitmap.Config.ARGB_8888, false)) }, Modifier.fillMaxWidth(), enabled = coverage >= 60, moss = true)
}
