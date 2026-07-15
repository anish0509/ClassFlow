package com.anish18.classflow.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log

/**
 * Receives alarm broadcasts for class reminders, task reminders,
 * daily digest, and study-mode mute/unmute.
 *
 * Intentionally does NOT use @AndroidEntryPoint / Hilt injection.
 * Reason: Hilt component initialisation in a background alarm broadcast
 * is fragile (process may be freshly spawned, component graph timing
 * issues in release / R8-optimised builds).  Instead, everything this
 * receiver needs is already packed into the Intent extras at scheduling
 * time, or is readable via raw SharedPreferences.
 */
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        // ── 1. Daily Digest ───────────────────────────────────────────────────
        if (intent.getBooleanExtra("daily_digest", false)) {
            val prefs = context.getSharedPreferences("classflow_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("daily_digest_enabled", true)) {
                NotificationHelper.showNotification(
                    context, 999_001,
                    "Good morning! ☀️",
                    "Open ClassFlow to review your schedule and tasks."
                )
            }
            // Reschedule for tomorrow
            AlarmScheduler.scheduleDailyDigest(context, prefs.getBoolean("daily_digest_enabled", true))
            return
        }

        // ── 2. Study-Mode mute / unmute ───────────────────────────────────────
        val studyModeAction = intent.getStringExtra("study_mode_action")
        if (studyModeAction != null) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val hasDndPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    notificationManager.isNotificationPolicyAccessGranted
                } else {
                    true
                }

                if (hasDndPermission) {
                    val prefs = context.getSharedPreferences("classflow_prefs", Context.MODE_PRIVATE)
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    when (studyModeAction) {
                        "mute" -> {
                            val current = audioManager.ringerMode
                            if (current != AudioManager.RINGER_MODE_VIBRATE &&
                                current != AudioManager.RINGER_MODE_SILENT
                            ) {
                                prefs.edit().putInt("saved_ringer_mode", current).apply()
                                audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
                                Log.d("StudyMode", "Muted. Saved mode: $current")
                            }
                        }
                        "unmute" -> {
                            val saved = prefs.getInt("saved_ringer_mode", -1)
                            if (saved != -1) {
                                audioManager.setRingerMode(saved)
                                prefs.edit().putInt("saved_ringer_mode", -1).apply()
                                Log.d("StudyMode", "Restored ringer mode: $saved")
                            }
                        }
                    }
                } else {
                    Log.w("StudyMode", "DND access policy not granted; skipping automatic ringer mode change")
                }
            } catch (e: Exception) {
                Log.e("StudyMode", "Error toggling ringer mode", e)
            }
            // Reschedule for next week
            val exactTimeMs = intent.getLongExtra("exact_time_ms", 0L)
            if (exactTimeMs > 0L) {
                val id = intent.getIntExtra("id", 0)
                AlarmScheduler.scheduleStudyModeActionAlarm(
                    context, id, studyModeAction,
                    exactTimeMs + WEEK_MS
                )
            }
            return
        }

        // ── 3. Class / Task Reminder ──────────────────────────────────────────
        val id          = intent.getIntExtra("id", 0)
        val title       = intent.getStringExtra("title")    ?: "ClassFlow Reminder"
        val message     = intent.getStringExtra("message")  ?: "You have an upcoming class!"
        val exactTimeMs = intent.getLongExtra("exact_time_ms", 0L)
        val courseId    = intent.getStringExtra("courseId")
        val classId     = intent.getStringExtra("classId")

        // Show the notification immediately — all info is already in the Intent extras.
        // No database access needed at fire time.
        NotificationHelper.showNotification(context, id, title, message)
        Log.d("NotificationReceiver", "Notification shown: id=$id title='$title'")

        // Reschedule for next week so the weekly reminder cycle continues
        if (exactTimeMs > 0L) {
            AlarmScheduler.scheduleExactAlarm(
                context, id, title, message,
                exactTimeMs + WEEK_MS,
                courseId, classId
            )
        }
    }

    companion object {
        private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
    }
}
