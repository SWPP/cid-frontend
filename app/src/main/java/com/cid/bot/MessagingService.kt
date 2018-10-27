package com.cid.bot

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MessagingService : FirebaseMessagingService() {
    companion object {
        const val CHANNEL_ID = "noti_channel"
        const val ACTION_MESSAGE_RECEIVED = "ACTION_MESSAGE_RECEIVED"
    }

    override fun onNewToken(token: String?) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(m: RemoteMessage) {
        super.onMessageReceived(m)

        if (isAppBackground(this)) {
            notify(m.data)
        } else {
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_MESSAGE_RECEIVED).apply {
                putExtra("message", m.data.toString())
            })
        }
    }

    private fun notify(data: Map<String, String>) {
        val title = data["title"]
        val body = data["body"]
        if (title == null || body == null) return

        val intent = Intent(this, ChatActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("Notification", body)
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_foreground) // TODO: replace to larger icon image
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(sound)
                .setContentIntent(pendingIntent)

        with (this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(CHANNEL_ID, "μBot Notification", NotificationManager.IMPORTANCE_DEFAULT)
                createNotificationChannel(channel)
            }
            notify(2, builder.build())
        }
    }

    private fun isAppBackground(context: Context): Boolean {
        val runningProcesses = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).runningAppProcesses
        for (processInfo in runningProcesses) {
            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                for (process in processInfo.pkgList) {
                    if (process == context.packageName) {
                        return false
                    }
                }
            }
        }
        return true
    }
}
