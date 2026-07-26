package com.anish18.classflow.ui.screens.coursedetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anish18.classflow.data.model.Attendance
import com.anish18.classflow.data.model.ClassSession
import com.anish18.classflow.data.model.Course
import com.anish18.classflow.data.model.Exam
import com.anish18.classflow.data.model.Semester
import com.anish18.classflow.data.repository.ExamRepository
import com.anish18.classflow.data.repository.TimetableRepository
import android.content.Context
import android.widget.Toast
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CourseDetailsViewModel @Inject constructor(
    private val repository: TimetableRepository,
    private val examRepository: ExamRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    val courseId: String = savedStateHandle.get<String>("courseId") ?: ""

    val course: StateFlow<Course?> = repository.getCourseByIdFlow(courseId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val courseSemester: StateFlow<Semester?> = course.flatMapLatest { c ->
        if (c == null) flowOf(null)
        else repository.activeSemesterFlow.map { sem ->
            if (sem?.id == c.semesterId) sem else repository.getSemesterById(c.semesterId)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val classes: StateFlow<List<ClassSession>> = repository.activeClassesFlow
        .map { list -> list.filter { it.courseId == courseId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendance: StateFlow<List<Attendance>> = repository.getAttendanceForCourseFlow(courseId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attachments: StateFlow<List<com.anish18.classflow.data.model.CourseAttachment>> = repository.getAttachmentsForCourseFlow(courseId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<com.anish18.classflow.data.model.Task>> = repository.allTasksFlow
        .map { list -> list.filter { it.courseId == courseId && it.status != "completed" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSemester: StateFlow<Semester?> = repository.activeSemesterFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val courseExams: StateFlow<List<Exam>> = activeSemester.flatMapLatest { sem ->
        if (sem == null) flowOf(emptyList())
        else examRepository.getExamsForSemester(sem.id).map { list -> list.filter { it.courseId == courseId } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun normalizeDayOfWeek(day: String): String {
        val d = day.trim()
        return when {
            d.startsWith("MON", ignoreCase = true) -> "Monday"
            d.startsWith("TUE", ignoreCase = true) -> "Tuesday"
            d.startsWith("WED", ignoreCase = true) -> "Wednesday"
            d.startsWith("THU", ignoreCase = true) -> "Thursday"
            d.startsWith("FRI", ignoreCase = true) -> "Friday"
            d.startsWith("SAT", ignoreCase = true) -> "Saturday"
            d.startsWith("SUN", ignoreCase = true) -> "Sunday"
            else -> d.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.US) else it.toString() }
        }
    }

    fun addClassSession(dayOfWeek: String, startTime: String, endTime: String, room: String?) {
        viewModelScope.launch {
            val normalizedDay = normalizeDayOfWeek(dayOfWeek)
            val activeSem = repository.getActiveSemester()
            val clash = repository.checkClash(normalizedDay, startTime, endTime, activeSem?.id)
            if (clash != null) {
                _toastMessage.emit("Class Slot Clash: Overlaps with an existing class session!")
                return@launch
            }
            repository.insertClass(
                ClassSession(
                    id = UUID.randomUUID().toString(),
                    courseId = courseId,
                    dayOfWeek = normalizedDay,
                    startTime = startTime,
                    endTime = endTime,
                    room = room,
                    semesterId = activeSem?.id
                )
            )
        }
    }

    fun deleteClassSession(classSession: ClassSession) {
        viewModelScope.launch {
            repository.deleteClass(classSession)
        }
    }

    fun addExam(title: String, type: String, examDate: String, examTime: String, location: String?, notes: String?) {
        viewModelScope.launch {
            val semId = courseSemester.value?.id ?: activeSemester.value?.id ?: return@launch
            val exam = Exam(
                title = title.trim(),
                examType = type,
                courseId = courseId,
                examDate = examDate,
                examTime = examTime,
                location = location?.trim()?.ifEmpty { null },
                notes = notes?.trim()?.ifEmpty { null },
                semesterId = semId
            )
            examRepository.addExam(exam)
        }
    }

    fun deleteExam(exam: Exam) {
        viewModelScope.launch {
            examRepository.deleteExam(exam)
        }
    }

    fun saveCourseNotes(notes: String) {
        viewModelScope.launch {
            val current = course.value
            if (current != null) {
                val updated = current.copy(notes = notes)
                repository.updateCourse(updated)
                _toastMessage.emit("Course notes saved successfully!")
            }
        }
    }

    fun saveMinAttendanceRequirement(requirement: Int) {
        viewModelScope.launch {
            val current = course.value
            if (current != null) {
                val updated = current.copy(minAttendanceRequirement = requirement)
                repository.updateCourse(updated)
            }
        }
    }

    fun deleteCourse() {
        viewModelScope.launch {
            val current = course.value
            if (current != null) {
                repository.deleteCourse(current)
            }
        }
    }

    fun updateCourseDetails(name: String, shortName: String, professor: String, credits: Int, room: String, colorHex: String) {
        viewModelScope.launch {
            val current = course.value
            if (current != null) {
                val updated = current.copy(
                    name = name,
                    shortName = shortName,
                    professor = professor,
                    credits = credits,
                    room = room,
                    color = colorHex
                )
                repository.updateCourse(updated)
            }
        }
    }

    fun addAttachment(fileName: String, fileUri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val attachmentsDir = java.io.File(context.filesDir, "attachments/$courseId")
                if (!attachmentsDir.exists()) {
                    attachmentsDir.mkdirs()
                }
                
                val destinationFile = java.io.File(attachmentsDir, fileName)
                context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                    destinationFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                val fileType = if (fileName.lowercase().endsWith(".pdf")) "pdf" else "image"
                
                val attachment = com.anish18.classflow.data.model.CourseAttachment(
                    id = java.util.UUID.randomUUID().toString(),
                    courseId = courseId,
                    fileName = fileName,
                    localPath = destinationFile.absolutePath,
                    fileType = fileType
                )
                repository.insertAttachment(attachment)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteAttachment(attachment: com.anish18.classflow.data.model.CourseAttachment) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val file = java.io.File(attachment.localPath)
                if (file.exists()) {
                    file.delete()
                }
                repository.deleteAttachment(attachment)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markAttendance(date: String, status: String?, notes: String? = null) {
        viewModelScope.launch {
            try {
                val localDate = java.time.LocalDate.parse(date)
                val weekdayStr = localDate.dayOfWeek.name.lowercase()
                val session = classes.value.find { it.dayOfWeek.lowercase() == weekdayStr }
                val classId = session?.id ?: "unknown"

                val existing = repository.getAttendanceForClassAndDate(classId, date)
                
                if (status == null) {
                    if (existing != null) {
                        repository.deleteAttendance(existing)
                    }
                } else {
                    if (existing != null) {
                        repository.updateAttendance(existing.copy(status = status, notes = notes ?: existing.notes))
                    } else {
                        repository.insertAttendance(
                            Attendance(
                                id = UUID.randomUUID().toString(),
                                classId = classId,
                                courseId = courseId,
                                date = date,
                                status = status,
                                notes = notes,
                                markedAt = java.time.Instant.now().toString()
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveAttendanceNote(date: String, note: String) {
        viewModelScope.launch {
            try {
                val localDate = java.time.LocalDate.parse(date)
                val weekdayStr = localDate.dayOfWeek.name.lowercase()
                val session = classes.value.find { it.dayOfWeek.lowercase() == weekdayStr }
                val classId = session?.id ?: "unknown"

                val existing = repository.getAttendanceForClassAndDate(classId, date)
                
                if (existing != null) {
                    repository.updateAttendance(existing.copy(notes = note.ifEmpty { null }))
                } else {
                    repository.insertAttendance(
                        Attendance(
                            id = UUID.randomUUID().toString(),
                            classId = classId,
                            courseId = courseId,
                            date = date,
                            status = "absent",
                            notes = note.ifEmpty { null },
                            markedAt = java.time.Instant.now().toString()
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun shiftClassSession(
        classSession: ClassSession,
        originalDate: java.time.LocalDate,
        newDate: java.time.LocalDate,
        startTime: String,
        endTime: String,
        room: String
    ) {
        viewModelScope.launch {
            try {
                val newDayOfWeek = newDate.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.US)
                val clash = repository.checkClash(newDayOfWeek, startTime, endTime, classSession.semesterId, excludeClassId = classSession.id)
                if (clash != null) {
                    _toastMessage.emit("Class Slot Clash: Overlaps with an existing class session!")
                    return@launch
                }
                
                val allAttendance = repository.getAttendanceForCourse(classSession.courseId)
                val parentRecord = allAttendance.find { 
                    it.classId == classSession.id && it.shiftedToDate == originalDate.toString() && it.status == "shifted" 
                }
                
                if (parentRecord != null) {
                    if (newDate.toString() == parentRecord.date) {
                        repository.deleteAttendance(parentRecord)
                    } else {
                        val updatedParent = parentRecord.copy(
                            shiftedToDate = newDate.toString(),
                            shiftedStartTime = startTime,
                            shiftedEndTime = endTime,
                            shiftedRoom = room,
                            markedAt = System.currentTimeMillis().toString()
                        )
                        repository.insertAttendance(updatedParent)
                    }
                    
                    val originalDateRecord = allAttendance.find {
                        it.classId == classSession.id && it.date == originalDate.toString() && it.status != "shifted"
                    }
                    if (originalDateRecord != null) {
                        repository.deleteAttendance(originalDateRecord)
                    }
                } else {
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
