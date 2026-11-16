package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Visit

class VisitReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val body = intent.getStringExtra(EXTRA_BODY) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 5001)
        VisitReminderScheduler.showNotification(context, title, body, notifId)
    }

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_NOTIF_ID = "extra_notif_id"
    }
}

object VisitReminderScheduler {
    private const val REMINDER_CHANNEL_ID = "bubbles_reminders"
    private const val ALERTS_CHANNEL_ID = "bubbles_alerts"
    private val OFFSETS_MINUTES = listOf(30L, 15L, 10L)

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val reminders = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "تذكيرات الزيارات",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات قبل موعد الزيارة بـ 30 و 15 و 10 دقائق"
            }
            val alerts = NotificationChannel(
                ALERTS_CHANNEL_ID,
                "تنبيهات التشغيل والوصول",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات الدوام والوصول والتأخير والتحصيل"
            }
            nm.createNotificationChannel(reminders)
            nm.createNotificationChannel(alerts)
        }
    }

    fun syncVisitReminders(context: Context, visits: List<Visit>, isArabic: Boolean) {
        ensureChannels(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()

        visits.forEach { visit ->
            val isActiveUpcoming = visit.status in listOf("NOT_STARTED", "EN_ROUTE") && visit.scheduledAtEpochMs > now
            OFFSETS_MINUTES.forEach { offsetMin ->
                val reqCode = (visit.id + "_" + offsetMin).hashCode()
                val triggerAt = visit.scheduledAtEpochMs - offsetMin * 60_000L
                val title = if (isArabic) {
                    "تذكير زيارة بعد $offsetMin دقيقة"
                } else {
                    "Visit reminder in $offsetMin min"
                }
                val body = if (isArabic) {
                    "${visit.customerName ?: ""} - ${visit.locationName} (${visit.operationType})"
                } else {
                    "${visit.customerName ?: ""} - ${visit.locationName} (${visit.operationType})"
                }
                val intent = Intent(context, VisitReminderReceiver::class.java).apply {
                    putExtra(VisitReminderReceiver.EXTRA_TITLE, title)
                    putExtra(VisitReminderReceiver.EXTRA_BODY, body)
                    putExtra(VisitReminderReceiver.EXTRA_NOTIF_ID, reqCode)
                }
                val pending = PendingIntent.getBroadcast(
                    context,
                    reqCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                if (isActiveUpcoming && triggerAt > now) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                } else {
                    alarmManager.cancel(pending)
                }
            }
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        body: String,
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt(),
        channelId: String = REMINDER_CHANNEL_ID
    ) {
        ensureChannels(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
}
