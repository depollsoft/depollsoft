package depollsoft.tagmaster

import android.app.Application
import android.content.Intent
import android.os.Looper
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performSemanticsAction
import bolts.Task
import bolts.TaskCompletionSource
import depollsoft.lib.activity.RichApplication
import depollsoft.tagmaster.barbershop.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
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

/**
 * The tag detail screen's loading states: nothing but the loading state until a tag arrives, the
 * tag replacing it as soon as it does, errors with a working retry, and requests that belong to a
 * tag the screen has moved on from never landing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TagLoadingStateTest {
    @get:Rule
    val compose = createEmptyComposeRule()

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

    @Before
    fun context() {
        RichApplication.setAppContextForTesting(RuntimeEnvironment.getApplication())
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

    private fun idle() {
        shadowOf(Looper.getMainLooper()).idle()
        compose.waitForIdle()
    }

    private fun shown(tag: String) = compose.onAllNodesWithTagCount(tag) > 0

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTagCount(tag: String) =
        onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().size

    private fun assertState(
        a: TagDetailActivity,
        loading: Boolean,
        loaded: Boolean,
        error: Boolean = false,
    ) {
        idle()
        assertEquals("loading", loading, a.isLoading)
        assertEquals("failed", error, a.loadFailed)
        assertEquals("loading state", loading && !loaded, shown("detailLoading"))
        assertEquals("pages", loaded, shown("detailPager"))
        assertEquals("tabs", loaded, shown("detailTabs"))
        assertEquals("refresh bar", loading && loaded, shown("refreshProgress"))
        assertEquals("error state", error && !loaded, shown("detailError"))
        if (error && !loaded) {
            if (loading) compose.onNodeWithTag("detailRetry").assertIsNotEnabled() else compose.onNodeWithTag("detailRetry").assertIsEnabled()
        }
        // The tag's toolbar actions (Refresh among them, in the overflow on a phone) only exist
        // once there is a tag.
        assertEquals("tag actions", loaded, shown("overflowMenu"))
    }

    private fun refresh(a: TagDetailActivity) {
        a.detail.refresh()
        idle()
    }

    private fun retry() {
        compose.onNodeWithTag("detailRetry").performSemanticsAction(SemanticsActions.OnClick)
        idle()
    }

    @Test
    fun initialRequestBeforeResumeHasNoPlaceholderPages() {
        val c = build().create().start()
        try {
            // Nothing is on screen yet; the request is already out and no tag is assumed.
            assertEquals(true, c.get().isLoading)
            assertNull(c.get().tag)
            assertEquals(id, pending().first)
        } finally {
            c.destroy()
        }
    }

    @Test
    fun aPendingLoadIsReplacedAsSoonAsItCompletes() {
        val c = build().setup().visible()
        try {
            assertState(c.get(), loading = true, loaded = false)
            compose.onNodeWithTag("detailLoading").assert(hasContentDescription("Loading tag $id…", substring = true))
            pending().second.setResult(tag())
            assertState(c.get(), loading = false, loaded = true)
            assertEquals(id, c.get().tag!!.id)
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test
    fun aFastCacheResultNeverShowsTheLoadingState() {
        val c = build(fast = true).setup().visible()
        try {
            assertState(c.get(), loading = false, loaded = true)
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test
    fun anInitialFailureRetriesAndACancellationStopsWaiting() {
        val c = build().setup().visible()
        try {
            pending().second.setError(IllegalStateException("offline"))
            assertState(c.get(), loading = false, loaded = false, error = true)
            retry()
            assertState(c.get(), loading = true, loaded = false)
            pending().second.setCancelled()
            assertState(c.get(), loading = false, loaded = false, error = true)
            retry()
            pending().second.setResult(tag())
            assertState(c.get(), loading = false, loaded = true)
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test
    fun aFailedRefreshKeepsTheTagAndARetryUpdatesIt() {
        val c = build().setup().visible()
        try {
            pending().second.setResult(tag())
            idle()
            val original = c.get().tag
            refresh(c.get())
            assertState(c.get(), loading = true, loaded = true)
            pending().second.setError(IllegalStateException("offline"))
            assertState(c.get(), loading = false, loaded = true, error = true)
            assertSame(original, c.get().tag)
            val failedMessage = c.get().getString(R.string.detail_tag_refresh_failed, c.get().detail.tagId)
            fun snackbarShown() = compose.onAllNodes(androidx.compose.ui.test.hasText(failedMessage)).fetchSemanticsNodes().isNotEmpty()
            assertTrue("the failure is announced", snackbarShown())
            refresh(c.get())
            assertFalse("a new load takes the old failure down", snackbarShown())
            pending().second.setResult(tag(title = "Updated quartet"))
            assertState(c.get(), loading = false, loaded = true)
            assertEquals("Updated quartet", c.get().tag!!.title)
            idle()
            assertFalse(snackbarShown())
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test
    fun anOldRequestCannotPopulateANewTag() {
        val c = build().setup().visible()
        try {
            val old = pending()
            c.newIntent(Intent(c.get(), TagDetailActivity::class.java).putExtra(TagDetailActivity.TAG_ID_EXTRA, id + 1))
            val current = pending()
            assertEquals(id + 1, current.first)
            old.second.setResult(tag())
            assertState(c.get(), loading = true, loaded = false)
            current.second.setResult(tag(id + 1))
            idle()
            assertEquals(id + 1, c.get().tag!!.id)
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test
    fun leavingTheScreenRejectsALateCompletion() {
        val c = build().setup().visible()
        val request = pending()
        c.get().finish()
        c.pause().stop().destroy()
        request.second.setResult(tag())
        shadowOf(Looper.getMainLooper()).idle()
        assertNull(c.get().tag)
    }

    @Test
    fun aResultForAnotherTagIsAnErrorNotADifferentTag() {
        val c = build().setup().visible()
        try {
            pending().second.setResult(tag(id + 1))
            assertState(c.get(), loading = false, loaded = false, error = true)
        } finally {
            c.pause().stop().destroy()
        }
    }

    @Test
    fun aLoaderThatThrowsOffersRetry() {
        val c = build()
        c.get().tagLoader = { _, _ -> throw IllegalStateException("offline") }
        c.setup().visible()
        try {
            assertState(c.get(), loading = false, loaded = false, error = true)
        } finally {
            c.pause().stop().destroy()
        }
    }
}
