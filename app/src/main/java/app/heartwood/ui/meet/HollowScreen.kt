package app.heartwood.ui.meet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.heartwood.container
import app.heartwood.domain.Hollow
import app.heartwood.ui.Go
import app.heartwood.ui.Home
import app.heartwood.ui.T
import app.heartwood.ui.components.Gap
import app.heartwood.ui.components.InkButton
import app.heartwood.ui.components.Label
import app.heartwood.ui.components.Page
import app.heartwood.ui.components.Said
import app.heartwood.ui.components.Sheet
import app.heartwood.ui.components.Stamp
import app.heartwood.ui.theme.S
import app.heartwood.ui.theme.SType

/** The hollow: a note to whoever comes back here in a year. It opens only here, and only then. */
@Composable
fun HollowScreen(id: Long, h: Home, go: Go) {
    val t = h.tree(id) ?: return
    val ru = h.ru
    val c = LocalContext.current.container
    val sealed = h.sealed(id)
    val ripe = sealed?.takeIf { Hollow.ripe(it, h.now) }
    var reading by rememberSaveable { mutableStateOf(ripe?.text) }
    var readFrom by rememberSaveable { mutableStateOf(ripe?.leftAt ?: 0L) }
    // Opening it is final: the note is read now.
    LaunchedEffect(ripe?.id) { ripe?.let { c.openNote(it) } }

    Page(if (ru) "Дупло «${t.name}»" else "“${t.name}”'s hollow", go::back) {
        reading?.let { text ->
            Label((if (ru) "Оставлено " else "Left on ") + T.stamp(readFrom, ru))
            Gap(8)
            Sheet(Modifier.rotate(-1f)) { Text(text, style = SType.Hand.copy(fontSize = 25.sp())) }
            Gap(16)
            Said(if (ru) "Я хранил${if (h.species(t).male) "" else "а"} её целый год." else "I kept it for a whole year.", size = 23)
            Gap(20)
        }
        val still = h.sealed(id)?.takeIf { it.openedAt == 0L && !Hollow.ripe(it, h.now) }
        if (still != null) {
            Sheet(tape = true) {
                Stamp(if (ru) "запечатано" else "sealed", color = S.c.rust)
                Gap(8)
                Text(
                    if (ru) "В дупле уже лежит записка. Она откроется ${T.stamp(still.openAt, ru)} — и только здесь."
                    else "There's already a note in the hollow. It opens on ${T.stamp(still.openAt, ru)} — and only here.",
                    style = SType.Body,
                )
            }
        } else {
            var text by rememberSaveable { mutableStateOf("") }
            val open = h.now + Hollow.YEAR
            Text(
                if (ru) "Напишите себе — тому, кто придёт сюда через год. Записка откроется ${T.stamp(open, ru)}, только у этого дерева."
                else "Write to yourself — to whoever comes here a year from now. The note opens on ${T.stamp(open, ru)}, only at this tree.",
                style = SType.Muted,
            )
            Gap(12)
            Sheet {
                BasicTextField(
                    value = text, onValueChange = { text = it.take(600) },
                    textStyle = SType.Hand.copy(fontSize = 24.sp()),
                    cursorBrush = SolidColor(S.c.ink),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp).background(S.c.paper).padding(4.dp),
                    decorationBox = { inner ->
                        if (text.isEmpty()) Text(if (ru) "Привет из сегодня…" else "Hello from today…", style = SType.Hand.copy(fontSize = 24.sp(), color = S.c.faint))
                        inner()
                    },
                )
            }
            Gap(14)
            InkButton(if (ru) "Запечатать до ${T.stamp(open, ru)}" else "Seal until ${T.stamp(open, ru)}", {
                val note = text
                c.write { leaveNote(t, note) }
                go.back()
            }, Modifier.fillMaxWidth(), enabled = text.isNotBlank(), moss = true)
        }
        Gap(20)
    }
}

private fun Int.sp() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)
