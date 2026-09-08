package depollsoft.tagmaster

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import depollsoft.lib.json.JsonSerializer
import depollsoft.tagmaster.barbershop.RemoteLocation
import depollsoft.tagmaster.barbershop.Tag
import depollsoft.tagmaster.barbershop.Video
import org.hamcrest.Matchers.allOf
import org.junit.Assert.*
import org.junit.rules.ExternalResource
import java.io.File

/** Local data only. Activities, fragments, adapters and navigation remain production code. */
class NavigationTestFixture : ExternalResource() {
    val context: Context = ApplicationProvider.getApplicationContext()
    val tag =
        Tag().apply {
            id = 2147483017
            title = "Navigation fixture"
            writtenKey = "C"
            parts = 4
            arranger = "Fixture arranger"
            yearArranged = "2026"
            classicTagNumber = 17
            rating = 4.0
            downloadCount = 123
            recordingMethod = "Separate parts for navigation coverage"
            allPartsTrackUri = track("all")
            tenorTrackUri = track("tenor")
            leadTrackUri = track("lead")
            baritoneTrackUri = track("baritone")
            bassTrackUri = track("bass")
            videos =
                mutableListOf(
                    Video().apply {
                        id = 1
                        sungBy = "Fixture quartet"
                        sungKey = "C"
                        youTubeCode = "navigation-fixture"
                    },
                )
        }
    private val cacheFile get() = File(context.filesDir, "TagCache/${tag.id}")
    private var original: ByteArray? = null

    override fun before() {
        original = if (cacheFile.exists()) cacheFile.readBytes() else null
        cacheFile.parentFile!!.mkdirs()
        // Write synchronously, then let TagDetailActivity use loadTagById's real disk path.
        cacheFile.writeText(JsonSerializer.serialize(tag).toString())
    }

    override fun after() {
        original?.let { cacheFile.writeBytes(it) } ?: cacheFile.delete()
    }

    fun detailIntent() =
        Intent(context, TagDetailActivity::class.java)
            .putExtra(TagDetailActivity.TAG_ID_EXTRA, tag.id)

    /** Replace only the query data, not the fragment or its adapter. Old requests own old models. */
    fun populateResults() {
        onResumed<TagSearchResultsActivity> { activity ->
            val fragment = activity.supportFragmentManager.findFragmentById(R.id.tagQueryFragment) as TagQueryFragment
            val query = fragment.model!!.query
            fragment.model =
                QueryModel().apply {
                    this.query = query
                    hasMoreResults = false
                    tags.add(tag)
                }
        }
        onView(withId(R.id.titleTextView)).check(matches(withText(tag.title)))
    }

    companion object {
        private fun track(part: String) =
            RemoteLocation().apply {
                // Selecting a part does not download or play it. Each URI distinguishes the binding.
                uri = "https://example.invalid/navigation-$part.mp3"
                type = "mp3"
            }

        inline fun <reified T : Activity> onResumed(crossinline assertion: (T) -> Unit) {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            // During an activity transition no activity is RESUMED yet; wait for exactly one.
            val deadline = android.os.SystemClock.uptimeMillis() + 5000
            var resumed: Activity? = null
            while (resumed == null && android.os.SystemClock.uptimeMillis() < deadline) {
                instrumentation.waitForIdleSync()
                instrumentation.runOnMainSync {
                    resumed =
                        ActivityLifecycleMonitorRegistry
                            .getInstance()
                            .getActivitiesInStage(Stage.RESUMED)
                            .singleOrNull()
                }
                if (resumed == null) Thread.sleep(50)
            }
            instrumentation.runOnMainSync {
                val activity =
                    resumed ?: ActivityLifecycleMonitorRegistry
                        .getInstance()
                        .getActivitiesInStage(Stage.RESUMED)
                        .single()
                assertTrue("Expected ${T::class.java.simpleName}, got ${activity.javaClass.simpleName}", activity is T)
                assertion(activity as T)
            }
        }

        fun selectTab(
            label: Int,
            position: Int,
        ) {
            onView(allOf(withText(label), isDescendantOfA(withId(R.id.tabLayout))))
                .perform(click())
            assertPage(position)
        }

        fun assertPage(position: Int) {
            onView(withId(R.id.viewPager)).perform(
                object : ViewAction {
                    override fun getConstraints() = isAssignableFrom(ViewPager2::class.java)

                    override fun getDescription() = "Wait for pager to settle on page $position"

                    override fun perform(
                        controller: UiController,
                        view: View,
                    ) {
                        val pager = view as ViewPager2
                        val deadline = android.os.SystemClock.uptimeMillis() + 5000
                        while ((pager.currentItem != position || pager.scrollState != ViewPager2.SCROLL_STATE_IDLE) &&
                            android.os.SystemClock.uptimeMillis() < deadline
                        ) {
                            controller.loopMainThreadForAtLeast(16)
                        }
                        assertEquals(position, pager.currentItem)
                        assertEquals(ViewPager2.SCROLL_STATE_IDLE, pager.scrollState)
                        assertEquals(position, pager.rootView.findViewById<TabLayout>(R.id.tabLayout).selectedTabPosition)
                    }
                },
            )
        }
    }
}
