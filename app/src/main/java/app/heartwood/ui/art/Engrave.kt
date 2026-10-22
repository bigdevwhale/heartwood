package app.heartwood.ui.art

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.min

/** Paper colours shared by the art. */
const val PAPER = 0xFFF1E7D1.toInt()
const val KRAFT = 0xFFD8C39D.toInt()

/**
 * A photo as an engraving: the picture sampled on a grid and redrawn as diagonal ink hatching,
 * crossed where it's darker. The tree's portrait is made this way from the first photo.
 */
object Engrave {
    fun of(src: Bitmap, w: Int, h: Int): Bitmap {
        val step = max(3, w / 110)
        val cols = (w + step - 1) / step
        val rows = (h + step - 1) / step
        // Cover-crop the photo onto the grid.
        val k = max(cols.toFloat() / src.width, rows.toFloat() / src.height)
        val small = Bitmap.createBitmap(cols, rows, Bitmap.Config.ARGB_8888)
        Canvas(small).apply {
            val iw = src.width * k
            val ih = src.height * k
            drawBitmap(src, null, android.graphics.RectF((cols - iw) / 2, (rows - ih) / 2, (cols + iw) / 2, (rows + ih) / 2), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        val px = IntArray(cols * rows)
        small.getPixels(px, 0, cols, 0, 0, cols, rows)
        var lo = 1f
        var hi = 0f
        val b = FloatArray(px.size) { i ->
            val c = px[i]
            ((Color.red(c) * .3f + Color.green(c) * .59f + Color.blue(c) * .11f) / 255f).also { lo = min(lo, it); hi = max(hi, it) }
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(PAPER)
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; strokeCap = Paint.Cap.ROUND }
        val s = step.toFloat()
        for (y in 0 until rows) for (x in 0 until cols) {
            val v = (b[y * cols + x] - lo) / max(.001f, hi - lo)
            val dk = 1 - v
            val X = x * s
            val Y = y * s
            if (dk > .22f) {
                ink.strokeWidth = dk * s * .48f
                c.drawLine(X, Y + s, X + s, Y, ink)
            }
            if (dk > .55f) {
                ink.strokeWidth = (dk - .4f) * s * .4f
                c.drawLine(X, Y, X + s, Y + s, ink)
            }
            if (dk > .82f) {
                ink.strokeWidth = s * .25f
                c.drawLine(X, Y + s / 2, X + s, Y + s / 2, ink)
            }
        }
        return out
    }
}
