package app.heartwood

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import app.heartwood.data.DemoSeed
import app.heartwood.ui.Go
import app.heartwood.ui.Home
import app.heartwood.ui.book.BookScreen
import app.heartwood.ui.components.Art
import app.heartwood.ui.grove.GroveScreen
import app.heartwood.ui.meet.ChronicleScreen
import app.heartwood.ui.meet.HollowScreen
import app.heartwood.ui.meet.HugScreen
import app.heartwood.ui.meet.LeafScreen
import app.heartwood.ui.meet.MeetScreen
import app.heartwood.ui.meet.SitScreen
import app.heartwood.ui.newtree.NewTreeScreen
import app.heartwood.ui.onboarding.WelcomeScreen
import app.heartwood.ui.theme.HeartwoodTheme
import app.heartwood.ui.tree.TreeScreen
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every main screen over the demo grove, drawn to PNGs (run with `-Prender`), in both languages. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }

    @Before fun still() {
        Art.still = true
    }

    private fun home(): Home = runBlocking {
        val app = ApplicationProvider.getApplicationContext<HeartwoodApp>()
        val c = app.container
        DemoSeed.seedIfEmpty(app, c)
        val dao = c.db.dao()
        Home(dao.trees(), dao.visits(), dao.notes(), c.settings.now(), System.currentTimeMillis(), app.ru)
    }

    private fun shot(name: String, h: Home, content: @Composable (Home, Go) -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val nav = rememberNavController()
            HeartwoodTheme { Box(Modifier.fillMaxSize()) { content(h, Go(nav)) } }
        }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        Thread.sleep(800)
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun oak(h: Home) = h.trees.first().id
    private fun birch(h: Home) = h.trees[1].id

    @Test fun welcome() = home().let { shot("0-welcome", it) { _, _ -> WelcomeScreen {} } }
    @Test fun welcomeRings() = home().let { shot("0-welcome-2", it) { _, _ -> WelcomeScreen(startPage = 1) {} } }
    @Test fun grove() = home().let { shot("1-grove", it) { h, go -> GroveScreen(h, go) } }
    @Test fun tree() = home().let { shot("2-tree", it) { h, go -> TreeScreen(oak(h), h, go) } }
    @Test fun meet() = home().let { shot("3-meet", it) { h, go -> MeetScreen(oak(h), h, go) } }
    @Test fun hug() = home().let { shot("4-hug", it) { h, go -> HugScreen(oak(h), h, go) } }
    @Test fun book() = home().let { shot("5-herbarium", it) { h, go -> BookScreen(h, go) } }
    @Test fun chronicle() = home().let { shot("6-chronicle", it) { h, go -> ChronicleScreen(oak(h), h, go) } }
    @Test fun hollow() = home().let { shot("7-hollow", it) { h, go -> HollowScreen(oak(h), h, go) } }
    @Test fun sit() = home().let { shot("8-sit", it) { h, go -> SitScreen(oak(h), h, go) } }
    @Test fun leaf() = home().let { shot("9-leaf", it) { h, go -> LeafScreen(birch(h), h, go) } }
    @Test fun mossy() = home().let { shot("10-birch", it) { h, go -> TreeScreen(birch(h), h, go) } }
    @Test fun key() = home().let { shot("11-key", it) { h, go -> NewTreeScreen(h, go, startStep = 1) } }
    @Test fun girth() = home().let { shot("12-girth", it) { h, go -> NewTreeScreen(h, go, startStep = 2, preset = app.heartwood.domain.Species.LINDEN) } }

}

class RenderArtScreensEn : ScreensBase() {
    override val lang = "en"
}

@Config(qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderArtScreensRu : ScreensBase() {
    override val lang = "ru"
}
