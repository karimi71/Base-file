package io.github.karimi71.basefile.tikaro

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class StackUnitTest {
    @Test
    fun serializationAndFlowToolingAreUsable() = runTest {
        val payload = BackupPayload("1404-01-01", completed = true)
        assertThat(Json.decodeFromString<BackupPayload>(Json.encodeToString(payload)))
            .isEqualTo(payload)

        flowOf(payload).test {
            assertThat(awaitItem()).isEqualTo(payload)
            awaitComplete()
        }
    }
}
