package dev.basefile.future

import com.google.crypto.tink.Aead
import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.BinaryKeysetWriter
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.config.TinkConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import java.nio.file.Path
import kotlin.io.path.toFile

object CryptoVault {
    init {
        TinkConfig.register()
    }

    fun newHandle(): KeysetHandle = KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)

    fun writeEncrypted(handle: KeysetHandle, target: Path, masterAead: Aead) {
        handle.write(BinaryKeysetWriter.withFile(target.toFile()), masterAead)
    }

    fun readEncrypted(target: Path, masterAead: Aead): KeysetHandle =
        KeysetHandle.read(BinaryKeysetReader.withFile(target.toFile()), masterAead)
}
