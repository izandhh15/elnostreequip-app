package cat.elnostreequip.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import cat.elnostreequip.app.ElNostreEquipApp
import cat.elnostreequip.app.MainActivity
import java.util.concurrent.TimeUnit

/**
 * Worker que comprova els partits en directe cada 15 min (mínim d'Android)
 * i envia notificacions quan:
 * - Es publica l'alineació (partit passa a prèvia amb XI)
 * - Comença el partit (0' Inicia el partit)
 * - Hi ha un gol (¡GOOOOOL!)
 *
 * L'estat anterior es guarda a SharedPreferences per detectar canvis.
 */
class MatchNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "match_notifications"
        const val CHANNEL_ID = "partits_en_directe"
        private const val PREFS = "match_notif_state"
        private const val KEY_PREFIX = "partit_"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MatchNotificationWorker>(
                15, TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as ElNostreEquipApp
            val repo = app.repository
            val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

            val partits = repo.partitPosts()
            for (partit in partits) {
                val live = repo.matchLive(partit.id) ?: continue
                val prevState = prefs.getString("${KEY_PREFIX}${partit.id}_estat", null)
                val prevGolsLocal = prefs.getInt("${KEY_PREFIX}${partit.id}_gols_local", -1)
                val prevGolsRival = prefs.getInt("${KEY_PREFIX}${partit.id}_gols_rival", -1)

                // 1. Partit passa a prèvia (alineació publicada)
                if (live.estat == "pre" && prevState != "pre") {
                    val teamName = extractValenciaTeam(partit.title)
                    val rival = extractRival(partit.title, teamName)
                    sendNotification(
                        title = "ALINEACIÓ DEL $teamName",
                        text = "Contra el $rival",
                        partitId = partit.id
                    )
                }

                // 2. Comença el partit
                if (live.estat == "primera" && prevState != "primera") {
                    sendNotification(
                        title = "0' Inicia el partit",
                        text = partit.title,
                        partitId = partit.id
                    )
                }

                // 3. Gol del València (gols_local puja)
                if (prevGolsLocal >= 0 && live.golsLocal > prevGolsLocal) {
                    // Intentar obtenir la golejadora de la cronologia
                    val timeline = repo.matchTimeline(partit.link)
                    val lastGoal = timeline.lastOrNull { it.text.contains("gol", ignoreCase = true) }
                    val scorerText = lastGoal?.text ?: "Gol del València"
                    sendNotification(
                        title = "¡GOOOOOL!",
                        text = "$scorerText · Min. ${live.minutDirecte}",
                        partitId = partit.id
                    )
                }

                // 4. Gol del rival
                if (prevGolsRival >= 0 && live.golsRival > prevGolsRival) {
                    sendNotification(
                        title = "Gol del rival",
                        text = "${partit.title} · ${live.golsLocal} - ${live.golsRival} · Min. ${live.minutDirecte}",
                        partitId = partit.id
                    )
                }

                // Guardar estat actual
                prefs.edit()
                    .putString("${KEY_PREFIX}${partit.id}_estat", live.estat)
                    .putInt("${KEY_PREFIX}${partit.id}_gols_local", live.golsLocal)
                    .putInt("${KEY_PREFIX}${partit.id}_gols_rival", live.golsRival)
                    .apply()
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    /**
     * Extreu el nom de l'equip del València del títol del partit.
     * Ex: "València B - Murcia" → "VALÈNCIA B"
     */
    private fun extractValenciaTeam(title: String): String {
        val lower = title.lowercase()
        return when {
            "valència b" in lower || "valencia b" in lower -> "VALÈNCIA B"
            "valència c" in lower || "valencia c" in lower -> "VALÈNCIA C"
            "valència d" in lower || "valencia d" in lower -> "VALÈNCIA D"
            else -> "VALÈNCIA"
        }
    }

    /** Extreu el rival del títol (la part que no és València). */
    private fun extractRival(title: String, valenciaTeam: String): String {
        val parts = title.split("-", "vs", "VS", "–").map { it.trim() }
        for (part in parts) {
            if (!part.lowercase().contains("valència") && !part.lowercase().contains("valencia")) {
                return part.uppercase()
            }
        }
        return "RIVAL"
    }

    private fun sendNotification(title: String, text: String, partitId: Long) {
        val context = applicationContext
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Partits en directe",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificacions de partits en directe"
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, partitId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(partitId.toInt(), notification)
    }
}
