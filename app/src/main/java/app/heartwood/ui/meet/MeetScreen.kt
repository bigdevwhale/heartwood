package app.heartwood.ui.meet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.heartwood.BuildConfig
import app.heartwood.container
import app.heartwood.data.Kind
import app.heartwood.data.Tree
import app.heartwood.domain.Geo
import app.heartwood.domain.Season
import app.heartwood.domain.day
import app.heartwood.sense.Fix
import app.heartwood.ui.Go
import app.heartwood.ui.Home
import app.heartwood.ui.T
import app.heartwood.ui.components.Art
import app.heartwood.ui.components.Gap
import app.heartwood.ui.components.InkButton
import app.heartwood.ui.components.Label
import app.heartwood.ui.components.LineButton
import app.heartwood.ui.components.Page
import app.heartwood.ui.components.Said
import app.heartwood.ui.components.Sheet
import app.heartwood.ui.components.Stamp
import app.heartwood.ui.components.rememberWhereabouts
import app.heartwood.ui.theme.S
import app.heartwood.ui.theme.SType

/** Everything here happens only at the tree: within 50 metres of its point. */
@Composable
fun MeetScreen(id: Long, h: Home, go: Go) {
    val t = h.tree(id) ?: return
    val ru = h.ru
    val c = LocalContext.current.container
    val demo = BuildConfig.DEMO || Art.still
    val where = rememberWhereabouts(if (demo) Fix(t.lat, t.lon, 5f, System.currentTimeMillis()) else null)
    var forced by rememberSaveable { mutableStateOf(false) }
    val dist = where.fix?.let { Geo.meters(it.lat, it.lon, t.lat, t.lon) }
    val acc = where.fix?.accuracy ?: 999f
    val near = forced || (dist != null && Geo.near(dist, acc))
    val metToday = h.visits(id).any { day(it.at) == h.today }

    // Arriving counts as a meeting: the day's ring.
    LaunchedEffect(near) {
        if (near && !metToday && !Art.still) c.record(t, if (h.visits(id).isEmpty()) Kind.MEET else Kind.VISIT)
    }

    Page(if (ru) "У дерева «${t.name}»" else "At “${t.name}”", go::back) {
        Sheet {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Label(if (ru) "Где вы" else "Where you are")
                    Text(
                        when {
                            forced -> if (ru) "У дерева (отладка)" else "At the tree (debug)"
                            !where.permitted -> if (ru) "Нужна геопозиция: встречаться можно только у самого дерева." else "Location is needed: you can only meet at the tree itself."
                            dist == null -> if (ru) "Ищу спутники…" else "Looking for satellites…"
                            near -> if (ru) "Вы у дерева · ${T.meters(dist, ru)}" else "You're at the tree · ${T.meters(dist, ru)}"
                            else -> if (ru) "До дерева ${T.meters(dist, ru)}. Дружба не переносится по воздуху." else "${T.meters(dist, ru)} to the tree. Friendship doesn't travel by air."
                        },
                        style = SType.Body,
                    )
                    if (dist != null && !near) Text((if (ru) "точность ±" else "accuracy ±") + T.meters(acc.toDouble(), ru), style = SType.Typed)
                }
                if (near) Stamp(if (ru) "рядом" else "here", color = S.c.moss)
            }
            if (!where.permitted) { Gap(10); InkButton(if (ru) "Разрешить геопозицию" else "Allow location", where.ask, Modifier.fillMaxWidth()) }
            if (BuildConfig.DEBUG && !demo && !near) { Gap(8); LineButton("I'm here (debug)", { forced = true }, Modifier.fillMaxWidth()) }
        }
        if (near) {
            Gap(10)
            Said(
                if (metToday) (if (ru) "Кольцо сегодняшнего дня уже растёт. Чем займёмся?" else "Today's ring is already growing. What shall we do?")
                else (if (ru) "Здравствуй." else "Hello."),
                size = 24,
            )
        }
        h.ripe(id)?.let { n ->
            if (near) {
                Gap(12)
                Sheet(tape = true, onClick = { go.act("hollow", id) }) {
                    Label((if (ru) "В дупле · оставлено " else "In the hollow · left ") + T.stamp(n.leftAt, ru))
                    Said(if (ru) "Здесь записка годичной давности. Сегодня её можно открыть." else "There's a note here from a year ago. Today it can be opened.", size = 23)
                }
            }
        }
        Gap(14)
        val leafDone = h.leafThisSeason(id)
        val bare = !h.species(t).evergreen && Season.of(h.today) == Season.WINTER
        val acts = listOf(
            Act("hug", if (ru) "Обнять" else "Hug", if (ru) "Телефон к груди, грудь к стволу. Минута тишины." else "Phone to your chest, chest to the trunk. A minute of quiet.", ::hug),
            Act("listen", if (ru) "Послушать" else "Listen", if (ru) "Микрофон к коре: 10 секунд — голос дня." else "Microphone to the bark: 10 seconds, the voice of the day.", ::ear),
            Act("rub", if (ru) "Фроттаж" else "Bark rubbing", if (ru) "Сфотографируйте кору и водите пальцем, как грифелем." else "Photograph the bark, then rub it out with a finger, like graphite.", ::rub),
            Act(
                "leaf", if (ru) "Лист в гербарий" else "A leaf to press",
                when {
                    leafDone -> if (ru) "В этом сезоне лист уже есть." else "This season's leaf is pressed."
                    bare -> if (ru) "Зимой листьев нет — весной." else "No leaves in winter — come spring."
                    else -> if (ru) "Один упавший лист в сезон." else "One fallen leaf a season."
                },
                ::leaf, enabled = !leafDone && !bare,
            ),
            Act("shoot", if (ru) "Снимок по кальке" else "Photo over the last", if (ru) "С того же места, что в прошлый раз, — для хроники." else "From the same spot as last time, for the chronicle.", ::camera),
            Act("sit", if (ru) "Посидеть рядом" else "Sit beside it", if (ru) "Телефон экраном вниз. Ничего не происходит." else "Phone face down. Nothing happens.", ::bench),
            Act(
                "hollow", if (ru) "Тайник в дупле" else "The hollow",
                if (h.sealed(id) != null && h.ripe(id) == null) (if (ru) "Записка запечатана до ${T.stamp(h.sealed(id)!!.openAt, ru)}." else "A note is sealed until ${T.stamp(h.sealed(id)!!.openAt, ru)}.")
                else (if (ru) "Записка себе. Откроется здесь через год." else "A note to yourself. It opens here in a year."),
                ::hollow,
            ),
        )
        for (row in acts.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (a in row) {
                    val on = near && a.enabled
                    Sheet(Modifier.weight(1f).alpha(if (on) 1f else .45f), onClick = { if (on) go.act(a.route, id) }, padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                        androidx.compose.foundation.Canvas(Modifier.size(34.dp)) { a.icon(this, S_INK) }
                        Gap(4)
                        Text(a.title, style = SType.Title.copy(fontSize = 15.sp))
                        Text(a.hint, style = SType.Muted.copy(fontSize = 12.5.sp, lineHeight = 16.sp), modifier = Modifier.height(50.dp))
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Gap(10)
        }
        Gap(14)
    }
}

private val S_INK = Color(0xFF2F2A24)

private class Act(val route: String, val title: String, val hint: String, val icon: DrawScope.(Color) -> Unit, val enabled: Boolean = true)

private fun DrawScope.u() = size.width / 24f
private fun DrawScope.st() = Stroke(1.5f * u(), cap = StrokeCap.Round)

private fun hug(d: DrawScope, c: Color) = with(d) {
    val s = u()
    val p = androidx.compose.ui.graphics.Path().apply {
        moveTo(12 * s, 21 * s); cubicTo(7 * s, 18 * s, 3 * s, 14.5f * s, 3 * s, 10 * s); cubicTo(3 * s, 5 * s, 10 * s, 4 * s, 12 * s, 8.5f * s)
        cubicTo(14 * s, 4 * s, 21 * s, 5 * s, 21 * s, 10 * s); cubicTo(21 * s, 14.5f * s, 17 * s, 18 * s, 12 * s, 21 * s); close()
    }
    drawPath(p, c, style = st())
}

private fun ear(d: DrawScope, c: Color) = with(d) {
    val s = u()
    val p = androidx.compose.ui.graphics.Path().apply { moveTo(6 * s, 9 * s); cubicTo(6 * s, 1 * s, 18 * s, 1 * s, 18 * s, 9 * s); cubicTo(18 * s, 13 * s, 14 * s, 14 * s, 14 * s, 18 * s); cubicTo(14 * s, 21 * s, 10 * s, 22 * s, 9 * s, 20 * s) }
    drawPath(p, c, style = st())
    drawArc(c, 180f, 180f, false, Offset(9 * s, 6 * s), androidx.compose.ui.geometry.Size(6 * s, 6 * s), style = st())
}

private fun rub(d: DrawScope, c: Color) = with(d) {
    val s = u()
    drawLine(c, Offset(4 * s, 20 * s), Offset(16 * s, 8 * s), 1.5f * s, StrokeCap.Round)
    drawLine(c, Offset(7 * s, 23 * s), Offset(19 * s, 11 * s), 1.5f * s, StrokeCap.Round)
    drawLine(c, Offset(16 * s, 8 * s), Offset(19 * s, 11 * s), 1.5f * s, StrokeCap.Round)
    for (y in listOf(5f, 9f, 13f)) drawLine(c, Offset(2 * s, y * s), Offset((11 - y / 2) * s, (y - 1) * s), 1.2f * s, StrokeCap.Round)
}

private fun leaf(d: DrawScope, c: Color) = with(d) {
    val s = u()
    val p = androidx.compose.ui.graphics.Path().apply { moveTo(5 * s, 19 * s); cubicTo(5 * s, 9 * s, 12 * s, 4 * s, 20 * s, 4 * s); cubicTo(20 * s, 12 * s, 15 * s, 19 * s, 5 * s, 19 * s); close() }
    drawPath(p, c, style = st())
    drawLine(c, Offset(5 * s, 19 * s), Offset(14 * s, 10 * s), 1.5f * s, StrokeCap.Round)
}

private fun camera(d: DrawScope, c: Color) = with(d) {
    val s = u()
    drawRoundRect(c, Offset(3 * s, 7 * s), androidx.compose.ui.geometry.Size(18 * s, 13 * s), androidx.compose.ui.geometry.CornerRadius(2 * s), style = st())
    drawCircle(c, 3.6f * s, Offset(12 * s, 13.5f * s), style = st())
    drawLine(c, Offset(8 * s, 7 * s), Offset(10 * s, 4 * s), 1.5f * s, StrokeCap.Round)
    drawLine(c, Offset(10 * s, 4 * s), Offset(14 * s, 4 * s), 1.5f * s, StrokeCap.Round)
    drawLine(c, Offset(14 * s, 4 * s), Offset(16 * s, 7 * s), 1.5f * s, StrokeCap.Round)
}

private fun bench(d: DrawScope, c: Color) = with(d) {
    val s = u()
    drawLine(c, Offset(4 * s, 12 * s), Offset(20 * s, 12 * s), 1.5f * s, StrokeCap.Round)
    drawLine(c, Offset(6 * s, 12 * s), Offset(6 * s, 20 * s), 1.5f * s, StrokeCap.Round)
    drawLine(c, Offset(18 * s, 12 * s), Offset(18 * s, 20 * s), 1.5f * s, StrokeCap.Round)
    drawLine(c, Offset(4 * s, 7 * s), Offset(20 * s, 7 * s), 1.5f * s, StrokeCap.Round)
    drawLine(c, Offset(7 * s, 7 * s), Offset(7 * s, 12 * s), 1.5f * s, StrokeCap.Round)
    drawLine(c, Offset(17 * s, 7 * s), Offset(17 * s, 12 * s), 1.5f * s, StrokeCap.Round)
}

private fun hollow(d: DrawScope, c: Color) = with(d) {
    val s = u()
    drawLine(c, Offset(6 * s, 2 * s), Offset(6 * s, 22 * s), 1.5f * s, StrokeCap.Round)
    drawLine(c, Offset(18 * s, 2 * s), Offset(18 * s, 22 * s), 1.5f * s, StrokeCap.Round)
    drawOval(c, Offset(9 * s, 8 * s), androidx.compose.ui.geometry.Size(6 * s, 9 * s), style = st())
}
