package com.anish18.classflow.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish18.classflow.data.model.Attendance
import com.anish18.classflow.data.model.ClassSession
import com.anish18.classflow.data.model.Course
import com.anish18.classflow.data.model.Semester
import com.anish18.classflow.data.repository.TimetableRepository
import android.content.Context
import android.widget.Toast
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import java.time.format.TextStyle
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TimetableRepository,
    private val appSettings: com.anish18.classflow.data.repository.AppSettings
) : ViewModel() {

    val currentStreak = appSettings.currentStreak
    val longestStreak = appSettings.longestStreak

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.activeSemesterFlow.filterNotNull().collectLatest { semester ->
                try {
                    // 1. Clean up duplicate weekly class sessions
                    val classesInSem = repository.getClassesBySemester(semester.id)
                    val groupedClasses = classesInSem.groupBy { 
                        Triple(it.courseId, it.dayOfWeek.lowercase(), it.startTime + "-" + it.endTime) 
                    }
                    groupedClasses.forEach { (_, sessions) ->
                        if (sessions.size > 1) {
                            for (i in 1 until sessions.size) {
                                repository.deleteClass(sessions[i])
                            }
                        }
                    }
                    
                    // 2. Clean up duplicate attendance records for the same class and date
                    val allAttendance = repository.allAttendanceFlow.first()
                    val groupedAtt = allAttendance.groupBy { Pair(it.classId, it.date) }
                    groupedAtt.forEach { (_, records) ->
                        if (records.size > 1) {
                            for (i in 1 until records.size) {
                                repository.deleteAttendance(records[i])
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val activeSemester: StateFlow<Semester?> = repository.activeSemesterFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val courses: StateFlow<List<Course>> = repository.activeCoursesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val classes: StateFlow<List<ClassSession>> = repository.activeClassesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendance: StateFlow<List<Attendance>> = repository.allAttendanceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<com.anish18.classflow.data.model.Task>> = repository.allTasksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val holidays: StateFlow<List<com.anish18.classflow.data.model.Holiday>> = repository.allHolidaysFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _currentWeekStart = MutableStateFlow(
        calculateSunday(LocalDate.now())
    )
    val currentWeekStart: StateFlow<LocalDate> = _currentWeekStart.asStateFlow()

    val classesForSelectedDate: StateFlow<List<ClassSession>> = combine(
        selectedDate,
        classes,
        attendance,
        activeSemester
    ) { date, classList, attendanceList, sem ->
        if (sem != null) {
            val dateStr = date.toString()
            if (dateStr < sem.startDate || dateStr > sem.endDate) {
                return@combine emptyList()
            }
        }
        val dayOfWeekStr = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.US)
        
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
        
        val targetDayNormalized = normalizeDay(dayOfWeekStr)
        
        // 1. Regular class sessions for this weekday
        val regularClasses = classList.filter { normalizeDay(it.dayOfWeek).equals(targetDayNormalized, ignoreCase = true) }
        
        // 2. Class sessions from other weekdays that were SHIFTED to this selected date
        val shiftedClasses = attendanceList.filter { it.status == "shifted" && it.shiftedToDate == date.toString() }
            .mapNotNull { attRecord ->
                val originalSession = classList.find { it.id == attRecord.classId } ?: return@mapNotNull null
                val shiftedDay = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.US)
                
                // Dynamically construct class session with shifted times and room
                ClassSession(
                    id = originalSession.id,
                    courseId = originalSession.courseId,
                    dayOfWeek = shiftedDay,
                    startTime = attRecord.shiftedStartTime ?: originalSession.startTime,
                    endTime = attRecord.shiftedEndTime ?: originalSession.endTime,
                    room = attRecord.shiftedRoom ?: originalSession.room,
                    semesterId = originalSession.semesterId
                )
            }
            
        (regularClasses + shiftedClasses).sortedBy { it.startTime }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun calculateSunday(date: LocalDate): LocalDate {
        val offset = date.dayOfWeek.value % 7
        return date.minusDays(offset.toLong())
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        val startOfWeek = calculateSunday(date)
        if (startOfWeek != _currentWeekStart.value) {
            _currentWeekStart.value = startOfWeek
        }
    }

    fun selectToday() {
        selectDate(LocalDate.now())
    }

    fun navigateWeek(weeksOffset: Long) {
        val newWeekStart = _currentWeekStart.value.plusWeeks(weeksOffset)
        _currentWeekStart.value = newWeekStart
        _selectedDate.value = _selectedDate.value.plusWeeks(weeksOffset)
    }

    fun addClassSession(courseId: String, dayOfWeek: String, startTime: String, endTime: String, room: String, semesterId: String) {
        viewModelScope.launch {
            val clash = repository.checkClash(dayOfWeek, startTime, endTime, semesterId)
            if (clash != null) {
                _toastMessage.emit("Class Slot Clash: Overlaps with an existing class session!")
                return@launch
            }
            repository.insertClass(
                ClassSession(
                    id = java.util.UUID.randomUUID().toString(),
                    courseId = courseId,
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = endTime,
                    room = room,
                    semesterId = semesterId
                )
            )
        }
    }

    fun deleteClassSession(classSession: ClassSession) {
        viewModelScope.launch {
            repository.deleteClass(classSession)
        }
    }

    fun markAttendance(classId: String, courseId: String, date: String, status: String, notes: String? = null) {
        viewModelScope.launch {
            val existing = repository.getAttendanceForClassAndDate(classId, date)
            if (existing != null) {
                repository.insertAttendance(
                    existing.copy(
                        status = status,
                        notes = notes ?: existing.notes,
                        markedAt = System.currentTimeMillis().toString()
                    )
                )
            } else {
                repository.insertAttendance(
                    Attendance(
                        id = java.util.UUID.randomUUID().toString(),
                        classId = classId,
                        courseId = courseId,
                        date = date,
                        status = status,
                        notes = notes,
                        markedAt = System.currentTimeMillis().toString()
                    )
                )
            }

            // Update streak when marking today's attendance
            val today = java.time.LocalDate.now().toString()
            if (date == today) {
                try {
                    val todayDay = java.time.LocalDate.now().dayOfWeek
                        .getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.US)
                    fun norm(d: String) = when {
                        d.startsWith("MON", true) -> "Monday"
                        d.startsWith("TUE", true) -> "Tuesday"
                        d.startsWith("WED", true) -> "Wednesday"
                        d.startsWith("THU", true) -> "Thursday"
                        d.startsWith("FRI", true) -> "Friday"
                        d.startsWith("SAT", true) -> "Saturday"
                        d.startsWith("SUN", true) -> "Sunday"
                        else -> d
                    }
                    val todayClasses = classes.value.filter { norm(it.dayOfWeek).equals(todayDay, true) }
                    val allAttendance = attendance.value
                    val allMarkedAttended = todayClasses.isNotEmpty() && todayClasses.all { session ->
                        val att = allAttendance.find { it.classId == session.id && it.date == today }
                        att?.status == "attended" || (session.id == classId && status == "attended")
                    }
                    appSettings.updateStreak(today, allMarkedAttended)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    fun saveAttendanceNote(classId: String, courseId: String, date: String, note: String) {
        viewModelScope.launch {
            val existing = repository.getAttendanceForClassAndDate(classId, date)
            if (existing != null) {
                repository.insertAttendance(
                    existing.copy(
                        notes = note.ifEmpty { null },
                        markedAt = System.currentTimeMillis().toString()
                    )
                )
            } else {
                repository.insertAttendance(
                    Attendance(
                        id = java.util.UUID.randomUUID().toString(),
                        classId = classId,
                        courseId = courseId,
                        date = date,
                        status = "absent", // default to absent if unmarked and they write a note
                        notes = note.ifEmpty { null },
                        markedAt = System.currentTimeMillis().toString()
                    )
                )
            }
        }
    }

    fun removeAttendance(classId: String, date: String) {
        viewModelScope.launch {
            val existing = repository.getAttendanceForClassAndDate(classId, date)
            if (existing != null) {
                repository.deleteAttendance(existing)
            }
        }
    }

    fun shiftClassSession(
        classSession: ClassSession,
        originalDate: LocalDate,
        newDate: LocalDate,
        startTime: String,
        endTime: String,
        room: String
    ) {
        viewModelScope.launch {
            val newDayOfWeek = newDate.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.US)
            val clash = repository.checkClash(newDayOfWeek, startTime, endTime, classSession.semesterId, excludeClassId = classSession.id)
            if (clash != null) {
                _toastMessage.emit("Class Slot Clash: Overlaps with an existing class session!")
                return@launch
            }
            
            // Check if originalDate was itself a shifted-to date for this class
            val allAttendance = repository.getAttendanceForCourse(classSession.courseId)
            val parentRecord = allAttendance.find { 
                it.classId == classSession.id && it.shiftedToDate == originalDate.toString() && it.status == "shifted" 
            }
            
            if (parentRecord != null) {
                // The class was previously shifted from parentRecord.date to originalDate.
                if (newDate.toString() == parentRecord.date) {
                    // Shifting back to original date -> delete the shift record
                    repository.deleteAttendance(parentRecord)
                } else {
                    // Shifting to a new date -> update the original shift record
                    val updatedParent = parentRecord.copy(
                        shiftedToDate = newDate.toString(),
                        shiftedStartTime = startTime,
                        shiftedEndTime = endTime,
                        shiftedRoom = room,
                        markedAt = System.currentTimeMillis().toString()
                    )
                    repository.insertAttendance(updatedParent)
                }
                
                // Since it is no longer shifted to originalDate, delete any attendance record 
                // the user might have marked on originalDate
                val originalDateRecord = allAttendance.find {
                    it.classId == classSession.id && it.date == originalDate.toString() && it.status != "shifted"
                }
                if (originalDateRecord != null) {
                    repository.deleteAttendance(originalDateRecord)
                }
            } else {
                // originalDate is the original day of the class session
                val existing = repository.getAttendanceForClassAndDate(classSession.id, originalDate.toString())
                val attendanceRecord = existing?.copy(
                    status = "shifted",
                    shiftedToDate = newDate.toString(),
                    shiftedStartTime = startTime,
                    shiftedEndTime = endTime,
                    shiftedRoom = room,
                    markedAt = System.currentTimeMillis().toString()
                ) ?: Attendance(
                    id = java.util.UUID.randomUUID().toString(),
                    classId = classSession.id,
                    courseId = classSession.courseId,
                    date = originalDate.toString(),
                    status = "shifted",
                    shiftedToDate = newDate.toString(),
                    shiftedStartTime = startTime,
                    shiftedEndTime = endTime,
                    shiftedRoom = room,
                    markedAt = System.currentTimeMillis().toString()
                )
                repository.insertAttendance(attendanceRecord)
            }
        }
    }
}
