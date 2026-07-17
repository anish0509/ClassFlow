package com.anish18.classflow.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "classes",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Semester::class,
            parentColumns = ["id"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["courseId"]),
        Index(value = ["semesterId"])
    ]
)
data class ClassSession(
    @PrimaryKey val id: String,
    val courseId: String,
    val dayOfWeek: String, // e.g. "monday", "tuesday"
    val startTime: String, // HH:MM
    val endTime: String,
    val room: String?,
    val semesterId: String?
)
