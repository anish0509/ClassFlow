package com.anish18.classflow.data.repository

import com.anish18.classflow.data.database.dao.*
import com.anish18.classflow.data.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import com.anish18.classflow.data.database.TimetableDatabase
import androidx.room.withTransaction
import com.anish18.classflow.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimetableRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: TimetableDatabase,
    private val semesterDao: SemesterDao,
    private val courseDao: CourseDao,
    private val classSessionDao: ClassSessionDao,
    private val attendanceDao: AttendanceDao,
    private val taskDao: TaskDao,
    private val holidayDao: HolidayDao,
    private val appSettings: AppSettings,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    init {
        database.invalidationTracker.addObserver(
            object : androidx.room.InvalidationTracker.Observer(
                arrayOf("semesters", "courses", "classes", "tasks", "attendance", "holidays")
            ) {
                override fun onInvalidated(tables: Set<String>) {
                    updateWidgets()
                }
            }
        )
    }

    private fun updateWidgets() {
        val updateClassesIntent = android.content.Intent(context, com.anish18.classflow.ui.widgets.ClassesWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
        val classesWidgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, com.anish18.classflow.ui.widgets.ClassesWidgetProvider::class.java)
        )
        updateClassesIntent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, classesWidgetIds)
        context.sendBroadcast(updateClassesIntent)

        val updateTasksIntent = android.content.Intent(context, com.anish18.classflow.ui.widgets.TasksWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val tasksWidgetIds = appWidgetManager.getAppWidgetIds(
            android.content.ComponentName(context, com.anish18.classflow.ui.widgets.TasksWidgetProvider::class.java)
        )
        updateTasksIntent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, tasksWidgetIds)
        context.sendBroadcast(updateTasksIntent)
    }

    private suspend fun syncCalendarIfEnabled() = withContext(ioDispatcher) {
        try {
            val enabled = appSettings.calendarSyncEnabled.value
            if (enabled) {
                val semester = getActiveSemester()
                if (semester != null) {
                    val classes = getClassesBySemester(semester.id)
                    val courses = getCoursesBySemester(semester.id).associateBy { it.id }
                    com.anish18.classflow.utils.CalendarSyncHelper.syncTimetableToCalendar(
                        context = context,
                        classes = classes,
                        courses = courses,
                        semester = semester
                    )
                } else {
                    com.anish18.classflow.utils.CalendarSyncHelper.clearAllSyncedEvents(context)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Semesters
    val allSemestersFlow: Flow<List<Semester>> = semesterDao.getAllSemestersFlow()
    val activeSemesterFlow: Flow<Semester?> = semesterDao.getActiveSemesterFlow()

    // All Courses & Classes
    val allCoursesFlow: Flow<List<Course>> = courseDao.getAllCoursesFlow()
    val allClassesFlow: Flow<List<ClassSession>> = classSessionDao.getAllClassesFlow()

    suspend fun getActiveSemester(): Semester? = semesterDao.getActiveSemester()
    
    suspend fun insertSemester(semester: Semester) = withContext(ioDispatcher) {
        semesterDao.insertSemester(semester)
        syncCalendarIfEnabled()
    }
    
    suspend fun updateSemester(semester: Semester) = withContext(ioDispatcher) {
        semesterDao.updateSemester(semester)
        syncCalendarIfEnabled()
    }
    
    suspend fun deleteSemester(semester: Semester) = withContext(ioDispatcher) {
        semesterDao.deleteSemester(semester)
        syncCalendarIfEnabled()
    }
    
    suspend fun setActiveSemester(semesterId: String) = withContext(ioDispatcher) {
        semesterDao.setActiveSemester(semesterId)
        syncCalendarIfEnabled()
    }

    // Courses (dynamically filtered by active semester)
    @OptIn(ExperimentalCoroutinesApi::class)
    val activeCoursesFlow: Flow<List<Course>> = activeSemesterFlow.flatMapLatest { semester ->
        if (semester != null) {
            courseDao.getCoursesBySemesterFlow(semester.id)
        } else {
            flowOf(emptyList())
        }
    }

    fun getCoursesBySemesterFlow(semesterId: String): Flow<List<Course>> =
        courseDao.getCoursesBySemesterFlow(semesterId)

    suspend fun getCoursesBySemester(semesterId: String): List<Course> =
        courseDao.getCoursesBySemester(semesterId)

    suspend fun getCourseById(id: String): Course? = courseDao.getCourseById(id)
    
    suspend fun insertCourse(course: Course) = withContext(ioDispatcher) {
        courseDao.insertCourse(course)
        syncCalendarIfEnabled()
    }
    
    suspend fun updateCourse(course: Course) = withContext(ioDispatcher) {
        courseDao.updateCourse(course)
        syncCalendarIfEnabled()
    }
    
    suspend fun deleteCourse(course: Course) = withContext(ioDispatcher) {
        try {
            val attachmentsDir = java.io.File(context.filesDir, "attachments/${course.id}")
            if (attachmentsDir.exists()) {
                attachmentsDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        courseDao.deleteCourse(course)
        syncCalendarIfEnabled()
    }

    fun getAttachmentsForCourseFlow(courseId: String): Flow<List<com.anish18.classflow.data.model.CourseAttachment>> =
        courseDao.getAttachmentsForCourseFlow(courseId)

    suspend fun insertAttachment(attachment: com.anish18.classflow.data.model.CourseAttachment) =
        courseDao.insertAttachment(attachment)

    suspend fun deleteAttachment(attachment: com.anish18.classflow.data.model.CourseAttachment) =
        courseDao.deleteAttachment(attachment)

    // Classes (dynamically filtered by active semester)
    @OptIn(ExperimentalCoroutinesApi::class)
    val activeClassesFlow: Flow<List<ClassSession>> = activeSemesterFlow.flatMapLatest { semester ->
        if (semester != null) {
            classSessionDao.getClassesBySemesterFlow(semester.id)
        } else {
            flowOf(emptyList())
        }
    }

    suspend fun getClassesBySemester(semesterId: String): List<ClassSession> =
        classSessionDao.getClassesBySemester(semesterId)

    suspend fun getClassById(id: String): ClassSession? = classSessionDao.getClassById(id)

    fun getClassesForDayFlow(semesterId: String, day: String): Flow<List<ClassSession>> =
        classSessionDao.getClassesForDayFlow(semesterId, day)

    suspend fun insertClass(classSession: ClassSession) = withContext(ioDispatcher) {
        classSessionDao.insertClass(classSession)
        syncCalendarIfEnabled()
    }
    
    suspend fun updateClass(classSession: ClassSession) = withContext(ioDispatcher) {
        classSessionDao.updateClass(classSession)
        syncCalendarIfEnabled()
    }
    
    suspend fun deleteClass(classSession: ClassSession) = withContext(ioDispatcher) {
        classSessionDao.deleteClass(classSession)
        syncCalendarIfEnabled()
    }

    // Attendance
    val allAttendanceFlow: Flow<List<Attendance>> = attendanceDao.getAllAttendanceFlow()

    fun getAttendanceForCourseFlow(courseId: String): Flow<List<Attendance>> =
        attendanceDao.getAttendanceForCourseFlow(courseId)

    suspend fun getAttendanceForCourse(courseId: String): List<Attendance> =
        attendanceDao.getAttendanceForCourse(courseId)

    suspend fun getAttendanceForClassAndDate(classId: String, date: String): Attendance? =
        attendanceDao.getAttendanceForClassAndDate(classId, date)

    suspend fun insertAttendance(attendance: Attendance) = attendanceDao.insertAttendance(attendance)
    suspend fun updateAttendance(attendance: Attendance) = attendanceDao.updateAttendance(attendance)
    suspend fun deleteAttendance(attendance: Attendance) = attendanceDao.deleteAttendance(attendance)

    // Tasks
    val allTasksFlow: Flow<List<Task>> = taskDao.getAllTasksFlow()

    fun getTasksForCourseFlow(courseId: String): Flow<List<Task>> =
        taskDao.getTasksForCourseFlow(courseId)

    suspend fun getTaskById(id: String): Task? = taskDao.getTaskById(id)
    suspend fun insertTask(task: Task) = taskDao.insertTask(task)
    suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun clearAllData() = withContext(ioDispatcher) {
        database.clearAllTables()
    }

    suspend fun seedDemoData() = withContext(ioDispatcher) {
        // Clear all tables first
        database.clearAllTables()
        
        val semesterId = UUID.randomUUID().toString()
        val today = LocalDate.now()
        val semester = Semester(
            id = semesterId,
            name = "Semester 4",
            startDate = today.toString(),
            endDate = today.plusMonths(4).toString(),
            isActive = true
        )
        insertSemester(semester)
        
        val course1Id = UUID.randomUUID().toString()
        val course1 = Course(
            id = course1Id,
            name = "Mobile Application Development",
            shortName = "MAD",
            professor = "Dr. Smith",
            credits = 4,
            room = "Lab 3",
            color = "#FF00E5",
            semesterId = semesterId
        )
        insertCourse(course1)

        val course2Id = UUID.randomUUID().toString()
        val course2 = Course(
            id = course2Id,
            name = "Artificial Intelligence",
            shortName = "AI",
            professor = "Dr. Jones",
            credits = 3,
            room = "Room 101",
            color = "#00E5FF",
            semesterId = semesterId
        )
        insertCourse(course2)

        val course3Id = UUID.randomUUID().toString()
        val course3 = Course(
            id = course3Id,
            name = "Design & Analysis of Algorithms",
            shortName = "DAA",
            professor = "Dr. Dave",
            credits = 4,
            room = "Room 204",
            color = "#00FF66",
            semesterId = semesterId
        )
        insertCourse(course3)

        val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
        days.forEach { day ->
            if (day == "Monday" || day == "Wednesday") {
                insertClass(ClassSession(
                    id = UUID.randomUUID().toString(),
                    courseId = course1Id,
                    dayOfWeek = day,
                    startTime = "09:00",
                    endTime = "10:30",
                    room = "Lab 3",
                    semesterId = semesterId
                ))
            }
            if (day == "Tuesday" || day == "Thursday") {
                insertClass(ClassSession(
                    id = UUID.randomUUID().toString(),
                    courseId = course2Id,
                    dayOfWeek = day,
                    startTime = "11:00",
                    endTime = "12:30",
                    room = "Room 101",
                    semesterId = semesterId
                ))
            }
            if (day == "Wednesday" || day == "Friday") {
                insertClass(ClassSession(
                    id = UUID.randomUUID().toString(),
                    courseId = course3Id,
                    dayOfWeek = day,
                    startTime = "14:00",
                    endTime = "15:30",
                    room = "Room 204",
                    semesterId = semesterId
                ))
            }
        }

        insertTask(Task(
            id = UUID.randomUUID().toString(),
            courseId = course1Id,
            title = "MAD App Overhaul Project",
            description = "Complete rewrite of the app in Kotlin and Compose",
            priority = "high",
            status = "pending",
            dueDate = today.plusDays(7).toString(),
            createdAt = System.currentTimeMillis().toString()
        ))

        insertTask(Task(
            id = UUID.randomUUID().toString(),
            courseId = course2Id,
            title = "AI Neural Networks Quiz",
            description = "Prepare chapters 3 and 4 on backpropagation",
            priority = "medium",
            status = "pending",
            dueDate = today.plusDays(3).toString(),
            createdAt = System.currentTimeMillis().toString()
        ))

        insertTask(Task(
            id = UUID.randomUUID().toString(),
            courseId = course3Id,
            title = "Algorithms Assignment 2",
            description = "Solve dynamic programming and greedy algorithm problems",
            priority = "low",
            status = "completed",
            dueDate = today.minusDays(1).toString(),
            createdAt = System.currentTimeMillis().toString()
        ))
    }

    suspend fun restoreBackup(
        semesters: List<Semester>,
        courses: List<Course>,
        classes: List<ClassSession>,
        attendance: List<Attendance>,
        tasks: List<Task>,
        holidays: List<Holiday> = emptyList()
    ) = withContext(ioDispatcher) {
        database.withTransaction {
            val db = database.openHelper.writableDatabase
            db.execSQL("DELETE FROM course_attachments")
            db.execSQL("DELETE FROM holidays")
            db.execSQL("DELETE FROM tasks")
            db.execSQL("DELETE FROM attendance")
            db.execSQL("DELETE FROM classes")
            db.execSQL("DELETE FROM courses")
            db.execSQL("DELETE FROM semesters")

            semesters.forEach { semesterDao.insertSemester(it) }
            courses.forEach { courseDao.insertCourse(it) }
            classes.forEach { classSessionDao.insertClass(it) }
            attendance.forEach { attendanceDao.insertAttendance(it) }
            tasks.forEach { taskDao.insertTask(it) }
            holidays.forEach { holidayDao.insertHoliday(it) }
        }
    }

    private fun timeToMinutes(timeStr: String): Int {
        return try {
            val parts = timeStr.split(":")
            val hours = parts[0].toInt()
            val minutes = parts[1].toInt()
            hours * 60 + minutes
        } catch (e: Exception) {
            0
        }
    }

    suspend fun checkClash(dayOfWeek: String, startTime: String, endTime: String, semesterId: String?, excludeClassId: String? = null): ClassSession? {
        if (semesterId == null) return null
        
        fun normalizeDay(d: String): String = when {
            d.startsWith("MON", ignoreCase = true) -> "Monday"
            d.startsWith("TUE", ignoreCase = true) -> "Tuesday"
            d.startsWith("WED", ignoreCase = true) -> "Wednesday"
            d.startsWith("THU", ignoreCase = true) -> "Thursday"
            d.startsWith("FRI", ignoreCase = true) -> "Friday"
            d.startsWith("SAT", ignoreCase = true) -> "Saturday"
            d.startsWith("SUN", ignoreCase = true) -> "Sunday"
            else -> d
        }
        
        val targetDayNormalized = normalizeDay(dayOfWeek)
        val classesInSemester = getClassesBySemester(semesterId)
        val start1 = timeToMinutes(startTime)
        val end1 = timeToMinutes(endTime)
        for (session in classesInSemester) {
            if (session.id == excludeClassId) continue
            if (normalizeDay(session.dayOfWeek).equals(targetDayNormalized, ignoreCase = true)) {
                val start2 = timeToMinutes(session.startTime)
                val end2 = timeToMinutes(session.endTime)
                if (start1 < end2 && start2 < end1) {
                    return session
                }
            }
        }
        return null
    }

    // Holidays
    val allHolidaysFlow: Flow<List<Holiday>> = holidayDao.getAllHolidaysFlow()

    suspend fun addHoliday(date: String, reason: String) = withContext(ioDispatcher) {
        attendanceDao.deleteAttendanceForDate(date)
        holidayDao.insertHoliday(Holiday(date, reason))
    }

    suspend fun removeHoliday(date: String) = withContext(ioDispatcher) {
        val existing = holidayDao.getHolidayByDate(date)
        if (existing != null) {
            holidayDao.deleteHoliday(existing)
        }
    }
}
