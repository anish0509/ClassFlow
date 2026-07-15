package com.anish18.classflow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "semesters")
data class Semester(
    @PrimaryKey val id: String,
    val name: String,
    val startDate: String, // ISO String (yyyy-MM-dd)
    val endDate: String,
    val isActive: Boolean = false
)
