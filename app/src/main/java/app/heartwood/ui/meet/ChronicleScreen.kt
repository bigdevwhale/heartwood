package app.heartwood.ui.meet

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.heartwood.container
import app.heartwood.data.Kind
import app.heartwood.domain.Friend
import app.heartwood.domain.Season
import app.heartwood.domain.day
import app.heartwood.ui.Frame
import app.heartwood.ui.Go
import app.heartwood.ui.Home
import app.heartwood.ui.T
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

/** How a chronicle photo sits over the one before it: scale, shift (fractions of the frame) and turn. */
data class Fit(val scale: Float = 1f, val dx: Float = 0f, val dy: Float = 0f, val rot: Float = 0f) {
    fun encode() = "%.4f,%.4f,%.4f,%.2f".format(Locale.US, scale, dx, dy, rot)

    companion object {
        fun of(s: String): Fit = s.split(',').mapNotNull { it.toFloatOrNull() }.takeIf { it.size == 4 }?.let { Fit(it[0], it[1], it[2], it[3]) } ?: Fit()
    }
}

/** Draws a photo cover-fitted into a w×h frame, then moved by its fit. */
fun drawFitted(c: Canvas, b: Bitmap, w: Float, h: Float, fit: Fit, alpha: Int = 255) {
    val k = maxOf(w / b.width, h / b.height)
    val bw = b.width * k
    val bh = b.height * k
    c.save()
    c.translate(w / 2 + fit.dx * w, h / 2 + fit.dy * h)
    c.rotate(fit.rot)
    c.scale(fit.scale, fit.scale)
    c.drawBitmap(b, null, RectF(-bw / 2, -bh / 2, bw / 2, bh / 2), Paint(Paint.FILTER_BITMAP_FLAG).apply { this.alpha = alpha })
    c.restore()
}

@Composable
private fun rememberFrameBitmaps(frames: List<Frame>): List<Bitmap?> {
    val c = LocalContext.current.container
    val list by produceState(List<Bitmap?>(frames.size) { null }, frames) {
        value = withContext(Dispatchers.IO) { frames.map { Photos.load(c.file(it.file), 1200) } }
    }
    return list
}

/** Photographing the tree from the same spot as last time: the new photo is fitted over a ghost of the previous one. */
@Composable
fun ShootScreen(id: Long, h: Home, go: Go) {
    val t = h.tree(id) ?: return
    val ru = h.ru
    val c = LocalContext.current.container
    val frames = h.frames(t)
    val last = frames.lastOrNull()
    var shot by rememberSaveable { mutableStateOf("") }
    val camera = rememberCamera("photos") { rel -> if (rel != null) shot = rel }
    Page(if (ru) "Снимок по кальке" else "A photo over the last", go::back) {
        if (shot.isEmpty()) {
            Text(
                if (last == null) (if (ru) "Снимите дерево целиком — с места, куда легко вернуться: от скамейки, от угла дома. Это первый кадр хроники."
                else "Photograph the whole tree from a spot you can come back to — a bench, a corner of a house. This is the chronicle's first frame.")
                else (if (ru) "Встаньте туда же, где снимали в прошлый раз (${T.stamp(last.at, ru)}), и снимите дерево. Потом совместите кадр с калькой прошлого."
                else "Stand where you stood last time (${T.stamp(last.at, ru)}) and photograph the tree. Then fit the frame over a ghost of the last one."),
                style = SType.Muted,
            )
            Gap(16)
            if (last != null) {
                val prev = rememberFrameBitmaps(listOf(last)).firstOrNull()
                Box(Modifier.fillMaxWidth().aspectRatio(.75f).shadow(6.dp, S.RSm).clip(S.RSm).background(S.c.paper)) {
                    prev?.let { b -> NativeCanvas(Modifier.fillMaxSize()) { cv, w, hh -> drawFitted(cv, b, w, hh, Fit.of(last.align)) } }
                }
                Gap(12)
            }
            InkButton(if (ru) "Открыть камеру" else "Open the camera", camera.shoot, Modifier.fillMaxWidth())
        } else {
            val now = rememberFrameBitmaps(listOf(Frame(shot, h.now, ""))).firstOrNull()
            val prev = last?.let { rememberFrameBitmaps(listOf(it)).firstOrNull() }
            var fit by remember { mutableStateOf(Fit()) }
            Text(
                if (last == null) (if (ru) "Первый кадр хроники." else "The chronicle's first frame.")
                else (if (ru) "Двигайте, приближайте и поворачивайте снимок двумя пальцами, пока он не совпадёт с калькой." else "Move, zoom and turn the photo with two fingers until it matches the ghost."),
                style = SType.Muted,
            )
            Gap(10)
            Box(
                Modifier.fillMaxWidth().aspectRatio(.75f).shadow(6.dp, S.RSm).clip(S.RSm).background(S.c.paper)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, rotation ->
                            fit = fit.copy(scale = (fit.scale * zoom).coerceIn(.5f, 3f), dx = fit.dx + pan.x / size.width, dy = fit.dy + pan.y / size.height, rot = fit.rot + rotation)
                        }
                    },
            ) {
                NativeCanvas(Modifier.fillMaxSize()) { cv, w, hh ->
                    now?.let { drawFitted(cv, it, w, hh, fit) }
                    if (prev != null && last != null) drawFitted(cv, prev, w, hh, Fit.of(last.align), alpha = 110)
                }
            }
            Gap(12)
            InkButton(if (ru) "В хронику" else "Into the chronicle", {
                val f = shot
                val a = fit.encode()
                c.write {
                    if (t.photo.isEmpty()) db.dao().update(t.copy(photo = f)) else record(t, Kind.PHOTO, file = f, align = a)
                }
                go.back()
            }, Modifier.fillMaxWidth(), moss = true)
            Gap(10)
            LineButton(if (ru) "Переснять" else "Take it again", { c.file(shot).delete(); shot = "" }, Modifier.fillMaxWidth())
        }
        Gap(20)
    }
}

/** The tree through the seasons, frame by frame: a timelapse of the friendship. */
@Composable
fun ChronicleScreen(id: Long, h: Home, go: Go) {
    val t = h.tree(id) ?: return
    val ru = h.ru
    val frames = h.frames(t)
    if (frames.isEmpty()) { go.back(); return }
    val bitmaps = rememberFrameBitmaps(frames)
    var at by remember { mutableIntStateOf(frames.size - 1) }
    var playing by remember { mutableStateOf(false) }
    var slider by remember { mutableFloatStateOf(at.toFloat()) }
    LaunchedEffect(playing) {
        if (!playing) return@LaunchedEffect
        for (i in frames.indices) {
            at = i
            slider = i.toFloat()
            delay(700)
        }
        playing = false
    }
    Page(if (ru) "Хроника «${t.name}»" else "“${t.name}” through time", go::back) {
        Box(Modifier.fillMaxWidth().aspectRatio(.75f).shadow(6.dp, S.RSm).clip(S.RSm).background(S.c.paper)) {
            val b = bitmaps.getOrNull(at)
            if (b != null) NativeCanvas(Modifier.fillMaxSize()) { cv, w, hh -> drawFitted(cv, b, w, hh, Fit.of(frames[at].align)) }
        }
        Gap(10)
        val d = day(frames[at].at)
        Row {
            Text("${T.stamp(d, ru)} · ${Friend.seasonName(Season.of(d), ru)}", style = SType.Typed, modifier = Modifier.weight(1f))
            Text("${at + 1} / ${frames.size}", style = SType.Typed)
        }
        if (frames.size > 1) {
            Slider(
                value = slider, onValueChange = { slider = it; at = it.toInt().coerceIn(0, frames.size - 1) },
                valueRange = 0f..(frames.size - 1).toFloat(), steps = (frames.size - 2).coerceAtLeast(0),
                colors = SliderDefaults.colors(thumbColor = S.c.ink, activeTrackColor = S.c.ink, inactiveTrackColor = S.c.line),
            )
            Row {
                Spacer(Modifier.weight(1f))
                LineButton(if (playing) (if (ru) "■ стоп" else "■ stop") else (if (ru) "▶ таймлапс" else "▶ timelapse"), { playing = !playing }, Modifier.width(170.dp))
            }
        }
        Gap(20)
    }
}
