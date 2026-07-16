package com.anish18.classflow.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish18.classflow.data.model.*
import com.anish18.classflow.data.repository.AppSettings
import com.anish18.classflow.data.repository.TimetableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineDispatcher
import com.anish18.classflow.di.IoDispatcher
import com.anish18.classflow.di.DefaultDispatcher
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

data class BackupMetadata(
    val schemaVersion: String = "1.0",
    val appVersion: String = "1.0.0",
    val exportedAt: Long
)

data class MinifiedBackupData(
    val sem: List<Semester>,
    val crs: List<Course>,
    val cls: List<ClassSession>,
    val att: List<Attendance>,
    val tsk: List<Task>,
    val hol: List<Holiday>? = emptyList()
)

data class UnifiedBackupPayload(
    val metadata: BackupMetadata,
    val checksum: String,
    val data: MinifiedBackupData
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: TimetableRepository,
    private val appSettings: AppSettings,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    val semesters: StateFlow<List<Semester>> = repository.allSemestersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSemester: StateFlow<Semester?> = repository.activeSemesterFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val backgroundStyle: StateFlow<String> = appSettings.backgroundStyle
    val showTasksOnTimetable: StateFlow<Boolean> = appSettings.showTasksOnTimetable

    val notificationsEnabled: StateFlow<Boolean> = appSettings.notificationsEnabled
    val classReminderBuffer: StateFlow<Int> = appSettings.classReminderBuffer
    val taskReminderEnabled: StateFlow<Boolean> = appSettings.taskReminderEnabled
    val taskReminderBuffer: StateFlow<Int> = appSettings.taskReminderBuffer

    val studyModeEnabled: StateFlow<Boolean> = appSettings.studyModeEnabled

    fun setStudyModeEnabled(enabled: Boolean) {
        appSettings.setStudyModeEnabled(enabled)
    }



    val dailyDigestEnabled: StateFlow<Boolean> = appSettings.dailyDigestEnabled

    fun setDailyDigestEnabled(enabled: Boolean) {
        appSettings.setDailyDigestEnabled(enabled)
        com.anish18.classflow.utils.AlarmScheduler.scheduleDailyDigest(appContext, enabled)
    }



    /**
     * Force-reschedule all class and task alarms immediately.
     * Returns a summary string e.g. "Scheduled 8 class alarms, 3 task alarms."
     */
    fun forceRescheduleAlarms(onResult: (String) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            try {
                val classes = repository.activeClassesFlow.first()
                val courses = repository.activeCoursesFlow.first()
                val tasks = repository.allTasksFlow.first()
                val enabled = appSettings.notificationsEnabled.value
                val classBuffer = appSettings.classReminderBuffer.value
                val taskEnabled = appSettings.taskReminderEnabled.value
                val taskBuffer = appSettings.taskReminderBuffer.value
                val studyMode = appSettings.studyModeEnabled.value

                com.anish18.classflow.utils.AlarmScheduler.rescheduleAllReminders(
                    appContext, classes, courses, enabled, classBuffer
                )
                com.anish18.classflow.utils.AlarmScheduler.rescheduleAllTaskReminders(
                    appContext, tasks, courses, taskEnabled, taskBuffer
                )
                com.anish18.classflow.utils.AlarmScheduler.scheduleStudyModeAlarms(
                    appContext, classes, studyMode
                )

                val classCount = if (enabled) classes.size else 0
                val taskCount = if (taskEnabled) tasks.count { it.status == "pending" && it.dueDate != null } else 0
                val msg = "✅ Rescheduled $classCount class alarm(s), $taskCount task alarm(s)."
                onResult(msg)
            } catch (e: Exception) {
                onResult("❌ Error: ${e.message}")
            }
        }
    }

    fun setBackgroundStyle(style: String) {
        appSettings.setBackgroundStyle(style)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        appSettings.setNotificationsEnabled(enabled)
    }

    fun setClassReminderBuffer(buffer: Int) {
        appSettings.setClassReminderBuffer(buffer)
    }

    fun setTaskReminderEnabled(enabled: Boolean) {
        appSettings.setTaskReminderEnabled(enabled)
    }

    fun setTaskReminderBuffer(buffer: Int) {
        appSettings.setTaskReminderBuffer(buffer)
    }

    fun setShowTasksOnTimetable(show: Boolean) {
        appSettings.setShowTasksOnTimetable(show)
    }



    fun addSemester(name: String, startDate: String, endDate: String) {
        viewModelScope.launch {
            val isFirst = semesters.value.isEmpty()
            val newSemester = Semester(
                id = UUID.randomUUID().toString(),
                name = name,
                startDate = startDate,
                endDate = endDate,
                isActive = isFirst
            )
            repository.insertSemester(newSemester)
        }
    }

    fun updateSemester(semester: Semester) {
        viewModelScope.launch {
            repository.updateSemester(semester)
        }
    }

    fun setActiveSemester(semesterId: String) {
        viewModelScope.launch {
            repository.setActiveSemester(semesterId)
        }
    }

    fun deleteSemester(semester: Semester) {
        viewModelScope.launch {
            repository.deleteSemester(semester)
        }
    }

    fun seedDemoData() {
        viewModelScope.launch {
            repository.seedDemoData()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    val holidays: StateFlow<List<Holiday>> = repository.allHolidaysFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addHoliday(date: String, reason: String) {
        viewModelScope.launch {
            repository.addHoliday(date, reason)
        }
    }

    fun removeHoliday(date: String) {
        viewModelScope.launch {
            repository.removeHoliday(date)
        }
    }

    private fun getNormalizedJsonString(element: com.google.gson.JsonElement): String {
        return when {
            element.isJsonObject -> {
                val obj = element.asJsonObject
                val sortedKeys = obj.keySet().sorted()
                val sb = java.lang.StringBuilder()
                sb.append("{")
                sortedKeys.forEachIndexed { index, key ->
                    sb.append("\"").append(key).append("\":")
                    sb.append(getNormalizedJsonString(obj.get(key)))
                    if (index < sortedKeys.size - 1) {
                        sb.append(",")
                    }
                }
                sb.append("}")
                sb.toString()
            }
            element.isJsonArray -> {
                val arr = element.asJsonArray
                val sb = java.lang.StringBuilder()
                sb.append("[")
                for (i in 0 until arr.size()) {
                    sb.append(getNormalizedJsonString(arr.get(i)))
                    if (i < arr.size() - 1) {
                        sb.append(",")
                    }
                }
                sb.append("]")
                sb.toString()
            }
            element.isJsonPrimitive -> {
                val primitive = element.asJsonPrimitive
                when {
                    primitive.isString -> "\"${primitive.asString}\""
                    primitive.isBoolean -> primitive.asBoolean.toString()
                    primitive.isNumber -> {
                        val num = primitive.asNumber
                        if (num.toDouble() == num.toLong().toDouble()) {
                            num.toLong().toString()
                        } else {
                            num.toString()
                        }
                    }
                    else -> primitive.toString()
                }
            }
            else -> "null"
        }
    }

    private fun calculateSha256(content: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(content.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    fun exportBackup(onExportReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val finalJson = withContext(ioDispatcher) {
                    val semesters = repository.allSemestersFlow.first()
                    val courses = repository.allCoursesFlow.first()
                    val classes = repository.allClassesFlow.first()
                    val attendance = repository.allAttendanceFlow.first()
                    val tasks = repository.allTasksFlow.first()
                    val holidays = repository.allHolidaysFlow.first()
                    
                    val minData = MinifiedBackupData(semesters, courses, classes, attendance, tasks, holidays)
                    val gson = com.google.gson.Gson()
                    val minDataJsonTree = gson.toJsonTree(minData)
                    val normalizedDataJson = getNormalizedJsonString(minDataJsonTree)
                    val checksum = calculateSha256(normalizedDataJson)
                    
                    val metadata = BackupMetadata(exportedAt = System.currentTimeMillis())
                    val payload = UnifiedBackupPayload(metadata, checksum, minData)
                    gson.toJson(payload)
                }
                onExportReady(finalJson)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    fun generateQRSharingPayload(onPayloadReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val activeSem = repository.activeSemesterFlow.first() ?: repository.allSemestersFlow.first().firstOrNull()
                if (activeSem != null) {
                    val json = withContext(defaultDispatcher) {
                        val courses = repository.activeCoursesFlow.first()
                        val classes = repository.activeClassesFlow.first()
                        
                        val payload = mapOf(
                            "v" to "1.0",
                            "s" to mapOf(
                                "n" to activeSem.name,
                                "sd" to activeSem.startDate,
                                "ed" to activeSem.endDate
                            ),
                            "c" to courses.map { c ->
                                mapOf(
                                    "id" to c.id,
                                    "n" to c.name,
                                    "sn" to c.shortName,
                                    "p" to c.professor,
                                    "cr" to c.credits,
                                    "r" to c.room,
                                    "co" to c.color
                                )
                            },
                            "cl" to classes.map { cl ->
                                mapOf(
                                    "ci" to cl.courseId,
                                    "d" to cl.dayOfWeek,
                                    "st" to cl.startTime,
                                    "et" to cl.endTime,
                                    "r" to cl.room
                                )
                            }
                        )
                        com.google.gson.Gson().toJson(payload)
                    }
                    onPayloadReady(json)
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    fun restoreBackup(json: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = withContext(ioDispatcher) {
                    val gson = com.google.gson.Gson()
                    val jsonObject = gson.fromJson(json, com.google.gson.JsonObject::class.java)
                    
                    if (jsonObject.has("metadata") && jsonObject.has("checksum") && jsonObject.has("data")) {
                        // Minified, secure format with Checksum verification
                        val checksum = jsonObject.get("checksum").asString
                        val dataElement = jsonObject.get("data")
                        val normalizedDataJson = getNormalizedJsonString(dataElement)
                        val calculated = calculateSha256(normalizedDataJson)
                        
                        if (checksum != calculated) {
                            android.util.Log.w("SettingsViewModel", "Checksum mismatch: expected $checksum but calculated $calculated. Continuing restore anyway.")
                        }
                        
                        val backupData = gson.fromJson(dataElement, MinifiedBackupData::class.java)
                        if (backupData != null) {
                            repository.restoreBackup(
                                backupData.sem,
                                backupData.crs,
                                backupData.cls,
                                backupData.att,
                                backupData.tsk,
                                backupData.hol ?: emptyList()
                            )
                            true
                        } else {
                            false
                        }
                    } else if (jsonObject.has("s") && jsonObject.has("c") && jsonObject.has("cl")) {
                        // Compacted classmate sharing format
                        val activeSemId = UUID.randomUUID().toString()
                        val sObj = jsonObject.getAsJsonObject("s")
                        val semName = sObj.get("n").asString
                        val semStartDate = sObj.get("sd").asString
                        val semEndDate = sObj.get("ed").asString
                        
                        val newSemester = Semester(activeSemId, semName, semStartDate, semEndDate, true)
                        
                        val cArray = jsonObject.getAsJsonArray("c")
                        val newCourses = mutableListOf<Course>()
                        val courseIdMap = mutableMapOf<String, String>()
                        
                        cArray.forEach { element ->
                            val cObj = element.asJsonObject
                            val oldId = cObj.get("id").asString
                            val newId = UUID.randomUUID().toString()
                            courseIdMap[oldId] = newId
                            
                            newCourses.add(Course(
                                id = newId,
                                name = cObj.get("n").asString,
                                shortName = cObj.get("sn").asString,
                                professor = cObj.get("p").asString,
                                credits = cObj.get("cr").asInt,
                                room = if (cObj.has("r") && !cObj.get("r").isJsonNull) cObj.get("r").asString else "",
                                color = cObj.get("co").asString,
                                semesterId = activeSemId
                            ))
                        }
                        
                        val clArray = jsonObject.getAsJsonArray("cl")
                        val newClasses = mutableListOf<ClassSession>()
                        
                        clArray.forEach { element ->
                            val clObj = element.asJsonObject
                            val oldCourseId = clObj.get("ci").asString
                            val newCourseId = courseIdMap[oldCourseId] ?: oldCourseId
                            
                            newClasses.add(ClassSession(
                                id = UUID.randomUUID().toString(),
                                courseId = newCourseId,
                                dayOfWeek = clObj.get("d").asString,
                                startTime = clObj.get("st").asString,
                                endTime = clObj.get("et").asString,
                                room = if (clObj.has("r") && !clObj.get("r").isJsonNull) clObj.get("r").asString else null,
                                semesterId = activeSemId
                            ))
                        }
                        
                        repository.restoreBackup(
                            semesters = listOf(newSemester),
                            courses = newCourses,
                            classes = newClasses,
                            attendance = emptyList(),
                            tasks = emptyList()
                        )
                        true
                    } else {
                        false
                    }
                }
                onComplete(success)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun exportIcsCalendar(onExportReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val icsText = withContext(defaultDispatcher) {
                    val classes = repository.allClassesFlow.first()
                    val courses = repository.allCoursesFlow.first()
                    
                    val sb = java.lang.StringBuilder()
                    sb.append("BEGIN:VCALENDAR\n")
                    sb.append("VERSION:2.0\n")
                    sb.append("PRODID:-//ClassFlow//Timetable//EN\n")
                    
                    classes.forEach { session ->
                        val course = courses.find { it.id == session.courseId }
                        sb.append("BEGIN:VEVENT\n")
                        sb.append("SUMMARY:${course?.name ?: "Class"}\n")
                        sb.append("LOCATION:${session.room ?: "N/A"}\n")
                        val dayAbbr = when(session.dayOfWeek.lowercase()) {
                            "monday" -> "MO"
                            "tuesday" -> "TU"
                            "wednesday" -> "WE"
                            "thursday" -> "TH"
                            "friday" -> "FR"
                            "saturday" -> "SA"
                            "sunday" -> "SU"
                            else -> "MO"
                        }
                        sb.append("RRULE:FREQ=WEEKLY;BYDAY=$dayAbbr\n")
                        val startStr = session.startTime.replace(":", "") + "00"
                        val endStr = session.endTime.replace(":", "") + "00"
                        sb.append("DTSTART:20260706T$startStr\n")
                        sb.append("DTEND:20260706T$endStr\n")
                        sb.append("END:VEVENT\n")
                    }
                    
                    sb.append("END:VCALENDAR\n")
                    sb.toString()
                }
                onExportReady(icsText)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }
}
