package depollsoft.tagmaster

import android.app.Activity
import android.app.Application
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import bolts.Task
import com.bindroid.trackable.TrackableCollection
import com.google.android.material.appbar.MaterialToolbar
import depollsoft.lib.activity.RichApplication
import depollsoft.lib.util.Preferences
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

/**
 * The tablet list-detail surface: a tag opens beside the list it came from, the row it came from
 * stays lit, and the chevrons walk that list. Phones keep the full-screen navigation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28], qualifiers = "w1280dp-h800dp")
class TabletListDetailTest {
    private val ids = listOf(2147483301, 2147483302, 2147483303)

    private val listScreens: List<Class<out AppCompatActivity>> =
        listOf(
            TagBrowserActivity::class.java,
            TagSearchResultsActivity::class.java,
            MeActivity::class.java,
            TeachableTagsActivity::class.java,
        )

    @Before fun setUp() {
        RichApplication::class.java
            .getDeclaredField("context")
            .apply { isAccessible = true }
            .set(null, RuntimeEnvironment.getApplication())
        ListModel.setTestMode(true)
        Preferences.setTestMode(false)
        Preferences::class.java
            .getDeclaredField("initialized")
            .apply { isAccessible = true }
            .setBoolean(null, false)
        TeachableTagsModel.teachableTagIds = TrackableCollection(ids.toMutableList())
    }

    private fun tag(id: Int) =
        Tag().apply {
            this.id = id
            title = "Tag $id"
            parts = 4
            writtenKey = "C"
        }

    private fun <A : Activity> launch(clazz: Class<A>): ActivityController<A> {
        val controller = Robolectric.buildActivity(clazz)
        controller.get().setTheme(R.style.AppTheme)
        return controller.setup()
    }

    @Suppress("UNCHECKED_CAST")
    private fun launchScreen(clazz: Class<out AppCompatActivity>): ActivityController<AppCompatActivity> =
        launch(clazz as Class<AppCompatActivity>)

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun teachable(): ActivityController<TeachableTagsActivity> {
        val controller = launch(TeachableTagsActivity::class.java)
        controller.get().tagPane.tagLoader = { id, _ -> Task.forResult(tag(id)) }
        return controller
    }

    @Test fun every_list_screen_opens_a_detail_pane_at_tablet_width() {
        for (clazz in listScreens) {
            val controller = launchScreen(clazz)
            try {
                val activity = controller.get()
                assertNotNull(clazz.simpleName, activity.findViewById<View>(R.id.detailPane))
                assertTrue(clazz.simpleName, (activity as TagPaneHost).hasDetailPane)
                assertEquals(
                    clazz.simpleName,
                    View.VISIBLE,
                    activity.findViewById<View>(R.id.tagPaneEmptyState).visibility,
                )
                assertEquals(clazz.simpleName, "", activity.findViewById<MaterialToolbar>(R.id.detailToolbar).title)
            } finally {
                controller.pause().stop().destroy()
            }
        }
    }

    @Config(qualifiers = "w411dp-h914dp")
    @Test fun no_list_screen_has_a_detail_pane_at_phone_width() {
        for (clazz in listScreens) {
            val controller = launchScreen(clazz)
            try {
                val activity = controller.get()
                assertNull(clazz.simpleName, activity.findViewById<View>(R.id.detailPane))
                assertFalse(clazz.simpleName, (activity as TagPaneHost).hasDetailPane)
            } finally {
                controller.pause().stop().destroy()
            }
        }
    }

    @Test fun showing_a_tag_attaches_one_detail_fragment_and_lights_its_row() {
        val controller = teachable()
        try {
            val activity = controller.get()
            val row = TagItemView(activity)
            activity.findViewById<ViewGroup>(R.id.listPane).addView(row)
            row.bind(tag(ids[1]))
            idle()
            assertFalse("Nothing is showing yet", row.isActivated)

            activity.showTag(ids[1])
            idle()
            val fragment = activity.supportFragmentManager.findFragmentByTag(TagPaneController.DETAIL_FRAGMENT_TAG)
            assertTrue(fragment is TagDetailFragment)
            assertEquals(ids[1], activity.selectedTagId)
            assertEquals(View.GONE, activity.findViewById<View>(R.id.tagPaneEmptyState).visibility)
            assertEquals("Tag ${ids[1]}", activity.findViewById<MaterialToolbar>(R.id.detailToolbar).title)
            assertTrue("The open tag's row stays lit", row.isActivated)

            // A second selection reuses the same fragment, so the open page survives the change.
            activity.showTag(ids[2])
            idle()
            assertSame(fragment, activity.supportFragmentManager.findFragmentByTag(TagPaneController.DETAIL_FRAGMENT_TAG))
            assertEquals(ids[2], (fragment as TagDetailFragment).tag!!.id)
            assertEquals(ids[2], activity.selectedTagId)
            assertFalse("The previous row unlights", row.isActivated)
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test fun ctrl_arrows_walk_the_list_like_the_ipad_command_arrows() {
        val controller = teachable()
        try {
            val activity = controller.get()
            fun key(
                code: Int,
                ctrl: Boolean = true,
            ): Boolean {
                val meta = if (ctrl) android.view.KeyEvent.META_CTRL_ON else 0
                val event = android.view.KeyEvent(0, 0, android.view.KeyEvent.ACTION_DOWN, code, 0, meta)
                return activity.onKeyDown(code, event)
            }
            assertFalse("Nothing selected: the list keeps its arrows", key(android.view.KeyEvent.KEYCODE_DPAD_DOWN))

            activity.showTag(ids[0])
            idle()
            assertFalse("Plain arrows belong to the focused list", key(android.view.KeyEvent.KEYCODE_DPAD_DOWN, ctrl = false))
            assertEquals(ids[0], activity.selectedTagId)

            assertTrue(key(android.view.KeyEvent.KEYCODE_DPAD_DOWN))
            idle()
            assertEquals(ids[1], activity.selectedTagId)
            assertTrue(key(android.view.KeyEvent.KEYCODE_DPAD_UP))
            idle()
            assertEquals(ids[0], activity.selectedTagId)
            assertTrue("Consumed at the top so focus does not jump", key(android.view.KeyEvent.KEYCODE_DPAD_UP))
            assertEquals(ids[0], activity.selectedTagId)
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test fun chevrons_walk_the_list_the_user_is_looking_at() {
        val controller = teachable()
        try {
            val activity = controller.get()
            assertEquals(ids, activity.listedTagIds())
            val menu = activity.findViewById<MaterialToolbar>(R.id.detailToolbar).menu
            val previous = menu.findItem(R.id.previousTagMenuItem)
            val next = menu.findItem(R.id.nextTagMenuItem)
            assertFalse("Nothing selected yet", previous.isEnabled)
            assertFalse("Nothing selected yet", next.isEnabled)
            assertFalse("The empty pane's bar stays quiet", previous.isVisible)
            assertFalse("The empty pane's bar stays quiet", next.isVisible)

            activity.showTag(ids[1])
            idle()
            assertTrue(previous.isVisible)
            assertTrue(next.isVisible)
            assertTrue(previous.isEnabled)
            assertTrue(next.isEnabled)

            activity.tagPane.onMenuItem(next)
            idle()
            assertEquals(ids[2], activity.selectedTagId)
            assertTrue(previous.isEnabled)
            assertFalse("No tag after the last one", next.isEnabled)

            activity.tagPane.onMenuItem(previous)
            activity.tagPane.onMenuItem(previous)
            idle()
            assertEquals(ids[0], activity.selectedTagId)
            assertFalse("No tag before the first one", previous.isEnabled)
            assertTrue(next.isEnabled)
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Test fun a_row_opens_beside_the_list_on_a_tablet() {
        val controller = teachable()
        try {
            val activity = controller.get()
            val row = TagItemView(activity)
            row.bind(tag(ids[0]))
            row.performClick()
            idle()
            assertEquals(ids[0], activity.selectedTagId)
            assertNull("Nothing goes full-screen", shadowOf(activity).nextStartedActivity)
        } finally {
            controller.pause().stop().destroy()
        }
    }

    @Config(qualifiers = "w411dp-h914dp")
    @Test fun a_row_still_opens_full_screen_on_a_phone() {
        val controller = launch(TeachableTagsActivity::class.java)
        try {
            val activity = controller.get()
            val row = TagItemView(activity)
            row.bind(tag(ids[0]))
            row.performClick()
            idle()
            val started = shadowOf(activity).nextStartedActivity
            assertEquals(TagDetailActivity::class.java.name, started.component!!.className)
            assertEquals(ids[0], started.getIntExtra(TagDetailActivity.TAG_ID_EXTRA, -1))
            assertNull(activity.selectedTagId)
        } finally {
            controller.pause().stop().destroy()
        }
    }
}
