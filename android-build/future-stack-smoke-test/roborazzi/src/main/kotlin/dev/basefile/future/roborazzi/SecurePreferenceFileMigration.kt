package dev.basefile.future.roborazzi

import android.content.SharedPreferences
import com.google.crypto.tink.Aead
import java.io.File

/** Host-tested migration core used by the Android Security Crypto migration sample. */
object SecurePreferenceFileMigration {
    fun migrate(source: SharedPreferences, destination: File, aead: Aead) {
        val canonical = source.all.toSortedMap().entries.joinToString("\n") { (key, value) ->
            "$key=${value.toString()}"
        }.encodeToByteArray()
        val temporary = File(destination.parentFile, ".${destination.name}.tmp")
        temporary.outputStream().use { stream ->
            stream.write(aead.encrypt(canonical, destination.name.encodeToByteArray()))
            stream.fd.sync()
        }
        check(temporary.renameTo(destination))
        check(source.edit().clear().commit())
    }
}
