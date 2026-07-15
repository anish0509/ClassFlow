package com.anish18.classflow.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "courses",
    foreignKeys = [
        ForeignKey(
            entity = Semester::class,
            parentColumns = ["id"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Course(
    @PrimaryKey val id: String,
    val name: String,
    val shortName: String,
    val professor: String,
    val credits: Int = 3,
    val room: String,
    val color: String, // Hex color code
    val semesterId: String?,
    val minAttendanceRequirement: Int? = null,
    val professorEmail: String? = null,
    val professorPhone: String? = null,
    val syllabusUrl: String? = null,
    val notes: String? = null
)
