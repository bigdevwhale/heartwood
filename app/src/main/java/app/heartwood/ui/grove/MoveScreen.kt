package app.heartwood.ui.grove

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.heartwood.container
import app.heartwood.ui.Go
import app.heartwood.ui.Home
import app.heartwood.ui.components.Gap
import app.heartwood.ui.components.InkButton
import app.heartwood.ui.components.LineButton
import app.heartwood.ui.components.Page
import app.heartwood.ui.components.rememberWhereabouts
import app.heartwood.ui.theme.S
import app.heartwood.ui.theme.SType
import org.osmdroid.util.GeoPoint

/** Moving a tree's point on the map, when the first one was off. */
@Composable
fun MoveScreen(id: Long, h: Home, go: Go) {
    val t = h.tree(id) ?: return
    val ru = h.ru
    val c = LocalContext.current.container
    val where = rememberWhereabouts()
    var mark by remember { mutableStateOf(GeoPoint(t.lat, t.lon)) }
    Page(if (ru) "Где стоит «${t.name}»" else "Where “${t.name}” stands", go::back, scroll = false) {
        Text(if (ru) "Нажмите на карте точно туда, где растёт дерево. От этой точки считается «рядом» — 50 метров." else "Tap the map exactly where the tree grows. “At the tree” means within 50 metres of this point.", style = SType.Muted)
        Gap(10)
        Box(Modifier.fillMaxWidth().weight(1f).clip(S.RSm)) {
            PickMap(null, mark, h.trees.filter { it.id != id }, h.today.monthValue, { mark = it }, Modifier.fillMaxWidth().height(2000.dp))
        }
        Gap(10)
        where.fix?.let { f -> LineButton(if (ru) "Я сейчас у дерева — поставить сюда" else "I'm at the tree now — put it here", { mark = GeoPoint(f.lat, f.lon) }, Modifier.fillMaxWidth()) }
            ?: LineButton(
                if (where.permitted && !where.on) (if (ru) "Включить геолокацию" else "Turn on location") else (if (ru) "Определить, где я" else "Find where I am"),
                where.ask, Modifier.fillMaxWidth(), enabled = !where.ready,
            )
        Gap(8)
        InkButton(if (ru) "Сохранить точку" else "Save the point", {
            c.write { db.dao().update(t.copy(lat = mark.latitude, lon = mark.longitude)) }
            go.back()
        }, Modifier.fillMaxWidth(), moss = true)
        Gap(16)
    }
}
