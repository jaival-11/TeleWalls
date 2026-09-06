package me.jaival.telewalls.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.jaival.telewalls.data.local.entity.CategoryEntity

@Dao
interface CategoryDao {

    @Query("SELECT name FROM categories ORDER BY sortOrder ASC, createdAt ASC")
    fun getAllCategories(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Query("SELECT name FROM categories ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getCategoryList(): List<String>

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getAllCategoryEntities(): List<CategoryEntity>

    @Query("DELETE FROM categories WHERE name = :name")
    suspend fun deleteCategory(name: String)

    @Query("UPDATE categories SET name = :newName WHERE name = :oldName")
    suspend fun renameCategory(oldName: String, newName: String)

    @Query("DELETE FROM categories")
    suspend fun clearCategories()
}

