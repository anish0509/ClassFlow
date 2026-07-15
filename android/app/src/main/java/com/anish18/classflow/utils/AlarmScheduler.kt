package com.anish18.classflow.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.anish18.classflow.data.model.ClassSession
import com.anish18.classflow.data.model.Course
import java.util.Calendar
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast



object AlarmScheduler {

    // ─── Safe alarm ID helpers ─────────────────────────────────────────────────
    // Each alarm type uses a distinct ID namespace so they never collide.
    // Math.abs() prevents negative PendingIntent request codes (undefined on some OEMs).
    //   Class reminders : abs(hash)            range  0 ..  ~2B
    //   Study-mode mute : abs(hash) + 3_000_000  (distinct range)
    //   Study-mode unmute: abs(hash) + 6_000_000
    //   Task reminders  : abs(hash) + 9_000_000
    private fun classAlarmId(sessionId: String): Int =
        Math.abs(sessionId.hashCode() % 2_000_000)

    private fun studyMuteId(sessionId: String): Int =
        Math.abs(sessionId.hashCode() % 2_000_000) + 3_000_000

    private fun studyUnmuteId(sessionId: String): Int =
        Math.abs(sessionId.hashCode() % 2_000_000) + 6_000_000

    private fun taskAlarmId(taskId: String): Int =
        Math.abs(taskId.hashCode() % 2_000_000) + 9_000_000

    fun scheduleExactAlarm(
        context: Context,
        id: Int,
        title: String,
        message: String,
        timeInMillis: Long,
        courseId: String? = null,
        classId: String? = null
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("id", id)
            putExtra("title", title)
            putExtra("message", message)
            putExtra("exact_time_ms", timeInMillis)
            if (courseId != null) putExtra("courseId", courseId)
            if (classId != null) putExtra("classId", classId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context, id: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    fun scheduleStudyModeActionAlarm(
        context: Context,
        id: Int,
        action: String,
        timeInMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("id", id)
            putExtra("study_mode_action", action)
            putExtra("exact_time_ms", timeInMillis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelStudyModeAlarm(context: Context, id: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    fun rescheduleAllReminders(
        context: Context,
        classes: List<ClassSession>,
        courses: List<Course>,
        enabled: Boolean,
        bufferMinutes: Int
    ) {
        // Cancel all existing alarms first to clear stale reminders
        classes.forEach { session ->
            cancelReminder(context, classAlarmId(session.id))
        }

        if (!enabled) return

        val courseMap = courses.associateBy { it.id }

        classes.forEach { session ->
            try {
                val dayOfWeek = parseDayOfWeek(session.dayOfWeek.trim()) ?: return@forEach

                val timeParts = session.startTime.trim().split(":")
                if (timeParts.size < 2) return@forEach
                val hour = timeParts[0].toIntOrNull() ?: return@forEach
                val minute = timeParts[1].toIntOrNull() ?: return@forEach

                val now = java.time.LocalDateTime.now()

                // Find the NEXT (or today's) occurrence of this weekday, at class start time
                val classStartTime = now
                    .with(java.time.temporal.TemporalAdjusters.nextOrSame(dayOfWeek))
                    .withHour(hour)
                    .withMinute(minute)
                    .withSecond(0)
                    .withNano(0)

                var triggerTime = classStartTime.minusMinutes(bufferMinutes.toLong())

                // If this trigger is already in the past, handle it gracefully
                if (!triggerTime.isAfter(now)) {
                    if (classStartTime.isAfter(now)) {
                        // Class is still in the future! Trigger warning immediately (2 seconds from now)
                        triggerTime = now.plusSeconds(2)
                    } else {
                        // Class has already started or passed today, move to next week
                        triggerTime = triggerTime.plusWeeks(1)
                    }
                }

                val timeInMillis = triggerTime
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()

                val course = courseMap[session.courseId]
                val title = if (course != null) {
                    "${course.name} starts in $bufferMinutes minutes"
                } else {
                    "Class starts in $bufferMinutes minutes"
                }
                val message = "Room: ${session.room ?: "N/A"} | Time: ${session.startTime}"

                scheduleExactAlarm(
                    context = context,
                    id = classAlarmId(session.id),
                    title = title,
                    message = message,
                    timeInMillis = timeInMillis,
                    courseId = session.courseId,
                    classId = session.id
                )
            } catch (e: Exception) {
                android.util.Log.e("AlarmScheduler", "Failed to schedule alarm for session ${session.id}", e)
            }
        }
    }

    /**
     * Parses a day-of-week string (e.g. "Monday", "TUESDAY") into a [java.time.DayOfWeek].
     * Locale-independent — does NOT use Calendar.DAY_OF_WEEK which has locale-dependent behaviour.
     */
    private fun parseDayOfWeek(day: String): java.time.DayOfWeek? = when {
        day.startsWith("MON", ignoreCase = true) -> java.time.DayOfWeek.MONDAY
        day.startsWith("TUE", ignoreCase = true) -> java.time.DayOfWeek.TUESDAY
        day.startsWith("WED", ignoreCase = true) -> java.time.DayOfWeek.WEDNESDAY
        day.startsWith("THU", ignoreCase = true) -> java.time.DayOfWeek.THURSDAY
        day.startsWith("FRI", ignoreCase = true) -> java.time.DayOfWeek.FRIDAY
        day.startsWith("SAT", ignoreCase = true) -> java.time.DayOfWeek.SATURDAY
        day.startsWith("SUN", ignoreCase = true) -> java.time.DayOfWeek.SUNDAY
        else -> null
    }

    fun scheduleStudyModeAlarms(
        context: Context,
        classes: List<ClassSession>,
        enabled: Boolean
    ) {
        // Cancel all existing study mode alarms
        classes.forEach { session ->
            cancelStudyModeAlarm(context, studyMuteId(session.id))
            cancelStudyModeAlarm(context, studyUnmuteId(session.id))
        }

        val prefs = context.getSharedPreferences("classflow_prefs", Context.MODE_PRIVATE)

        if (!enabled) {
            // Restore previous ringer mode immediately if we disabled study mode
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val hasDndPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    notificationManager.isNotificationPolicyAccessGranted
                } else {
                    true
                }
                if (hasDndPermission) {
                    val saved = prefs.getInt("saved_ringer_mode", -1)
                    if (saved != -1) {
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        audioManager.setRingerMode(saved)
                        prefs.edit().putInt("saved_ringer_mode", -1).apply()
                        android.util.Log.d("AlarmScheduler", "Study Mode disabled. Restored ringer mode: $saved")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AlarmScheduler", "Failed to restore ringer mode on disable", e)
            }
            return
        }

        val now = java.time.LocalDateTime.now()
        var anyClassActiveNow = false

        classes.forEach { session ->
            val dayOfWeek = parseDayOfWeek(session.dayOfWeek.trim()) ?: return@forEach

            // Check if this class session is active right now (today, and between start/end times)
            try {
                val startParts = session.startTime.trim().split(":")
                val endParts = session.endTime.trim().split(":")
                if (startParts.size >= 2 && endParts.size >= 2) {
                    val startHour = startParts[0].toIntOrNull() ?: 0
                    val startMin = startParts[1].toIntOrNull() ?: 0
                    val endHour = endParts[0].toIntOrNull() ?: 0
                    val endMin = endParts[1].toIntOrNull() ?: 0

                    if (now.dayOfWeek == dayOfWeek) {
                        val classStart = now.withHour(startHour).withMinute(startMin).withSecond(0).withNano(0)
                        val classEnd = now.withHour(endHour).withMinute(endMin).withSecond(0).withNano(0)
                        if (now.isAfter(classStart) && now.isBefore(classEnd)) {
                            anyClassActiveNow = true
                        }
                    }
                }
            } catch (e: Exception) {
                // ignore
            }

            // 1. Mute alarm at class start time
            try {
                val startParts = session.startTime.trim().split(":")
                if (startParts.size >= 2) {
                    val hour = startParts[0].toIntOrNull() ?: 0
                    val minute = startParts[1].toIntOrNull() ?: 0
                    var muteTime = now
                        .with(java.time.temporal.TemporalAdjusters.nextOrSame(dayOfWeek))
                        .withHour(hour)
                        .withMinute(minute)
                        .withSecond(0)
                        .withNano(0)
                    if (!muteTime.isAfter(now)) muteTime = muteTime.plusWeeks(1)
                    val muteMillis = muteTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    scheduleStudyModeActionAlarm(context, studyMuteId(session.id), "mute", muteMillis)
                }
            } catch (e: Exception) {
                android.util.Log.e("AlarmScheduler", "Failed mute alarm ${session.id}", e)
            }

            // 2. Unmute alarm at class end time
            try {
                val endParts = session.endTime.trim().split(":")
                if (endParts.size >= 2) {
                    val hour = endParts[0].toIntOrNull() ?: 0
                    val minute = endParts[1].toIntOrNull() ?: 0
                    var unmuteTime = now
                        .with(java.time.temporal.TemporalAdjusters.nextOrSame(dayOfWeek))
                        .withHour(hour)
                        .withMinute(minute)
                        .withSecond(0)
                        .withNano(0)
                    if (!unmuteTime.isAfter(now)) unmuteTime = unmuteTime.plusWeeks(1)
                    val unmuteMillis = unmuteTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    scheduleStudyModeActionAlarm(context, studyUnmuteId(session.id), "unmute", unmuteMillis)
                }
            } catch (e: Exception) {
                android.util.Log.e("AlarmScheduler", "Failed unmute alarm ${session.id}", e)
            }
        }

        // Apply vibrate mode immediately if a class session is active right now
        if (anyClassActiveNow) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val hasDndPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    notificationManager.isNotificationPolicyAccessGranted
                } else {
                    true
                }
                if (hasDndPermission) {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val current = audioManager.ringerMode
                    if (current != AudioManager.RINGER_MODE_VIBRATE &&
                        current != AudioManager.RINGER_MODE_SILENT
                    ) {
                        prefs.edit().putInt("saved_ringer_mode", current).apply()
                        audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
                        android.util.Log.d("AlarmScheduler", "Muted active class immediately. Saved ringer mode: $current")
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, "📳 Study Mode: Active class detected! Phone muted.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // Already in vibrate or silent, but class is active
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, "📳 Study Mode: Class active (phone already in silent/vibrate).", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "⚠️ Study Mode: Cannot mute (missing DND permission).", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AlarmScheduler", "Failed to immediately mute active class", e)
            }
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "📅 Study Mode: Enabled (no active classes right now).", Toast.LENGTH_LONG).show()
            }
        }
    }


    fun rescheduleAllTaskReminders(
        context: Context,
        tasks: List<com.anish18.classflow.data.model.Task>,
        courses: List<Course>,
        enabled: Boolean,
        bufferMinutes: Int
    ) {
        // Cancel all existing task alarms
        tasks.forEach { task ->
            cancelReminder(context, taskAlarmId(task.id))
        }

        if (!enabled) return

        val now = java.time.LocalDateTime.now()
        val courseMap = courses.associateBy { it.id }

        tasks.filter { it.status.equals("pending", ignoreCase = true) && it.dueDate != null }.forEach { task ->
            try {
                val dateStr = task.dueDate!!.trim() // yyyy-MM-dd
                val timeStr = (task.dueTime ?: "23:59").trim() // HH:mm
                
                val dateParts = dateStr.split("-")
                val year = dateParts[0].toInt()
                val month = dateParts[1].toInt()
                val day = dateParts[2].toInt()

                val timeParts = timeStr.split(":")
                val hour = timeParts[0].toInt()
                val minute = timeParts[1].toInt()

                val dueDateTime = java.time.LocalDateTime.of(year, month, day, hour, minute)
                var reminderDateTime = dueDateTime.minusMinutes(bufferMinutes.toLong())

                if (!reminderDateTime.isAfter(now)) {
                    if (dueDateTime.isAfter(now)) {
                        // Task is still in the future! Trigger reminder immediately (2 seconds from now)
                        reminderDateTime = now.plusSeconds(2)
                    } else {
                        // Task is already in the past, do not schedule
                        return@forEach
                    }
                }

                val timeInMillis = reminderDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                val course = courseMap[task.courseId]
                val coursePrefix = if (course != null) "[${course.name}] " else ""
                val title = "Task Deadline Alert"
                val message = "$coursePrefix\"${task.title}\" is due in $bufferMinutes mins!"

                scheduleExactAlarm(context, taskAlarmId(task.id), title, message, timeInMillis)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    // ─── Daily Digest ─────────────────────────────────────────────────────────────
    private const val DAILY_DIGEST_ID = 999_001

    fun scheduleDailyDigest(context: Context, enabled: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("daily_digest", true)
        }
        val pi = PendingIntent.getBroadcast(
            context, DAILY_DIGEST_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!enabled) {
            alarmManager.cancel(pi)
            return
        }

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
