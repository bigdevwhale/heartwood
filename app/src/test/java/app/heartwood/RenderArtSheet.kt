package app.heartwood

import android.graphics.Bitmap
import android.graphics.Canvas
import app.heartwood.domain.Species
import app.heartwood.ui.art.BarkArt
import app.heartwood.ui.art.LeafArt
import app.heartwood.ui.art.PAPER
import app.heartwood.ui.art.TreeArt
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every species in four months, its leaf and its bark, on one sheet (run with `-Prender`). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class RenderArtSheet {
    @Test fun sheet() {
        val months = intArrayOf(1, 5, 7, 10)
        val cell = 240
        val cols = months.size + 2
        val b = Bitmap.createBitmap(cell * cols, cell * Species.entries.size, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        c.drawColor(PAPER)
        Species.entries.forEachIndexed { row, sp ->
            months.forEachIndexed { col, m ->
                c.save()
                c.translate(col * cell.toFloat(), row * cell.toFloat())
                TreeArt.draw(c, cell.toFloat(), cell.toFloat(), sp, m, 11L + row)
                c.restore()
            }
            c.save()
            c.translate(months.size * cell.toFloat(), row * cell.toFloat())
            LeafArt.draw(c, cell.toFloat(), cell.toFloat(), sp.leaf, LeafArt.colour(sp, 7), -10f)
            c.restore()
            c.drawBitmap(BarkArt.rubbing(sp.bark, cell - 20, 5), (months.size + 1) * cell + 10f, row * cell + 10f, null)
        }
        val out = File(System.getProperty("render.out") ?: "build/art").apply { mkdirs() }
        File(out, "species.png").outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
