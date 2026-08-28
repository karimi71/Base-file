package io.github.karimi71.basefile.quality

data class QualityProbe(val name: String) {
    fun normalized(): String = name.trim().lowercase()
}
