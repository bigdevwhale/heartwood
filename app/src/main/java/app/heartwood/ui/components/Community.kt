package app.heartwood.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.heartwood.ui.theme.S
import app.heartwood.ui.theme.SType

/** The StillCraft bot: deep links file a bug or an idea for this app, or follow its releases. */
object Community {
    private const val BOT = "https://t.me/ArcaneWhisperBot?start="
    const val SLUG = "heartwood"
    const val BUG = "${BOT}bug_$SLUG"
    const val IDEA = "${BOT}wish_$SLUG"
    const val FOLLOW = "${BOT}f_$SLUG"
    const val CHAT = "https://t.me/+Q4gFEDk8O842OGVi"
}

/** Report a bug, suggest an idea, follow updates, join the chat; each opens Telegram. */
@Composable
fun CommunityLinks(ru: Boolean) {
    val ctx = LocalContext.current
    fun open(url: String) { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    Sheet {
        Text(
            if (ru) "Приложение делается вместе с теми, кто им пользуется. Всё идёт через бота StillCraft в Telegram."
            else "The app is made together with the people who use it. It all goes through the StillCraft bot on Telegram.",
            style = SType.Muted, modifier = Modifier.padding(bottom = 4.dp),
        )
        for ((icon, label, sub, url) in listOf(
            Link("🐞", if (ru) "Сообщить о баге" else "Report a bug", if (ru) "что сломалось и как повторить" else "what broke and how to repeat it", Community.BUG),
            Link("💡", if (ru) "Предложить идею" else "Suggest an idea", if (ru) "функция, дерево, примета сезона" else "a feature, a tree, a sign of the season", Community.IDEA),
            Link("🔔", if (ru) "Подписаться на обновления" else "Follow updates", if (ru) "бот напишет, когда выйдет версия" else "the bot writes when a version is out", Community.FOLLOW),
            Link("💬", if (ru) "Чат StillCraft" else "StillCraft chat", if (ru) "все приложения и люди" else "all the apps and people", Community.CHAT),
        )) {
            Row(Modifier.fillMaxWidth().clickable { open(url) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(icon, style = SType.Title, modifier = Modifier.width(34.dp))
                Column(Modifier.weight(1f)) {
                    Text(label, style = SType.Body)
                    Text(sub, style = SType.Muted)
                }
                Text("›", style = SType.Title.copy(color = S.c.moss))
            }
        }
    }
}

private data class Link(val icon: String, val label: String, val sub: String, val url: String)
