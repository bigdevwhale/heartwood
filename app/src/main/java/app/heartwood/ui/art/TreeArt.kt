package app.heartwood.ui.art

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.LruCache
import app.heartwood.domain.Form
import app.heartwood.domain.Species
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

const val INK = 0xFF2F2A24.toInt()
const val SNOW = 0xFFFBF8F1.toInt()

/**
 * A tree as an ink engraving on paper: graphite branches grown from a seed, a wash of leaves in the
 * month's colours, stipple, snow in winter, fallen leaves in autumn. The same tree (same seed) looks
 * like itself all year, which is what the chronicle and the grove map rely on.
 */
object TreeArt {
    private class Seg(val x: Float, val y: Float, val x2: Float, val y2: Float, val w: Float, val cx: Float, val cy: Float)
    private class End(val x: Float, val y: Float, val small: Boolean = false)

    private class Params(val depth: Int, val ratio: Float, val spread: Float, val jit: Float, val trunk: Float, val w: Float, val wob: Float, val grav: Float = 0f)

    private fun params(f: Form) = when (f) {
        Form.BROAD -> Params(7, .72f, .62f, .55f, .26f, .062f, .4f)
        Form.ROUND -> Params(7, .75f, .5f, .35f, .28f, .048f, .15f)
        Form.OVAL -> Params(7, .77f, .38f, .3f, .3f, .048f, .1f)
        Form.SLENDER -> Params(7, .79f, .3f, .28f, .33f, .032f, .12f, .14f)
        Form.WEEPING -> Params(5, .72f, .65f, .4f, .26f, .06f, .25f)
                Form.SMALL -> Params(6, .72f, .58f, .6f, .2f, .05f, .45f)
        Form.PINE, Form.SPRUCE, Form.COLUMN -> Params(0, 0f, 0f, 0f, 0f, 0f, 0f)
    }

    /** How leafy a deciduous crown is in a month, and its colours. */
    private fun leafiness(month: Int) = when (month) {
        3 -> .12f
        4 -> .5f
        5 -> .85f
        in 6..8 -> 1f
        9 -> .95f
        10 -> .75f
        11 -> .25f
        else -> 0f
    }

    private val SPRING = intArrayOf(0xFFA8C27A.toInt(), 0xFFBCD38E.toInt(), 0xFF93B366.toInt())
    private val SUMMER = intArrayOf(0xFF5E7F3C.toInt(), 0xFF77984F.toInt(), 0xFF4C6A33.toInt())
    private val CONIFER = intArrayOf(0xFF3F5A32.toInt(), 0xFF4F6B3A.toInt(), 0xFF5C7A46.toInt())

    fun crownColours(sp: Species, month: Int): IntArray {
        if (sp.evergreen) return CONIFER
        val autumn = sp.autumn.map { it.toInt() }.toIntArray()
        return when (month) {
            3, 4, 5 -> SPRING
            in 6..8 -> SUMMER
            9 -> intArrayOf(SUMMER[0], autumn[0], SUMMER[1], autumn.getOrElse(1) { autumn[0] })
            10, 11 -> autumn
            else -> autumn
        }
    }

    /**
     * Draws the tree standing on a ground line at the bottom of a w×h box. [dx], [dy], [sc] shift
     * and scale it (the chronicle's ghost frames); [alpha] fades the whole drawing.
     */
    fun draw(c: Canvas, w: Float, h: Float, sp: Species, month: Int, seed: Long, dx: Float = 0f, dy: Float = 0f, sc: Float = 1f, snow: Boolean = month == 12 || month <= 2, alpha: Int = 255) {
        val r = Random(seed)
        val u = min(w * .95f, h * .82f)
        val thin = u * .0028f
        val segs = ArrayList<Seg>()
        val ends = ArrayList<End>()
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; this.alpha = alpha }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        c.save()
        c.translate(w / 2 + dx, h * .9f + dy)
        if (sc != 1f) c.scale(sc, sc)

        // Ground.
        ink.strokeWidth = max(thin, u * .004f)
        val ground = Path()
        var gx = -w * .42f
        ground.moveTo(gx, 0f)
        while (gx <= w * .42f) {
            ground.lineTo(gx, sin(gx / u * 18 + seed % 7) * u * .006f)
            gx += u * .02f
        }
        c.drawPath(ground, ink)
        ink.strokeWidth = thin
        repeat(26) {
            val x = (r.nextFloat() - .5f) * w * .8f
            c.drawLine(x, 0f, x + (r.nextFloat() - .5f) * u * .015f, -u * (.01f + r.nextFloat() * .02f), ink)
        }

        when (sp.form) {
            Form.PINE -> pine(r, u, segs, ends)
            Form.SPRUCE -> spruce(r, u, segs, ends)
            Form.COLUMN -> poplar(r, u, segs, ends)
            else -> {
                val p = params(sp.form)
                fun br(x: Float, y: Float, len: Float, a: Float, bw: Float, d: Int) {
                    val x2 = x + cos(a) * len
                    val y2 = y + sin(a) * len
                    val b = (r.nextFloat() - .5f) * p.wob * len
                    segs += Seg(x, y, x2, y2, bw, (x + x2) / 2 - sin(a) * b, (y + y2) / 2 + cos(a) * b)
                    if (d == 0) {
                        ends += End(x2, y2)
                        return
                    }
                    val n = if (r.nextFloat() < .35f) 3 else 2
                    for (i in 0 until n) {
                        var na = a + (i.toFloat() / (n - 1) - .5f) * 2 * p.spread + (r.nextFloat() - .5f) * p.jit
                        if (p.grav > 0 && d < 4) na += p.grav * sign(cos(na)) * (4 - d)
                        br(x2, y2, len * p.ratio * (.85f + r.nextFloat() * .3f), na, bw * .66f, d - 1)
                    }
                }
                br(0f, 0f, u * p.trunk, (-Math.PI / 2).toFloat() + (r.nextFloat() - .5f) * .08f, u * p.w, p.depth)
                if (sp.form == Form.WEEPING) {
                    val hang = ArrayList(ends)
                    ends.clear()
                    for (e in hang) {
                        val l = u * (.2f + r.nextFloat() * .3f)
                        val dir = if (e.x > 0) 1f else -1f
                        val x2 = e.x + dir * l * .18f
                        val y2 = min(-2f, e.y + l)
                        val cx = e.x + dir * l * .22f
                        val cy = e.y + l * .15f
                        segs += Seg(e.x, e.y, x2, y2, thin * 1.2f, cx, cy)
                        var t = .15f
                        while (t <= 1f) {
                            val q = 1 - t
                            ends += End(q * q * e.x + 2 * q * t * cx + t * t * x2, q * q * e.y + 2 * q * t * cy + t * t * y2, small = true)
                            t += .17f
                        }
                    }
                }
            }
        }

        // Fit the crown into the box: tall and wide trees are scaled down, never up.
        var top = 0f
        var side = 0f
        for (s in segs) { top = min(top, min(s.y2, s.cy)); side = max(side, max(abs(s.x2), abs(s.cx))) }
        for (e in ends) { top = min(top, e.y - u * .03f); side = max(side, abs(e.x) + u * .03f) }
        val fit = min(1f, min(h * .86f / -top.coerceAtMost(-1f), w * .48f / side.coerceAtLeast(1f)))
        c.scale(fit, fit)
        val thinFit = thin / fit

        // Branches.
        val path = Path()
        for (s in segs) {
            ink.strokeWidth = max(thinFit, s.w)
            path.reset()
            path.moveTo(s.x, s.y)
            path.quadTo(s.cx, s.cy, s.x2, s.y2)
            c.drawPath(path, ink)
        }
        if (sp == Species.BIRCH) birchBark(c, r, segs, u, alpha)
        else if (segs.isNotEmpty()) {
            // Light hatching down the trunk, as an engraver would cut it.
            val t = segs[0]
            ink.color = 0xAAF1E7D1.toInt()
            ink.alpha = alpha * 170 / 255
            ink.strokeWidth = thin
            for (i in 0 until 14) {
                val y = t.y + (t.y2 - t.y) * (i / 14f)
                c.drawLine(t.w * .05f, y, t.w * .38f, y - u * .008f, ink)
            }
            ink.color = INK
            ink.alpha = alpha
        }

        // Leaves.
        val leafy = if (sp.evergreen) 1f else leafiness(month)
        val cols = crownColours(sp, month)
        if (leafy > 0f) {
            val n = when {
                sp.evergreen -> 4
                month in 6..8 -> 5
                month == 3 -> 1
                month in 4..5 -> 3
                else -> 4
            }
            val rad = u * when {
                sp.evergreen -> .026f
                month == 3 -> .008f
                month in 4..5 -> .02f
                else -> .028f
            }
            fill.alpha = alpha * (if (sp.evergreen) 140 else 107) / 255
            for (e in ends) {
                if (!sp.evergreen && r.nextFloat() > leafy) continue
                val rr = if (e.small) rad * .5f else rad
                for (k in 0 until n) {
                    fill.color = cols[k % cols.size]
                    fill.alpha = alpha * (if (sp.evergreen) 140 else 107) / 255
                    c.drawCircle(e.x + (r.nextFloat() - .5f) * rr * 2.4f, e.y + (r.nextFloat() - .5f) * rr * 2.4f, rr * (.5f + r.nextFloat() * .6f), fill)
                }
            }
            fill.color = INK
            fill.alpha = alpha
            for (e in ends) if (r.nextFloat() < .45f * leafy) c.drawCircle(e.x + (r.nextFloat() - .5f) * rad * 2, e.y + (r.nextFloat() - .5f) * rad * 2, u * .004f, fill)
        }
        extras(c, r, sp, month, ends, u, alpha, fill)

        // Fallen leaves.
        if (!sp.evergreen && month in 10..11) {
            repeat(if (month == 11) 50 else 34) { i ->
                fill.color = cols[i % cols.size]
                fill.alpha = alpha
                c.save()
                c.translate((r.nextFloat() - .5f) * w * .7f, -u * .003f - r.nextFloat() * u * .015f)
                c.rotate(r.nextFloat() * 180)
                c.drawOval(-u * .012f, -u * .006f, u * .012f, u * .006f, fill)
                c.restore()
            }
        }
        if (snow) {
            ink.color = SNOW
            ink.alpha = alpha
            for (s in segs) {
                if (s.w < u * .006f) continue
                val a = abs(atan2(s.y2 - s.y, s.x2 - s.x))
                if (a > 1.2f && a < 1.95f) continue
                ink.strokeWidth = max(u * .006f, s.w * .5f)
                path.reset()
                path.moveTo(s.x, s.y - s.w * .45f)
                path.quadTo(s.cx, s.cy - s.w * .45f, s.x2, s.y2 - s.w * .45f)
                c.drawPath(path, ink)
            }
            if (sp.evergreen) {
                fill.color = SNOW
                fill.alpha = alpha
                for (e in ends) if (r.nextFloat() < .55f) c.drawOval(e.x - u * .022f, e.y - u * .02f, e.x + u * .022f, e.y - u * .006f, fill)
            }
            fill.color = SNOW
            fill.alpha = alpha
            c.drawArc(-w * .42f, -u * .025f, w * .42f, u * .025f, 180f, 180f, true, fill)
        }
        c.restore()
    }

    private fun pine(r: Random, u: Float, segs: MutableList<Seg>, ends: MutableList<End>) {
        val h = u * .98f
        var y = 0f
        var w = u * .042f
        for (i in 0 until 8) {
            val y2 = -h * (i + 1) / 8
            segs += Seg((r.nextFloat() - .5f) * u * .006f, y, (r.nextFloat() - .5f) * u * .006f, y2, w, 0f, (y + y2) / 2)
            y = y2
            w *= .82f
        }
        var ty = -h * .36f
        while (ty > -h * .98f) {
            val k = -ty / h
            val len = u * (.36f * (1 - k) + .05f) * (.75f + r.nextFloat() * .5f)
            for (side in intArrayOf(-1, 1)) {
                val a = if (side > 0) .12f + r.nextFloat() * .25f else (Math.PI.toFloat() - .12f - r.nextFloat() * .25f)
                val x2 = cos(a) * len
                val y2 = ty + sin(a) * len
                segs += Seg(0f, ty, x2, y2, max(u * .003f, u * .008f * (1 - k)), x2 * .5f, ty + (y2 - ty) * .3f - len * .08f)
                var t = .35f
                while (t <= 1f) {
                    ends += End(x2 * t, ty + (y2 - ty) * t - u * .008f)
                    t += .22f
                }
            }
            ty -= h * (.045f + r.nextFloat() * .03f)
        }
    }

    private fun spruce(r: Random, u: Float, segs: MutableList<Seg>, ends: MutableList<End>) {
        val h = u * 1.0f
        var y = 0f
        var w = u * .036f
        for (i in 0 until 8) {
            val y2 = -h * (i + 1) / 8
            segs += Seg(0f, y, 0f, y2, w, 0f, (y + y2) / 2)
            y = y2
            w *= .8f
        }
        var ty = -h * .07f
        while (ty > -h * .99f) {
            val k = -ty / h
            val len = u * (.34f * (1 - k) + .025f) * (.85f + r.nextFloat() * .3f)
            for (side in intArrayOf(-1, 1)) {
                // Spruce branches droop and turn up at the tips.
                val a = if (side > 0) .3f + r.nextFloat() * .25f else (Math.PI.toFloat() - .3f - r.nextFloat() * .25f)
                val x2 = cos(a) * len
                val y2 = ty + sin(a) * len * .7f
                segs += Seg(0f, ty, x2, y2, max(u * .003f, u * .006f * (1 - k)), x2 * .6f, ty + len * .25f)
                var t = .2f
                while (t <= 1f) {
                    ends += End(x2 * t, ty + (y2 - ty) * t + len * .1f * sin(t * 3.1f) - u * .006f)
                    t += .16f
                }
            }
            ty -= h * (.035f + r.nextFloat() * .02f)
        }
    }

    /** A Lombardy poplar: one straight trunk, short branches all the way up, every one reaching for the sky. */
    private fun poplar(r: Random, u: Float, segs: MutableList<Seg>, ends: MutableList<End>) {
        val h = u * 1.0f
        var y = 0f
        var w = u * .045f
        for (i in 0 until 8) {
            val y2 = -h * (i + 1) / 8
            segs += Seg((r.nextFloat() - .5f) * u * .008f, y, (r.nextFloat() - .5f) * u * .008f, y2, w, 0f, (y + y2) / 2)
            y = y2
            w *= .8f
        }
        var ty = -h * .12f
        while (ty > -h * .97f) {
            val k = -ty / h
            val len = u * (.15f * sin(k * Math.PI.toFloat()) + .04f) * (.8f + r.nextFloat() * .4f)
            for (side in intArrayOf(-1, 1)) {
                val a = -Math.PI.toFloat() / 2 + side * (.28f + r.nextFloat() * .22f)
                val x2 = cos(a) * len
                val y2 = ty + sin(a) * len
                segs += Seg(0f, ty, x2, y2, max(u * .003f, u * .007f * (1 - k)), x2 * .6f, ty + (y2 - ty) * .4f)
                var t = .3f
                while (t <= 1f) {
                    ends += End(x2 * t, ty + (y2 - ty) * t)
                    t += .25f
                }
            }
            ty -= h * (.03f + r.nextFloat() * .02f)
        }
    }

    private fun birchBark(c: Canvas, r: Random, segs: List<Seg>, u: Float, alpha: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
        val path = Path()
        for (s in segs) {
            if (s.w < u * .012f) continue
            p.color = 0xFFF4EFE6.toInt()
            p.alpha = alpha
            p.strokeWidth = s.w * .62f
            path.reset()
            path.moveTo(s.x, s.y)
            path.quadTo(s.cx, s.cy, s.x2, s.y2)
            c.drawPath(path, p)
            p.color = INK
            p.alpha = alpha
            p.strokeWidth = u * .006f
            val len = hypot(s.x2 - s.x, s.y2 - s.y)
            val a = atan2(s.y2 - s.y, s.x2 - s.x) + Math.PI.toFloat() / 2
            repeat((len / (u * .025f)).toInt()) {
                if (r.nextFloat() < .5f) return@repeat
                val t = r.nextFloat()
                val x = s.x + (s.x2 - s.x) * t
                val y = s.y + (s.y2 - s.y) * t
                val l = s.w * .3f * (.3f + r.nextFloat() * .7f)
                c.drawLine(x - cos(a) * l * .2f, y - sin(a) * l * .2f, x + cos(a) * l, y + sin(a) * l, p)
            }
        }
    }

    /** Blossom, catkins, berries, apples and chestnut candles, in their months. */
    private fun extras(c: Canvas, r: Random, sp: Species, month: Int, ends: List<End>, u: Float, alpha: Int, fill: Paint) {
        fun dots(colour: Int, chance: Float, rad: Float) {
            fill.color = colour
            fill.alpha = alpha
            for (e in ends) if (r.nextFloat() < chance) c.drawCircle(e.x + (r.nextFloat() - .5f) * u * .03f, e.y + (r.nextFloat() - .5f) * u * .03f, rad, fill)
        }
        when (sp) {
            Species.APPLE -> when (month) {
                5 -> { dots(0xFFF7E6E8.toInt(), .7f, u * .012f); dots(0xFFE9B7C0.toInt(), .35f, u * .008f) }
                8, 9 -> dots(0xFFC0392B.toInt(), .18f, u * .011f)
            }
            Species.ROWAN -> when (month) {
                6 -> dots(0xFFF4EEDC.toInt(), .4f, u * .01f)
                8, 9, 10, 11, 12, 1 -> { dots(0xFFC2361F.toInt(), if (month in 8..10) .35f else .2f, u * .007f); dots(0xFFE0502A.toInt(), .2f, u * .005f) }
            }
            Species.CHESTNUT -> if (month == 5) {
                fill.color = 0xFFF6F0E2.toInt()
                fill.alpha = alpha
                for (e in ends) if (r.nextFloat() < .3f) c.drawOval(e.x - u * .006f, e.y - u * .03f, e.x + u * .006f, e.y, fill)
            }
            Species.WILLOW -> if (month in 3..4) dots(0xFFD8D6C8.toInt(), .35f, u * .006f)
            Species.LINDEN -> if (month == 7) dots(0xFFE6DC8A.toInt(), .3f, u * .007f)
            Species.SPRUCE, Species.PINE -> if (month in 5..6) dots(0xFF9CC56A.toInt(), .4f, u * .007f)
            else -> Unit
        }
    }

    private val cache = LruCache<String, Bitmap>(48)

    /** The tree on a transparent bitmap, cached: for lists, map pins and portraits. */
    fun bitmap(sp: Species, month: Int, seed: Long, w: Int, h: Int): Bitmap {
        val key = "${sp.name}/$month/$seed/$w/$h"
        cache.get(key)?.let { return it }
        val b = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        draw(Canvas(b), w.toFloat(), h.toFloat(), sp, month, seed)
        cache.put(key, b)
        return b
    }
}
