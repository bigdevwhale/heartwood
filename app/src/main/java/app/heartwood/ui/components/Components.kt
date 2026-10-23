package app.heartwood.ui.components

import android.os.SystemClock
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.heartwood.domain.Words
import app.heartwood.ru
import app.heartwood.ui.theme.S
import app.heartwood.ui.theme.SType
import app.heartwood.ui.theme.kraft
import kotlinx.coroutines.delay

/** Screens are drawn still for the renders: no clocks, no live map. */
object Art {
    @Volatile var still = false
}

@Composable
fun ru(): Boolean = LocalContext.current.ru

@Composable
fun Words.s(): String = this(ru())

/** A sheet of notebook paper, optionally taped at the top. */
@Composable
fun Sheet(modifier: Modifier = Modifier, tape: Boolean = false, onClick: (() -> Unit)? = null, padding: PaddingValues = PaddingValues(16.dp), content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(5.dp, S.RSm, ambientColor = Color(0x55000000), spotColor = Color(0x66000000))
                .clip(S.RSm)
                .background(S.c.paper)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(padding),
            content = content,
        )
        if (tape) Tape(Modifier.align(Alignment.TopCenter).offset(y = (-9).dp))
    }
}

@Composable
fun Tape(modifier: Modifier = Modifier, width: Dp = 78.dp) {
    Box(modifier.rotate(-3f).size(width, 18.dp).background(S.c.tape))
}

/** Ink, for the thing to do. */
@Composable
fun InkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null, moss: Boolean = false) {
    val bg = if (moss) S.c.moss else S.c.ink
    // These buttons save and leave: a second tap while the screen is going would save twice.
    val last = remember { longArrayOf(0) }
    Row(
        modifier
            .height(52.dp)
            .clip(S.RSm)
            .background(if (enabled) bg else S.c.line)
            .then(
                if (enabled) Modifier.clickable {
                    val now = SystemClock.uptimeMillis()
                    if (now - last[0] > 700) { last[0] = now; onClick() }
                } else Modifier,
            )
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp), tint = S.c.paper)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = SType.Button, color = if (enabled) S.c.paper else S.c.faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** An outlined button, for the second thing. */
@Composable
fun LineButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    Row(
        modifier
            .height(48.dp)
            .clip(S.RSm)
            .border(1.5.dp, if (enabled) S.c.ink else S.c.line, S.RSm)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val tint = if (enabled) S.c.ink else S.c.faint
        if (icon != null) {
            Icon(icon, null, Modifier.size(19.dp), tint = tint)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = SType.Button.copy(fontSize = 15.sp), color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A rubber stamp: the friendship level, "SEALED", "OPEN". */
@Composable
fun Stamp(text: String, modifier: Modifier = Modifier, color: Color = S.c.amber) {
    Text(
        text.uppercase(),
        style = SType.Stamp.copy(color = color),
        modifier = modifier.rotate(-3f).border(1.5.dp, color, S.RSm).padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

/** What a tree says, in pencil, with a dash. */
@Composable
fun Said(text: String, modifier: Modifier = Modifier, size: Int = 25, align: TextAlign = TextAlign.Start) {
    Text("— $text", style = SType.Hand.copy(fontSize = size.sp, lineHeight = (size + 3).sp), modifier = modifier, textAlign = align)
}

@Composable
fun Label(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = SType.Typed.copy(fontSize = 11.5.sp, letterSpacing = 1.sp), modifier = modifier)
}

/** A screen of kraft with a back arrow and a title, scrolling. */
@Composable
fun Page(title: String, onBack: (() -> Unit)?, modifier: Modifier = Modifier, actions: @Composable () -> Unit = {}, scroll: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    // Edge to edge the window no longer shrinks for the keyboard: the page does, so a focused field scrolls into view.
    Column(modifier.fillMaxSize().kraft().statusBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = S.c.ink) }
            else Spacer(Modifier.width(16.dp))
            Text(title, style = SType.Title.copy(fontSize = 21.sp), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            actions()
        }
        Column(
            Modifier.fillMaxWidth().weight(1f).then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier).padding(horizontal = 16.dp),
            content = content,
        )
    }
}

/** A thin progress bar in ink. */
@Composable
fun Bar(progress: Float, modifier: Modifier = Modifier, color: Color = S.c.moss) {
    Box(modifier.height(7.dp).border(1.dp, S.c.ink).padding(1.dp)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(5.dp).background(color))
    }
}

/** Draws with an android.graphics.Canvas: the art objects are written for it. */
@Composable
fun NativeCanvas(modifier: Modifier, draw: (android.graphics.Canvas, Float, Float) -> Unit) {
    Canvas(modifier) { drawContext.canvas.nativeCanvas.let { draw(it, size.width, size.height) } }
}

/** The current time, ticking every [period] ms (still for the renders). */
@Composable
fun rememberNow(period: Long): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(period) {
        if (Art.still) return@LaunchedEffect
        while (true) {
            delay(period)
            now = System.currentTimeMillis()
        }
    }
    return now
}

/** Keeps the screen on while shown: a hug, a recording, a sit. */
@Composable
fun KeepScreenOn() {
    val view: View = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
fun Gap(h: Int) = Spacer(Modifier.height(h.dp))

@Composable
fun Centered(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) = Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center, content = content)

/** A small graphite dot, the separator in typed lines. */
@Composable
fun Dot() = Canvas(Modifier.size(4.dp)) { drawCircle(Color(0xFF6F5F4A), radius = size.minDimension / 2, center = Offset(size.width / 2, size.height / 2)) }
