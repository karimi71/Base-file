package dev.basefile.future

import com.google.gson.Gson
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import java.time.Duration
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SerializationAndTimeTest {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun `generated Moshi adapter and Gson round trip the same profile`() {
        val source = GeneratedProfile("fixture", 7)
        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter<GeneratedProfile>()
        assertEquals(source, adapter.fromJson(adapter.toJson(source)))
        assertNotNull(Class.forName("dev.basefile.future.GeneratedProfileJsonAdapter"))

        val gson = Gson()
        assertEquals(source, gson.fromJson(gson.toJson(source), GeneratedProfile::class.java))
    }

    @Test
    fun `datetime serialization preserves an Oslo daylight-saving boundary`() {
        val oslo = TimeZone.of("Europe/Oslo")
        val before = LocalDateTime(2024, 3, 31, 1, 30).toInstant(oslo)
        val after = LocalDateTime(2024, 3, 31, 3, 30).toInstant(oslo)
        assertEquals(Duration.ofHours(1), Duration.between(before.toJavaInstant(), after.toJavaInstant()))

        val envelope = TimestampEnvelope("dst-boundary", after)
        assertEquals(envelope, Json.decodeFromString<TimestampEnvelope>(Json.encodeToString(envelope)))
    }
}
