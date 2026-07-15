package com.anish18.classflow

import android.app.Application
import com.anish18.classflow.data.repository.AppSettings
import com.anish18.classflow.data.repository.TimetableRepository
import com.anish18.classflow.utils.AlarmScheduler
import com.anish18.classflow.utils.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application() {

    @Inject
    lateinit var repository: TimetableRepository

    @Inject
    lateinit var appSettings: AppSettings

    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        
        // Schedule daily digest if enabled
        com.anish18.classflow.utils.AlarmScheduler.scheduleDailyDigest(
            this, appSettings.dailyDigestEnabled.value
        )

        // Start background flows to reschedule alarms on data / setting changes
        observeAndRescheduleAlarms()
    }

    private fun observeAndRescheduleAlarms() {
        applicationScope.launch {
            combine(
                repository.activeClassesFlow,
                appSettings.notificationsEnabled,
                appSettings.classReminderBuffer
            ) { classes, enabled, buffer ->
                Triple(classes, enabled, buffer)
            }.collect { (classes, enabled, buffer) ->
                try {
                    val courses = repository.activeCoursesFlow.first()
                    AlarmScheduler.rescheduleAllReminders(this@MainApplication, classes, courses, enabled, buffer)
                } catch (e: Exception) {
                    android.util.Log.e("MainApplication", "Error rescheduling class reminders", e)
                }
            }
        }

        applicationScope.launch {
            combine(
                repository.allTasksFlow,
                appSettings.taskReminderEnabled,
                appSettings.taskReminderBuffer
            ) { tasks, enabled, buffer ->
                Triple(tasks, enabled, buffer)
            }.collect { (tasks, enabled, buffer) ->
                try {
                    val courses = repository.activeCoursesFlow.first()
                    AlarmScheduler.rescheduleAllTaskReminders(this@MainApplication, tasks, courses, enabled, buffer)
                } catch (e: Exception) {
                    android.util.Log.e("MainApplication", "Error rescheduling task reminders", e)
                }
            }
        }

        applicationScope.launch {
            combine(
                repository.activeClassesFlow,
                appSettings.studyModeEnabled
            ) { classes, enabled ->
                Pair(classes, enabled)
            }.collect { (classes, enabled) ->
                try {
                    AlarmScheduler.scheduleStudyModeAlarms(this@MainApplication, classes, enabled)
                } catch (e: Exception) {
                    android.util.Log.e("MainApplication", "Error scheduling study mode alarms", e)
                }
            }
        }
    }

}
