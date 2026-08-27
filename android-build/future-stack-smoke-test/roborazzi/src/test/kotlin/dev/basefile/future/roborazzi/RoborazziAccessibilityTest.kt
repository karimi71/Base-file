package dev.basefile.future.roborazzi

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.AccessibilityCheckAfterTestStrategy
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.checkRoboAccessibility
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesElements
import com.google.android.apps.common.testing.accessibility.framework.matcher.ElementMatchers.withTestTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalRoborazziApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = RobolectricDeviceQualifiers.Pixel4)
class RoborazziAccessibilityTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val roborazzi = RoborazziRule(
        composeRule = compose,
        captureRoot = compose.onRoot(),
        options = RoborazziRule.Options(
            roborazziAccessibilityOptions = RoborazziATFAccessibilityCheckOptions(
                checker = RoborazziATFAccessibilityChecker(
                    preset = AccessibilityCheckPreset.LATEST,
                    suppressions = matchesElements(withTestTag("documented-suppression")),
                ),
                failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Warning,
            ),
            accessibilityCheckStrategy = AccessibilityCheckAfterTestStrategy(),
        ),
    )

    @Test
    fun `API 35 native graphics checks semantics contrast and touch target`() {
        compose.setContent {
            Column(
                modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text("Accessible offline documents", color = Color.Black)
                Button(
                    onClick = {},
                    modifier = Modifier.size(width = 180.dp, height = 56.dp).testTag("open-document"),
                ) {
                    Text("Open document")
                }
                Text(
                    "Known decorative watermark",
                    color = Color(0xFFFDFDFD),
                    modifier = Modifier.testTag("documented-suppression"),
                )
            }
        }
        compose.onNodeWithTag("open-document").checkRoboAccessibility(
            roborazziATFAccessibilityCheckOptions = RoborazziATFAccessibilityCheckOptions(
                checker = RoborazziATFAccessibilityChecker(preset = AccessibilityCheckPreset.LATEST),
                failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Warning,
            ),
        )
    }
}
