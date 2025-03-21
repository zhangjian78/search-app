package jerry.gadgets.gsearch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import jerry.gadgets.R

class SearchService : Service() {
    companion object {
        val START_SERVICE = "start"
        val STOP_SERVICE = "stop"
        val FOREGROUND_SERVICE = "foreground"
        const val TAG = "TabGmailSearchService"
    }

    var isForeGroundService = false

    private val CHANNEL_ID: String = "channelId"

    inner class LocalBinder : Binder() {
        fun getService(): SearchService = this@SearchService
    }

    override fun onCreate() {
        super.onCreate()
        isForeGroundService = false
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (val intentAction = intent?.action) {
            START_SERVICE -> {
                // showToast("Service started")
            }
            STOP_SERVICE -> stopService()
            FOREGROUND_SERVICE -> doForegroundThings()
            else -> {
                // showToast(intentAction ?: "Empty action intent")
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun doForegroundThings() {
        // showToast("Going foreground")
        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        notificationIntent.putExtra("notification", true)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        isForeGroundService = true
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.toolbar_icn_search)
            .setContentTitle(getString(R.string.app_name))
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            // .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))

        val notification = builder.build()
        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(4, notification)
        }

        startForeground(4, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = resources.getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun stopService() {
        // showToast("Service stopping")
        try {
            stopForeground(true)
            isForeGroundService = false
            stopSelf()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
