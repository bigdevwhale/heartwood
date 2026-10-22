package app.heartwood.ui.theme

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.heartwood.R
import kotlin.random.Random

/**
 * A forester's notebook: kraft paper, graphite, a typewriter for the facts, a pencil hand for what
 * the trees say, moss for the thing to do, rust for a highlight. Only the daylight look.
 */
@Immutable
data class Palette(
    val kraft: Color,
    val kraftDark: Color,
    val paper: Color,
    val paper2: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val line: Color,
    /** Moss: the thing to do. */
    val moss: Color,
    val mossSoft: Color,
    val onMoss: Color,
    /** Rust: a highlighted ring, a stamp, a warning. */
    val rust: Color,
    val amber: Color,
    val soil: Color,
    val glow: Color,
    val tape: Color,
)

val Notebook = Palette(
    kraft = Color(0xFFD8C39D),
    kraftDark = Color(0xFFC4AB80),
    paper = Color(0xFFF1E7D1),
    paper2 = Color(0xFFE9DCC0),
    ink = Color(0xFF2F2A24),
    muted = Color(0xFF6F5F4A),
    faint = Color(0xFF9A8768),
    line = Color(0xFFB59D78),
    moss = Color(0xFF55703A),
    mossSoft = Color(0xFFDDE2C4),
    onMoss = Color(0xFFF6F1E4),
    rust = Color(0xFFA9533F),
    amber = Color(0xFFA8602A),
    soil = Color(0xFF231B14),
    glow = Color(0xFFE9D58A),
    tape = Color(0xCCE8DCB6),
)

val LocalPalette = staticCompositionLocalOf { Notebook }

/** PT Serif: headings and the body of the notebook. */
val Serif = FontFamily(
    Font(R.font.pt_serif_regular, FontWeight.Normal),
    Font(R.font.pt_serif_bold, FontWeight.Bold),
    Font(R.font.pt_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

/** PT Mono: the forester's typed facts — girth, dates, weather. */
val Mono = FontFamily(Font(R.font.pt_mono, FontWeight.Normal))

/** Caveat: what the trees say. */
val Hand = FontFamily(Font(R.font.caveat, FontWeight.Normal))

object S {
    val c: Palette @Composable get() = LocalPalette.current

    val RSm = RoundedCornerShape(3.dp)
    val RMd = RoundedCornerShape(4.dp)
    val RLg = RoundedCornerShape(8.dp)
}

object SType {
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 33.sp, color = S.c.ink)
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 24.sp, color = S.c.ink)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontSize = 16.sp, lineHeight = 23.sp, color = S.c.ink)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontSize = 14.sp, lineHeight = 20.sp, color = S.c.muted)
    val Typed: TextStyle @Composable get() = TextStyle(fontFamily = Mono, fontSize = 12.5.sp, lineHeight = 19.sp, color = S.c.muted)
    val Stamp: TextStyle @Composable get() = TextStyle(fontFamily = Mono, fontSize = 12.sp, letterSpacing = 0.12.em, color = S.c.amber)
    val Hand: TextStyle @Composable get() = TextStyle(fontFamily = app.heartwood.ui.theme.Hand, fontSize = 25.sp, lineHeight = 28.sp, color = S.c.ink)
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 16.sp)
}

/** The kraft paper behind every screen: fibres and specks, tiled. */
object Paper {
    private var tile: Bitmap? = null

    fun tile(): Bitmap = tile ?: run {
        val n = 256
        val b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        c.drawColor(0xFFD8C39D.toInt())
        val r = Random(5)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        repeat(260) {
            p.color = if (r.nextBoolean()) 0x16AA8E62 else 0x10F4E6C8
            p.strokeWidth = 1f + r.nextFloat()
            val x = r.nextFloat() * n
            val y = r.nextFloat() * n
            val l = 6 + r.nextFloat() * 22
            for (dx in intArrayOf(-n, 0, n)) for (dy in intArrayOf(-n, 0, n)) c.drawLine(x + dx, y + dy, x + dx + l, y + dy + (r.nextFloat() - .5f) * 3, p)
        }
        repeat(500) {
            p.color = 0x22A98D60
            c.drawCircle(r.nextFloat() * n, r.nextFloat() * n, .6f + r.nextFloat() * .8f, p)
        }
        b.also { tile = it }
    }
}

fun Modifier.kraft(): Modifier = drawBehind {
    drawRect(ShaderBrush(ImageShader(Paper.tile().asImageBitmap(), TileMode.Repeated, TileMode.Repeated)))
}

@Composable
fun HeartwoodTheme(content: @Composable () -> Unit) {
    val p = Notebook
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = p.moss,
                onPrimary = p.onMoss,
                primaryContainer = p.mossSoft,
                onPrimaryContainer = p.ink,
                secondary = p.rust,
                background = p.kraft,
                surface = p.paper,
                surfaceContainer = p.paper,
                surfaceContainerHigh = p.paper,
                surfaceContainerHighest = p.paper2,
                surfaceContainerLow = p.paper,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.muted,
                outline = p.line,
                outlineVariant = p.line,
            ),
            typography = MaterialTheme.typography.copy(
                bodyLarge = SType.Body,
                bodyMedium = SType.Body.copy(fontSize = 15.sp),
                labelLarge = SType.Button,
            ),
            content = content,
        )
    }
}
