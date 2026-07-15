package com.anish18.classflow.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anish18.classflow.data.repository.AppSettings
import com.anish18.classflow.data.repository.TimetableRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: TimetableRepository

    @Inject
    lateinit var appSettings: AppSettings

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob()).launch {
                try {
                    val classes = repository.activeClassesFlow.first()
                    val courses = repository.activeCoursesFlow.first()
                    
                    // Class reminders
                    val classRemindersEnabled = appSettings.notificationsEnabled.first()
                    val classBuffer = appSettings.classReminderBuffer.first()
                    AlarmScheduler.rescheduleAllReminders(context, classes, courses, classRemindersEnabled, classBuffer)

                    // Task reminders
                    val tasks = repository.allTasksFlow.first()
                    val taskRemindersEnabled = appSettings.taskReminderEnabled.first()
                    val taskBuffer = appSettings.taskReminderBuffer.first()
                    AlarmScheduler.rescheduleAllTaskReminders(context, tasks, courses, taskRemindersEnabled, taskBuffer)

                    // Study Mode alarms
                    val studyModeEnabled = appSettings.studyModeEnabled.first()
                    AlarmScheduler.scheduleStudyModeAlarms(context, classes, studyModeEnabled)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
