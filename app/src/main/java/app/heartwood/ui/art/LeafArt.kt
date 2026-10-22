package app.heartwood.ui.art

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import androidx.core.graphics.PathParser
import app.heartwood.domain.LeafShape
import app.heartwood.domain.Species

/** Pressed leaves for the herbarium and the species key, drawn in a 100×140 box with the stalk at the bottom. */
object LeafArt {
    private val SIMPLE = mapOf(
        LeafShape.OAK to "M50 135 L50 120 C40 118 30 112 34 104 C22 104 18 94 28 88 C16 86 14 74 26 70 C16 64 20 52 32 52 C26 42 34 30 44 34 C42 22 48 12 50 8 C52 12 58 22 56 34 C66 30 74 42 68 52 C80 52 84 64 74 70 C86 74 84 86 72 88 C82 94 78 104 66 104 C70 112 60 118 50 120 Z",
        LeafShape.BIRCH to "M50 134 L50 118 C30 112 18 92 22 70 C26 48 40 28 50 12 C60 28 74 48 78 70 C82 92 70 112 50 118 Z",
        LeafShape.MAPLE to "M50 134 L50 112 L30 118 L34 104 L12 100 L22 88 L6 70 L26 70 L22 50 L38 60 L36 34 L45 42 L50 14 L55 42 L64 34 L62 60 L78 50 L74 70 L94 70 L78 88 L88 100 L66 104 L70 118 L50 112 Z",
        LeafShape.LINDEN to "M50 134 L50 116 C28 116 10 98 14 74 C18 54 34 48 46 56 C42 40 46 26 52 14 C60 30 76 44 84 62 C92 84 76 112 50 116 Z",
        LeafShape.WILLOW to "M50 136 L50 124 C42 100 40 60 50 8 C60 60 58 100 50 124 Z",
        LeafShape.OVAL to "M50 134 L50 116 C34 112 24 96 24 74 C24 50 36 28 50 14 C64 28 76 50 76 74 C76 96 66 112 50 116 Z",
        LeafShape.POPLAR to "M50 134 L50 112 C30 112 12 100 14 84 C16 64 34 40 50 22 C66 40 84 64 86 84 C88 100 70 112 50 112 Z",
    )
    private const val LEAFLET = "M0 0 C-9 -12 -10 -36 0 -50 C10 -36 9 -12 0 0 Z"
    private const val PINNA = "M0 0 C4 -5 14 -6 22 0 C14 6 4 5 0 0 Z"

    private val parsed = HashMap<String, Path>()
    private fun path(d: String): Path = parsed.getOrPut(d) { PathParser.createPathFromPathData(d) }

    fun colour(sp: Species, month: Int): Int = when {
        sp.evergreen -> 0xFF4F6B3A.toInt()
        month in 3..5 -> 0xFF9CBF63.toInt()
        month in 6..8 -> 0xFF5E8A3A.toInt()
        else -> sp.autumn.first().toInt()
    }

    /** Draws the leaf into a w×h box, turned by [rotation] degrees. */
    fun draw(c: Canvas, w: Float, h: Float, shape: LeafShape, colour: Int, rotation: Float = 0f) {
        val s = minOf(w / 100f, h / 140f)
        c.save()
        c.translate((w - 100 * s) / 2, (h - 140 * s) / 2)
        c.scale(s, s)
        c.rotate(rotation, 50f, 70f)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colour; style = Paint.Style.FILL }
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; style = Paint.Style.STROKE; strokeWidth = 1.2f; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND }
        val vein = Paint(ink).apply { strokeWidth = .7f; alpha = 150 }
        when (shape) {
            LeafShape.NEEDLES -> {
                val p = Paint(fill).apply { style = Paint.Style.STROKE; strokeWidth = 3f; strokeCap = Paint.Cap.ROUND }
                c.drawPath(Path().apply { moveTo(50f, 130f); cubicTo(46f, 90f, 40f, 50f, 30f, 12f) }, p)
                c.drawPath(Path().apply { moveTo(50f, 130f); cubicTo(54f, 90f, 62f, 50f, 72f, 14f) }, p)
                p.color = 0xFF6B5039.toInt(); p.strokeWidth = 6f
                c.drawLine(46f, 132f, 54f, 132f, p)
            }
            LeafShape.SPRIG -> {
                val p = Paint(fill).apply { style = Paint.Style.STROKE; strokeWidth = 2.2f; strokeCap = Paint.Cap.ROUND }
                val twig = Paint(ink).apply { color = 0xFF7A5A3C.toInt(); strokeWidth = 2.6f }
                c.drawLine(50f, 134f, 50f, 10f, twig)
                var y = 120f
                while (y > 14f) {
                    val l = 12f + (y / 134f) * 6f
                    c.drawLine(50f, y, 50f - l, y - 7f, p)
                    c.drawLine(50f, y, 50f + l, y - 7f, p)
                    y -= 5f
                }
            }
            LeafShape.HAND -> {
                val m = Matrix()
                for ((i, a) in floatArrayOf(-78f, -50f, -24f, 0f, 24f, 50f, 78f).withIndex()) {
                    val k = 1f - kotlin.math.abs(i - 3) * .14f
                    val p = Path(path(LEAFLET))
                    m.reset(); m.setScale(k * 1.1f, k * 1.6f); m.postRotate(a); m.postTranslate(50f, 100f)
                    p.transform(m)
                    c.drawPath(p, fill); c.drawPath(p, ink)
                }
                c.drawLine(50f, 100f, 50f, 136f, ink)
            }
            LeafShape.FEATHER -> {
                c.drawLine(50f, 136f, 50f, 16f, ink)
                val m = Matrix()
                var y = 116f
                while (y > 26f) {
                    for (side in intArrayOf(-1, 1)) {
                        val p = Path(path(PINNA))
                        m.reset(); m.setScale(side * 1.25f, 1.1f); m.postRotate(-side * 18f); m.postTranslate(50f, y)
                        p.transform(m)
                        c.drawPath(p, fill); c.drawPath(p, ink)
                    }
                    y -= 15f
                }
                val tip = Path(path(PINNA))
                m.reset(); m.setScale(1.25f, 1.1f); m.postRotate(-90f); m.postTranslate(50f, 30f)
                tip.transform(m)
                c.drawPath(tip, fill); c.drawPath(tip, ink)
            }
            else -> {
                val p = path(SIMPLE.getValue(shape))
                c.drawPath(p, fill)
                c.drawPath(p, ink)
                c.drawLine(50f, 134f, 50f, 112f, vein)
                c.save()
                c.clipPath(p)
                c.drawLine(50f, 134f, 50f, 20f, vein)
                var y = 100f
                while (y > 40f) {
                    c.drawLine(50f, y, 32f, y - 14f, vein)
                    c.drawLine(50f, y, 68f, y - 14f, vein)
                    y -= 20f
                }
                c.restore()
            }
        }
        c.restore()
    }
}
