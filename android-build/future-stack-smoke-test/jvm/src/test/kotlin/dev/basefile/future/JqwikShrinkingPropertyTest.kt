package dev.basefile.future

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ForAll
import net.jqwik.api.Provide
import net.jqwik.api.Property
import net.jqwik.api.ShrinkingMode
import org.junit.jupiter.api.Assertions.assertEquals

class JqwikShrinkingPropertyTest {
    @Provide
    fun dayKeys(): Arbitrary<DayKey> =
        Arbitraries.integers().between(0, 200_000).map(DayKey::fromOrdinal)

    @Property(tries = 300, seed = "20241201051104", shrinking = ShrinkingMode.FULL)
    fun `jqwik generated domain values round trip and remain shrinkable`(
        @ForAll("dayKeys") key: DayKey,
    ) {
        assertEquals(key, DayKey.decode(key.encode()))
    }
}
