package app.heartwood.ui.art

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import app.heartwood.domain.BarkStyle
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * What graphite picks up when paper is rubbed over bark: the raised parts. Either drawn for the
 * species, or lifted from a photo of the real bark (the bright ridges against their surroundings).
 */
object BarkArt {
    const val GRAPHITE = 0xFF2A251F.toInt()

    /** The species' bark as dark marks on a transparent bitmap. */
    fun pattern(bark: BarkStyle, w: Int, h: Int, seed: Long): Bitmap {
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        val r = Random(seed)
        val s = w / 220f
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GRAPHITE; strokeCap = Paint.Cap.ROUND; style = Paint.Style.FILL }
        val line = Paint(p).apply { style = Paint.Style.STROKE }
        fun wander(step: Float, lw: ClosedFloatingPointRange<Float>, breaks: Float) {
            var x = 0f
            while (x < w) {
                var y = 0f
                var xx = x
                val path = Path().apply { moveTo(xx, y) }
                line.strokeWidth = (lw.start + r.nextFloat() * (lw.endInclusive - lw.start)) * s
                while (y < h) {
                    y += (8 + r.nextFloat() * 16) * s
                    xx += (r.nextFloat() - .5f) * 6 * s
                    if (r.nextFloat() < breaks) {
                        c.drawPath(path, line)
                        path.reset()
                        y += (6 + r.nextFloat() * 10) * s
                        path.moveTo(xx, y)
                    } else path.lineTo(xx, y)
                }
                c.drawPath(path, line)
                x += step * s * (.7f + r.nextFloat() * .6f)
            }
        }
        when (bark) {
            BarkStyle.FURROWS -> wander(13f, 4f..9f, .12f)
            BarkStyle.SHALLOW -> wander(9f, 2f..5f, .16f)
            BarkStyle.FINE -> wander(7f, 1.5f..3f, .1f)
            BarkStyle.LENTICELS -> {
                repeat(70) {
                    val x = r.nextFloat() * w
                    val y = r.nextFloat() * h
                    c.drawRect(x, y, x + (10 + r.nextFloat() * 34) * s, y + (1.5f + r.nextFloat() * 3) * s, p)
                }
                repeat(5) {
                    val x = r.nextFloat() * w
                    val y = r.nextFloat() * h
                    val k = (10 + r.nextFloat() * 16) * s
                    c.drawPath(Path().apply { moveTo(x, y - k); lineTo(x + k * .6f, y); lineTo(x, y + k); lineTo(x - k * .6f, y); close() }, p)
                }
            }
            BarkStyle.PLATES -> {
                var y = 0f
                while (y < h) {
                    var x = -10f * s
                    while (x < w) {
                        c.drawRoundRect(x + r.nextFloat() * 4 * s, y + r.nextFloat() * 3 * s, x + (18 + r.nextFloat() * 16) * s, y + (9 + r.nextFloat() * 8) * s, 3 * s, 3 * s, p)
                        x += (24 + r.nextFloat() * 20) * s
                    }
                    y += (14 + r.nextFloat() * 10) * s
                }
            }
            BarkStyle.SCALES -> {
                var y = 0f
                var row = 0
                while (y < h) {
                    var x = if (row % 2 == 0) 0f else -8f * s
                    while (x < w) {
                        c.drawOval(x, y, x + (12 + r.nextFloat() * 8) * s, y + (8 + r.nextFloat() * 6) * s, p)
                        x += (17 + r.nextFloat() * 6) * s
                    }
                    y += (11 + r.nextFloat() * 4) * s
                    row++
                }
            }
            BarkStyle.DIAMONDS -> {
                line.strokeWidth = 5 * s
                var i = -h.toFloat()
                while (i < w) {
                    c.drawLine(i, 0f, i + h * .6f, h.toFloat(), line)
                    c.drawLine(i + h * .6f, 0f, i, h.toFloat(), line)
                    i += (16 + r.nextFloat() * 8) * s
                }
            }
            BarkStyle.SMOOTH -> {
                repeat(40) {
                    val x = r.nextFloat() * w
                    val y = r.nextFloat() * h
                    c.drawRect(x, y, x + (4 + r.nextFloat() * 10) * s, y + 1.6f * s, p)
                }
                line.strokeWidth = 1.2f * s
                repeat(6) { val y = r.nextFloat() * h; c.drawLine(0f, y, w.toFloat(), y + (r.nextFloat() - .5f) * 10 * s, line) }
            }
        }
        return b
    }

    /**
     * A photo of bark as a rubbing mask: graphite where the bark is brighter than around it (the
     * raised parts), nothing in the dark cracks. [b] is scaled to w×h first.
     */
    fun fromPhoto(src: Bitmap, w: Int, h: Int): Bitmap {
        val scaled = Bitmap.createScaledBitmap(src, w, h, true)
        val px = IntArray(w * h)
        scaled.getPixels(px, 0, w, 0, 0, w, h)
        val g = FloatArray(w * h) { i -> val c = px[i]; (Color.red(c) * .3f + Color.green(c) * .59f + Color.blue(c) * .11f) / 255f }
        val blur = boxBlur(boxBlur(g, w, h, max(4, w / 40)), w, h, max(4, w / 40))
        var lo = 1f
        var hi = -1f
        val d = FloatArray(w * h) { i -> (g[i] - blur[i]).also { lo = min(lo, it); hi = max(hi, it) } }
        val span = max(.0001f, hi * .7f)
        val out = IntArray(w * h) { i ->
            val a = ((d[i] / span).coerceIn(0f, 1f) * 255).toInt()
            Color.argb(a, 0x2A, 0x25, 0x1F)
        }
        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun boxBlur(src: FloatArray, w: Int, h: Int, r: Int): FloatArray {
        val tmp = FloatArray(src.size)
        val out = FloatArray(src.size)
        for (y in 0 until h) {
            var sum = 0f
            for (x in -r..r) sum += src[y * w + x.coerceIn(0, w - 1)]
            for (x in 0 until w) {
                tmp[y * w + x] = sum / (2 * r + 1)
                sum += src[y * w + (x + r + 1).coerceIn(0, w - 1)] - src[y * w + (x - r).coerceIn(0, w - 1)]
            }
        }
        for (x in 0 until w) {
            var sum = 0f
            for (y in -r..r) sum += tmp[y.coerceIn(0, h - 1) * w + x]
            for (y in 0 until h) {
                out[y * w + x] = sum / (2 * r + 1)
                sum += tmp[(y + r + 1).coerceIn(0, h - 1) * w + x] - tmp[(y - r).coerceIn(0, h - 1) * w + x]
            }
        }
        return out
    }

    /** A finished rubbing for the demo and previews: the pattern pressed into paper. */
    fun rubbing(style: BarkStyle, size: Int, seed: Long): Bitmap {
        val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        c.drawColor(0xFFF1E7D1.toInt())
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 185 }
        c.drawBitmap(pattern(style, size, size, seed), 0f, 0f, p)
        val smudge = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GRAPHITE; alpha = 14 }
        c.drawOval(size * .03f, size * .06f, size * .97f, size * .94f, smudge)
        return b
    }
}
