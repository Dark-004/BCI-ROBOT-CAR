package com.neurodrive.smartcar.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val userId: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val createdAt: Long = System.currentTimeMillis()
)
