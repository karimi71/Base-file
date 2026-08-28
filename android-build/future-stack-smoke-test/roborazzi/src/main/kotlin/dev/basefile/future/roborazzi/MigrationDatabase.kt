package dev.basefile.future.roborazzi

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "parents")
data class ParentEntity(
    @PrimaryKey val id: Long,
    val name: String,
)

@Entity(
    tableName = "children",
    foreignKeys = [
        ForeignKey(
            entity = ParentEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("parent_id"), Index(value = ["title"], unique = true)],
)
data class ChildEntity(
    @PrimaryKey val id: Long,
    val parent_id: Long,
    val title: String,
    val done: Boolean = false,
)

@Dao
interface MigrationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertChild(child: ChildEntity)

    @Query("SELECT * FROM children ORDER BY id")
    suspend fun children(): List<ChildEntity>

    @Query("DELETE FROM parents WHERE id = :id")
    suspend fun deleteParent(id: Long)

    @Query("SELECT COUNT(*) FROM children")
    suspend fun childCount(): Int
}

@Database(
    entities = [ParentEntity::class, ChildEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class MigrationDatabase : RoomDatabase() {
    abstract fun migrationDao(): MigrationDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE children ADD COLUMN done INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_children_parent_id ON children(parent_id)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_children_title ON children(title)")
    }
}
