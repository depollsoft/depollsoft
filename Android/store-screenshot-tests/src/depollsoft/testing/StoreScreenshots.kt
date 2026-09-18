package depollsoft.testing

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Instrumentation
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import java.io.File

/** Native store captures must show the app without a covering system dialog. */
object StoreScreenshots {
    fun capture(instrumentation: Instrumentation, name: String) {
        val automation = instrumentation.uiAutomation
        val service = automation.serviceInfo
        service.flags = service.flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        automation.serviceInfo = service
        val packageName = instrumentation.targetContext.packageName
        val deadline = SystemClock.uptimeMillis() + 5000
        var foreground = false
        while (SystemClock.uptimeMillis() < deadline) {
            val root = automation.rootInActiveWindow
            if (root?.packageName?.toString() == packageName) {
                foreground = true
                break
            }
            val close = root?.findAccessibilityNodeInfosByViewId("android:id/aerr_close")?.singleOrNull()
            if (root != null && close != null) {
                // The API 35 capture image uses Pixel Launcher. Its background
                // crash/ANR can cover a healthy app; an error from our app must fail.
                check(root.packageName?.toString() == "android" &&
                    root.findAccessibilityNodeInfosByText("Pixel Launcher").isNotEmpty()) {
                    "An application error is covering store capture $name"
                }
                if (close.isEnabled) {
                    check(close.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        "Could not close the emulator launcher error"
                    }
                    // Input focus returns before the system dialog's exit animation
                    // finishes. Wait for the accessibility events to settle too.
                    automation.waitForIdle(500, 5000)
                }
            }
            SystemClock.sleep(100)
        }
        check(foreground) { "A system window is covering store capture $name" }
        val image = requireNotNull(automation.takeScreenshot())
        try {
            check(automation.rootInActiveWindow?.packageName?.toString() == packageName) {
                "A system window appeared during store capture $name"
            }
            val directory = File(instrumentation.targetContext.filesDir, "store-screenshots")
            check(directory.isDirectory || directory.mkdirs())
            File(directory, "$name.png").outputStream().use {
                check(image.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        } finally {
            image.recycle()
        }
    }
}
