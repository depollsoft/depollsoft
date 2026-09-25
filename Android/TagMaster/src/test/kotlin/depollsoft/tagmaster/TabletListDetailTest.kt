package depollsoft.tagmaster

import android.app.Application
import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performSemanticsAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The tablet list/detail split: every list screen opens tags beside itself at tablet width and
 * full-screen on a phone, one detail is reused for each selection, and Ctrl+arrows and the pane's
 * chevrons walk the list the user is looking at.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w1280dp-h800dp-land-xhdpi")
class TabletListDetailTest : ComposeScreenTest() {
    private val ids = listOf(2147483017, 2147483018, 2147483019)

    private fun cacheTags() {
        for ((index, id) in ids.withIndex()) {
            ScreenTestSupport.cacheOnDisk(ScreenTestSupport.fixtureTag().apply { this.id = id; title = "Fixture ${index + 1}" })
        }
    }

    private fun teachable(): TeachableTagsActivity {
        cacheTags()
        ids.forEach(TeachableTagsModel::addTeachableTag)
        return launch(TeachableTagsActivity::class.java)
    }

    private fun awaitShown(host: TagPaneHost) {
        val detail = (host as TeachableTagsActivity).tagPane.detail!!
        ScreenTestSupport.await("the pane's tag to load") { !detail.isLoading && detail.tag != null }
        idle()
    }

    private fun key(code: Int) = KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, code, 0, KeyEvent.META_CTRL_ON)

    @Test
    fun everyListScreenHasADetailPaneAtTabletWidth() {
        assertTrue(launch(MeActivity::class.java).hasDetailPane)
        assertTrue(teachable().hasDetailPane)
        assertTrue(launch(TagBrowserActivity::class.java).hasDetailPane)
    }

    @Test
    fun theEmptyPaneInvitesAChoiceUntilARowIsOpened() {
        val activity = teachable()
        assertTrue(exists("tagPaneEmpty"))
        activity.showTag(ids[1])
        awaitShown(activity)
        assertFalse(exists("tagPaneEmpty"))
        assertEquals(ids[1], activity.selectedTagId)
        assertTrue(exists("detailPager"))
    }

    @Test
    fun aRowOpensBesideTheListAndLightsUp() {
        val activity = teachable()
        compose
            .onAllNodes(
                androidx.compose.ui.test.hasAnyAncestor(androidx.compose.ui.test.hasTestTag("savedTag:${ids[0]}")) and
                    androidx.compose.ui.test.hasClickAction(),
                useUnmergedTree = true,
            ).onFirst()
            .performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        idle()
        assertNull("nothing opens full screen", nextStarted(activity))
        assertEquals(ids[0], activity.selectedTagId)
    }

    @Test
    fun oneDetailIsReusedAndKeepsItsPage() {
        val activity = teachable()
        activity.showTag(ids[0])
        awaitShown(activity)
        val detail = activity.tagPane.detail
        click("detailTab:2")
        activity.showTag(ids[1])
        awaitShown(activity)
        assertTrue(detail === activity.tagPane.detail)
        assertEquals(2, detail!!.page)
    }

    @Test
    fun ctrlArrowsWalkTheList() {
        val activity = teachable()
        activity.showTag(ids[0])
        awaitShown(activity)
        assertTrue(activity.onKeyDown(KeyEvent.KEYCODE_DPAD_DOWN, key(KeyEvent.KEYCODE_DPAD_DOWN)))
        assertEquals(ids[1], activity.selectedTagId)
        assertTrue(activity.onKeyDown(KeyEvent.KEYCODE_DPAD_UP, key(KeyEvent.KEYCODE_DPAD_UP)))
        assertEquals(ids[0], activity.selectedTagId)
        // At the top there is nowhere to go, and the key is still the pane's.
        assertTrue(activity.onKeyDown(KeyEvent.KEYCODE_DPAD_UP, key(KeyEvent.KEYCODE_DPAD_UP)))
        assertEquals(ids[0], activity.selectedTagId)
    }

    @Test
    fun theChevronsWalkTheListTheUserIsLookingAt() {
        val activity = teachable()
        activity.showTag(ids[0])
        awaitShown(activity)
        node("previousTag").assertIsNotEnabled()
        click("nextTag")
        assertEquals(ids[1], activity.selectedTagId)
        click("nextTag")
        assertEquals(ids[2], activity.selectedTagId)
        node("nextTag").assertIsNotEnabled()
        click("previousTag")
        assertEquals(ids[1], activity.selectedTagId)
    }

    @Test
    fun theSelectionSurvivesRecreation() {
        val activity = teachable()
        activity.showTag(ids[2])
        awaitShown(activity)
        val recreated = recreate<TeachableTagsActivity>()
        assertEquals(ids[2], recreated.selectedTagId)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun onAPhoneARowStillOpensFullScreen() {
        val activity = teachable()
        assertFalse(activity.hasDetailPane)
        activity.showTag(ids[0])
        assertEquals(TagDetailActivity::class.java.name, nextStarted(activity)?.component?.className)
        assertFalse(exists("tagPaneEmpty"))
    }
}
