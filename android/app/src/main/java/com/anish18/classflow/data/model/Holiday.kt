package com.anish18.classflow.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "holidays")
data class Holiday(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val reason: String
)
