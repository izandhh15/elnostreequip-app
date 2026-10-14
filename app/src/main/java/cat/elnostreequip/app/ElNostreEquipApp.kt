package cat.elnostreequip.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.room.Room
import cat.elnostreequip.app.data.ElNostreEquipRepository
import cat.elnostreequip.app.data.MatchNotificationWorker
import cat.elnostreequip.app.data.api.EneClient
import cat.elnostreequip.app.data.api.RetrofitClient
import cat.elnostreequip.app.data.db.AppDatabase
import cat.elnostreequip.app.data.db.MIGRATION_1_2
import cat.elnostreequip.app.data.db.MIGRATION_2_3
import cat.elnostreequip.app.notifications.TopicManager

class ElNostreEquipApp : Application() {

    lateinit var repository: ElNostreEquipRepository
        private set
    lateinit var topicManager: TopicManager
        private set

    override fun onCreate() {
        super.onCreate()
        val db = Room.databaseBuilder(this, AppDatabase::class.java, "ene.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
        repository = ElNostreEquipRepository(RetrofitClient.api, db, EneClient.api)
        topicManager = TopicManager(this)
        createNotificationChannel()
        // Programar notificacions de partits en directe (cada 15 min)
        MatchNotificationWorker.schedule(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "El Nostre Equip",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notícies, dia de partit i directe dels teus equips" }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "ene_general"
    }
}
