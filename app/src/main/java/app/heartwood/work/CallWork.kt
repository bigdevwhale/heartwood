package app.heartwood.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.heartwood.BuildConfig
import app.heartwood.MainActivity
import app.heartwood.R
import app.heartwood.container
import app.heartwood.domain.Calls
import app.heartwood.domain.Species
import app.heartwood.domain.day
import app.heartwood.localized
import app.heartwood.ru
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** A few times a day: does any tree have a reason to call? */
class CallWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        CallWork.check(applicationContext)
        return Result.success()
    }
}

object CallWork {
    private const val CHANNEL = "calls"
    const val EXTRA_TREE = "tree"

    fun schedule(context: Context) {
        if (BuildConfig.DEMO) return
        val req = PeriodicWorkRequestBuilder<CallWorker>(4, TimeUnit.HOURS).build()
        // Robolectric renders have no WorkManager; a phone always does.
        runCatching { WorkManager.getInstance(context).enqueueUniquePeriodicWork("calls", ExistingPeriodicWorkPolicy.KEEP, req) }
    }

    fun allowed(context: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    suspend fun check(context: Context) {
        val c = context.container
        val s = c.settings.now()
        if (!s.calls || !allowed(context)) return
        // Daytime only: a tree doesn't call at night.
        if (LocalTime.now().hour !in 9..19) return
        val dao = c.db.dao()
        val trees = dao.trees()
        if (trees.isEmpty()) return
        val visits = dao.visits()
        val firstMet = visits.groupBy { it.treeId }.mapValues { (_, v) -> day(v.minOf { it.at }) }
        val today = LocalDate.now()
        val snowAt = trees.filter { c.weatherAt(it)?.sky?.snow == true }.map { it.id }.toSet()
        val r = context.localized()
        val call = Calls.pick(trees, firstMet, dao.notes(), snowAt, s.callLog, today, System.currentTimeMillis(), r.ru) ?: return
        val sp = Species.of(call.tree.species)
        show(context, call.tree.id, "«${call.tree.name}» · ${sp.title(r.ru, call.tree.kind)}", call.text)
        c.settings.update {
            it.copy(lastCallAt = System.currentTimeMillis(), callLog = Calls.append(it.callLog, Calls.id(call.tree, call.key, Calls.year(call, today)), System.currentTimeMillis()))
        }
    }

    private fun show(context: Context, treeId: Long, title: String, text: String) {
        val m = context.getSystemService(NotificationManager::class.java)
        if (m.getNotificationChannel(CHANNEL) == null) {
            m.createNotificationChannel(NotificationChannel(CHANNEL, context.localized().getString(R.string.channel_calls), NotificationManager.IMPORTANCE_DEFAULT))
        }
        val open = PendingIntent.getActivity(
            context, treeId.toInt(), Intent(context, MainActivity::class.java).putExtra(EXTRA_TREE, treeId).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_note)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify(treeId.toInt(), n)
    }
}
