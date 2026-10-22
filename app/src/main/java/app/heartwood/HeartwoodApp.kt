package app.heartwood

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import app.heartwood.data.DemoSeed
import app.heartwood.data.GroveDatabase
import app.heartwood.data.Kind
import app.heartwood.data.Note
import app.heartwood.data.SettingsStore
import app.heartwood.data.Tree
import app.heartwood.data.Visit
import app.heartwood.domain.Hollow
import app.heartwood.domain.TreeGift
import app.heartwood.domain.Weather
import app.heartwood.domain.WeatherNow
import app.heartwood.work.CallWork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration as Osm
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

class AppContainer(private val context: Context) {
    val db = GroveDatabase.create(context)
    val settings = SettingsStore(context)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val dao get() = db.dao()

    /** A tree passed on by a friend (a QR link), waiting to be accepted. */
    val gift = MutableStateFlow<TreeGift?>(null)

    /** A tree whose call was tapped in the notification shade. */
    val open = MutableStateFlow<Long?>(null)

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }

    /** A file in the app's own storage, by the relative name kept in the database. */
    fun file(rel: String): File = File(context.filesDir, rel)

    /** A fresh file name in a folder: "photos/1f3a….jpg". */
    fun newFile(folder: String, ext: String): String {
        File(context.filesDir, folder).mkdirs()
        return "$folder/${UUID.randomUUID().toString().take(12)}.$ext"
    }

    private val weather = HashMap<Long, Pair<WeatherNow, Long>>()

    /** The weather at a tree, kept for half an hour; null offline. */
    suspend fun weatherAt(t: Tree): WeatherNow? = withContext(Dispatchers.IO) {
        weather[t.id]?.takeIf { System.currentTimeMillis() - it.second < 30 * 60_000L }?.let { return@withContext it.first }
        if (BuildConfig.DEMO) return@withContext null
        runCatching { Weather.fetch(t.lat, t.lon) }.getOrNull()?.also { weather[t.id] = it to System.currentTimeMillis() }
    }

    /** Writes down something done at the tree, with the weather there now. */
    suspend fun record(t: Tree, kind: Kind, seconds: Int = 0, bpm: Int = 0, file: String = "", wave: String = "", align: String = ""): Long {
        val w = weatherAt(t)
        return dao.insert(
            Visit(
                treeId = t.id, kind = kind.name, sky = w?.sky?.name.orEmpty(), temp = w?.temp?.roundToInt() ?: Visit.NO_TEMP,
                seconds = seconds, bpm = bpm, file = file, wave = wave, align = align,
            ),
        )
    }

    suspend fun plant(t: Tree, met: Boolean): Long {
        val id = dao.insert(t)
        if (met) record(t.copy(id = id), Kind.MEET)
        return id
    }

    suspend fun leaveNote(t: Tree, text: String) {
        val now = System.currentTimeMillis()
        dao.insert(Note(treeId = t.id, leftAt = now, openAt = now + Hollow.YEAR, text = text.trim()))
    }

    suspend fun openNote(n: Note) = dao.update(n.copy(openedAt = System.currentTimeMillis()))

    /** Forgets a tree, its rings and everything kept from it. */
    suspend fun forget(t: Tree) = withContext(Dispatchers.IO) {
        val files = dao.visits().filter { it.treeId == t.id && it.file.isNotEmpty() }.map { it.file } + listOfNotNull(t.photo.takeIf { it.isNotEmpty() })
        dao.deleteTree(t.id)
        files.forEach { runCatching { file(it).delete() } }
    }
}

class HeartwoodApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Osm.getInstance().apply {
            userAgentValue = "Heartwood/1.0 (github.com/bigdevwhale/heartwood)"
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }
        if (BuildConfig.DEMO) container.scope.launch { DemoSeed.seedIfEmpty(this@HeartwoodApp, container) }
        CallWork.schedule(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as HeartwoodApp).container

/** Resources in the in-app language, which AppCompat doesn't localise outside activities before Android 13. */
fun Context.localized(): Context {
    val tags = AppCompatDelegate.getApplicationLocales()
    if (tags.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocale(tags[0])
    return createConfigurationContext(config)
}

/** Is this context in Russian? */
val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"
