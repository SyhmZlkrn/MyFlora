package com.example.flora.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.flora.data.database.PlantCareDatabase
import java.util.concurrent.TimeUnit

/**
 * Periodically scans CareSchedule rows; any task due within the next 12 h
 * fires a notification. Duplicates are suppressed by using the task id as the
 * notification id.
 */
class CareReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Scope reminders to the currently logged-in user. Notifications must not leak
        // across accounts on a shared device.
        val prefs = applicationContext.getSharedPreferences("flora_prefs", Context.MODE_PRIVATE)
        val userId = prefs.getInt("logged_in_user_id", -1)
        if (userId == -1) {
            // No one logged in (guest or logged out) → wipe any lingering reminders.
            FloraNotifications.cancelAllCareReminders(applicationContext)
            return Result.success()
        }
        // Respect user's notification preferences.
        if (!prefs.getBoolean("notif_master", true) || !prefs.getBoolean("notif_care_reminders", true)) {
            return Result.success()
        }

        val db = PlantCareDatabase.getInstance(applicationContext)
        val cutoff = System.currentTimeMillis() + LOOKAHEAD_MILLIS
        val due = db.careScheduleDao().getDueBeforeForUser(cutoff, userId)

        for (task in due) {
            val dueInMs = task.nextDueDate - System.currentTimeMillis()
            val dueText = when {
                dueInMs <= 0 -> "Overdue — tap to mark done."
                dueInMs < TimeUnit.HOURS.toMillis(3) -> "Due soon (${TimeUnit.MILLISECONDS.toHours(dueInMs)} h). ${task.dueTime}."
                else -> "Due today at ${task.dueTime}."
            }
            FloraNotifications.postCareReminder(
                context = applicationContext,
                id = task.id,
                plantName = task.plantName,
                taskType = task.taskType,
                dueText = dueText
            )
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "flora_care_reminder_worker"
        private val LOOKAHEAD_MILLIS = TimeUnit.HOURS.toMillis(12)

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<CareReminderWorker>(
                6, TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                req
            )
        }
    }
}
