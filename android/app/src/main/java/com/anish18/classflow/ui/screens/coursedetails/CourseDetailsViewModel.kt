package com.anish18.classflow.ui.screens.coursedetails

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CourseDetailsViewModel @Inject constructor(
    private val repository: TimetableRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    val courseId: String = savedStateHandle.get<String>("courseId") ?: ""

    private val _course = MutableStateFlow<Course?>(null)
    val course: StateFlow<Course?> = _course.asStateFlow()

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

    init {
        viewModelScope.launch {
            _course.value = repository.getCourseById(courseId)
        }
    }

    fun addClassSession(dayOfWeek: String, startTime: String, endTime: String, room: String?) {
        viewModelScope.launch {
            val activeSem = repository.getActiveSemester()
            val clash = repository.checkClash(dayOfWeek, startTime, endTime, activeSem?.id)
            if (clash != null) {
                _toastMessage.emit("Class Slot Clash: Overlaps with an existing class session!")
                return@launch
            }
            repository.insertClass(
                ClassSession(
                    id = UUID.randomUUID().toString(),
                    courseId = courseId,
                    dayOfWeek = dayOfWeek,
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

    fun saveCourseNotes(notes: String) {
        viewModelScope.launch {
            val current = _course.value
            if (current != null) {
                val updated = current.copy(notes = notes)
                repository.updateCourse(updated)
                _course.value = updated
            }
        }
    }

    fun saveMinAttendanceRequirement(requirement: Int) {
        viewModelScope.launch {
            val current = _course.value
            if (current != null) {
                val updated = current.copy(minAttendanceRequirement = requirement)
                repository.updateCourse(updated)
                _course.value = updated
            }
        }
    }

    fun deleteCourse() {
        viewModelScope.launch {
            val current = _course.value
            if (current != null) {
                repository.deleteCourse(current)
            }
        }
    }

    fun updateCourseDetails(name: String, shortName: String, professor: String, credits: Int, room: String, colorHex: String) {
        viewModelScope.launch {
            val current = _course.value
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
                _course.value = updated
            }
        }
    }

    fun addAttachment(fileName: String, fileUri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch {
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
        viewModelScope.launch {
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
                // Find which class session corresponds to this date's weekday
                val localDate = java.time.LocalDate.parse(date)
                val weekdayStr = localDate.dayOfWeek.name.lowercase() // e.g. "monday"
                val session = classes.value.find { it.dayOfWeek.lowercase() == weekdayStr }
                val classId = session?.id ?: "unknown"

                // Check if there is already an attendance log for this class and date
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
                val weekdayStr = localDate.dayOfWeek.name.lowercase() // e.g. "monday"
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
                            status = "absent", // default to absent if unmarked and they write a note
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
