package depollsoft.tagmaster

import android.app.Application
import android.content.Intent
import android.os.Looper
import android.view.View
import androidx.appcompat.view.menu.MenuBuilder
import androidx.viewpager2.widget.ViewPager2
import bolts.Task
import bolts.TaskCompletionSource
import depollsoft.lib.activity.RichApplication
import depollsoft.tagmaster.barbershop.Tag
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class TagLoadingStateTest {
    private val requests = LinkedBlockingQueue<Pair<Int, TaskCompletionSource<Tag>>>()
    private val id = 2147483018

    private fun tag(
        id: Int = this.id,
        title: String = "Quartet fixture",
    ) = Tag().apply {
        this.id = id
        this.title = title
        parts = 4
        writtenKey = "C"
    }

    @Before fun context() {
        RichApplication::class.java
            .getDeclaredField("context")
            .apply { isAccessible = true }
            .set(null, RuntimeEnvironment.getApplication())
    }

    private fun build(fast: Boolean = false): ActivityController<TagDetailActivity> {
        val controller =
            Robolectric.buildActivity(
                TagDetailActivity::class.java,
                Intent(RuntimeEnvironment.getApplication(), TagDetailActivity::class.java).putExtra(TagDetailActivity.TAG_ID_EXTRA, id),
            )
        controller.get().setTheme(R.style.AppTheme)
        controller.get().tagLoader = { requested, _ ->
            if (fast) Task.forResult(tag(requested)) else TaskCompletionSource<Tag>().also { requests.add(requested to it) }.task
        }
        return controller
    }

    private fun pending() = requests.poll(3, TimeUnit.SECONDS) ?: error("No request")

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun assertState(
        a: TagDetailActivity,
        loading: Boolean,
        loaded: Boolean,
        error: Boolean = false,
    ) {
        assertEquals(loading, a.isLoading)
        assertEquals(error, a.loadFailed)

        fun visible(
            id: Int,
            expected: Boolean,
        ) = assertEquals(id.toString(), if (expected) View.VISIBLE else View.GONE, a.findViewById<View>(id).visibility)
        visible(R.id.detailLoadingState, loading && !loaded)
        visible(R.id.viewPager, loaded)
        visible(R.id.tabLayout, loaded)
        visible(R.id.imageView1, loaded)
        visible(R.id.progress, loading && loaded)
        visible(R.id.detailErrorState, error && !loaded)
        assertEquals(!loading, a.findViewById<View>(R.id.detailRetryButton).isEnabled)
        if (!loaded) assertNull("No placeholder fragments", a.findViewById<ViewPager2>(R.id.viewPager).adapter)
        if (!loading || loaded) assertFalse(a.findViewById<TagLoadingView>(R.id.quartetIllustration).isAnimating)
        val menu = MenuBuilder(a)
        assertEquals(loaded, a.onCreateOptionsMenu(menu))
        if (loaded) {
            a.onPrepareOptionsMenu(menu)
            assertEquals(!loading, menu.findItem(R.id.refreshMenuItem).isEnabled)
        }
    }

    private fun refresh(a: TagDetailActivity) {
        val menu = MenuBuilder(a)
        a.onCreateOptionsMenu(menu)
        a.onOptionsItemSelected(menu.findItem(R.id.refreshMenuItem))
    }

    @Test fun initial_request_before_resume_has_no_placeholder_pages() {
        val c = build().create()
        try {
            assertState(c.get(), true, false)
            assertEquals(id, pending().first)
        } finally {
            c.destroy()
        }
    }

    @Test fun visible_pending_replaces_immediately_on_completion() {
        val c = build().setup().visible()
        try {
            assertState(c.get(), true, false)
            assertEquals(
                "Loading tag $id…",
                c
                    .get()
                    .findViewById<android.widget.TextView>(R.id.detailLoadingStatus)
                    .text
                    .toString(),
            )
            pending().second.setResult(tag())
            idle()
            assertState(c.get(), false, true)
            assertEquals(id, c.get().tag!!.id)
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test fun fast_cache_result_never_waits_for_animation() {
        val c = build(fast = true).setup().visible()
        try {
            assertState(c.get(), false, true)
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test fun initial_failure_retry_and_cancellation_stop_waiting() {
        val c = build().setup().visible()
        try {
            pending().second.setError(IllegalStateException("offline"))
            idle()
            assertState(c.get(), false, false, true)
            c.get().findViewById<View>(R.id.detailRetryButton).performClick()
            assertState(c.get(), true, false)
            pending().second.setCancelled()
            idle()
            assertState(c.get(), false, false, true)
            c.get().findViewById<View>(R.id.detailRetryButton).performClick()
            pending().second.setResult(tag())
            idle()
            assertState(c.get(), false, true)
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test fun failed_refresh_retains_tag_and_retry_updates_current_content() {
        val c = build().setup().visible()
        try {
            pending().second.setResult(tag())
            idle()
            val original = c.get().tag
            refresh(c.get())
            assertState(c.get(), true, true)
            pending().second.setError(IllegalStateException("offline"))
            idle()
            assertState(c.get(), false, true, true)
            assertSame(original, c.get().tag)
            refresh(c.get())
            pending().second.setResult(tag(title = "Updated quartet"))
            idle()
            assertState(c.get(), false, true)
            assertEquals("Updated quartet", c.get().tag!!.title)
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test fun old_request_cannot_populate_a_new_tag() {
        val c = build().setup().visible()
        try {
            val old = pending()
            c.newIntent(Intent(c.get(), TagDetailActivity::class.java).putExtra(TagDetailActivity.TAG_ID_EXTRA, id + 1))
            val current = pending()
            assertEquals(id + 1, current.first)
            old.second.setResult(tag())
            idle()
            assertState(c.get(), true, false)
            current.second.setResult(tag(id + 1))
            idle()
            assertEquals(id + 1, c.get().tag!!.id)
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test fun navigating_away_rejects_late_completion_and_stops_motion() {
        val c = build().setup().visible()
        val view = c.get().findViewById<TagLoadingView>(R.id.quartetIllustration)
        val request = pending()
        c.get().finish()
        c.pause().stop().destroy()
        request.second.setResult(tag())
        idle()
        assertNull(c.get().tag)
        assertFalse(view.isAnimating)
    }

    @Test fun mismatched_result_is_an_error_not_a_different_tag() {
        val c = build().setup().visible()
        try {
            pending().second.setResult(tag(id + 1))
            idle()
            assertState(c.get(), false, false, true)
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test fun synchronous_request_failure_offers_retry() {
        val c = build()
        c.get().tagLoader = { _, _ -> throw IllegalStateException("offline") }
        c.setup().visible()
        try {
            assertState(c.get(), false, false, true)
        } finally {
            c.pause().stop().destroy()
        }
    }
}
