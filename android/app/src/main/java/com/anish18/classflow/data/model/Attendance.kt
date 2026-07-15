package com.anish18.classflow.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance",
    foreignKeys = [
        ForeignKey(
            entity = ClassSession::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Attendance(
    @PrimaryKey val id: String,
    val classId: String,
    val courseId: String,
    val date: String, // yyyy-MM-dd
    val status: String, // "attended", "absent", "cancelled", "holiday"
    val markedAt: String, // ISO timestamp
    val notes: String? = null,
    val shiftedToDate: String? = null,
    val shiftedStartTime: String? = null,
    val shiftedEndTime: String? = null,
    val shiftedRoom: String? = null
)
