package dev.basefile.future.android

import java.time.LocalDate
import java.util.function.Predicate
import java.util.stream.Stream

object DesugaringProbe {
    fun isoDate(): String = LocalDate.of(2026, 8, 27).plusDays(5).toString()

    fun filteredTotal(values: List<Int>): Int = Stream.of(*values.toTypedArray())
        .filter(Predicate { it % 2 == 0 })
        .mapToInt { value -> value }
        .sum()
}
