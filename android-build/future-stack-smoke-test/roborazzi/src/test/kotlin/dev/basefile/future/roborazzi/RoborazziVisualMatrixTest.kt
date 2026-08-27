package dev.basefile.future.roborazzi

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel4)
class RoborazziVisualMatrixTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun lightLtr() = capture("light-ltr", dark = false)
    @Test fun darkLtr() = capture("dark-ltr", dark = true)
    @Test fun lightRtl() = capture("light-rtl", dark = false, rtl = true)
    @Test fun darkRtl() = capture("dark-rtl", dark = true, rtl = true)
    @Test fun largeFont() = capture("large-font", dark = false, fontScale = 1.6f)
    @Test fun longData() = capture("long-data", dark = false, longData = true)

    private fun capture(
        name: String,
        dark: Boolean,
        rtl: Boolean = false,
        fontScale: Float = 1f,
        longData: Boolean = false,
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalDensity provides Density(density.density, fontScale),
            ) {
                FixtureCard(dark = dark, longData = longData)
            }
        }
        compose.onRoot().captureRoboImage(
            filePath = "$name.png",
            roborazziOptions = RoborazziOptions(
                compareOptions = RoborazziOptions.CompareOptions(
                    changeThreshold = 0.001f,
                    comparisonStyle = RoborazziOptions.CompareOptions.ComparisonStyle.Grid(),
                ),
            ),
        )
    }
}

@Composable
private fun FixtureCard(dark: Boolean, longData: Boolean) {
    val background = if (dark) Color(0xFF101820) else Color(0xFFF6FAFF)
    val foreground = if (dark) Color(0xFFF7F9FC) else Color(0xFF17202A)
    val variant = System.getProperty("basefile.roborazzi.variant", "baseline")
    MaterialTheme {
        Surface(color = background, contentColor = foreground) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(if (variant == "baseline") "Offline documents" else "Changed documents")
                Text(
                    if (longData) {
                        "Quarterly-accessibility-and-encrypted-migration-reference-document.pdf"
                    } else {
                        "guide.pdf"
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = "PDF document", Modifier.size(48.dp))
                    Icon(Icons.Filled.CalendarMonth, contentDescription = "Modified date", Modifier.size(48.dp))
                }
                Button(onClick = {}, modifier = Modifier.size(width = 180.dp, height = 56.dp)) {
                    Text("Open document")
                }
            }
        }
    }
}
