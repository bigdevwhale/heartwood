package app.heartwood.ui.meet

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.heartwood.container
import app.heartwood.data.Kind
import app.heartwood.data.Tree
import app.heartwood.domain.Species
import app.heartwood.ui.Go
import app.heartwood.ui.Home
import app.heartwood.ui.T
import app.heartwood.ui.art.LeafArt
import app.heartwood.ui.components.Centered
import app.heartwood.ui.components.Gap
import app.heartwood.ui.components.InkButton
import app.heartwood.ui.components.LineButton
import app.heartwood.ui.components.NativeCanvas
import app.heartwood.ui.components.Page
import app.heartwood.ui.components.Sheet
import app.heartwood.ui.components.Tape
import app.heartwood.ui.components.rememberCamera
import app.heartwood.ui.components.rememberPhoto
import app.heartwood.ui.theme.S
import app.heartwood.ui.theme.SType

/** A photo pressed between pages: a little faded and warm. */
val Pressed: ColorFilter = ColorFilter.colorMatrix(
    ColorMatrix().apply { setToSaturation(.7f) }.also {
        it.timesAssign(ColorMatrix(floatArrayOf(1f, 0f, 0f, 0f, 10f, 0f, .96f, 0f, 0f, 6f, 0f, 0f, .84f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)))
    },
)

/** A pressed leaf as it sits in the herbarium: the photo, or the drawn leaf, taped onto paper. */
@Composable
fun LeafCard(t: Tree, file: String, month: Int, caption: String, rotation: Float, modifier: Modifier = Modifier) {
    val photo by rememberPhoto(file, 700)
    Box(modifier) {
        Sheet(padding = androidx.compose.foundation.layout.PaddingValues(10.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(.85f).clip(S.RSm)) {
                val p = photo
                if (p != null) Image(p.asImageBitmap(), null, Modifier.fillMaxSize().rotate(rotation * .3f), contentScale = ContentScale.Crop, colorFilter = Pressed)
                else NativeCanvas(Modifier.fillMaxSize().padding(8.dp)) { c, w, h -> LeafArt.draw(c, w, h, Species.of(t.species).leaf, LeafArt.colour(Species.of(t.species), month), rotation) }
            }
            Gap(4)
            Text(caption, style = SType.Hand.copy(fontSize = 17.sp, lineHeight = 18.sp), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        Tape(Modifier.align(Alignment.TopCenter).padding(top = 0.dp), width = 60.dp)
    }
}

@Composable
fun LeafScreen(id: Long, h: Home, go: Go) {
    val t = h.tree(id) ?: return
    val ru = h.ru
    val c = LocalContext.current.container
    var file by rememberSaveable { mutableStateOf("") }
    var drawn by rememberSaveable { mutableStateOf(false) }
    val camera = rememberCamera("leaves") { rel -> if (rel != null) { file = rel; drawn = false } }
    Page(if (ru) "Лист в гербарий" else "A leaf to press", go::back) {
        Text(
            if (ru) "Найдите упавший лист — живой не срывайте. Положите на ладонь и сфотографируйте: он ляжет между страниц тетради."
            else "Find a fallen leaf — don't pick a living one. Lay it on your palm and photograph it: it goes between the notebook's pages.",
            style = SType.Muted,
        )
        Gap(16)
        if (file.isNotEmpty() || drawn) {
            Centered {
                LeafCard(
                    t, file, h.today.monthValue,
                    "«${t.name}» · ${T.stamp(h.today, ru)}", -8f, Modifier.width(230.dp),
                )
            }
            Gap(16)
            InkButton(if (ru) "Засушить" else "Press it", {
                val f = file
                c.write { record(t, Kind.LEAF, file = f) }
                go.back()
            }, Modifier.fillMaxWidth(), moss = true)
            Gap(10)
            LineButton(if (ru) "Переснять" else "Take it again", camera.shoot, Modifier.fillMaxWidth())
        } else {
            InkButton(if (ru) "Сфотографировать лист" else "Photograph the leaf", camera.shoot, Modifier.fillMaxWidth())
            Gap(10)
            LineButton(if (ru) "Без фото — нарисованный лист" else "No photo — a drawn leaf", { drawn = true }, Modifier.fillMaxWidth())
        }
        Gap(20)
    }
}
