package com.example.flora.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.flora.data.database.PlantCareDatabase
import com.example.flora.data.database.entities.CareSchedule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class CareScheduleViewModel(application: Application) : AndroidViewModel(application) {
    private val db = PlantCareDatabase.getInstance(application)
    private val scheduleDao = db.careScheduleDao()
    private val prefs = application.getSharedPreferences("flora_prefs", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val _userId = MutableStateFlow<Int?>(null)

    // Per-day completion counter so the "Current Progress" bar reflects today's work
    // even after [completeAndAdvance] rolls a task into the future.
    // Starts empty — populated once a userId is set so accounts don't share state.
    private val _doneToday = MutableStateFlow(0)
    val doneToday: StateFlow<Int> = _doneToday.asStateFlow()

    /**
     * Snapshot of a task at the moment the user ticked it complete.
     * Used to let the user undo the advance — restore original due date, delete
     * the PlantHealthLog row inserted at tick time, and restore the plant's
     * previous lastWatered timestamp. Persisted in prefs (per user, 7-day TTL).
     */
    data class CompletionRecord(
        val taskId: Int,
        val plantId: Int,
        val plantName: String,
        val taskType: String,
        val intervalDays: Int,
        val originalDueDate: Long,
        val completedAt: Long,
        /** Row id of the PlantHealthLog inserted at completion. 0 = none. */
        val healthLogId: Int,
        /** plant.lastWatered before this completion overwrote it. null = was unset. */
        val previousLastWatered: Long?,
    )

    private val _recent = MutableStateFlow<List<CompletionRecord>>(emptyList())
    val recent: StateFlow<List<CompletionRecord>> = _recent.asStateFlow()

    val allTasks: StateFlow<List<CareSchedule>> = _userId.flatMapLatest { userId ->
        userId?.let(scheduleDao::getTasksByUser) ?: flowOf(emptyList())
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setUserId(id: Int?) {
        if (_userId.value == id) return
        _userId.value = id
        // Reload per-account progress + history. Logged-out → empty.
        if (id == null) {
            _doneToday.value = 0
            _recent.value = emptyList()
        } else {
            _doneToday.value = loadDoneTodayFromPrefs(id)
            _recent.value = loadRecentFromPrefs(id)
        }
    }

    fun getTasksByPlant(plantId: Int): Flow<List<CareSchedule>> =
        _userId.value?.let { scheduleDao.getTasksByPlant(it, plantId) } ?: flowOf(emptyList())

    fun toggleTaskCompletion(taskId: Int, completed: Boolean) {
        val userId = _userId.value ?: return
        viewModelScope.launch {
            scheduleDao.setCompleted(taskId, userId, completed)
        }
    }

    /**
     * Mark a task as done and immediately roll it forward by its interval.
     * Hibiscus (interval 2) ticked today → nextDueDate becomes 2 days from now and
     * the task reappears in the corresponding date group, ready to be done again.
     * Updates [com.example.flora.data.database.entities.Plant.lastWatered] if a
     * matching plant exists, so the watering history reflects reality.
     */
    fun completeAndAdvance(taskId: Int) {
        val userId = _userId.value ?: return
        viewModelScope.launch {
            ensureFreshDay()
            // Use the fresh DB-side row so we don't update a stale snapshot, which
            // can happen when the user confirms a delayed dialog and allTasks has
            // since re-emitted.
            val task = allTasks.value.firstOrNull { it.id == taskId } ?: return@launch
            val interval = task.intervalDays.coerceAtLeast(1)
            val now = System.currentTimeMillis()
            // For early-water: ensure new date is *strictly later than the original*
            // due date — otherwise the task can appear to stay in its old slot when
            // `now + interval` lands on the same calendar day as `task.nextDueDate`.
            // Schedule "slips" by one interval from whichever is later — now, or the
            // original due date. This way:
            //   * Late/on-time ticks: nextDue = now + interval (normal cycle reset)
            //   * Early ticks:        nextDue = originalDue + interval (forward shift)
            // so an early-watered task always lands on a later calendar day than where
            // it started, and visibly moves out of its current date group.
            val baseline = maxOf(now, task.nextDueDate)
            val nextDue = baseline + interval * 86_400_000L
            val advanced = task.copy(
                isCompleted = false,
                nextDueDate = nextDue,
            )
            android.util.Log.d(
                "CareScheduleVM",
                "completeAndAdvance(id=$taskId) ${task.plantName}: was=${task.nextDueDate} -> now=$nextDue (interval=${interval}d)",
            )
            scheduleDao.update(advanced)

            // Mirror the action onto the plant row when the task is a watering one.
            // Capture the pre-update lastWatered so undo can restore it (otherwise
            // My Plants / Plant Detail keep showing the new value after rollback).
            var previousLastWatered: Long? = null
            if (task.taskType.equals("Water", ignoreCase = true)) {
                val plant = db.plantDao().getPlantByIdForUser(userId, task.plantId)
                if (plant != null) {
                    previousLastWatered = plant.lastWatered
                    db.plantDao().update(plant.copy(lastWatered = now))
                }
            }

            // Log the tick to PlantHealthLog so the user can see history per plant.
            // Capture row id so undo can delete this exact entry.
            val healthLogId = db.plantHealthLogDao().insertReturningId(
                com.example.flora.data.database.entities.PlantHealthLog(
                    plantId = task.plantId,
                    logType = task.taskType,
                    healthScore = 100,
                    notes = "Auto-logged from care schedule",
                    isResolved = true,
                    timestamp = now,
                )
            ).toInt()

            // Bump today's "Current Progress" counter.
            val next = _doneToday.value + 1
            _doneToday.value = next
            saveDoneTodayToPrefs(next)

            // Push a record onto the undo history (keep last 10, newest first).
            val record = CompletionRecord(
                taskId = task.id,
                plantId = task.plantId,
                plantName = task.plantName,
                taskType = task.taskType,
                intervalDays = task.intervalDays,
                originalDueDate = task.nextDueDate,
                completedAt = now,
                healthLogId = healthLogId,
                previousLastWatered = previousLastWatered,
            )
            val updated = (listOf(record) + _recent.value).take(MAX_RECENT)
            _recent.value = updated
            saveRecentToPrefs(userId, updated)
        }
    }

    /**
     * Revert a previously-completed task in full:
     *  - Restore the schedule's original nextDueDate.
     *  - Delete the PlantHealthLog row inserted at tick time (so the plant's
     *    Health Logs tab + active history match the schedule history).
     *  - Restore Plant.lastWatered to its pre-tick value (so My Plants and
     *    Plant Detail "Watered N days ago" reflect the rollback).
     *  - Decrement today's progress counter and drop the record from history.
     */
    fun undoCompletion(record: CompletionRecord) {
        val userId = _userId.value ?: return
        viewModelScope.launch {
            val current = allTasks.value.firstOrNull { it.id == record.taskId }
            if (current != null) {
                scheduleDao.update(current.copy(nextDueDate = record.originalDueDate, isCompleted = false))
            }
            // Delete the matching log row, if we still have its id (records persisted
            // before this change have healthLogId == 0).
            if (record.healthLogId > 0) {
                db.plantHealthLogDao().deleteById(record.healthLogId)
            }
            // Restore plant.lastWatered for watering tasks. Only do it if the current
            // value still matches what we set at tick time — otherwise the user has
            // watered again since and we'd clobber that newer value.
            if (record.taskType.equals("Water", ignoreCase = true) && record.plantId > 0) {
                val plant = db.plantDao().getPlantByIdForUser(userId, record.plantId)
                if (plant != null && plant.lastWatered == record.completedAt) {
                    db.plantDao().update(plant.copy(lastWatered = record.previousLastWatered))
                }
            }
            val filtered = _recent.value.filterNot { it === record || it.completedAt == record.completedAt && it.taskId == record.taskId }
            _recent.value = filtered
            saveRecentToPrefs(userId, filtered)
            val next = (_doneToday.value - 1).coerceAtLeast(0)
            _doneToday.value = next
            saveDoneTodayToPrefs(next)
        }
    }

    // ── Daily progress counter ──────────────────────────────────────────────
    // Persisted per-user in flora_prefs as { progress_date_<uid>, progress_done_today_<uid> }.
    // Per-user keys keep guest / registered account counters from cross-bleeding.
    // If the stored date != today, treat counter as 0.

    private fun progressDateKey(userId: Int) = "progress_date_$userId"
    private fun progressDoneKey(userId: Int) = "progress_done_today_$userId"
    private fun recentKey(userId: Int) = "recent_completions_$userId"

    private fun loadDoneTodayFromPrefs(userId: Int): Int {
        val storedDate = prefs.getString(progressDateKey(userId), null)
        val today = dateFormat.format(Date())
        return if (storedDate == today) prefs.getInt(progressDoneKey(userId), 0) else 0
    }

    /**
     * Reset [_doneToday] to 0 when the stored progress date is stale (date rolled over).
     * Called on every completion and via [refreshIfNewDay] so VMs that stay alive
     * across midnight don't keep showing yesterday's count.
     */
    private fun ensureFreshDay() {
        val userId = _userId.value ?: return
        val storedDate = prefs.getString(progressDateKey(userId), null)
        val today = dateFormat.format(Date())
        if (storedDate != today) {
            _doneToday.value = 0
            prefs.edit()
                .putString(progressDateKey(userId), today)
                .putInt(progressDoneKey(userId), 0)
                .apply()
        }
        pruneStaleRecent()
    }

    /** Drop records older than RECENT_TTL_MILLIS (24h). */
    private fun pruneStaleRecent() {
        val userId = _userId.value ?: return
        val cutoff = System.currentTimeMillis() - RECENT_TTL_MILLIS
        val pruned = _recent.value.filter { it.completedAt >= cutoff }
        if (pruned.size != _recent.value.size) {
            _recent.value = pruned
            saveRecentToPrefs(userId, pruned)
        }
    }

    /** Call from screen resume so a long-running app catches the day flip live. */
    fun refreshIfNewDay() {
        ensureFreshDay()
    }

    private fun saveDoneTodayToPrefs(n: Int) {
        val userId = _userId.value ?: return
        prefs.edit()
            .putString(progressDateKey(userId), dateFormat.format(Date()))
            .putInt(progressDoneKey(userId), n)
            .apply()
    }

    // ── Recent completions persistence (7-day TTL) ──────────────────────────
    // Stored per-user as a single JSON array under "recent_completions_<uid>".

    private fun loadRecentFromPrefs(userId: Int): List<CompletionRecord> {
        val raw = prefs.getString(recentKey(userId), null) ?: return emptyList()
        return try {
            val arr = org.json.JSONArray(raw)
            val cutoff = System.currentTimeMillis() - RECENT_TTL_MILLIS
            val out = ArrayList<CompletionRecord>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val completedAt = o.optLong("completedAt", 0L)
                if (completedAt < cutoff) continue
                out.add(
                    CompletionRecord(
                        taskId = o.optInt("taskId"),
                        plantId = o.optInt("plantId", 0),
                        plantName = o.optString("plantName"),
                        taskType = o.optString("taskType"),
                        intervalDays = o.optInt("intervalDays", 1),
                        originalDueDate = o.optLong("originalDueDate"),
                        completedAt = completedAt,
                        healthLogId = o.optInt("healthLogId", 0),
                        previousLastWatered = if (o.isNull("previousLastWatered")) null
                            else o.optLong("previousLastWatered"),
                    )
                )
            }
            out
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveRecentToPrefs(userId: Int, records: List<CompletionRecord>) {
        val arr = org.json.JSONArray()
        for (r in records) {
            arr.put(
                org.json.JSONObject()
                    .put("taskId", r.taskId)
                    .put("plantId", r.plantId)
                    .put("plantName", r.plantName)
                    .put("taskType", r.taskType)
                    .put("intervalDays", r.intervalDays)
                    .put("originalDueDate", r.originalDueDate)
                    .put("completedAt", r.completedAt)
                    .put("healthLogId", r.healthLogId)
                    .put("previousLastWatered", r.previousLastWatered ?: org.json.JSONObject.NULL)
            )
        }
        prefs.edit().putString(recentKey(userId), arr.toString()).apply()
    }

    fun addTask(schedule: CareSchedule) {
        val userId = _userId.value ?: return
        viewModelScope.launch { scheduleDao.insert(schedule.copy(userId = userId)) }
    }

    fun deleteTask(schedule: CareSchedule) {
        val userId = _userId.value ?: return
        viewModelScope.launch { scheduleDao.deleteById(schedule.id, userId) }
    }

    companion object {
        private const val MAX_RECENT = 10
        private const val RECENT_TTL_MILLIS = 7L * 24L * 60L * 60L * 1000L  // 7 days
    }
}
