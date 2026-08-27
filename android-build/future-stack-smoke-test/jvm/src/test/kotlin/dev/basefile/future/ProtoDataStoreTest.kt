package dev.basefile.future

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import dev.basefile.future.proto.TikaroSettings
import java.nio.file.Path
import kotlin.io.path.writeBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ProtoDataStoreTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `protobuf values persist migrate and recover from corruption`() = runTest {
        val target = directory.resolve("settings.pb")
        val firstScope = CoroutineScope(Job() + Dispatchers.IO)
        var cleanedLegacyState = false
        val migration = object : DataMigration<TikaroSettings> {
            override suspend fun shouldMigrate(currentData: TikaroSettings): Boolean =
                currentData.schemaRevision == 0

            override suspend fun migrate(currentData: TikaroSettings): TikaroSettings =
                currentData.toBuilder().setSchemaRevision(1).setProfileName("migrated").build()

            override suspend fun cleanUp() {
                cleanedLegacyState = true
            }
        }
        val firstStore = DataStoreFactory.create(
            serializer = SettingsSerializer,
            migrations = listOf(migration),
            scope = firstScope,
            produceFile = target::toFile,
        )
        assertEquals("migrated", firstStore.data.first().profileName)
        assertTrue(cleanedLegacyState)
        firstStore.updateData { current ->
            current.toBuilder()
                .setProfileName("offline-user")
                .setSchemaRevision(2)
                .addPinnedDocuments("guide.pdf")
                .build()
        }
        firstScope.coroutineContext[Job]!!.cancelAndJoin()

        val secondScope = CoroutineScope(Job() + Dispatchers.IO)
        val reopened = DataStoreFactory.create(
            serializer = SettingsSerializer,
            scope = secondScope,
            produceFile = target::toFile,
        )
        val persisted = reopened.data.first()
        assertEquals("offline-user", persisted.profileName)
        assertEquals(listOf("guide.pdf"), persisted.pinnedDocumentsList)
        secondScope.coroutineContext[Job]!!.cancelAndJoin()

        target.writeBytes(byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte()))
        val recoveryScope = CoroutineScope(Job() + Dispatchers.IO)
        val recovered = DataStoreFactory.create(
            serializer = SettingsSerializer,
            corruptionHandler = ReplaceFileCorruptionHandler {
                TikaroSettings.newBuilder()
                    .setProfileName("recovered")
                    .setSchemaRevision(3)
                    .build()
            },
            scope = recoveryScope,
            produceFile = target::toFile,
        ).data.first()
        assertEquals("recovered", recovered.profileName)
        recoveryScope.coroutineContext[Job]!!.cancelAndJoin()
    }
}
