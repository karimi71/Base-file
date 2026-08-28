package dev.basefile.future.android

import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyJUnit4SmokeTest {
    @Test
    fun vintageEngineStillExecutesJUnit4() {
        assertEquals(12, DesugaringProbe.filteredTotal(listOf(1, 2, 4, 6)))
    }
}
