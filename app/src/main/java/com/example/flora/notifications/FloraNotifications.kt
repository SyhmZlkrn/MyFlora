package com.example.flora.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.flora.R
import com.example.flora.ui.MainActivity

object FloraNotifications {
    const val CHANNEL_CARE = "flora_care_reminders"
    private const val CHANNEL_CARE_NAME = "Plant care reminders"
    private const val CHANNEL_CARE_DESC = "Watering, fertilizing, and other care tasks"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_CARE) == null) {
            val channel = NotificationChannel(
                CHANNEL_CARE,
                CHANNEL_CARE_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = CHANNEL_CARE_DESC }
            nm.createNotificationChannel(channel)
        }
    }

    fun postCareReminder(
        context: Context,
        id: Int,
        plantName: String,
        taskType: String,
        dueText: String,
        deepLinkRoute: String = "care_schedule"
    ) {
        ensureChannels(context)
        if (!canNotify(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("deeplink", deepLinkRoute)
        }
        val pi = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "$taskType due: $plantName"
        val body = dueText.ifBlank { "Tap to view today's care schedule." }

        val notif = NotificationCompat.Builder(context, CHANNEL_CARE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .addAction(
                android.R.drawable.ic_menu_view,
                "Open Schedule",
                pi
            )
            .build()

        NotificationManagerCompat.from(context).notify(id, notif)
    }

    /**
     * Drops every active care-reminder notification. Call on logout / account switch
     * so the next user never sees the previous user's reminders.
     */
    fun cancelAllCareReminders(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }

    private fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
