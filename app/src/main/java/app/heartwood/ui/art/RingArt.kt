package app.heartwood.ui.art

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import app.heartwood.domain.Ring
import app.heartwood.domain.Season
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * The friendship as a sawn cross-section: bark round the edge, the first meeting as the pith, and a
 * ring for every day you met, widest for the days you hugged or sat. A ring's line is the season's
 * colour; a hug warms the wood; a long absence leaves a dark line, like a dry year.
 */
object RingArt {
    fun seasonColour(s: Season) = when (s) {
        Season.SPRING -> 0xFF7F9A55.toInt()
        Season.SUMMER -> 0xFF5F7D3C.toInt()
        Season.AUTUMN -> 0xFFA8602A.toInt()
        Season.WINTER -> 0xFF5F7488.toInt()
    }

    const val HIGHLIGHT = 0xFFA9533F.toInt()

    private fun shape(t: Float, s: Float) = (.035 * sin(3 * t + s) + .026 * sin(5 * t + s * 1.7) + .018 * sin(2 * t + s * .3) + .01 * sin(9 * t + s * 2.1)).toFloat()

    /** Where each ring ends, as a radius in a square of [size]: [r0] the pith, then one per ring. */
    class Layout(val size: Float, val seed: Long, val r0: Float, val radii: FloatArray) {
        val c = size / 2
        val big = size / 2 - size * .015f
    }

    fun layout(rings: List<Ring>, size: Float, seed: Long): Layout {
        val big = size / 2 - size * .015f
        val total = rings.sumOf { it.width.toDouble() }.toFloat().coerceAtLeast(1f)
        // A young friendship isn't stretched to the bark: few rings sit near the middle.
        val fill = min(1f, .25f + rings.size / 40f)
        val r0 = big * .06f
        val outer = r0 + (big * .88f - r0) * fill
        var acc = r0
        val radii = FloatArray(rings.size) { i -> acc += rings[i].width / total * (outer - r0); acc }
        return Layout(size, seed, r0, radii)
    }

    private fun ringPath(l: Layout, rad: Float, out: Path) {
        out.reset()
        val s = (l.seed % 1000).toFloat()
        for (i in 0..160) {
            val t = (i / 160f * 2 * PI).toFloat()
            val rr = rad * (1 + shape(t, s) * min(1f, rad / l.big * 1.6f))
            val x = l.c + cos(t) * rr
            val y = l.c + sin(t) * rr
            if (i == 0) out.moveTo(x, y) else out.lineTo(x, y)
        }
        out.close()
    }

    /** [highlight] is a ring index, -1 for the pith, null for none. */
    fun draw(c: Canvas, rings: List<Ring>, l: Layout, highlight: Int?) {
        val r = Random(l.seed)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val path = Path()
        val big = l.big
        val outerRing = l.radii.lastOrNull() ?: l.r0

        // Bark.
        ringPath(l, big, path)
        p.style = Paint.Style.FILL
        p.color = 0xFF4A3626.toInt()
        c.drawPath(path, p)
        c.save()
        c.clipPath(path)
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        repeat(420) {
            val t = r.nextFloat() * 2 * PI.toFloat()
            val a = big * (.89f + r.nextFloat() * .1f)
            val b = a + big * (.02f + r.nextFloat() * .04f)
            p.color = if (r.nextBoolean()) 0xFF2C2017.toInt() else 0xFF6B5039.toInt()
            p.strokeWidth = big * (.004f + r.nextFloat() * .008f)
            c.drawLine(l.c + cos(t) * a, l.c + sin(t) * a, l.c + cos(t + .01f) * b, l.c + sin(t + .01f) * b, p)
        }
        c.restore()

        // Sapwood between the last ring and the bark: the years still to come.
        ringPath(l, big * .885f, path)
        p.style = Paint.Style.FILL
        p.color = 0xFFDCC395.toInt()
        c.drawPath(path, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = big * .004f
        p.color = 0x22000000
        var g = outerRing + big * .02f
        while (g < big * .87f) {
            ringPath(l, g, path)
            c.drawPath(path, p)
            g += big * (.012f + r.nextFloat() * .01f)
        }
        ringPath(l, outerRing + big * .012f, path)
        p.style = Paint.Style.FILL
        p.color = 0xFFC79B62.toInt()
        c.drawPath(path, p)

        for (i in rings.indices.reversed()) {
            val ring = rings[i]
            ringPath(l, l.radii[i], path)
            p.style = Paint.Style.FILL
            p.color = if (ring.hug) Color.HSVToColor(floatArrayOf(18f, .3f, .92f + r.nextFloat() * .04f))
            else Color.HSVToColor(floatArrayOf(37f, .3f + r.nextFloat() * .06f, .88f + r.nextFloat() * .05f))
            c.drawPath(path, p)
            p.style = Paint.Style.STROKE
            p.color = seasonColour(ring.season)
            p.strokeWidth = big * .0075f
            c.drawPath(path, p)
            if (ring.absence && i > 0) {
                ringPath(l, (l.radii[i - 1] + l.radii[i]) / 2, path)
                p.color = INK
                p.strokeWidth = big * .007f
                c.drawPath(path, p)
            }
        }
        if (highlight != null && highlight >= 0 && highlight < l.radii.size) {
            ringPath(l, l.radii[highlight], path)
            p.style = Paint.Style.STROKE
            p.color = HIGHLIGHT
            p.strokeWidth = big * .018f
            c.drawPath(path, p)
        }
        ringPath(l, l.r0, path)
        p.style = Paint.Style.FILL
        p.color = if (highlight == -1) HIGHLIGHT else 0xFF7A4F2D.toInt()
        c.drawPath(path, p)

        // A radial check, as sawn wood cracks.
        p.style = Paint.Style.STROKE
        p.color = 0xFF3B2A1C.toInt()
        p.strokeWidth = big * .007f
        var t = (l.seed % 6).toFloat()
        var rr = big * .97f
        path.reset()
        path.moveTo(l.c + cos(t) * rr, l.c + sin(t) * rr)
        repeat(6) {
            rr -= big * .035f
            t += (r.nextFloat() - .5f) * .04f
            path.lineTo(l.c + cos(t) * rr, l.c + sin(t) * rr)
        }
        c.drawPath(path, p)
    }

    /** Which ring a tap at (x, y) falls on: an index, -1 for the pith, null outside the rings. */
    fun hit(l: Layout, x: Float, y: Float): Int? {
        val dx = x - l.c
        val dy = y - l.c
        val d = hypot(dx, dy)
        val t = atan2(dy, dx)
        val rad = d / (1 + shape(t, (l.seed % 1000).toFloat()) * min(1f, d / l.big * 1.6f))
        if (rad <= l.r0) return -1
        val i = l.radii.indexOfFirst { rad <= it }
        return if (i >= 0) i else if (l.radii.isNotEmpty() && rad <= l.radii.last() + l.big * .03f) l.radii.size - 1 else null
    }

    fun bitmap(rings: List<Ring>, size: Int, seed: Long, highlight: Int?): Pair<Bitmap, Layout> {
        val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val l = layout(rings, size.toFloat(), seed)
        draw(Canvas(b), rings, l, highlight)
        return b to l
    }
}
