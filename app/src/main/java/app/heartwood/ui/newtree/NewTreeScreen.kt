package app.heartwood.ui.newtree

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.heartwood.BuildConfig
import app.heartwood.container
import app.heartwood.data.Tree
import app.heartwood.domain.Species
import app.heartwood.ui.Go
import app.heartwood.ui.Home
import app.heartwood.ui.T
import app.heartwood.ui.art.Engrave
import app.heartwood.ui.art.LeafArt
import app.heartwood.ui.components.Art
import app.heartwood.ui.components.Gap
import app.heartwood.ui.components.InkButton
import app.heartwood.ui.components.Label
import app.heartwood.ui.components.LineButton
import app.heartwood.ui.components.NativeCanvas
import app.heartwood.ui.components.Page
import app.heartwood.ui.components.Photos
import app.heartwood.ui.components.Said
import app.heartwood.ui.components.Sheet
import app.heartwood.ui.components.rememberCamera
import app.heartwood.ui.components.rememberWhereabouts
import app.heartwood.ui.grove.PickMap
import app.heartwood.ui.theme.S
import app.heartwood.ui.theme.SType
import app.heartwood.sense.Fix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint

/** Arms closed round the trunk: about an arm span, the girth of a grown person's hug. */
const val HUG_GIRTH = 150

/** Meeting a tree: its photo, what it is, a hug for its girth, then its name and its place on the map. */
@Composable
fun NewTreeScreen(h: Home, go: Go, startStep: Int = 0, preset: Species? = null) {
    val ru = h.ru
    val c = LocalContext.current.container
    var step by rememberSaveable { mutableIntStateOf(startStep) }
    var photo by rememberSaveable { mutableStateOf("") }
    var species by rememberSaveable { mutableStateOf(preset?.name) }
    var closed by rememberSaveable { mutableStateOf<Boolean?>(if (startStep >= 2) false else null) }
    var girth by rememberSaveable { mutableIntStateOf(220) }
    var name by rememberSaveable { mutableStateOf("") }
    var why by rememberSaveable { mutableStateOf("") }
    var lat by rememberSaveable { mutableStateOf<Double?>(null) }
    var lon by rememberSaveable { mutableStateOf<Double?>(null) }
    val camera = rememberCamera("photos") { rel -> if (rel != null) photo = rel }

    val titles = listOf(
        if (ru) "Сфотографируйте дерево целиком — из снимка получится гравюра." else "Photograph the whole tree — the photo becomes an engraving.",
        if (ru) "Кто это? Определите по ключу, как в старом определителе." else "Who is it? Work it out with the key, like an old field guide.",
        if (ru) "Обнимите ствол. Руки сомкнулись?" else "Hug the trunk. Did your hands meet?",
        if (ru) "Имя, одна строка и место на карте." else "A name, one line and its place on the map.",
    )
    Page(if (ru) "Знакомство" else "Meeting a tree", { if (step > 0) step-- else go.back() }) {
        Text(titles[step], style = SType.Muted)
        Gap(8)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0..3) Box(Modifier.weight(1f).height(3.dp).background(if (i <= step) S.c.ink else S.c.line))
        }
        Gap(14)
        when (step) {
            0 -> {
                val px = with(LocalDensity.current) { 340.dp.roundToPx() }
                val engraved by produceState<android.graphics.Bitmap?>(null, photo) {
                    value = if (photo.isEmpty()) null else withContext(Dispatchers.Default) { Photos.load(c.file(photo), 1000)?.let { Engrave.of(it, px, px * 4 / 3) } }
                }
                Box(Modifier.fillMaxWidth().aspectRatio(.8f).shadow(6.dp, S.RSm).clip(S.RSm).background(S.c.paper).clickable(onClick = camera.shoot), contentAlignment = Alignment.Center) {
                    val e = engraved
                    if (e != null) Image(e.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Text(
                        if (photo.isEmpty()) (if (ru) "Нажмите, чтобы сфотографировать\n(откроется камера телефона)" else "Tap to take a photo\n(your phone's camera opens)") else "…",
                        style = SType.Hand.copy(fontSize = 24.sp, color = S.c.muted), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
                Gap(12)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LineButton(if (ru) "Из галереи" else "From gallery", camera.pick)
                    Spacer(Modifier.weight(1f))
                    LineButton(if (ru) "Без фото" else "No photo", { photo = ""; step = 1 })
                }
                Gap(10)
                InkButton(if (ru) "Дальше" else "Next", { step = 1 }, Modifier.fillMaxWidth(), enabled = photo.isNotEmpty())
            }
            1 -> {
                for (row in Species.entries.chunked(2)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (sp in row) {
                            val on = species == sp.name
                            Sheet(Modifier.weight(1f).border(2.dp, if (on) S.c.ink else androidx.compose.ui.graphics.Color.Transparent, S.RSm), onClick = { species = sp.name }, padding = androidx.compose.foundation.layout.PaddingValues(10.dp)) {
                                NativeCanvas(Modifier.fillMaxWidth().height(70.dp)) { cv, w, hh -> LeafArt.draw(cv, w, hh, sp.leaf, LeafArt.colour(sp, 7), -10f) }
                                Text(sp.title(ru), style = SType.Title.copy(fontSize = 15.sp), modifier = Modifier.align(Alignment.CenterHorizontally))
                                Text(sp.hint(ru), style = SType.Muted.copy(fontSize = 12.sp, lineHeight = 15.sp), modifier = Modifier.height(46.dp))
                            }
                        }
                    }
                    Gap(10)
                }
                InkButton(if (ru) "Дальше" else "Next", { step = 2 }, Modifier.fillMaxWidth(), enabled = species != null)
            }
            2 -> {
                val sp = Species.of(species)
                Sheet {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Choice(if (ru) "Да, сомкнулись" else "Yes, they met", closed == true, Modifier.weight(1f)) { closed = true; girth = HUG_GIRTH }
                        Choice(if (ru) "Нет" else "No", closed == false, Modifier.weight(1f)) { closed = false; if (girth == HUG_GIRTH) girth = 220 }
                    }
                    if (closed == false) {
                        Gap(12)
                        Label((if (ru) "Обхват на высоте груди: " else "Girth at chest height: ") + "$girth " + (if (ru) "см" else "cm"))
                        Slider(
                            value = girth.toFloat(), onValueChange = { girth = (it / 5).toInt() * 5 }, valueRange = 30f..800f,
                            colors = SliderDefaults.colors(thumbColor = S.c.ink, activeTrackColor = S.c.ink, inactiveTrackColor = S.c.line),
                        )
                        Text(if (ru) "Можно измерить шнурком или рулеткой — или прикинуть: два обхвата рук — около трёх метров." else "Measure with a string or a tape — or guess: two hugs round is about three metres.", style = SType.Typed)
                    }
                    if (closed != null) {
                        Gap(12)
                        val age = sp.age(girth)
                        val gens = age / 25
                        Said(
                            (if (closed == true) (if (ru) "Руки сомкнулись — обхват около $girth см. " else "Your hands met — a girth of about $girth cm. ") else "") +
                                (if (ru) "${if (sp.male) "Ему" else "Ей"} около ${T.yearsAbout(age, ru)}. " else "It's about ${T.years(age, ru)} old. ") +
                                when {
                                    gens >= 2 -> if (ru) "Это примерно $gens поколения людей." else "That's about $gens generations of people."
                                    age < 30 -> if (ru) "Совсем ${if (sp.male) "молодой" else "молодая"}." else "Still young."
                                    else -> ""
                                },
                            size = 23,
                        )
                        Text(
                            if (ru) "Возраст по формуле лесоводов: диаметр × коэффициент породы (${sp.title.ru.lowercase()} — ${sp.k}). Примерно, но честно."
                            else "Age by the forester's rule: diameter × the species' growth factor (${sp.title.en.lowercase()}: ${sp.k}). Rough, but honest.",
                            style = SType.Typed, modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                Gap(14)
                InkButton(if (ru) "Дальше" else "Next", { step = 3 }, Modifier.fillMaxWidth(), enabled = closed != null)
            }
            else -> {
                val demo = if (BuildConfig.DEMO || Art.still) h.trees.firstOrNull()?.let { Fix(it.lat + .0011, it.lon - .0016, 5f, 0) } else null
                val where = rememberWhereabouts(demo)
                val fix = where.fix
                if (lat == null && fix != null) { lat = fix.lat; lon = fix.lon }
                Field(name, { name = it.take(30) }, if (ru) "Имя" else "Name", if (ru) "например, Молчун" else "e.g. Old Quiet")
                Gap(8)
                Field(why, { why = it.take(120) }, if (ru) "Почему именно оно" else "Why this one", if (ru) "одна строка" else "one line")
                Gap(12)
                Label(if (ru) "Точка на карте — вы стоите у дерева, поправьте касанием" else "Its point — you're at the tree; tap to adjust")
                Gap(6)
                Box(Modifier.fillMaxWidth().height(260.dp).shadow(6.dp, S.RSm).clip(S.RSm)) {
                    PickMap(
                        fix?.let { GeoPoint(it.lat, it.lon) }, lat?.let { GeoPoint(it, lon ?: 0.0) }, h.trees, h.today.monthValue,
                        { lat = it.latitude; lon = it.longitude }, Modifier.fillMaxSize(),
                    )
                }
                if (!where.permitted) { Gap(8); LineButton(if (ru) "Определить, где я" else "Find where I am", where.ask, Modifier.fillMaxWidth()) }
                Gap(14)
                InkButton(if (ru) "Подружиться" else "Become friends", {
                    val tree = Tree(
                        name = name.trim(), species = species ?: Species.OAK.name, girth = girth, lat = lat ?: 0.0, lon = lon ?: 0.0,
                        photo = photo, why = why.trim(), seed = System.nanoTime() % 100_000,
                    )
                    c.write {
                        val id = plant(tree, met = true)
                        withContext(Dispatchers.Main) { go.planted(id) }
                    }
                }, Modifier.fillMaxWidth(), enabled = name.isNotBlank() && lat != null, moss = true)
            }
        }
        Gap(24)
    }
}

@Composable
private fun Choice(text: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(46.dp).clip(S.RSm).border(1.5.dp, S.c.ink, S.RSm).background(if (on) S.c.ink else S.c.paper).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = SType.Button.copy(color = if (on) S.c.paper else S.c.ink)) }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String, hint: String) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, placeholder = { Text(hint, color = S.c.faint) },
        singleLine = true, modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = S.c.ink, unfocusedBorderColor = S.c.line, focusedContainerColor = S.c.paper, unfocusedContainerColor = S.c.paper, cursorColor = S.c.ink, focusedLabelColor = S.c.ink),
        textStyle = SType.Body,
    )
}
