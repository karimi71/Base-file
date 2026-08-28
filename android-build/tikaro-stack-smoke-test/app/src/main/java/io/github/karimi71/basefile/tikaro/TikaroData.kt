package io.github.karimi71.basefile.tikaro

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

val Context.tikaroPreferences by preferencesDataStore(name = "tikaro-settings")

@Serializable
data class BackupPayload(
    val dayKey: String,
    val completed: Boolean
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val dayKey: String,
    val title: String,
    val completed: Boolean
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE dayKey = :dayKey ORDER BY id")
    fun observeForDay(dayKey: String): Flow<List<TaskEntity>>

    @Insert
    suspend fun insert(task: TaskEntity)
}

@Database(entities = [TaskEntity::class], version = 1, exportSchema = true)
abstract class TikaroDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
