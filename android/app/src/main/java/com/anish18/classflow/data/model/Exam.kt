package com.anish18.classflow.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exams",
    foreignKeys = [
        ForeignKey(
            entity = Semester::class,
            parentColumns = ["id"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["semesterId"]),
        Index(value = ["courseId"])
    ]
)
data class Exam(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val examType: String, // "Midterm", "Quiz", "Final", "Assignment Exam", "Lab Exam"
    val courseId: String? = null,
    val examDate: String, // "YYYY-MM-DD"
    val examTime: String = "09:00 AM", // "HH:mm AM/PM"
    val location: String? = null, // Room / Hall location, e.g. "Hall A17"
    val notes: String? = null, // Syllabus / Topics covered
    val semesterId: String,
    val reminderMinutesBefore: Int = 60, // Alarm reminder
    val isCompleted: Boolean = false
)
