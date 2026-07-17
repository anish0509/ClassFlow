package com.anish18.classflow.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["courseId"])
    ]
)
data class Task(
    @PrimaryKey val id: String,
    val courseId: String?,
    val title: String,
    val description: String?,
    val dueDate: String?,
    val dueTime: String? = null,
    val priority: String = "medium", // "low", "medium", "high"
    val status: String = "pending", // "pending", "completed"
    val createdAt: String,
    val completedAt: String? = null
)
