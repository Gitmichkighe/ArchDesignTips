// com/example/app/data/FavoriteEntity.kt
package com.example.appdir.data
import androidx.room.Entity
// FavoriteEntity.kt
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorites",
   // indices = [Index(value = ["category", "ruleText"], unique = true)]
)
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,
    val ruleText: String
)
