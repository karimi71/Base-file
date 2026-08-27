package dev.basefile.future

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import dev.basefile.future.proto.TikaroSettings
import java.io.InputStream
import java.io.OutputStream

object SettingsSerializer : Serializer<TikaroSettings> {
    override val defaultValue: TikaroSettings = TikaroSettings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): TikaroSettings = try {
        TikaroSettings.parseFrom(input)
    } catch (error: InvalidProtocolBufferException) {
        throw CorruptionException("Settings protobuf is corrupt", error)
    }

    override suspend fun writeTo(t: TikaroSettings, output: OutputStream) {
        t.writeTo(output)
    }
}
