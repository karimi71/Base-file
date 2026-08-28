package dev.basefile.future

import com.squareup.moshi.JsonClass
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@JsonClass(generateAdapter = true)
data class GeneratedProfile(
    val name: String,
    val revision: Int,
    val enabled: Boolean = true,
)

@Serializable
data class TimestampEnvelope(
    val event: String,
    val at: Instant,
)

data class DayKey private constructor(val ordinal: Int) {
    fun encode(): String = "day-${ordinal.toString().padStart(5, '0')}"

    companion object {
        fun fromOrdinal(value: Int): DayKey {
            require(value in 0..200_000)
            return DayKey(value)
        }

        fun decode(value: String): DayKey = fromOrdinal(value.removePrefix("day-").toInt())
    }
}
