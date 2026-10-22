package app.heartwood.sense

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

/** A position fix: where the phone is and how sure it is. */
data class Fix(val lat: Double, val lon: Double, val accuracy: Float, val at: Long)

/**
 * Where the phone is, while a screen listens. Plain LocationManager (no Google services): GPS,
 * with network fixes until the satellites answer.
 */
class Here(private val context: Context, private val onFix: (Fix) -> Unit) {
    private val lm = context.getSystemService(LocationManager::class.java)
    private val listener = LocationListener { l -> deliver(l) }

    fun permitted(): Boolean = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun start() {
        if (!permitted()) return
        try {
            val providers = lm.getProviders(true)
            if (LocationManager.GPS_PROVIDER in providers) lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener, Looper.getMainLooper())
            if (LocationManager.NETWORK_PROVIDER in providers) lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000L, 0f, listener, Looper.getMainLooper())
            providers.mapNotNull { lm.getLastKnownLocation(it) }
                .filter { System.currentTimeMillis() - it.time < 5 * 60_000L }
                .minByOrNull { it.accuracy }?.let { deliver(it) }
        } catch (_: SecurityException) {
        } catch (_: IllegalArgumentException) {
        }
    }

    fun stop() = lm.removeUpdates(listener)

    private fun deliver(l: Location) = onFix(Fix(l.latitude, l.longitude, if (l.hasAccuracy()) l.accuracy else 50f, l.time))
}

/**
 * The hug: the proximity sensor covered (phone against the chest) and the phone held still
 * (pressed between you and the trunk). Phones without a proximity sensor fall back to holding a button.
 */
class HugSense(context: Context) : SensorEventListener {
    private val sm = context.getSystemService(SensorManager::class.java)
    private val proximity: Sensor? = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val accel: Sensor? = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val available = proximity != null

    var covered by mutableStateOf(false)
        private set
    var still by mutableStateOf(true)
        private set

    private var jiggle = 0f
    private var last = FloatArray(3)

    fun start() {
        proximity?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        accel?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    fun stop() = sm.unregisterListener(this)

    override fun onSensorChanged(e: SensorEvent) {
        when (e.sensor.type) {
            Sensor.TYPE_PROXIMITY -> covered = e.values[0] < (proximity?.maximumRange ?: 5f).coerceAtMost(3f)
            Sensor.TYPE_ACCELEROMETER -> {
                val d = abs(e.values[0] - last[0]) + abs(e.values[1] - last[1]) + abs(e.values[2] - last[2])
                last = e.values.copyOf()
                jiggle = jiggle * .85f + d * .15f
                still = jiggle < 1.2f
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

/** Is the phone lying face down? For sitting beside the tree. */
class FaceDown(context: Context) : SensorEventListener {
    private val sm = context.getSystemService(SensorManager::class.java)
    private val accel: Sensor? = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val available = accel != null
    var down by mutableStateOf(false)
        private set

    fun start() { accel?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) } }
    fun stop() = sm.unregisterListener(this)

    override fun onSensorChanged(e: SensorEvent) {
        val (x, y, z) = Triple(e.values[0], e.values[1], e.values[2])
        down = z < -7.5f && sqrt(x * x + y * y) < 4f
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

/** A heartbeat in the hand: lub-dub. */
class Heart(context: Context) {
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java).defaultVibrator
    else @Suppress("DEPRECATION") context.getSystemService(Vibrator::class.java)

    fun beat(strength: Float = 1f) {
        if (!vibrator.hasVibrator()) return
        val a = (200 * strength).toInt().coerceIn(30, 255)
        vibrator.vibrate(
            if (vibrator.hasAmplitudeControl()) VibrationEffect.createWaveform(longArrayOf(0, 30, 120, 24), intArrayOf(0, a, 0, a * 2 / 3), -1)
            else VibrationEffect.createWaveform(longArrayOf(0, 30, 120, 24), -1),
        )
    }

    fun done() {
        if (vibrator.hasVibrator()) vibrator.vibrate(VibrationEffect.createOneShot(400, if (vibrator.hasAmplitudeControl()) 90 else VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

/** Ten seconds of the bark: a small AAC file and its loudness, one byte per 50 ms. */
class Recorder(private val context: Context) {
    private var rec: MediaRecorder? = null
    private val levels = ArrayList<Int>()

    fun permitted() = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun start(file: File): Boolean = runCatching {
        file.parentFile?.mkdirs()
        levels.clear()
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.UNPROCESSED.takeIf { Build.VERSION.SDK_INT >= 29 } ?: MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioSamplingRate(44100)
        r.setAudioEncodingBitRate(96_000)
        r.setOutputFile(file.absolutePath)
        r.prepare()
        r.start()
        rec = r
        true
    }.getOrElse {
        // Some phones refuse the unprocessed source; the plain microphone always works.
        runCatching {
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            rec = r
            true
        }.getOrDefault(false)
    }

    /** The loudness now, 0..1, and remembered for the waveform. */
    fun level(): Float {
        val a = runCatching { rec?.maxAmplitude ?: 0 }.getOrDefault(0)
        val v = (sqrt(a / 32767f) * 255).toInt().coerceIn(0, 255)
        levels += v
        return v / 255f
    }

    fun stop(): String {
        runCatching { rec?.stop() }
        rec?.release()
        rec = null
        return Waves.encode(levels)
    }
}

object Waves {
    fun encode(levels: List<Int>): String = Base64.encodeToString(ByteArray(levels.size) { levels[it].toByte() }, Base64.NO_WRAP)
    fun decode(s: String): List<Float> = runCatching { Base64.decode(s, Base64.NO_WRAP).map { (it.toInt() and 255) / 255f } }.getOrDefault(emptyList())
}

/** Plays one voice at a time. */
object Voices {
    private var player: MediaPlayer? = null
    var playing by mutableStateOf<String?>(null)
        private set

    fun toggle(file: File, key: String) {
        if (playing == key) {
            stop()
            return
        }
        stop()
        runCatching {
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { stop() }
                prepare()
                start()
            }
            playing = key
        }
    }

    fun stop() {
        runCatching { player?.stop() }
        player?.release()
        player = null
        playing = null
    }
}
