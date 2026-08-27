package io.github.karimi71.basefile.paparazzi

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

class PaparazziComposeTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5)

    @Test
    fun rendersComposeWithoutAndroidStudioOrDevice() {
        paparazzi.snapshot {
            MaterialTheme {
                Surface {
                    Text("Tikaro golden test")
                }
            }
        }
    }
}
