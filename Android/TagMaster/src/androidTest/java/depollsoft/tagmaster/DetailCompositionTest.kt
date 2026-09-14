package depollsoft.tagmaster

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.children
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewpager2.widget.ViewPager2
import bolts.Task
import depollsoft.tagmaster.barbershop.RemoteLocation
import depollsoft.tagmaster.barbershop.Tag
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Date

/** Real activities/fragments with local model injection. No cache, rating or list writes. */
@RunWith(AndroidJUnit4::class)
class DetailCompositionTest {
    private val instrument = InstrumentationRegistry.getInstrumentation()
    private val app get() = instrument.targetContext.applicationContext as Application
    private val args get() = InstrumentationRegistry.getArguments()

    private fun tag() =
        Tag().apply {
            id = 1809
            title = "Lost"
            alternativeTitle = "In Your Eyes"
            parts = 4
            rating = 3.49
            tagType = "Barbershop"
            writtenKey = "Minor:G"
            downloadCount = 12345
            lastRefreshed = Date(1600000000000)
            posted = lastRefreshed
            provider = "David Wright"
            arranger = "David Wright"
            sungBy = "The New Tradition"
            yearArranged = "2020"
            sungYear = "2021"
            providerWebsite = "https://example.invalid/provider"
            sungByWebsite = "https://example.invalid/quartet"
            lyrics = "And I will wait to face the skies,\never roaming in your eyes.\nThere I go lost in your eyes."
            notes = "Hold the last chord."
            sheetMusicUri =
                RemoteLocation().apply {
                    uri = "https://example.invalid/composition.pdf"
                    type = "pdf"
                }
        }

    private fun bounds(view: View): Rect =
        Rect(0, 0, view.width, view.height).also { r ->
            val xy = IntArray(2)
            view.getLocationOnScreen(xy)
            r.offset(xy[0], xy[1])
        }

    private fun settle() {
        instrument.waitForIdleSync()
        Thread.sleep(120)
        instrument.waitForIdleSync()
    }

    private fun capture(name: String) {
        val label = args.getString("compositionCapture") ?: return
        val bitmap = instrument.uiAutomation.takeScreenshot()
        val scale = 800f / maxOf(bitmap.width, bitmap.height)
        val small = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        val directory = File(app.cacheDir, "detail-composition-captures").apply { mkdirs() }
        File(directory, "tagmaster-detail-composition-android-$label-$name.jpg").outputStream().use {
            small.compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
        small.recycle()
        bitmap.recycle()
    }

    private fun metadata(group: DetailMetadataLayout) {
        val pairs =
            group.children
                .filterIsInstance<DetailPairLayout>()
                .filter { it.visibility != View.GONE }
                .toList()
        val mode = pairs.first().captionWidth
        val axis = bounds(pairs.first().getChildAt(1)).left
        pairs.forEach { pair ->
            assertEquals("Whole metadata group uses one mode", mode, pair.captionWidth)
            val caption = pair.getChildAt(0) as TextView
            val value = pair.getChildAt(1)
            assertEquals("Shared value edge", axis, bounds(value).left)
            assertTrue("Useful value width", value.width >= 96 * value.resources.displayMetrics.density)
            if (mode >= 0 &&
                value.baseline >= 0
            ) {
                assertEquals(
                    "First visible baseline ${caption.text}",
                    bounds(caption).top + caption.baseline,
                    bounds(value).top + value.baseline,
                )
            }
            if (value is TextView &&
                value.layout != null
            ) {
                assertTrue(
                    "Final line fits ${caption.text}",
                    value.height >= value.compoundPaddingTop + value.layout.height + value.compoundPaddingBottom,
                )
            }
        }
    }

    @Test fun real_summary_details_geometry_and_optional_collapse() {
        val model = tag().apply {
            if (args.getString("compositionLong") == "true") {
                title = "Lost with a long title that wraps at accessibility sizes"
                alternativeTitle = "In Your Eyes with another long alternate title"
                provider = "Alexandria Montgomery and the International Harmony Society"
                arranger = provider
                sungBy = "The International Harmony Society Quartet"
                lyrics = listOf(lyrics, lyrics, lyrics).joinToString("\n")
                notes = "Sing the phrase together, then hold the last chord. Listen to the lead and balance the other parts. Repeat the phrase quietly before returning to full voice."
            }
        }
        val callbacks =
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityPreCreated(
                    activity: Activity,
                    state: Bundle?,
                ) {
                    if (activity is TagDetailActivity) activity.tagLoader = { _, _ -> Task.forResult(model) }
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
        app.registerActivityLifecycleCallbacks(callbacks)
        try {
            ActivityScenario
                .launch<TagDetailActivity>(
                    Intent(app, TagDetailActivity::class.java).putExtra(TagDetailActivity.TAG_ID_EXTRA, 1809),
                ).use { scenario ->
                    settle()
                    lateinit var root: View
                    scenario.onActivity { activity ->
                        root =
                            activity.supportFragmentManager.fragments
                                .filterIsInstance<TagSummaryFragment>()
                                .single()
                                .requireView()
                        val density = root.resources.displayMetrics.density
                        val leading = bounds(root.findViewById(R.id.titleTextView)).left
                        for (id in listOf(
                            R.id.akaLayout,
                            R.id.versionLayout,
                            R.id.savedStatusLayout,
                        )) {
                            assertEquals(leading, bounds(root.findViewById(id)).left)
                        }
                        val columns = root.findViewById<SummaryColumnsLayout>(R.id.summaryColumns)
                        if (columns.orientation ==
                            android.widget.LinearLayout.VERTICAL
                        ) {
                            for (id in listOf(
                                R.id.lyticsTitleTextView,
                                R.id.lyricsTextView,
                                R.id.textView6,
                                R.id.notesTextView,
                            )) {
                                assertEquals("Prose reading edge", leading, bounds(root.findViewById(id)).left)
                            }
                        }
                        metadata(root.findViewById(R.id.summaryFacts))
                        val key = root.findViewById<View>(R.id.playKeyNoteButton)
                        val sheet = root.findViewById<com.google.android.material.button.MaterialButton>(R.id.sheetMusicLink)
                        val keyFrame = bounds(key)
                        val sheetFrame = bounds(sheet)
                        assertEquals(keyFrame.left, sheetFrame.left)
                        assertEquals(keyFrame.right, sheetFrame.right)
                        assertEquals(key.height, sheet.height)
                        assertEquals(0, sheet.insetTop)
                        assertEquals(0, sheet.insetBottom)
                        assertTrue(key.height >= 48 * density)
                        val rating = bounds(root.findViewById(R.id.ratingTextView))
                        val rate = bounds(root.findViewById(R.id.rateButton))
                        assertTrue("Rate stays inside rating unit", rate.left - rating.right <= 40 * density)
                        assertEquals("3.49", root.findViewById<TextView>(R.id.ratingTextView).text.toString())
                    }
                    lateinit var idleKey: Rect
                    lateinit var idleSheet: Rect
                    scenario.onActivity {
                        idleKey = bounds(root.findViewById(R.id.playKeyNoteButton))
                        idleSheet = bounds(root.findViewById(R.id.sheetMusicLink))
                    }
                    for (busy in listOf(true, false)) {
                        scenario.onActivity {
                            root.findViewById<BarberPoleLoadingView>(R.id.sheetMusicProgress).loading = busy
                            root.findViewById<View>(R.id.sheetMusicLink).isEnabled = !busy
                        }
                        settle()
                        scenario.onActivity {
                            assertEquals("Key frame survives a real layout pass", idleKey, bounds(root.findViewById(R.id.playKeyNoteButton)))
                            assertEquals("Sheet frame survives a real layout pass", idleSheet, bounds(root.findViewById(R.id.sheetMusicLink)))
                        }
                    }
                    capture("summary")
                    scenario.onActivity { it.findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(1, false) }
                    settle()
                    scenario.onActivity { activity ->
                        val detail =
                            activity.supportFragmentManager.fragments
                                .filterIsInstance<TagMiscFragment>()
                                .single()
                                .requireView()
                        metadata(detail.findViewById(R.id.tableLayout1))
                    }
                    capture("details")
                    scenario.onActivity { it.findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(0, false) }
                    settle()
                    for (prose in 0..3) {
                        scenario.onActivity {
                            model.sheetMusicUri = null
                            model.writtenKey = null
                            model.alternativeTitle = null
                            model.version = null
                            model.lyrics = if (prose and 1 != 0) "Only lyrics" else null
                            model.notes = if (prose and 2 != 0) "Only notes" else null
                        }
                        settle()
                        scenario.onActivity {
                            for (id in listOf(
                                R.id.linearLayout3,
                                R.id.keyRow,
                                R.id.akaLayout,
                                R.id.versionLayout,
                                R.id.savedStatusLayout,
                            )) {
                                assertEquals("Optional group collapses", View.GONE, root.findViewById<View>(id).visibility)
                            }
                            val columns = root.findViewById<SummaryColumnsLayout>(R.id.summaryColumns)
                            assertEquals(prose == 0, columns.getChildAt(1).visibility == View.GONE)
                            if (prose == 0) assertEquals(android.widget.LinearLayout.VERTICAL, columns.orientation)
                        }
                        if (prose == 0) capture("missing")
                    }
                }
        } finally {
            app.unregisterActivityLifecycleCallbacks(callbacks)
        }
    }
}
