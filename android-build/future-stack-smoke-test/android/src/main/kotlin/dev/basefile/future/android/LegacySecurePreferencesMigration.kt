package dev.basefile.future.android

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.crypto.tink.Aead
import java.io.File

/**
 * One-shot migration helper. The encrypted source is cleared only after an
 * authenticated destination has been durably written.
 */
object LegacySecurePreferencesMigration {
    fun openLegacyEncryptedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            "legacy-encrypted-preferences",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun migrate(source: SharedPreferences, destination: File, aead: Aead) {
        val canonical = source.all.toSortedMap().entries.joinToString("\n") { (key, value) ->
            "$key=${value.toString()}"
        }.encodeToByteArray()
        val ciphertext = aead.encrypt(canonical, destination.name.encodeToByteArray())
        val temporary = File(destination.parentFile, ".${destination.name}.tmp")
        temporary.outputStream().use { stream ->
            stream.write(ciphertext)
            stream.fd.sync()
        }
        check(temporary.renameTo(destination)) { "Unable to atomically publish migrated preferences" }
        source.edit().clear().commit()
    }
}
