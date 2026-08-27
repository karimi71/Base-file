package dev.basefile.future.roborazzi

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class RoomMigrationIntegrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "room-migration-fixture.db"

    @Before
    @After
    fun clearDatabase() {
        context.deleteDatabase(name)
    }

    @Test
    fun `Room migrates real framework SQLite file then enforces constraints and cascade`() = runTest {
        createVersionOneDatabase()
        val room = Room.databaseBuilder(context, MigrationDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2)
            .build()
        room.openHelper.writableDatabase

        assertEquals(
            listOf(ChildEntity(id = 10, parent_id = 1, title = "legacy.pdf", done = false)),
            room.migrationDao().children(),
        )
        var constraintFailure: Throwable? = null
        try {
            room.migrationDao().insertChild(
                ChildEntity(id = 11, parent_id = 1, title = "legacy.pdf"),
            )
        } catch (failure: Throwable) {
            constraintFailure = failure
        }
        assertNotNull("A unique title constraint must reject the duplicate", constraintFailure)
        room.migrationDao().deleteParent(1)
        assertEquals(0, room.migrationDao().childCount())
        room.close()

        // SupportSQLiteOpenHelper rejects a v2 -> v1 open by default. No fixture
        // calls fallbackToDestructiveMigration or fallbackToDestructiveMigrationOnDowngrade.
        val downgrade = frameworkHelper(version = 1)
        assertThrows(Exception::class.java) { downgrade.writableDatabase }
        downgrade.close()
        assertNotNull(MigrationTestHelper::class.java)
    }

    private fun createVersionOneDatabase() {
        val helper = frameworkHelper(version = 1)
        helper.writableDatabase.apply {
            execSQL("PRAGMA foreign_keys = ON")
            execSQL("CREATE TABLE parents(id INTEGER NOT NULL PRIMARY KEY, name TEXT NOT NULL)")
            execSQL(
                "CREATE TABLE children(id INTEGER NOT NULL PRIMARY KEY, parent_id INTEGER NOT NULL, " +
                    "title TEXT NOT NULL, FOREIGN KEY(parent_id) REFERENCES parents(id) ON DELETE CASCADE)",
            )
            execSQL("INSERT INTO parents(id, name) VALUES (1, 'legacy')")
            execSQL("INSERT INTO children(id, parent_id, title) VALUES (10, 1, 'legacy.pdf')")
        }
        helper.close()
    }

    private fun frameworkHelper(version: Int): SupportSQLiteOpenHelper {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(object : SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) = Unit
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(configuration)
    }
}
