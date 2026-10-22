package app.heartwood.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.heartwood.container
import app.heartwood.data.Kind
import app.heartwood.data.Note
import app.heartwood.data.Settings
import app.heartwood.data.Tree
import app.heartwood.data.Visit
import app.heartwood.domain.Call
import app.heartwood.domain.Calls
import app.heartwood.domain.Friend
import app.heartwood.domain.Friendship
import app.heartwood.domain.Hollow
import app.heartwood.domain.Memory
import app.heartwood.domain.Mycelium
import app.heartwood.domain.Ring
import app.heartwood.domain.Rings
import app.heartwood.domain.Season
import app.heartwood.domain.Species
import app.heartwood.domain.day
import app.heartwood.ru
import app.heartwood.ui.book.BookScreen
import app.heartwood.ui.components.rememberNow
import app.heartwood.ui.grove.GroveScreen
import app.heartwood.ui.grove.MoveScreen
import app.heartwood.ui.meet.ChronicleScreen
import app.heartwood.ui.meet.HollowScreen
import app.heartwood.ui.meet.HugScreen
import app.heartwood.ui.meet.LeafScreen
import app.heartwood.ui.meet.ListenScreen
import app.heartwood.ui.meet.MeetScreen
import app.heartwood.ui.meet.RubScreen
import app.heartwood.ui.meet.ShootScreen
import app.heartwood.ui.meet.SitScreen
import app.heartwood.ui.newtree.NewTreeScreen
import app.heartwood.ui.onboarding.WelcomeScreen
import app.heartwood.ui.settings.SettingsScreen
import app.heartwood.ui.tree.TreeScreen
import java.time.LocalDate

/** A frame of a tree's chronicle: the first photo, then each photo taken over the last one. */
class Frame(val file: String, val at: Long, val align: String)

/** Everything written down, plus what follows from it, at one moment. Screens read through here. */
@Immutable
class Home(
    val trees: List<Tree>,
    val visits: List<Visit>,
    val notes: List<Note>,
    val settings: Settings,
    val now: Long,
    val ru: Boolean,
) {
    val today: LocalDate = day(now)
    private val byTree = visits.groupBy { it.treeId }
    private val ringCache = HashMap<Long, List<Ring>>()

    fun tree(id: Long): Tree? = trees.firstOrNull { it.id == id }
    fun visits(id: Long): List<Visit> = byTree[id].orEmpty()
    fun rings(id: Long): List<Ring> = ringCache.getOrPut(id) { Rings.of(visits(id)) }
    fun species(t: Tree) = Species.of(t.species)

    val lastMet: Map<Long, LocalDate> = trees.mapNotNull { t -> rings(t.id).lastOrNull()?.let { t.id to it.day } }.toMap()
    val firstMet: Map<Long, LocalDate> = trees.mapNotNull { t -> rings(t.id).firstOrNull()?.let { t.id to it.day } }.toMap()

    /** The friend met most recently: news along the roots starts from it. */
    val latest: Tree? = trees.maxByOrNull { lastMet[it.id]?.toEpochDay() ?: Long.MIN_VALUE }?.takeIf { lastMet[it.id] != null }

    fun friendship(id: Long): Friendship = Friend.of(rings(id), today, ru)
    fun line(t: Tree): String = Memory.line(t, rings(t.id), today, ru)
    fun news(t: Tree) = Mycelium.news(t, trees, lastMet, today, ru)
    fun sealed(id: Long): Note? = Hollow.sealed(notes, id)
    fun ripe(id: Long): Note? = sealed(id)?.takeIf { Hollow.ripe(it, now) }
    fun count(id: Long, k: Kind) = visits(id).count { it.kindOf == k }

    /** One leaf a season from each tree. */
    fun leafThisSeason(id: Long): Boolean = visits(id).any { it.kindOf == Kind.LEAF && Season.of(day(it.at)) == Season.of(today) && java.time.temporal.ChronoUnit.DAYS.between(day(it.at), today) < 100 }

    val leaves: List<Visit> = visits.filter { it.kindOf == Kind.LEAF }.sortedByDescending { it.at }
    val rubbings: List<Visit> = visits.filter { it.kindOf == Kind.RUB && it.file.isNotEmpty() }.sortedByDescending { it.at }
    val voices: List<Visit> = visits.filter { it.kindOf == Kind.LISTEN && it.file.isNotEmpty() }.sortedByDescending { it.at }

    fun frames(t: Tree): List<Frame> =
        listOfNotNull(t.photo.takeIf { it.isNotEmpty() }?.let { Frame(it, t.createdAt, "") }) +
            visits(t.id).filter { it.kindOf == Kind.PHOTO && it.file.isNotEmpty() }.map { Frame(it.file, it.at, it.align) }

    /** The call on the grove's envelope: a tree with a reason to see you now. */
    val call: Call? = Calls.pick(trees, firstMet, notes, emptySet(), "", today, now, ru)
}

/** Everything a screen can open. */
class Go(private val nav: NavHostController) {
    private fun to(route: String) = nav.navigate(route) { launchSingleTop = true }
    fun grove() = nav.navigate("grove") { popUpTo("grove") { inclusive = true }; launchSingleTop = true }
    fun book() = nav.navigate("book") { popUpTo("grove"); launchSingleTop = true }
    fun tree(id: Long) = to("tree/$id")
    fun meet(id: Long) = to("meet/$id")
    fun act(kind: String, id: Long) = to("$kind/$id")
    fun chronicle(id: Long) = to("chronicle/$id")
    fun move(id: Long) = to("move/$id")
    fun newTree() = to("new")
    fun settings() = to("settings")
    fun guide() = to("guide")
    fun back() { nav.popBackStack() }

    /** A tree just planted: its page, instead of the form. */
    fun planted(id: Long) = nav.navigate("tree/$id") { popUpTo("grove") }
}

@Composable
fun HeartwoodRoot(settings: Settings) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = context.container
    val nav = rememberNavController()
    val start = remember { if (settings.onboarded) "grove" else "welcome" }
    val dao = container.db.dao()
    val trees by dao.observeTrees().collectAsStateWithLifecycle(initialValue = emptyList())
    val visits by dao.observeVisits().collectAsStateWithLifecycle(initialValue = emptyList())
    val notes by dao.observeNotes().collectAsStateWithLifecycle(initialValue = emptyList())
    val now = rememberNow(60_000)
    val ru = context.ru
    val home = remember(trees, visits, notes, settings, now, ru) { Home(trees, visits, notes, settings, now, ru) }
    val go = remember(nav) { Go(nav) }

    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    LaunchedEffect(settings.onboarded, route) {
        if (settings.onboarded && route == "welcome") nav.navigate("grove") { popUpTo("welcome") { inclusive = true }; launchSingleTop = true }
    }
    // A call tapped in the notification shade: that tree's page.
    val open by container.open.collectAsStateWithLifecycle()
    LaunchedEffect(open, settings.onboarded, trees) {
        val id = open ?: return@LaunchedEffect
        if (settings.onboarded && trees.any { it.id == id }) {
            container.open.value = null
            go.tree(id)
        }
    }

    // A tree passed on by a friend: ask, then plant it unmet.
    val gift by container.gift.collectAsStateWithLifecycle()
    gift?.let { g ->
        AlertDialog(
            onDismissRequest = { container.gift.value = null },
            title = { Text(if (ru) "Дерево в подарок" else "A tree, passed on") },
            text = {
                Text(
                    if (ru) "${g.species.title.ru} «${g.name}», обхват ${g.girth} см${if (g.from.isNotBlank()) " — от ${g.from}" else ""}. Принять в рощу? Кольца у вас будут свои: первое — когда встретитесь."
                    else "${g.species.title.en} “${g.name}”, girth ${g.girth} cm${if (g.from.isNotBlank()) ", from ${g.from}" else ""}. Take it into your grove? Your rings will be your own: the first when you meet.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    container.gift.value = null
                    container.write {
                        val id = plant(Tree(name = g.name, species = g.species.name, girth = g.girth, lat = g.lat, lon = g.lon, why = g.why, from = g.from, seed = System.nanoTime() % 100_000), met = false)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { if (settings.onboarded) go.tree(id) }
                    }
                }) { Text(if (ru) "Принять" else "Take it") }
            },
            dismissButton = { TextButton(onClick = { container.gift.value = null }) { Text(if (ru) "Не сейчас" else "Not now") } },
        )
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = start,
            enterTransition = { fadeIn(tween(240)) },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(240)) },
            popExitTransition = { fadeOut(tween(160)) },
        ) {
            val id = listOf(navArgument("id") { type = NavType.LongType })
            fun arg(e: androidx.navigation.NavBackStackEntry) = e.arguments?.getLong("id") ?: 0L
            composable("welcome") {
                WelcomeScreen { plantNow ->
                    container.write { this.settings.update { it.copy(onboarded = true) } }
                    nav.navigate("grove") { popUpTo("welcome") { inclusive = true } }
                    if (plantNow) nav.navigate("new")
                }
            }
            composable("guide") { WelcomeScreen(again = true) { plantNow -> nav.popBackStack(); if (plantNow) go.newTree() } }
            composable("grove") { GroveScreen(home, go) }
            composable("book") { BookScreen(home, go) }
            composable("tree/{id}", arguments = id) { e -> TreeScreen(arg(e), home, go) }
            composable("meet/{id}", arguments = id) { e -> MeetScreen(arg(e), home, go) }
            composable("hug/{id}", arguments = id) { e -> HugScreen(arg(e), home, go) }
            composable("listen/{id}", arguments = id) { e -> ListenScreen(arg(e), home, go) }
            composable("rub/{id}", arguments = id) { e -> RubScreen(arg(e), home, go) }
            composable("leaf/{id}", arguments = id) { e -> LeafScreen(arg(e), home, go) }
            composable("shoot/{id}", arguments = id) { e -> ShootScreen(arg(e), home, go) }
            composable("sit/{id}", arguments = id) { e -> SitScreen(arg(e), home, go) }
            composable("hollow/{id}", arguments = id) { e -> HollowScreen(arg(e), home, go) }
            composable("chronicle/{id}", arguments = id) { e -> ChronicleScreen(arg(e), home, go) }
            composable("move/{id}", arguments = id) { e -> MoveScreen(arg(e), home, go) }
            composable("new") { NewTreeScreen(home, go) }
            composable("settings") { SettingsScreen(home, go) }
        }
    }
}
