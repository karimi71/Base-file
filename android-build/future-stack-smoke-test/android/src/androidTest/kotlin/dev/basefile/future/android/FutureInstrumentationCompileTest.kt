package dev.basefile.future.android

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult
import com.google.crypto.tink.aead.AeadConfig
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compiled offline; execution requires an API-23+ emulator or physical device. */
@RunWith(AndroidJUnit4::class)
class FutureInstrumentationCompileTest {
    @get:Rule
    val activity = ActivityScenarioRule(FutureStackActivity::class.java)

    @Test
    fun espressoAccessibilityAndAndroidOnlyApisAreWired() {
        onView(withText("Offline future stack")).check(matches(isDisplayed()))
        val context = ApplicationProvider.getApplicationContext<Context>()
        PDFBoxResourceLoader.init(context)
        AeadConfig.register()
        assertNotNull(mockk<Context>(relaxed = true))
        assertNotNull(MigrationTestHelper::class.java)
        assertNotNull(AccessibilityCheckResult::class.java)
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun enableAccessibilityChecks() {
            AccessibilityChecks.enable().setRunChecksFromRootView(true)
        }
    }
}
