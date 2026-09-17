package depollsoft.pitchperfect

import android.os.Build
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import depollsoft.testing.StoreScreenshots
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in checks on the disposable API 35 Pixel emulator used for store assets. */
@RunWith(AndroidJUnit4::class)
class StoreScreenshotOverlayTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private fun enabled() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("storeOverlayCheck") == "true")
        check(Build.PRODUCT.startsWith("sdk_gphone")) { "Use a disposable Pixel emulator" }
        PurchaseService.areAdsRemoved = true
    }
    private fun shell(command: String): String =
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText() }

    @Test fun closesLauncherErrorBeforeCapture() {
        enabled()
        shell("settings put global show_background_errors 1")
        shell("settings put global show_first_crash_dialog 1")
        try {
            ActivityScenario.launch(PitchPerfectActivity::class.java).use {
                shell("am crash com.google.android.apps.nexuslauncher")
                val deadline = SystemClock.uptimeMillis() + 5000
                var shown = false
                while (!shown && SystemClock.uptimeMillis() < deadline) {
                    val root = instrumentation.uiAutomation.rootInActiveWindow
                    shown = root?.packageName?.toString() == "android" &&
                        root.findAccessibilityNodeInfosByText("Pixel Launcher").isNotEmpty()
                    if (!shown) SystemClock.sleep(100)
                }
                assertTrue("Expected a real Pixel Launcher error dialog", shown)
                StoreScreenshots.capture(instrumentation, "launcher-error-check")
                assertEquals(instrumentation.targetContext.packageName,
                    instrumentation.uiAutomation.rootInActiveWindow.packageName.toString())
            }
        } finally {
            shell("settings delete global show_background_errors")
            shell("settings delete global show_first_crash_dialog")
        }
    }

    @Test fun rejectsScreenshotsOfAnotherApp() {
        enabled()
        ActivityScenario.launch(PitchPerfectActivity::class.java).use {
            shell("am start -W -a android.settings.SETTINGS")
            val deadline = SystemClock.uptimeMillis() + 5000
            while (instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString() ==
                instrumentation.targetContext.packageName && SystemClock.uptimeMillis() < deadline) {
                SystemClock.sleep(100)
            }
            assertEquals("com.android.settings",
                instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString())
            assertThrows(IllegalStateException::class.java) {
                StoreScreenshots.capture(instrumentation, "must-not-save-settings-screen")
            }
        }
    }
}
