package cat.elnostreequip.app.notifications

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cat.elnostreequip.app.ElNostreEquipApp
import cat.elnostreequip.app.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Recibe los push de FCM (topics) y los muestra como notificación local.
 * Sin google-services.json este servicio no se activará en runtime; compila igual.
 */
class ElNostreEquipMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: "El Nostre Equip"
        val body = message.notification?.body ?: data["body"] ?: return
        Log.d(TAG, "Push recibido: $title — $body")
        showNotification(title, body, data["articleId"]?.toLongOrNull())
    }

    override fun onNewToken(token: String) {
        // Sin servidor de tokens: no hay nada que registrar. Los topics ya están suscritos.
        Log.d(TAG, "Token FCM renovado")
    }

    private fun showNotification(title: String, body: String, articleId: Long?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            articleId?.let { putExtra(EXTRA_ARTICLE_ID, it) }
        }
        val pending = PendingIntent.getActivity(
            this, articleId?.toInt() ?: 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, ElNostreEquipApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        try {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Sin permiso POST_NOTIFICATIONS: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "EneMessaging"
        const val EXTRA_ARTICLE_ID = "article_id"
    }
}
