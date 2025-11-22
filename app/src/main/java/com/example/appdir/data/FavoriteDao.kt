// com/example/app/data/FavoriteDao.kt
package com.example.appdir.data
import com.example.appdir.model.*
import androidx.room.OnConflictStrategy
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE category = :category AND ruleText = :ruleText")
    suspend fun deleteByCategoryAndText(category: String, ruleText: String)

    @Query("SELECT * FROM favorites")
    fun getAll(): kotlinx.coroutines.flow.Flow<List<FavoriteEntity>>




}
