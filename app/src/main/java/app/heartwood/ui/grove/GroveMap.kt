package app.heartwood.ui.grove

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.Typeface
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.heartwood.R
import app.heartwood.data.Tree
import app.heartwood.domain.Species
import app.heartwood.ui.art.INK
import app.heartwood.ui.art.TreeArt
import app.heartwood.ui.components.Art
import app.heartwood.ui.components.NativeCanvas
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Overlay
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** The map tiles as an old survey map: colour drained toward sepia on kraft. */
fun sepiaTiles(): ColorMatrixColorFilter {
    val m = ColorMatrix()
    m.setSaturation(0.25f)
    m.postConcat(ColorMatrix(floatArrayOf(1.0f, 0f, 0f, 0f, 4f, 0f, 0.92f, 0f, 0f, 0f, 0f, 0f, 0.74f, 0f, -8f, 0f, 0f, 0f, 1f, 0f)))
    return ColorMatrixColorFilter(m)
}

private const val SOIL = 0xF0231B14.toInt()
private const val GLOW = 0xFFE9D58A.toInt()

/** Draws the grove over a map, or a stand-in map: tree pins on the surface, the mycelium underground. */
class GroveLayer(context: Context, private val density: Float) {
    private val hand: Typeface? = ResourcesCompat.getFont(context, R.font.caveat)
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = hand; textSize = 19 * density; textAlign = Paint.Align.CENTER; color = INK }
    private val halo = Paint(label).apply { style = Paint.Style.STROKE; strokeWidth = 4 * density; color = 0xCCF1E7D1.toInt() }
    private val strand = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; color = 0x55E9D58A; setShadowLayer(6 * density, 0f, 0f, GLOW) }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GLOW; setShadowLayer(12 * density, 0f, 0f, GLOW) }
    private val root = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0x8CC99B62.toInt(); strokeWidth = density }
    private val pulse = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFF3C4.toInt(); setShadowLayer(10 * density, 0f, 0f, GLOW) }

    var trees: List<Tree> = emptyList()
    var underground = false
    var latest: Long? = null
    var selected: Long? = null
    var month = 1

    /** Draws onto a canvas where [at] gives each tree's pixel position. */
    fun draw(c: Canvas, w: Float, h: Float, at: (Tree) -> Point, time: Float) {
        val pts = trees.associateWith(at)
        if (!underground) {
            val s = (58 * density).toInt()
            for ((t, p) in pts) {
                val b = TreeArt.bitmap(Species.of(t.species), month, t.seed, s, s)
                c.drawBitmap(b, p.x - s / 2f, p.y - s * .9f, null)
                label.color = if (t.id == selected) 0xFFA9533F.toInt() else INK
                c.drawText(t.name, p.x.toFloat(), p.y + 17 * density, halo)
                c.drawText(t.name, p.x.toFloat(), p.y + 17 * density, label)
            }
            return
        }
        c.drawColor(SOIL)
        val r = Random(7)
        repeat(90) {
            val k = Paint().apply { color = 0x553A2E23 }
            c.drawCircle(r.nextFloat() * w, r.nextFloat() * h, (1 + r.nextFloat() * 4) * density, k)
        }
        val list = pts.entries.toList()
        val path = Path()
        for (i in list.indices) for (j in i + 1 until list.size) {
            val (a, pa) = list[i]
            val (b, pb) = list[j]
            val d = hypot((pb.x - pa.x).toFloat(), (pb.y - pa.y).toFloat())
            val rr = Random(a.id * 31 + b.id)
            for (k in 0 until 6) {
                val o1x = (rr.nextFloat() - .5f) * d * .35f
                val o1y = (rr.nextFloat() - .5f) * d * .35f
                val o2x = (rr.nextFloat() - .5f) * d * .35f
                val o2y = (rr.nextFloat() - .5f) * d * .35f
                val p1x = pa.x + (pb.x - pa.x) * .33f + o1x
                val p1y = pa.y + (pb.y - pa.y) * .33f + o1y
                val p2x = pa.x + (pb.x - pa.x) * .66f + o2x
                val p2y = pa.y + (pb.y - pa.y) * .66f + o2y
                strand.strokeWidth = (.5f + rr.nextFloat()) * density
                path.reset()
                path.moveTo(pa.x.toFloat(), pa.y.toFloat())
                path.cubicTo(p1x, p1y, p2x, p2y, pb.x.toFloat(), pb.y.toFloat())
                c.drawPath(path, strand)
                // News travels from the friend met last.
                val from = when (latest) { a.id -> 0; b.id -> 1; else -> -1 }
                if (from >= 0) {
                    var t = (time * .18f + rr.nextFloat()) % 1f
                    if (from == 1) t = 1 - t
                    val q = 1 - t
                    val x = q * q * q * pa.x + 3 * q * q * t * p1x + 3 * q * t * t * p2x + t * t * t * pb.x
                    val y = q * q * q * pa.y + 3 * q * q * t * p1y + 3 * q * t * t * p2y + t * t * t * pb.y
                    c.drawCircle(x, y, 2.4f * density, pulse)
                }
            }
        }
        label.color = 0xFFE6D6B8.toInt()
        for ((t, p) in list) {
            val rr = Random(t.seed)
            repeat(16) {
                val a = rr.nextFloat() * 6.283f
                val l = (14 + rr.nextFloat() * 18) * density
                path.reset()
                path.moveTo(p.x.toFloat(), p.y.toFloat())
                path.quadTo(p.x + kotlin.math.cos(a + .4f) * l * .5f, p.y + kotlin.math.sin(a + .4f) * l * .5f, p.x + kotlin.math.cos(a) * l, p.y + kotlin.math.sin(a) * l)
                c.drawPath(path, root)
            }
            c.drawCircle(p.x.toFloat(), p.y.toFloat(), 4.5f * density, glow)
            c.drawText(t.name, p.x.toFloat(), p.y - 26 * density, label)
        }
    }

    fun hit(x: Float, y: Float, at: (Tree) -> Point): Tree? =
        trees.minByOrNull { val p = at(it); hypot(p.x - x, p.y - y - 20 * density) }?.takeIf { val p = at(it); hypot(p.x - x, p.y - y - 20 * density) < 40 * density }
}

private class OsmGrove(private val layer: GroveLayer, private val onTap: (Tree) -> Unit) : Overlay() {
    private val start = System.nanoTime()
    override fun draw(c: Canvas, mv: MapView, shadow: Boolean) {
        if (shadow) return
        val proj = mv.projection
        layer.draw(c, mv.width.toFloat(), mv.height.toFloat(), { t -> proj.toPixels(GeoPoint(t.lat, t.lon), null) }, (System.nanoTime() - start) / 1e9f)
        if (layer.underground) mv.postInvalidateOnAnimation()
    }

    override fun onSingleTapConfirmed(e: MotionEvent, mv: MapView): Boolean {
        val proj = mv.projection
        val t = layer.hit(e.x, e.y) { proj.toPixels(GeoPoint(it.lat, it.lon), null) } ?: return false
        onTap(t)
        return true
    }
}

fun bounds(trees: List<Tree>): BoundingBox? {
    if (trees.isEmpty()) return null
    val n = trees.maxOf { it.lat }
    val s = trees.minOf { it.lat }
    val e = trees.maxOf { it.lon }
    val w = trees.minOf { it.lon }
    val pad = max(.0015, max(n - s, e - w) * .25)
    return BoundingBox(n + pad, e + pad, s - pad, w - pad)
}

/** The grove on an OpenStreetMap map. In the renders, a drawn stand-in. */
@Composable
fun GroveMap(trees: List<Tree>, underground: Boolean, latest: Long?, selected: Long?, month: Int, onTap: (Tree) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val layer = remember { GroveLayer(context, density) }
    layer.trees = trees
    layer.underground = underground
    layer.latest = latest
    layer.selected = selected
    layer.month = month
    if (Art.still) {
        StandInMap(layer, trees, modifier)
        return
    }
    val mapView = rememberMapView { mv ->
        mv.overlays.add(OsmGrove(layer, onTap))
        mv.addOnFirstLayoutListener { _, _, _, _, _ ->
            val b = bounds(trees)
            if (b != null && trees.size > 1) mv.zoomToBoundingBox(b, false, (40 * density).toInt())
            else if (b != null) { mv.controller.setZoom(17.0); mv.controller.setCenter(GeoPoint(trees[0].lat, trees[0].lon)) }
        }
    }
    LaunchedEffect(underground, trees, latest, selected) { mapView.invalidate() }
    AndroidView({ mapView }, modifier)
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun rememberMapView(setup: (MapView) -> Unit): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            minZoomLevel = 4.0
            maxZoomLevel = 19.5
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            overlayManager.tilesOverlay.setColorFilter(sepiaTiles())
            overlayManager.tilesOverlay.loadingBackgroundColor = 0xFFE8DCBE.toInt()
            overlayManager.tilesOverlay.loadingLineColor = 0xFFD8C39D.toInt()
            controller.setZoom(15.0)
            // Inside a scrolling column: keep the drag for the map.
            setOnTouchListener { v, e ->
                if (e.action == MotionEvent.ACTION_DOWN) v.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
            setup(this)
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(obs)
        onDispose {
            lifecycle.removeObserver(obs)
            mapView.onPause()
            mapView.onDetach()
        }
    }
    return mapView
}

/** A drawn map for the renders: streets, a park, a river, the trees by their real relative places. */
@Composable
private fun StandInMap(layer: GroveLayer, trees: List<Tree>, modifier: Modifier) {
    NativeCanvas(modifier) { c, w, h ->
        val r = Random(3)
        c.drawColor(0xFFE8DBBD.toInt())
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = 0x557F9A55
        p.strokeWidth = 2f
        c.save()
        c.clipPath(Path().apply { addOval(w * .08f, h * .2f, w * .58f, h * .72f, Path.Direction.CW) })
        var x = -h
        while (x < w) { c.drawLine(x, 0f, x + h, h, p); x += 14f }
        c.restore()
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.color = 0xFF9FB3B8.toInt()
        p.strokeWidth = h * .06f
        c.drawPath(Path().apply { moveTo(-10f, h * .92f); cubicTo(w * .3f, h * .75f, w * .55f, h * 1.02f, w + 10, h * .82f) }, p)
        for (s in listOf(floatArrayOf(0f, .18f, 1f, .12f), floatArrayOf(0f, .6f, 1f, .55f), floatArrayOf(.47f, 0f, .5f, 1f), floatArrayOf(.85f, 0f, .8f, 1f))) {
            p.strokeCap = Paint.Cap.BUTT
            p.color = 0xFFC9B48D.toInt(); p.strokeWidth = h * .045f
            c.drawLine(s[0] * w, s[1] * h, s[2] * w, s[3] * h, p)
            p.color = 0xFFF6EFDF.toInt(); p.strokeWidth = h * .034f
            c.drawLine(s[0] * w, s[1] * h, s[2] * w, s[3] * h, p)
        }
        r.nextFloat()
        val n = trees.maxOfOrNull { it.lat } ?: 0.0
        val s = trees.minOfOrNull { it.lat } ?: 0.0
        val e = trees.maxOfOrNull { it.lon } ?: 0.0
        val wv = trees.minOfOrNull { it.lon } ?: 0.0
        val spanLat = max(1e-5, n - s)
        val spanLon = max(1e-5, e - wv)
        layer.draw(c, w, h, { t ->
            Point(
                (w * .15f + (t.lon - wv) / spanLon * w * .7f).toInt(),
                (h * .22f + (n - t.lat) / spanLat * h * .6f).toInt(),
            )
        }, 2.2f)
    }
}

/** A map to set a tree's point on: tap where it stands. */
@Composable
fun PickMap(center: GeoPoint?, marker: GeoPoint?, others: List<Tree>, month: Int, onPick: (GeoPoint) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val layer = remember { GroveLayer(context, density) }
    layer.trees = others
    layer.month = month
    if (Art.still) {
        StandInMap(layer, others, modifier)
        return
    }
    val pin = remember { arrayOf<GeoPoint?>(null) }
    pin[0] = marker
    val mapView = rememberMapView { mv ->
        mv.overlays.add(OsmGrove(layer) {})
        mv.overlays.add(object : Overlay() {
            private val x = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFA9533F.toInt(); strokeWidth = 3 * density; strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE }
            override fun draw(c: Canvas, mv2: MapView, shadow: Boolean) {
                val g = pin[0] ?: return
                if (shadow) return
                val p = mv2.projection.toPixels(g, null)
                val k = 10 * density
                c.drawLine(p.x - k, p.y - k, p.x + k, p.y + k, x)
                c.drawLine(p.x + k, p.y - k, p.x - k, p.y + k, x)
                c.drawCircle(p.x.toFloat(), p.y.toFloat(), k * 1.7f, x)
            }
        })
        mv.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean { onPick(p); return true }
            override fun longPressHelper(p: GeoPoint): Boolean = false
        }))
        mv.controller.setZoom(18.0)
        (marker ?: center)?.let { mv.controller.setCenter(it) }
    }
    LaunchedEffect(center) {
        if (marker == null && center != null) mapView.controller.animateTo(center)
    }
    LaunchedEffect(marker) { mapView.invalidate() }
    AndroidView({ mapView }, modifier)
}

@Suppress("unused")
private fun clamp(v: Float, a: Float, b: Float) = min(max(v, a), b)
