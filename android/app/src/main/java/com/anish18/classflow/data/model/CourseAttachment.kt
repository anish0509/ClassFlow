package com.anish18.classflow.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "course_attachments",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CourseAttachment(
    @PrimaryKey val id: String,
    val courseId: String,
    val fileName: String,
    val localPath: String,
    val fileType: String // "pdf", "image", or "other"
)
