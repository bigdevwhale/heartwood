package app.heartwood.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.heartwood.BuildConfig
import app.heartwood.container
import app.heartwood.ui.Go
import app.heartwood.ui.Home
import app.heartwood.ui.Language
import app.heartwood.ui.components.Gap
import app.heartwood.ui.components.Label
import app.heartwood.ui.components.LineButton
import app.heartwood.ui.components.Page
import app.heartwood.ui.components.Sheet
import app.heartwood.ui.theme.S
import app.heartwood.ui.theme.SType

@Composable
fun SettingsScreen(h: Home, go: Go) {
    val ru = h.ru
    val context = LocalContext.current
    val c = context.container
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        c.write { settings.update { it.copy(calls = ok) } }
    }
    Page(if (ru) "Настройки" else "Settings", go::back) {
        Label(if (ru) "Язык" else "Language")
        Gap(6)
        val lang = Language.current()
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((tag, name) in listOf(Language.SYSTEM to (if (ru) "Как в системе" else "System"), Language.EN to "English", Language.RU to "Русский")) {
                val on = lang == tag
                Text(
                    name, style = SType.Typed.copy(color = if (on) S.c.paper else S.c.ink),
                    modifier = Modifier.clip(S.RSm).border(1.5.dp, S.c.ink, S.RSm).background(if (on) S.c.ink else S.c.kraft).clickable { Language.set(tag) }.padding(horizontal = 10.dp, vertical = 7.dp),
                )
            }
        }
        Gap(18)
        Sheet {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (ru) "Зов деревьев" else "Calls from the trees", style = SType.Title.copy(fontSize = 16.sp()))
                    Text(
                        if (ru) "Не чаще двух раз в месяц и только по делу: зацвела, облетает, первый снег, год знакомства, записка в дупле созрела."
                        else "No more than twice a month, and only for a reason: blossom, falling leaves, first snow, a year of friendship, a note ripe in the hollow.",
                        style = SType.Muted,
                    )
                }
                Switch(
                    checked = h.settings.calls,
                    onCheckedChange = { on ->
                        if (on && Build.VERSION.SDK_INT >= 33) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else c.write { settings.update { it.copy(calls = on) } }
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = S.c.moss),
                )
            }
        }
        Gap(14)
        LineButton(if (ru) "Как это устроено" else "How it works", go::guide, Modifier.fillMaxWidth())
        Gap(18)
        Label(if (ru) "О приложении" else "About")
        Gap(6)
        Text(
            if (ru) "Heartwood ${BuildConfig.VERSION_NAME} · Stillcraft. Без рекламы и аккаунтов, всё хранится на телефоне. В сеть уходят только запросы карты (OpenStreetMap) и погоды у дерева (Open-Meteo / MET Norway)."
            else "Heartwood ${BuildConfig.VERSION_NAME} · Stillcraft. No ads, no accounts; everything stays on the phone. The only network requests are map tiles (OpenStreetMap) and the weather at the tree (Open-Meteo / MET Norway).",
            style = SType.Muted,
        )
        Gap(8)
        Text("Map data © OpenStreetMap contributors", style = SType.Typed)
        Gap(10)
        LineButton(if (ru) "Исходный код на GitHub" else "Source code on GitHub", {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bigdevwhale/heartwood")))
        }, Modifier.fillMaxWidth())
        Gap(24)
    }
}

private fun Int.sp() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)
