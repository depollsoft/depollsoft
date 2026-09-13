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

    @Test fun initial_error_short_viewport_has_scroll_escape_and_retry() {
        launch().use { scenario ->
            val first = request(scenario)
            instrumentation.runOnMainSync { first.setError(IllegalStateException("offline fixture")) }
            settle()
            scenario.onActivity { a ->
                val error = a.findViewById<View>(R.id.detailErrorState)
                val text = a.findViewById<TextView>(R.id.detailErrorText)
                val retry = a.findViewById<View>(R.id.detailRetryButton)
                assertTrue(error.isShown)
                assertTrue(
                    "All error text is laid out",
                    text.layout.height <= text.height - text.compoundPaddingTop - text.compoundPaddingBottom,
                )
                if (error is ScrollView) error.fullScroll(View.FOCUS_DOWN)
                android.util.Log.i(
                    "LayoutRegression",
                    "error=${fullBounds(
                        error,
                    )} text=${fullBounds(
                        text,
                    )} retry=${fullBounds(retry)} visible=${fullyVisible(retry)} font=${a.resources.configuration.fontScale}",
                )
            }
            settle()
            capture("initial-error-bottom")
            scenario.onActivity { a ->
                val retry = a.findViewById<View>(R.id.detailRetryButton)
                assertTrue("Entire Retry must be reachable in short large-text viewport: ${fullBounds(retry)}", fullyVisible(retry))
                val error = a.findViewById<View>(R.id.detailErrorState)
                if (error is ScrollView) error.fullScroll(View.FOCUS_UP)
            }
            settle()
            scenario.onActivity { a ->
                val text = a.findViewById<TextView>(R.id.detailErrorText)
                val visible = Rect()
                assertTrue(text.getGlobalVisibleRect(visible))
                assertEquals("Start of error text reachable", fullBounds(text).top, visible.top)
                a.findViewById<View>(R.id.detailRetryButton).performClick()
                assertTrue(a.findViewById<View>(R.id.detailLoadingState).isShown)
            }
            val retry = request(scenario)
            instrumentation.runOnMainSync { retry.setResult(tag()) }
            settle()
            scenario.onActivity { a ->
                assertTrue(a.findViewById<ViewPager2>(R.id.viewPager).isShown)
                assertFalse(a.findViewById<View>(R.id.detailErrorState).isShown)
                a.onBackPressedDispatcher.onBackPressed()
            }
            settle()
            assertNotEquals("Native Back leaves the foreground", androidx.lifecycle.Lifecycle.State.RESUMED, scenario.state)
        }
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
                    it.supportFragmentManager.fragments
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

    @Test fun empty_tracks_notes_selection_and_same_id_replacement() {
        launch().use { scenario ->
            val first = request(scenario)
            instrumentation.runOnMainSync { first.setResult(tag()) }
            val fragment = tracks(scenario)
            val root = fragment.requireView()
            scenario.onActivity {
                transport(root, true)
                assertEquals(View.GONE, root.findViewById<View>(R.id.trackNotesLayout).visibility)
                val focusables = ArrayList<View>()
                root.addFocusables(focusables, View.FOCUS_FORWARD)
                assertFalse(focusables.any { v -> v.id == R.id.balanceSeekBar || v.id == R.id.playPauseButton })
                it.tag = tag().apply { recordingMethod = "No separate parts were supplied. ".repeat(10) }
            }
            settle()
            scenario.onActivity {
                transport(root, true)
                val explanation = root.findViewById<TextView>(R.id.sorryTextView)
                val notes = root.findViewById<TextView>(R.id.trackNotesTextView)
                assertTrue(fullBounds(explanation).bottom <= fullBounds(notes).top)
                assertTrue(notes.layout.height <= notes.height - notes.compoundPaddingTop - notes.compoundPaddingBottom)
            }
            capture("empty-tracks-notes")
            scenario.onActivity {
                it.tag =
                    populated().apply {
                        allPartsTrackUri = null
                        leadTrackUri = null
                        baritoneTrackUri = null
                        bassTrackUri = null
                        other1TrackUri = null
                        other2TrackUri = null
                        other3TrackUri = null
                        other4TrackUri = null
                    }
            }
            settle()
            scenario.onActivity {
                transport(root, false)
                assertNull(fragment.selectedTrack)
                assertEquals(View.VISIBLE, root.findViewById<View>(R.id.tenorButton).visibility)
                assertEquals(View.GONE, root.findViewById<View>(R.id.allPartsButton).visibility)
                it.tag = populated()
            }
            settle()
            scenario.onActivity {
                transport(root, false)
                for (id in listOf(
                    R.id.allPartsButton,
                    R.id.tenorButton,
                    R.id.leadButton,
                    R.id.bariButton,
                    R.id.bassButton,
                    R.id.other1Button,
                    R.id.other2Button,
                    R.id.other3Button,
                    R.id.other4Button,
                )) {
                    assertEquals(View.VISIBLE, root.findViewById<View>(id).visibility)
                }
                val last = root.findViewById<View>(R.id.other4Button)
                last.requestRectangleOnScreen(Rect(0, 0, last.width, last.height), true)
            }
            settle()
            scenario.onActivity {
                val last = root.findViewById<View>(R.id.other4Button)
                assertTrue("Last extra part reachable", fullyVisible(last))
                last.performClick()
                assertEquals(it.tag!!.other4TrackUri, fragment.selectedTrack)
            }
            capture("tracks-last-extra")
            // The two landscape columns have different heights. Scroll each target, not
            // the entire page bottom, which can scroll the final part above the viewport.
            for (id in listOf(
                R.id.allPartsButton,
                R.id.tenorButton,
                R.id.leadButton,
                R.id.bariButton,
                R.id.bassButton,
                R.id.other1Button,
                R.id.other2Button,
                R.id.other3Button,
                R.id.other4Button,
                R.id.playPauseButton,
                R.id.stopButton,
                R.id.counterSeekBar,
                R.id.balanceSeekBar,
            )) {
                scenario.onActivity {
                    val control = root.findViewById<View>(id)
                    control.requestRectangleOnScreen(Rect(0, 0, control.width, control.height), true)
                }
                settle()
                scenario.onActivity {
                    val control = root.findViewById<View>(id)
                    assertTrue("Every part and full player control reachable: $id ${fullBounds(control)}", fullyVisible(control))
                    if (control is android.widget.RadioButton) assertReachableButton(control)
                }
            }
            scenario.onActivity { root.findViewById<ScrollView>(R.id.scrollView1).fullScroll(View.FOCUS_UP) }
            settle()
            capture("tracks-long-notes")
            scenario.onActivity { it.tag = null }
            settle()
            scenario.onActivity { transport(root, false) }
            scenario.onActivity { it.tag = tag() }
            settle()
            scenario.onActivity {
                transport(root, true)
                assertNull(fragment.selectedTrack)
            }
        }
    }

    @Test fun media_pending_failure_retry_playing_pause_and_empty_cancellation() {
        val downloads = LinkedBlockingQueue<TaskCompletionSource<java.io.File>>()
        val silent = java.io.File(app.cacheDir, "layout-silent.wav")
        val original = silent.takeIf { it.exists() }?.readBytes()
        val pcmBytes = 8000 * 2 * 10
        val header =
            java.nio.ByteBuffer
                .allocate(44 + pcmBytes)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN)
        header
            .put("RIFF".toByteArray())
            .putInt(36 + pcmBytes)
            .put("WAVEfmt ".toByteArray())
            .putInt(16)
        header
            .putShort(1)
            .putShort(1)
            .putInt(8000)
            .putInt(16000)
            .putShort(2)
            .putShort(16)
        header.put("data".toByteArray()).putInt(pcmBytes)
        silent.writeBytes(header.array())
        try {
            launch().use { scenario ->
                val first = request(scenario)
                instrumentation.runOnMainSync { first.setResult(populated().apply { recordingMethod = null }) }
                val fragment = tracks(scenario)
                val root = fragment.requireView()
                val player = root.findViewById<MediaPlayerView>(R.id.mediaPlayer)
                val heldCache =
                    object : depollsoft.lib.util.ContentCache(app) {
                        override fun loadContentPublic(
                            url: String,
                            extension: String,
                            forceRefresh: Boolean,
                        ): bolts.Task<java.io.File> = TaskCompletionSource<java.io.File>().also { downloads.add(it) }.task
                    }
                instrumentation.runOnMainSync {
                    MediaPlayerView::class.java
                        .getDeclaredField("cache")
                        .apply { isAccessible = true }
                        .set(player, heldCache)
                    root.findViewById<View>(R.id.tenorButton).performClick()
                    player.findViewById<View>(R.id.playPauseButton).performClick()
                }
                val failed = downloads.poll(5, TimeUnit.SECONDS) ?: error("Expected media request")
                scenario.onActivity {
                    transport(root, false)
                    assertTrue(player.isLoading)
                }
                capture("tracks-pending")
                failed.setError(java.io.IOException("Fixture offline"))
                waitFor { !player.isLoading }
                settle()
                scenario.onActivity {
                    transport(root, false)
                    it.findViewById<View>(com.google.android.material.R.id.snackbar_action).performClick()
                }
                val stale = downloads.poll(5, TimeUnit.SECONDS) ?: error("Expected media retry")
                scenario.onActivity {
                    assertTrue(player.isLoading)
                    it.tag = tag()
                }
                settle()
                scenario.onActivity {
                    transport(root, true)
                    assertNull(fragment.selectedTrack)
                    assertNull(player.remoteLocation)
                    assertFalse(player.isLoading)
                    assertFalse(player.isPlaying)
                    assertEquals(-1, root.findViewById<android.widget.RadioGroup>(R.id.partsRadioGroup).checkedRadioButtonId)
                }
                stale.setResult(silent)
                settle()
                scenario.onActivity {
                    assertFalse(player.isPlaying)
                    it.tag = populated().apply { recordingMethod = null }
                }
                settle()
                scenario.onActivity {
                    root.findViewById<View>(R.id.allPartsButton).performClick()
                    player.findViewById<View>(R.id.playPauseButton).performClick()
                }
                downloads.poll(5, TimeUnit.SECONDS)!!.setResult(silent)
                waitFor { player.isPlaying }
                scenario.onActivity {
                    transport(root, false)
                    player.findViewById<View>(R.id.playPauseButton).performClick()
                    assertFalse(player.isPlaying)
                    transport(root, false)
                    player.findViewById<View>(R.id.playPauseButton).performClick()
                    assertTrue(player.isPlaying)
                    it.tag = tag()
                }
                settle()
                scenario.onActivity {
                    transport(root, true)
                    assertFalse(player.isPlaying)
                    assertNull(player.remoteLocation)
                }
                scenario.onActivity { it.tag = populated().apply { recordingMethod = null } }
                settle()
                scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
                scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
                settle()
                scenario.onActivity {
                    transport(root, false)
                    assertFalse(player.isPlaying)
                }
            }
        } finally {
            if (original == null) silent.delete() else silent.writeBytes(original)
        }
    }

    private fun textFits(text: TextView) {
        assertTrue("Readable width for ${text.text}", text.width > text.compoundPaddingLeft + text.compoundPaddingRight)
        assertTrue(
            "All lines laid out: ${text.text}",
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

    @Test fun browse_all_modes_and_search_empty_error_pagination_layouts() {
        ActivityScenario.launch(TagBrowserActivity::class.java).use { scenario ->
            val sorts = listOf("Posted", "Rating", "Downloaded", "Classic")
            for (position in 0..3) {
                scenario.onActivity { it.findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(position, false) }
                settle()
                scenario.onActivity { a ->
                    val fragment = a.supportFragmentManager.findFragmentByTag("f$position") as TagQueryFragment
                    assertEquals(sorts[position], fragment.model!!.sortBy.toString())
                    fragment.model!!.tags.addAll(
                        (0..11).map { index ->
                            tag().apply {
                                id += index
                                title = "A long arrangement title for the singers gathered around the room $index"
                                alternativeTitle =
                                    "An alternate title that also wraps"
                            }
                        },
                    )
                }
                settle()
                scenario.onActivity { a ->
                    equalTabs(a)
                    val fragment = a.supportFragmentManager.findFragmentByTag("f$position") as TagQueryFragment
                    val list = fragment.requireView().findViewById<android.widget.ListView>(R.id.queryResultListView)
                    textFits(list.getChildAt(0).findViewById(R.id.titleTextView))
                    assertTrue(list.height > 0)
                }
                capture("browse-${sorts[position].lowercase()}")
            }
        }
        ActivityScenario.launch(TagSearchResultsActivity::class.java).use { scenario ->
            for ((name, message) in listOf(
                "empty" to "No tags could be found that matched your query.",
                "error" to "An error has occurred: The connection was interrupted while loading the next group of tags. Please try again.",
            )) {
                scenario.onActivity { a ->
                    val fragment = a.supportFragmentManager.findFragmentById(R.id.tagQueryFragment) as TagQueryFragment
                    fragment.model!!.statusText = message
                }
                settle()
                scenario.onActivity { a ->
                    val text = a.findViewById<TextView>(R.id.statusTextView)
                    textFits(text)
                    assertTrue("Query status reachable", fullyVisible(text))
                }
                capture("search-$name")
            }
            scenario.onActivity { a ->
                val model = (a.supportFragmentManager.findFragmentById(R.id.tagQueryFragment) as TagQueryFragment).model!!
                model.statusText = null
                model.tags.addAll(
                    (0..15).map {
                        tag().apply {
                            id += it
                            title = "Long search result with a second phrase $it"
                        }
                    },
                )
                model.isLoading = true
            }
            settle()
            scenario.onActivity { a ->
                assertTrue(a.findViewById<View>(R.id.loadingProgressBar).isShown)
                assertTrue(a.findViewById<android.widget.ListView>(R.id.queryResultListView).height > 0)
            }
            capture("search-pagination")
            scenario.onActivity { a ->
                val model = (a.supportFragmentManager.findFragmentById(R.id.tagQueryFragment) as TagQueryFragment).model!!
                model.tags.add(
                    tag().apply {
                        id += 20
                        title = "Last result after the next page"
                    },
                )
                model.isLoading = false
            }
            waitFor {
                var ready = false
                scenario.onActivity { a ->
                    val list = a.findViewById<android.widget.ListView>(R.id.queryResultListView)
                    ready = list.adapter?.count == 17 && !list.isLayoutRequested && !a.findViewById<View>(R.id.loadingProgressBar).isShown
                }
                ready
            }
            scenario.onActivity { a ->
                a.findViewById<android.widget.ListView>(R.id.queryResultListView).setSelection(16)
            }
            settle()
            scenario.onActivity { a ->
                val list = a.findViewById<android.widget.ListView>(R.id.queryResultListView)
                assertEquals(16, list.lastVisiblePosition)
                val last = list.getChildAt(16 - list.firstVisiblePosition)
                val title = last.findViewById<TextView>(R.id.titleTextView)
                assertEquals("Last result after the next page", title.text.toString())
                textFits(title)
                assertTrue("Final query row fully reachable", fullyVisible(last))
            }
        }
    }

    @Test fun home_and_teachable_many_long_titles_reorder_geometry() {
        val tags =
            (0..14).map { index ->
                tag().apply {
                    id += 100 + index
                    title =
                        "A long title for an arrangement we want to sing together $index"
                }
            }
        val files = tags.map { java.io.File(app.filesDir, "TagCache/${it.id}") }
        val originals = files.map { it.takeIf { file -> file.exists() }?.readBytes() }
        files.zip(tags).forEach { (file, tag) ->
            file.parentFile!!.mkdirs()
            file.writeText(
                depollsoft.lib.json.JsonSerializer
                    .serialize(tag)
                    .toString(),
            )
        }
        try {
            for (home in listOf(true, false)) {
                val target = if (home) MeActivity::class.java else TeachableTagsActivity::class.java
                ActivityScenario.launch(target).use { scenario ->
                    if (home) {
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
                    }
                    val ids = com.bindroid.trackable.TrackableCollection<Int>()
                    ids.addAll(tags.map { it.id })
                    lateinit var adapter: SavedTagListAdapter
                    lateinit var list: androidx.recyclerview.widget.RecyclerView
                    scenario.onActivity { a ->
                        list = a.findViewById(if (home) R.id.homeList else R.id.teachableTagsItemsControl)
                        adapter =
                            SavedTagListAdapter(
                                { ids },
                            ) { context -> if (home) FavoriteTagItemView(context) else TeachableTagItemView(context) }
                        if (home) {
                            val current = list.adapter as androidx.recyclerview.widget.ConcatAdapter
                            current.removeAdapter((a as MeActivity).favoritesAdapter)
                            current.addAdapter(1, adapter)
                            a.findViewById<View>(R.id.favoritesEmptyText).visibility = View.GONE
                        } else {
                            list.adapter = adapter
                            a.findViewById<View>(R.id.teachableEmptyState).visibility = View.GONE
                        }
                        list.scrollToPosition(if (home) 1 else 0)
                    }
                    waitFor {
                        var ready = false
                        scenario.onActivity {
                            ready =
                                (0 until list.childCount)
                                    .map {
                                        list.getChildViewHolder(
                                            list.getChildAt(it),
                                        )
                                    }.filterIsInstance<SavedTagListAdapter.Holder>()
                                    .any {
                                        it.row.tag !=
                                            null
                                    }
                        }
                        ready
                    }
                    settle()
                    scenario.onActivity {
                        val rows =
                            (0 until list.childCount)
                                .map {
                                    list.getChildViewHolder(
                                        list.getChildAt(it),
                                    )
                                }.filterIsInstance<SavedTagListAdapter.Holder>()
                        rows.filter { it.row.tag != null }.forEach { holder ->
                            textFits(holder.row.findViewById(R.id.titleTextView))
                            val more = holder.row.findViewById<View>(R.id.savedTagMoreOptions)
                            assertTrue(more.width >= (48 * it.resources.displayMetrics.density).toInt())
                            assertTrue(more.height >= (48 * it.resources.displayMetrics.density).toInt())
                            assertTrue(fullBounds(holder.row).contains(fullBounds(more)))
                        }
                    }
                    capture(if (home) "home-many" else "teachable-many")
                    scenario.onActivity {
                        ids.removeAt(0)
                        ids.add(tags.first().id)
                    }
                    waitFor { adapter.currentList.lastOrNull() == tags.first().id }
                    settle()
                    scenario.onActivity {
                        list.itemAnimator?.endAnimations()
                        list.scrollToPosition(list.adapter!!.itemCount - 1)
                    }
                    settle()
                    scenario.onActivity { list.scrollBy(0, list.computeVerticalScrollRange()) }
                    settle()
                    if (home) {
                        scenario.onActivity { a ->
                            val link = a.findViewById<TextView>(R.id.donateHyperlink)
                            assertTrue("Last footer link reachable", fullyVisible(link))
                        }
                    }
                    capture(if (home) "home-footer" else "teachable-reordered")
                }
            }
        } finally {
            files.zip(originals).forEach { (file, original) -> if (original == null) file.delete() else file.writeBytes(original) }
        }
    }

    @Test fun videos_long_and_missing_metadata() {
        launch().use { scenario ->
            val first = request(scenario)
            val loaded =
                tag().apply {
                    videos =
                        mutableListOf(
                            depollsoft.tagmaster.barbershop.Video().apply {
                                id = 1
                                sungBy = "The singers from our combined evening rehearsal quartet"
                                sungKey = "B flat"
                                youTubeCode =
                                    "layout-fixture"
                            },
                            depollsoft.tagmaster.barbershop.Video().apply {
                                id = 2
                                sungBy = "Another quartet"
                                youTubeCode = "layout-fixture"
                            },
                        )
                }
            instrumentation.runOnMainSync { first.setResult(loaded) }
            settle()
            scenario.onActivity { it.findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(3, false) }
            settle()
            scenario.onActivity { a ->
                val list = a.findViewById<android.widget.ListView>(R.id.videoList)
                assertEquals(2, list.count)
                val firstRow = list.getChildAt(0)
                textFits(firstRow.findViewById(R.id.sungByTextView))
                textFits(firstRow.findViewById(R.id.keyTextView))
                assertTrue("Video list remains usable", list.height > 0)
                equalTabs(a)
            }
            capture("videos-long")
            scenario.onActivity { a -> a.findViewById<android.widget.ListView>(R.id.videoList).setSelection(1) }
            settle()
            scenario.onActivity { a ->
                val list = a.findViewById<android.widget.ListView>(R.id.videoList)
                val row = list.getChildAt(list.childCount - 1)
                assertEquals(View.GONE, row.findViewById<View>(R.id.keyRow).visibility)
                assertEquals(View.GONE, row.findViewById<View>(R.id.postedRow).visibility)
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
