package dev.basefile.future.roborazzi

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.config.TinkConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SecurityMigrationIntegrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `legacy preferences migrate to authenticated file before source is cleared`() {
        val source = context.getSharedPreferences("legacy-source", Context.MODE_PRIVATE)
        source.edit().clear().putString("token", "offline-secret").putInt("revision", 4).commit()
        val aead = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
            .getPrimitive(Aead::class.java)
        val destination = File(context.cacheDir, "migrated-preferences.aead").apply { delete() }

        SecurePreferenceFileMigration.migrate(source, destination, aead)

        assertTrue(destination.length() > 20)
        assertTrue(source.all.isEmpty())
        val plaintext = aead.decrypt(destination.readBytes(), destination.name.encodeToByteArray())
            .decodeToString()
        assertTrue(plaintext.contains("token=offline-secret"))
        assertTrue(plaintext.contains("revision=4"))
        assertFalse(plaintext.contains("androidx.security"))
        assertEquals("legacy-encrypted-preferences", "legacy-encrypted-preferences")
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun registerTink() {
            TinkConfig.register()
        }
    }
}
