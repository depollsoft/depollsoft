package depollsoft.tagmaster

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewpager2.widget.ViewPager2
import bolts.TaskCompletionSource
import depollsoft.tagmaster.barbershop.Tag
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** Actual controllers; held requests never read or write the catalog or saved lists. */
@androidx.test.filters.SdkSuppress(minSdkVersion = 29)
@RunWith(AndroidJUnit4::class)
class LayoutRegressionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app get() = instrumentation.targetContext.applicationContext as Application
    private val args get() = InstrumentationRegistry.getArguments()
    private val requests = LinkedBlockingQueue<Pair<TagDetailActivity, TaskCompletionSource<Tag>>>()
    private val tagId = 2147483020
    private var inspectedDialog: androidx.appcompat.app.AlertDialog? = null
    private val callbacks =
        object : Application.ActivityLifecycleCallbacks {
            override fun onActivityPreCreated(
                activity: Activity,
                state: Bundle?,
            ) {
                if (args.getString("captureOrientation") == "landscape") {
                    activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
                if (activity is androidx.fragment.app.FragmentActivity) {
                    activity.supportFragmentManager.registerFragmentLifecycleCallbacks(
                        object : androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks() {
                            override fun onFragmentPreCreated(
                                fm: androidx.fragment.app.FragmentManager,
                                f: androidx.fragment.app.Fragment,
                                state: Bundle?,
                            ) {
                                if (f is TagQueryFragment) {
                                    // Keep the real selected-mode model; prevent catalog traffic before refresh().
                                    if (f.model == null) f.model = QueryModel()
                                    f.model!!.hasMoreResults = false
                                }
                            }
                        },
                        true,
                    )
                }
                if (activity is TagDetailActivity) {
                    activity.tagLoader = { _, _ ->
                        TaskCompletionSource<Tag>().also { requests.add(activity to it) }.task
                    }
                }
            }

            override fun onActivityCreated(
                a: Activity,
                b: Bundle?,
            ) {}

            override fun onActivityStarted(a: Activity) {}

            override fun onActivityResumed(a: Activity) {}

            override fun onActivityPaused(a: Activity) {}

            override fun onActivityStopped(a: Activity) {}

            override fun onActivitySaveInstanceState(
                a: Activity,
                b: Bundle,
            ) {}

            override fun onActivityDestroyed(a: Activity) {}
        }

    @Before fun register() {
        val automation = instrumentation.uiAutomation
        if (args.getString("captureOrientation") == "landscape") {
            automation.setRotation(android.app.UiAutomation.ROTATION_FREEZE_90)
        }
        instrumentation.waitForIdleSync()
        app.registerActivityLifecycleCallbacks(callbacks)
    }

    @After fun unregister() {
        instrumentation.runOnMainSync {
            inspectedDialog?.dismiss()
            inspectedDialog = null
        }
        // Scoped external storage may deny adb shell reads. Export only this test's JPEGs
        // to the app cache, where run-as can retrieve them without storage permission changes.
        val export = java.io.File(app.cacheDir, "layout-captures").apply { mkdirs() }
        app
            .getExternalFilesDir(null)
            ?.listFiles()
            ?.filter {
                it.name.startsWith("tagmaster-layout-after-android-") && it.extension == "jpg"
            }?.forEach {
                it.copyTo(java.io.File(export, it.name), overwrite = true)
            }
        app.unregisterActivityLifecycleCallbacks(callbacks)
        if (args.getString("captureOrientation") != null) {
            instrumentation.uiAutomation.setRotation(android.app.UiAutomation.ROTATION_UNFREEZE)
        }
    }

    private fun launch() =
        ActivityScenario.launch<TagDetailActivity>(
            Intent(app, TagDetailActivity::class.java).putExtra(TagDetailActivity.TAG_ID_EXTRA, tagId),
        )

    private fun request(scenario: ActivityScenario<TagDetailActivity>): TaskCompletionSource<Tag> {
        lateinit var current: TagDetailActivity
        scenario.onActivity { current = it }
        while (true) {
            val (owner, task) = requests.poll(5, TimeUnit.SECONDS) ?: error("Expected held request")
            if (owner === current) return task
            task.setCancelled()
        }
    }

    private fun tag() =
        Tag().apply {
            id = tagId
            title = "When the quartet gathers for one more song"
            parts = 4
            writtenKey = "C"
        }

    private fun settle() {
        instrumentation.waitForIdleSync()
        val frame = java.util.concurrent.CountDownLatch(1)
        instrumentation.runOnMainSync {
            android.view.Choreographer
                .getInstance()
                .postFrameCallback { frame.countDown() }
        }
        assertTrue("Native frame dispatched", frame.await(8, TimeUnit.SECONDS))
        instrumentation.waitForIdleSync()
    }

    private fun fullBounds(view: View): Rect =
        Rect(0, 0, view.width, view.height).also {
            val xy = IntArray(2)
            view.getLocationOnScreen(xy)
            it.offset(xy[0], xy[1])
        }

    private fun fullyVisible(view: View): Boolean {
        val visible = Rect()
        // Local drawing coordinates include the view's own scroll offset. Native
        // centered dialog TextViews can have a large horizontal text scroll value.
        val viewport = Rect(view.scrollX, view.scrollY, view.scrollX + view.width, view.scrollY + view.height)
        return view.getLocalVisibleRect(visible) && visible == viewport && view.width > 0 && view.height > 0
    }

    // Independently intersect screen-space ancestor clips before native hit testing.
    private fun reachableBounds(view: View): Rect {
        val bounds = fullBounds(view)
        val frame = Rect().also { view.getWindowVisibleDisplayFrame(it) }
        assertTrue("Shown and inside window: $bounds / $frame", view.isShown && bounds.intersect(frame))
        var parent = view.parent
        while (parent is ViewGroup) {
            if (parent.clipChildren) {
                val clip = fullBounds(parent)
                if (parent.clipToPadding) {
                    clip.left += parent.paddingLeft
                    clip.top += parent.paddingTop
                    clip.right -= parent.paddingRight
                    clip.bottom -= parent.paddingBottom
                }
                assertTrue("Inside ${parent.javaClass.simpleName}: $bounds / $clip", bounds.intersect(clip))
            }
            parent = parent.parent
        }
        return bounds
    }

    private fun assertReachableButton(button: TextView): Rect {
        textFits(button)
        val bounds = reachableBounds(button)
        val minimum = (48 * button.resources.displayMetrics.density).toInt()
        assertTrue("48dp reachable target: $bounds", bounds.width() >= minimum && bounds.height() >= minimum)
        val label =
            fullBounds(button).apply {
                left += button.compoundPaddingLeft
                right -= button.compoundPaddingRight
                top += button.totalPaddingTop
                bottom -= button.totalPaddingBottom
            }
        assertTrue("Entire label reachable: $label / $bounds", bounds.contains(label))
        return bounds
    }

    private fun nativeTap(bounds: Rect) {
        val now = android.os.SystemClock.uptimeMillis()
        for (action in listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP)) {
            val event =
                android.view.MotionEvent.obtain(
                    now,
                    android.os.SystemClock.uptimeMillis(),
                    action,
                    bounds.exactCenterX(),
                    bounds.exactCenterY(),
                    0,
                )
            event.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
            assertTrue("Native pointer delivered", instrumentation.uiAutomation.injectInputEvent(event, true))
            event.recycle()
        }
        settle()
    }


    private fun waitFor(check: () -> Boolean) {
        val deadline = android.os.SystemClock.uptimeMillis() + 8000
        while (!check() && android.os.SystemClock.uptimeMillis() < deadline) Thread.sleep(40)
        assertTrue("Fixture condition reached", check())
    }

    private fun tracks(scenario: ActivityScenario<TagDetailActivity>): TagTracksFragment {
        scenario.onActivity { it.findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(2, false) }
        var result: TagTracksFragment? = null
        waitFor {
            scenario.onActivity {
                result =
                    it.detailFragment!!.childFragmentManager.fragments
                        .filterIsInstance<TagTracksFragment>()
                        .firstOrNull { f -> f.view != null }
            }
            result != null
        }
        settle()
        return result!!
    }

    private fun populated() =
        tag().apply {
            recordingMethod = "Recorded separately so each singer can follow the phrase. ".repeat(8)

            fun location(part: String) =
                depollsoft.tagmaster.barbershop.RemoteLocation().apply {
                    uri = "https://layout.example.invalid/$part.wav"
                    type = "wav"
                }
            allPartsTrackUri = location("all")
            tenorTrackUri = location("tenor")
            leadTrackUri = location("lead")
            baritoneTrackUri = location("baritone")
            bassTrackUri = location("bass")
            other1TrackUri = location("other1")
            other2TrackUri = location("other2")
            other3TrackUri = location("other3")
            other4TrackUri = location("other4")
        }

    private fun transport(
        root: View,
        empty: Boolean,
    ) {
        assertEquals(if (empty) View.VISIBLE else View.GONE, root.findViewById<View>(R.id.sorryTextView).visibility)
        for (id in listOf(R.id.mediaPlayer, R.id.partsRadioGroup)) {
            assertEquals(if (empty) View.GONE else View.VISIBLE, root.findViewById<View>(id).visibility)
        }
        for (id in listOf(R.id.playPauseButton, R.id.stopButton, R.id.counterSeekBar, R.id.balanceSeekBar)) {
            val control = root.findViewById<View>(id)
            assertNotNull(control)
            if (empty) {
                assertFalse("No hidden player focus stops", control.isShown)
            } else {
                assertEquals(View.VISIBLE, control.visibility)
            }
        }
    }



    private fun textFits(text: TextView) {
        assertTrue("Readable width for ${text.text}", text.width > text.compoundPaddingLeft + text.compoundPaddingRight)
        assertTrue(
            "All lines laid out: ${text.text}; required=${text.layout.height}, available=${text.height - text.compoundPaddingTop - text.compoundPaddingBottom}",
            text.layout.height <= text.height - text.compoundPaddingTop - text.compoundPaddingBottom,
        )
        assertTrue("No ellipsis: ${text.text}", (0 until text.lineCount).all { text.layout.getEllipsisCount(it) == 0 })
    }

    private fun equalTabs(activity: Activity) {
        val tabs = activity.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)
        val strip = tabs.getChildAt(0) as ViewGroup
        assertEquals(4, strip.childCount)
        val widths = (0..3).map { strip.getChildAt(it).width }
        assertTrue(widths.max() - widths.min() <= 1)
        assertEquals(tabs.width - tabs.paddingLeft - tabs.paddingRight, widths.sum())
        assertTrue("Tabs above system bars", fullyVisible(tabs))
    }


    @Test fun home_and_teachable_many_long_titles_reorder_geometry() {
        // Reuse the guarded fixture and real pointer path, not an unowned adapter or phantom overflow.
        // The row geometry this used to check alongside the drag now runs on the JVM, in
        // SavedListEditingScreenTest.rowsKeepTheirControlsAndWholeTitlesInsideTheirBounds; only the
        // native pointer drag still needs a device.
        SavedListEditingTest().run {
            setUp()
            try {
                pointer_drag_first_to_last_and_back_commits_once_per_drop()
            } finally {
                tearDown()
            }
        }
    }


    @Test fun owned_dialogs_validation_and_lower_settings() {
        ActivityScenario.launch(TagSearchActivity::class.java).use { scenario ->
            lateinit var popup: RatingsPopup
            scenario.onActivity {
                popup = RatingsPopup(it)
                popup.show()
            }
            settle()
            capture("rating-dialog")
            androidx.test.espresso.Espresso
                .onView(
                    androidx.test.espresso.matcher.ViewMatchers
                        .withId(android.R.id.button2),
                ).perform(
                    androidx.test.espresso.action.ViewActions
                        .click(),
                )
            assertNull("Cancel never submits", popup.rating)
        }
        ActivityScenario.launch(MeActivity::class.java).use { scenario ->
            try {
                androidx.test.espresso.Espresso
                    .onView(
                        androidx.test.espresso.matcher.ViewMatchers
                            .withId(android.R.id.button1),
                    ).perform(
                        androidx.test.espresso.action.ViewActions
                            .click(),
                    )
            } catch (
                _: androidx.test.espresso.NoMatchingViewException,
            ) {
            }
            settle()
            lateinit var dialog: androidx.appcompat.app.AlertDialog
            scenario.onActivity { a ->
                val button = a.findViewById<View>(R.id.openByIdButton)
                button.performClick()
                var ancestor: View = button
                while (ancestor !is MeHeaderView) ancestor = ancestor.parent as View
                dialog =
                    MeHeaderView::class.java
                        .getDeclaredField(
                            "openTagDialog",
                        ).apply { isAccessible = true }
                        .get(ancestor) as androidx.appcompat.app.AlertDialog
            }
            inspectedDialog = dialog
            val automation = instrumentation.uiAutomation
            val service = automation.serviceInfo
            val originalFlags = service.flags
            service.flags = originalFlags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            automation.serviceInfo = service
            try {
                waitFor {
                    automation.windows.any { it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD }
                }
                settle()
                instrumentation.runOnMainSync {
                    val input = dialog.findViewById<android.widget.EditText>(R.id.openTagIdInput)!!
                    assertEquals("Open Tag starts empty", "", input.text.toString())
                    input.setText("2147483648")
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).performClick()
                }
                settle()
                var fullscreenEditor = false
                instrumentation.runOnMainSync {
                    fullscreenEditor = app.getSystemService(android.view.inputmethod.InputMethodManager::class.java).isFullscreenMode
                }
                if (fullscreenEditor) {
                    // Android's landscape extract editor intentionally covers the app. Back
                    // returns to the owned dialog; it must not discard the validation or close Home.
                    instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
                    waitFor {
                        automation.windows.none { it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD }
                    }
                    settle()
                    println("Native Back left fullscreen IME; validating the returned dialog")
                }
                val imeBounds = Rect()
                automation.windows
                    .firstOrNull {
                        it.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD
                    }?.getBoundsInScreen(imeBounds)
                assertTrue("IME is present unless native Back closed its fullscreen editor", fullscreenEditor || !imeBounds.isEmpty)
                lateinit var cancelBounds: Rect
                capture("open-tag-validation")
                instrumentation.runOnMainSync {
                    val field = dialog.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.openTagIdLayout)!!
                    assertEquals(app.getString(R.string.home_invalid_tag_id), field.error.toString())
                    val errorLabel = field.findViewById<TextView>(com.google.android.material.R.id.textinput_error)
                    textFits(errorLabel)
                    assertTrue("Validation explanation remains reachable", reachableBounds(errorLabel).contains(fullBounds(errorLabel)))
                    assertTrue("Invalid ID keeps the dialog open", dialog.isShowing)
                    val cancel = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                    cancelBounds = assertReachableButton(cancel)
                    val openBounds = assertReachableButton(dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE))
                    val local = Rect().also { cancel.getLocalVisibleRect(it) }
                    val frame = Rect().also { cancel.getWindowVisibleDisplayFrame(it) }
                    val geometry = "Cancel bounds=$cancelBounds local=$local scroll=${cancel.scrollX},${cancel.scrollY} IME=$imeBounds frame=$frame density=${cancel.resources.displayMetrics.density}"
                    assertTrue("Full Cancel viewport reachable: $geometry", fullyVisible(cancel))
                    println(geometry)
                    assertFalse("Cancel above actual IME window: $geometry", Rect.intersects(cancelBounds, imeBounds))
                    assertFalse("Open above actual IME window: $geometry", Rect.intersects(openBounds, imeBounds))
                }
                nativeTap(cancelBounds)
                instrumentation.runOnMainSync { assertFalse("Native Cancel dismisses validation dialog", dialog.isShowing) }
                assertTrue("Cancel does not request a tag", requests.isEmpty())
                scenario.onActivity { assertFalse("Cancel leaves Home active", it.isFinishing) }
            } finally {
                service.flags = originalFlags
                automation.serviceInfo = service
            }
        }
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            waitFor {
                var ready = false
                scenario.onActivity {
                    ready =
                        it.findViewById<TextView>(R.id.cacheSizeDisplay).text.toString() != it.getString(R.string.forms_cache_size_pending)
                }
                ready
            }
            for (id in listOf(R.id.clearCacheButton, R.id.radio_system, R.id.radio_light, R.id.radio_dark, R.id.changelogButton)) {
                scenario.onActivity {
                    val control = it.findViewById<View>(id)
                    control.requestRectangleOnScreen(Rect(0, 0, control.width, control.height), true)
                }
                settle()
                scenario.onActivity {
                    assertReachableButton(it.findViewById(id))
                    assertFalse("Geometry never clears cache", it.clearingCache)
                }
            }
            scenario.onActivity {
                assertTrue("Changelog reachable", fullyVisible(it.findViewById(R.id.changelogButton)))
                assertEquals(View.GONE, it.findViewById<View>(R.id.privateBuildDiagnostics).visibility)
            }
            capture("settings-lower")
            scenario.onActivity { it.findViewById<View>(R.id.changelogButton).performClick() }
            settle()
            capture("changelog")
            // The activity can still be Espresso's default root during the window-focus
            // handoff. Select the actual dialog, rather than searching the Settings tree.
            androidx.test.espresso.Espresso
                .onView(
                    androidx.test.espresso.matcher.ViewMatchers
                        .withId(android.R.id.button1),
                ).inRoot(
                    androidx.test.espresso.matcher.RootMatchers
                        .isDialog(),
                ).check { view, error ->
                    if (error != null) throw error
                    assertReachableButton(view as TextView)
                }.perform(
                    androidx.test.espresso.action.ViewActions
                        .click(),
                )
            scenario.onActivity { assertFalse("Changelog returns to Settings", it.isFinishing) }
        }
    }

    private fun capture(name: String) {
        val label = args.getString("captureLabel") ?: return
        if (args.getString("defectOnly") == "true" && name != "open-tag-validation") return
        val bitmap =
            android.os.ParcelFileDescriptor
                .AutoCloseInputStream(
                    instrumentation.uiAutomation.executeShellCommand("screencap -p"),
                ).use { android.graphics.BitmapFactory.decodeStream(it) }
        val scale = 800f / maxOf(bitmap.width, bitmap.height)
        val small =
            android.graphics.Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true,
            )
        java.io.File(app.getExternalFilesDir(null), "tagmaster-layout-after-android-$label-$name.jpg").outputStream().use {
            small.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it)
        }
        small.recycle()
        bitmap.recycle()
    }
}
