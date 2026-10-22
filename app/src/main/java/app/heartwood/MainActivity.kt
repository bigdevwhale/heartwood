package app.heartwood

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.heartwood.data.Settings
import app.heartwood.domain.Gift
import app.heartwood.ui.HeartwoodRoot
import app.heartwood.ui.theme.HeartwoodTheme
import app.heartwood.work.CallWork

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val t = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(t, t), navigationBarStyle = SystemBarStyle.light(t, t))
        if (savedInstanceState == null) opened(intent)
        setContent {
            val settings: Settings? by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            // Wait for settings so the welcome screen doesn't flash for someone already here.
            settings?.let { s -> HeartwoodTheme { HeartwoodRoot(s) } }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        opened(intent)
    }

    /** A tree passed on by a friend: a heartwood:// link from a QR code, or the link shared as text. */
    private fun opened(intent: Intent?) {
        intent?.getLongExtra(CallWork.EXTRA_TREE, 0L)?.takeIf { it != 0L }?.let { container.open.value = it }
        val text = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        } ?: return
        Gift.parse(text)?.let { container.gift.value = it }
    }
}
