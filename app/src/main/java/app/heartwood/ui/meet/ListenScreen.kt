package app.heartwood.ui.meet

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.heartwood.container
import app.heartwood.data.Kind
import app.heartwood.sense.Recorder
import app.heartwood.sense.Voices
import app.heartwood.ui.Go
import app.heartwood.ui.Home
import app.heartwood.ui.components.Gap
import app.heartwood.ui.components.InkButton
import app.heartwood.ui.components.KeepScreenOn
import app.heartwood.ui.components.LineButton
import app.heartwood.ui.components.Page
import app.heartwood.ui.components.Said
import app.heartwood.ui.components.Sheet
import app.heartwood.ui.theme.S
import app.heartwood.ui.theme.SType
import kotlinx.coroutines.delay

private const val LISTEN_SECONDS = 10

/** Ten seconds of what the bark hears: wind in the crown, a woodpecker, the street. The voice of the day. */
@Composable
fun ListenScreen(id: Long, h: Home, go: Go) {
    val t = h.tree(id) ?: return
    val ru = h.ru
    val context = LocalContext.current
    val c = context.container
    val rec = remember { Recorder(context) }
    var permitted by remember { mutableStateOf(rec.permitted()) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permitted = it }
    var state by remember { mutableIntStateOf(0) } // 0 ready, 1 recording, 2 recorded
    var left by remember { mutableIntStateOf(LISTEN_SECONDS) }
    val levels = remember { mutableStateListOf<Float>() }
    var file by remember { mutableStateOf("") }
    var wave by remember { mutableStateOf("") }
    KeepScreenOn()
    DisposableEffect(Unit) { onDispose { Voices.stop(); if (state == 1) rec.stop() } }

    LaunchedEffect(state) {
        if (state != 1) return@LaunchedEffect
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < LISTEN_SECONDS * 1000L) {
            delay(50)
            levels += rec.level()
            left = LISTEN_SECONDS - ((System.currentTimeMillis() - start) / 1000).toInt()
        }
        wave = rec.stop()
        state = 2
    }

    Page(if (ru) "Голос дня" else "The voice of the day", go::back) {
        Text(
            if (ru) "Приложите телефон микрофоном (нижним краем) к коре и не шевелитесь. Дерево звучит ветром в кроне, птицами, улицей вокруг."
            else "Put the phone's microphone (its bottom edge) against the bark and keep still. A tree sounds of wind in its crown, of birds, of the street around it.",
            style = SType.Muted,
        )
        Gap(14)
        Sheet {
            val ink = S.c.ink
            Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                val n = LISTEN_SECONDS * 20
                val step = size.width / n
                if (levels.isEmpty()) drawLine(ink, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 1.5f)
                levels.forEachIndexed { i, v ->
                    val a = (v * size.height * .48f).coerceAtLeast(1f)
                    drawLine(ink, Offset(i * step, size.height / 2 - a), Offset(i * step, size.height / 2 + a), step * .7f, StrokeCap.Round)
                }
            }
            Gap(6)
            Text(
                when (state) {
                    1 -> "$left"
                    2 -> if (ru) "Записано" else "Recorded"
                    else -> "$LISTEN_SECONDS"
                },
                style = SType.Display.copy(fontSize = 30.sp), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
        Gap(16)
        when {
            !permitted -> InkButton(if (ru) "Разрешить микрофон" else "Allow the microphone", { ask.launch(Manifest.permission.RECORD_AUDIO) }, Modifier.fillMaxWidth())
            state == 0 -> InkButton(if (ru) "Слушать $LISTEN_SECONDS секунд" else "Listen for $LISTEN_SECONDS seconds", {
                val rel = c.newFile("voices", "m4a")
                if (rec.start(c.file(rel))) {
                    file = rel
                    levels.clear()
                    state = 1
                }
            }, Modifier.fillMaxWidth())
            state == 2 -> {
                Row {
                    LineButton(if (Voices.playing == file) "■" else "▶", { Voices.toggle(c.file(file), file) }, Modifier.width(72.dp))
                    androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                    LineButton(if (ru) "Заново" else "Again", {
                        Voices.stop()
                        c.file(file).delete()
                        file = ""
                        levels.clear()
                        left = LISTEN_SECONDS
                        state = 0
                    }, Modifier.weight(1f))
                }
                Gap(10)
                InkButton(if (ru) "Сохранить голос в тетрадь" else "Keep the voice in the notebook", {
                    Voices.stop()
                    val f = file
                    val w = wave
                    c.write { record(t, Kind.LISTEN, file = f, wave = w) }
                    go.back()
                }, Modifier.fillMaxWidth(), moss = true)
                Gap(12)
                Said(if (ru) "Послушай его зимой, когда я буду молчать." else "Listen to it in winter, when I'm quiet.", size = 23)
            }
        }
        Gap(20)
    }
}
