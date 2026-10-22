package app.heartwood.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.heartwood.sense.Fix
import app.heartwood.sense.Here

/** The phone's position while the screen is open, and a way to ask for the permission. */
class Whereabouts(val fix: Fix?, val permitted: Boolean, val ask: () -> Unit)

@Composable
fun rememberWhereabouts(demo: Fix? = null): Whereabouts {
    val context = LocalContext.current
    var fix by remember { mutableStateOf(demo) }
    val here = remember { Here(context) { f -> if (fix == null || f.accuracy <= (fix?.accuracy ?: 999f) * 1.5f || f.at - (fix?.at ?: 0) > 15_000) fix = f } }
    var permitted by remember { mutableStateOf(here.permitted()) }
    var restart by remember { mutableIntStateOf(0) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permitted = here.permitted()
        restart++
    }
    LifecycleResumeEffect(restart) {
        permitted = here.permitted()
        if (demo == null && !Art.still) here.start()
        onPauseOrDispose { here.stop() }
    }
    LaunchedEffect(demo) { if (demo != null) fix = demo }
    return Whereabouts(fix, permitted || demo != null) { ask.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
}
