package me.jaival.telewalls.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

