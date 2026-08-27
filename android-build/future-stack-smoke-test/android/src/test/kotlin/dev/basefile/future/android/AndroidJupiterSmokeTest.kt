package dev.basefile.future.android

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class AndroidJupiterSmokeTest {
    @Test
    fun `Jupiter coroutine test executes on Android JVM task`() = runTest {
        assertEquals("2026-09-01", DesugaringProbe.isoDate())
    }

    @Nested
    inner class NestedDesugaringTests {
        @ParameterizedTest
        @CsvSource("1,2,3,2", "2,4,6,12", "3,5,7,0")
        fun `parameterized stream use is desugaring-compatible`(
            first: Int,
            second: Int,
            third: Int,
            expected: Int,
        ) {
            assertEquals(expected, DesugaringProbe.filteredTotal(listOf(first, second, third)))
        }
    }
}
