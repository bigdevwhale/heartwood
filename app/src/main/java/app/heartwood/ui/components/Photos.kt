package app.heartwood.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import app.heartwood.container
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Photos are taken by the phone's own camera app and kept in the app's storage. */
object Photos {
    private val cache = LruCache<String, Bitmap>(24)

    /** A photo file, upright and no bigger than [max] on its long side. */
    fun load(file: File, max: Int): Bitmap? {
        if (!file.exists()) return null
        val key = "${file.absolutePath}/$max/${file.lastModified()}"
        cache.get(key)?.let { return it }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= max) sample *= 2
        val raw = BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val turn = runCatching {
            when (ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)
        val upright = if (turn == 0f) raw else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(turn) }, true)
        val k = max.toFloat() / maxOf(upright.width, upright.height)
        val out = if (k < 1f) Bitmap.createScaledBitmap(upright, (upright.width * k).toInt(), (upright.height * k).toInt(), true) else upright
        cache.put(key, out)
        return out
    }

    fun copy(context: Context, uri: Uri, to: File): Boolean = runCatching {
        to.parentFile?.mkdirs()
        context.contentResolver.openInputStream(uri)?.use { i -> to.outputStream().use { i.copyTo(it) } } != null
    }.getOrDefault(false)
}

@Composable
fun rememberPhoto(rel: String, max: Int): State<Bitmap?> {
    val c = LocalContext.current.container
    return produceState<Bitmap?>(null, rel, max) {
        value = if (rel.isEmpty()) null else withContext(Dispatchers.IO) { Photos.load(c.file(rel), max) }
    }
}

/** Starts the phone's camera; [onDone] gets the saved file's relative name, or null if cancelled. */
class Camera(val shoot: () -> Unit, val pick: () -> Unit)

@Composable
fun rememberCamera(folder: String = "photos", onDone: (String?) -> Unit): Camera {
    val context = LocalContext.current
    val c = context.container
    val pending = remember { arrayOfNulls<String>(1) }
    val take = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val rel = pending[0]
        if (ok && rel != null && c.file(rel).length() > 0) onDone(rel) else {
            rel?.let { c.file(it).delete() }
            onDone(null)
        }
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult onDone(null)
        val rel = c.newFile(folder, "jpg")
        onDone(if (Photos.copy(context, uri, c.file(rel))) rel else null)
    }
    return remember {
        Camera(
            shoot = {
                val rel = c.newFile(folder, "jpg")
                pending[0] = rel
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", c.file(rel))
                runCatching { take.launch(uri) }.onFailure { onDone(null) }
            },
            pick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        )
    }
}
