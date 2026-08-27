package dev.basefile.future

import com.google.crypto.tink.Aead
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.security.GeneralSecurityException

class CryptoVaultTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `encrypted keyset survives file persistence and detects tampering`() {
        val master = CryptoVault.newHandle().getPrimitive(Aead::class.java)
        val keysetFile = directory.resolve("encrypted-keyset.bin")
        CryptoVault.writeEncrypted(CryptoVault.newHandle(), keysetFile, master)
        val restored = CryptoVault.readEncrypted(keysetFile, master).getPrimitive(Aead::class.java)
        val associatedData = "profile:17".encodeToByteArray()
        val plaintext = "confidential offline state".encodeToByteArray()
        val ciphertext = restored.encrypt(plaintext, associatedData)
        assertArrayEquals(plaintext, restored.decrypt(ciphertext, associatedData))

        val tampered = ciphertext.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        assertThrows(GeneralSecurityException::class.java) {
            restored.decrypt(tampered, associatedData)
        }
        assertThrows(GeneralSecurityException::class.java) {
            restored.decrypt(ciphertext, "wrong-context".encodeToByteArray())
        }
    }
}
