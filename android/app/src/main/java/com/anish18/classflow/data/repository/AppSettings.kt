package com.anish18.classflow.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettings @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("classflow_prefs", Context.MODE_PRIVATE)

    private val _backgroundStyle = MutableStateFlow(prefs.getString("background_style", "Dark") ?: "Dark")
    val backgroundStyle: StateFlow<String> = _backgroundStyle.asStateFlow()

    private val _showTasksOnTimetable = MutableStateFlow(prefs.getBoolean("show_tasks_on_timetable", true))
    val showTasksOnTimetable: StateFlow<Boolean> = _showTasksOnTimetable.asStateFlow()

    // Persistent notification states
    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean("notifications_enabled", true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _classReminderBuffer = MutableStateFlow(prefs.getInt("class_reminder_buffer", 10))
    val classReminderBuffer: StateFlow<Int> = _classReminderBuffer.asStateFlow()

    private val _taskReminderEnabled = MutableStateFlow(prefs.getBoolean("task_reminder_enabled", true))
    val taskReminderEnabled: StateFlow<Boolean> = _taskReminderEnabled.asStateFlow()

    private val _taskReminderBuffer = MutableStateFlow(prefs.getInt("task_reminder_buffer", 30))
    val taskReminderBuffer: StateFlow<Int> = _taskReminderBuffer.asStateFlow()

    fun setBackgroundStyle(style: String) {
        prefs.edit().putString("background_style", style).apply()
        _backgroundStyle.value = style
    }

    fun setShowTasksOnTimetable(show: Boolean) {
        prefs.edit().putBoolean("show_tasks_on_timetable", show).apply()
        _showTasksOnTimetable.value = show
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
        _notificationsEnabled.value = enabled
    }

    fun setClassReminderBuffer(buffer: Int) {
        prefs.edit().putInt("class_reminder_buffer", buffer).apply()
        _classReminderBuffer.value = buffer
    }

    fun setTaskReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("task_reminder_enabled", enabled).apply()
        _taskReminderEnabled.value = enabled
    }

    fun setTaskReminderBuffer(buffer: Int) {
        prefs.edit().putInt("task_reminder_buffer", buffer).apply()
        _taskReminderBuffer.value = buffer
    }

    private val _studyModeEnabled = MutableStateFlow(prefs.getBoolean("study_mode_enabled", false))
    val studyModeEnabled: StateFlow<Boolean> = _studyModeEnabled.asStateFlow()

    fun setStudyModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("study_mode_enabled", enabled).apply()
        _studyModeEnabled.value = enabled
    }


    fun getSavedRingerMode(): Int {
        return prefs.getInt("saved_ringer_mode", -1)
    }

    fun saveRingerMode(mode: Int) {
        prefs.edit().putInt("saved_ringer_mode", mode).apply()
    }

    private val _calendarSyncEnabled = MutableStateFlow(prefs.getBoolean("calendar_sync_enabled", false))
    val calendarSyncEnabled: StateFlow<Boolean> = _calendarSyncEnabled.asStateFlow()

    fun setCalendarSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("calendar_sync_enabled", enabled).apply()
        _calendarSyncEnabled.value = enabled
    }

    // ─── Recent Rooms ────────────────────────────────────────────────────────────
    private val RECENT_ROOMS_KEY = "recent_rooms"
    private val MAX_RECENT_ROOMS = 5

    private fun loadRecentRooms(): List<String> {
        val raw = prefs.getString(RECENT_ROOMS_KEY, "") ?: ""
        return if (raw.isEmpty()) emptyList() else raw.split("|").filter { it.isNotBlank() }
    }

    private val _recentRooms = MutableStateFlow(loadRecentRooms())
    val recentRooms: StateFlow<List<String>> = _recentRooms.asStateFlow()

    fun addRecentRoom(room: String) {
        val trimmed = room.trim()
        if (trimmed.isEmpty()) return
        val updated = (listOf(trimmed) + _recentRooms.value.filter { it != trimmed })
            .take(MAX_RECENT_ROOMS)
        prefs.edit().putString(RECENT_ROOMS_KEY, updated.joinToString("|")).apply()
        _recentRooms.value = updated
    }

    // ─── Onboarding & Tutorial ───────────────────────────────────────────────────
    private val _hasSeenOnboarding = MutableStateFlow(prefs.getBoolean("has_seen_onboarding", false))
    val hasSeenOnboarding: StateFlow<Boolean> = _hasSeenOnboarding.asStateFlow()

    fun setHasSeenOnboarding(seen: Boolean) {
        prefs.edit().putBoolean("has_seen_onboarding", seen).apply()
        _hasSeenOnboarding.value = seen
    }

    private val _hasSeenTutorial = MutableStateFlow(prefs.getBoolean("has_seen_tutorial", false))
    val hasSeenTutorial: StateFlow<Boolean> = _hasSeenTutorial.asStateFlow()

    fun setHasSeenTutorial(seen: Boolean) {
        prefs.edit().putBoolean("has_seen_tutorial", seen).apply()
        _hasSeenTutorial.value = seen
    }

    // ─── Streak ───────────────────────────────────────────────────────────────────
    private val _currentStreak = MutableStateFlow(prefs.getInt("current_streak", 0))
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    private val _longestStreak = MutableStateFlow(prefs.getInt("longest_streak", 0))
    val longestStreak: StateFlow<Int> = _longestStreak.asStateFlow()

    // ISO date string of the last day the streak was updated
    fun getLastStreakDate(): String = prefs.getString("last_streak_date", "") ?: ""

    fun updateStreak(todayStr: String, maintained: Boolean) {
        val lastDate = getLastStreakDate()
        if (maintained) {
            if (lastDate != todayStr) {
                val newStreak = _currentStreak.value + 1
                val newLongest = maxOf(newStreak, _longestStreak.value)
                prefs.edit()
                    .putInt("current_streak", newStreak)
                    .putInt("longest_streak", newLongest)
                    .putString("last_streak_date", todayStr)
                    .apply()
                _currentStreak.value = newStreak
                _longestStreak.value = newLongest
            }
        } else {
            prefs.edit().putInt("current_streak", 0)
                .putString("last_streak_date", todayStr).apply()
            _currentStreak.value = 0
        }
    }



    // ─── Daily Digest ─────────────────────────────────────────────────────────────
    private val _dailyDigestEnabled = MutableStateFlow(prefs.getBoolean("daily_digest_enabled", true))
    val dailyDigestEnabled: StateFlow<Boolean> = _dailyDigestEnabled.asStateFlow()

    fun setDailyDigestEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("daily_digest_enabled", enabled).apply()
        _dailyDigestEnabled.value = enabled
    }
}
