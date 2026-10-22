package app.heartwood.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.heartwood.ui.components.ru
import app.heartwood.ui.theme.Mono

enum class Tab { GROVE, BOOK }

/** The two places at the bottom: the grove and the forester's notebook. */
@Composable
fun TabBar(current: Tab, go: Go) {
    val ru = ru()
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF3A3027)).navigationBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (t in Tab.entries) {
            val on = t == current
            Column(
                Modifier.weight(1f).clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .background(if (on) Color(0xFF54463A) else Color.Transparent)
                    .clickable(enabled = !on) { if (t == Tab.GROVE) go.grove() else go.book() }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val tint = Color(0xFFE6D6B8).copy(alpha = if (on) 1f else .6f)
                Canvas(Modifier.size(24.dp)) { if (t == Tab.GROVE) grove(tint) else book(tint) }
                Text(
                    if (t == Tab.GROVE) (if (ru) "Роща" else "Grove") else (if (ru) "Тетрадь" else "Notebook"),
                    style = androidx.compose.ui.text.TextStyle(fontFamily = Mono, fontSize = 12.sp, color = tint),
                )
            }
        }
    }
}

private fun DrawScope.grove(c: Color) {
    val s = size.width / 24f
    val st = Stroke(1.6f * s, cap = StrokeCap.Round)
    drawCircle(c, 4 * s, Offset(7 * s, 9 * s), style = st)
    drawCircle(c, 4.5f * s, Offset(16 * s, 7 * s), style = st)
    drawLine(c, Offset(7 * s, 13 * s), Offset(7 * s, 21 * s), 1.6f * s, StrokeCap.Round)
    drawLine(c, Offset(16 * s, 11.5f * s), Offset(16 * s, 21 * s), 1.6f * s, StrokeCap.Round)
    drawLine(c, Offset(3 * s, 21 * s), Offset(21 * s, 21 * s), 1.6f * s, StrokeCap.Round)
}

private fun DrawScope.book(c: Color) {
    val s = size.width / 24f
    val st = Stroke(1.6f * s, cap = StrokeCap.Round)
    drawRect(c, Offset(4 * s, 4 * s), androidx.compose.ui.geometry.Size(16 * s, 16 * s), style = st)
    drawLine(c, Offset(12 * s, 4 * s), Offset(12 * s, 20 * s), 1.6f * s)
    drawLine(c, Offset(14.5f * s, 8 * s), Offset(18 * s, 8 * s), 1.2f * s)
    drawLine(c, Offset(14.5f * s, 11 * s), Offset(18 * s, 11 * s), 1.2f * s)
}
