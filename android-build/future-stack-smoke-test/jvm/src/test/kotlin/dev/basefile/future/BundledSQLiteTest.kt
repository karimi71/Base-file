package dev.basefile.future

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.sqlite.use
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class BundledSQLiteTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `bundled driver persists transactions constraints and cascades`() {
        val database = directory.resolve("bundled.db").toString()
        val driver = BundledSQLiteDriver()
        driver.open(database).use { connection ->
            connection.execSQL("PRAGMA foreign_keys = ON")
            connection.execSQL("CREATE TABLE parent(id INTEGER PRIMARY KEY, title TEXT NOT NULL UNIQUE)")
            connection.execSQL(
                "CREATE TABLE child(id INTEGER PRIMARY KEY, parent_id INTEGER NOT NULL " +
                    "REFERENCES parent(id) ON DELETE CASCADE)",
            )
            connection.execSQL("BEGIN IMMEDIATE")
            connection.execSQL("INSERT INTO parent(id, title) VALUES (1, 'offline')")
            connection.execSQL("INSERT INTO child(id, parent_id) VALUES (7, 1)")
            connection.execSQL("COMMIT")
            assertThrows(Exception::class.java) {
                connection.execSQL("INSERT INTO parent(id, title) VALUES (2, 'offline')")
            }
        }

        driver.open(database).use { reopened ->
            reopened.execSQL("PRAGMA foreign_keys = ON")
            reopened.prepare("SELECT title FROM parent WHERE id = 1").use { statement ->
                assertEquals(true, statement.step())
                assertEquals("offline", statement.getText(0))
            }
            reopened.execSQL("DELETE FROM parent WHERE id = 1")
            reopened.prepare("SELECT COUNT(*) FROM child").use { statement ->
                assertEquals(true, statement.step())
                assertEquals(0L, statement.getLong(0))
            }
        }
    }
}
