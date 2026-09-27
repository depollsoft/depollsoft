package depollsoft.tagmaster

import android.app.Application
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import bolts.Task
import depollsoft.tagmaster.barbershop.Tag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Small flows across screens: opening a tag by id, and rating a tag. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
class PolishFlowScreenTest : ComposeScreenTest() {
    @Test
    fun theFootersCopyrightYearCanBePinned() {
        depollsoft.tagmaster.ui.FooterYear.pinned = 1999
        try {
            launch(MeActivity::class.java)
            compose.onNode(androidx.compose.ui.test.hasScrollToNodeAction()).performScrollToNode(androidx.compose.ui.test.hasText("© 1999"))
            assertTrue(compose.onAllNodes(androidx.compose.ui.test.hasText("© 1999"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
        } finally {
            depollsoft.tagmaster.ui.FooterYear.pinned = null
        }
    }

    @Test
    fun enterTagIdRejectsAnOutOfRangeIdAndOpensAValidOne() {
        val activity = launch(MeActivity::class.java)
        click("openByIdButton")
        node("openTagIdInput").performTextInput("0")
        click("openTagConfirm")
        assertTrue("the dialog stays open", exists("dialog"))
        assertTrue(text("dialog").contains(string(R.string.home_invalid_tag_id)))
        node("openTagIdInput").performTextInput("1809")
        click("openTagConfirm")
        val started = nextStarted(activity)
        assertEquals(TagDetailActivity::class.java.name, started?.component?.className)
        assertEquals(1809, started!!.getIntExtra(TagDetailActivity.TAG_ID_EXTRA, -1))
    }

    @Test
    fun ratingCancelDoesNotSubmitAndAnExplicitChoiceDoes() {
        val fixture = spy(ScreenTestSupport.fixtureTag())
        doReturn(Task.forResult(true)).`when`(fixture).rate(anyInt())
        launch(
            TagDetailActivity::class.java,
            Intent(app, TagDetailActivity::class.java).putExtra(TagDetailActivity.TAG_ID_EXTRA, fixture.id),
        ) { it.tagLoader = { _, _ -> Task.forResult<Tag>(fixture) } }
        idle()
        click("rateButton")
        node("ratingSubmit").assertIsNotEnabled()
        click("dialogButton:${string(android.R.string.cancel)}")
        verify(fixture, never()).rate(anyInt())
        click("rateButton")
        node("ratingPicker").performSemanticsAction(SemanticsActions.SetProgress) { it(4f) }
        idle()
        node("ratingSubmit").assertIsEnabled()
        click("ratingSubmit")
        verify(fixture).rate(4)
    }
}
