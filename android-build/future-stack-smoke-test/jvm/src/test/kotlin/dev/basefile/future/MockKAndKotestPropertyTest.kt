package dev.basefile.future

import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MockKAndKotestPropertyTest {
    interface RevisionSource {
        fun revision(profile: String): Int
    }

    @Test
    fun `MockK JVM agent records a typed interaction`() {
        val source = mockk<RevisionSource>()
        every { source.revision("offline") } returns 29
        assertEquals(29, source.revision("offline"))
        verify(exactly = 1) { source.revision("offline") }
    }

    @Test
    fun `Kotest custom range round trips with a fixed seed`() = runTest {
        checkAll(iterations = 300, Arb.int(0..200_000)) { ordinal ->
            val key = DayKey.fromOrdinal(ordinal)
            assertEquals(key, DayKey.decode(key.encode()))
        }
    }
}
