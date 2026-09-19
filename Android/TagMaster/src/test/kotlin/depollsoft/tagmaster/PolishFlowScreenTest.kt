package depollsoft.tagmaster

import android.app.Application
import android.content.Intent
import android.view.View
import android.widget.RatingBar
import androidx.appcompat.app.AlertDialog
import bolts.Task
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import depollsoft.tagmaster.ScreenTestSupport.assertDisplayed
import depollsoft.tagmaster.ScreenTestSupport.chooseDropdown
import depollsoft.tagmaster.ScreenTestSupport.idle
import depollsoft.tagmaster.ScreenTestSupport.scrollTo
import depollsoft.tagmaster.barbershop.RemoteLocation
import depollsoft.tagmaster.barbershop.Tag
import depollsoft.tagmaster.barbershop.TagSortOptions
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * The polish flows, migrated from the instrumented `PolishFlowRegressionTest`.
 *
 * The short-viewport scroll-and-select case it opened with is already covered on the JVM by
 * `PolishLayoutRegressionTest`; what is added here is the landscape detail screen actually routing
 * the selected part to the player, plus the search, Open-by-id and rating flows.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28], qualifiers = "w411dp-h891dp")
class PolishFlowScreenTest {
    private var controller: ActivityController<*>? = null

    @Before
    fun setUp() {
        ScreenTestSupport.startClean()
        ScreenTestSupport.clearTagCaches()
    }

    @After
    fun tearDown() {
        ScreenTestSupport.finish(controller)
        ScreenTestSupport.clearTagCaches()
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-land")
    fun landscapeTracksCanScrollToAndSelectBass() {
        val location =
            RemoteLocation().apply {
                uri = "https://example.invalid/bass.mp3"
                type = "mp3"
            }
        val tag =
            Tag().apply {
                id = 2147483001
                title = "Landscape regression"
                recordingMethod = "A recording note long enough to use space in a short viewport."
                bassTrackUri = location
                tenorTrackUri = location
                leadTrackUri = location
                baritoneTrackUri = location
            }
        ScreenTestSupport.cacheOnDisk(tag)
        val created =
            ScreenTestSupport.build(
                TagDetailActivity::class.java,
                Intent(RuntimeEnvironment.getApplication(), TagDetailActivity::class.java)
                    .putExtra(TagDetailActivity.TAG_ID_EXTRA, tag.id),
            )
        controller = created
        created.setup()
        ScreenTestSupport.awaitTagLoaded(created.get())

        created.get().findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
            .setCurrentItem(2, false)
        idle()

        val bass = scrollTo(created.get().findViewById(R.id.bassButton)) as android.widget.RadioButton
        bass.performClick()
        idle()

        assertTrue("the bass part ends up selected", bass.isChecked)
        assertEquals(
            "and its track reaches the player",
            location.uri,
            created.get().findViewById<MediaPlayerView>(R.id.mediaPlayer).remoteLocation.uri,
        )
    }

    @Test
    fun searchPreservesQueryAndFilterAfterRecreation() {
        val created = ScreenTestSupport.build(TagSearchActivity::class.java)
        controller = created
        created.setup()
        idle()
        created.get().findViewById<android.widget.EditText>(R.id.searchTextBox).setText("love")
        idle()
        chooseDropdown(created.get(), R.id.sortBySpinner, R.array.SortByChoices, "Rating")
        assertEquals(TagSortOptions.Rating, created.get().model.sortBy)

        created.recreate()
        idle()

        val restored = created.get()
        assertEquals("love", restored.model.query)
        assertEquals(TagSortOptions.Rating, restored.model.sortBy)
        assertEquals(
            "the dropdown shows the restored choice, not a default",
            restored.resources.getStringArray(R.array.SortByChoices)[3],
            restored.findViewById<MaterialAutoCompleteTextView>(R.id.sortBySpinner).text.toString(),
        )
        assertTrue("the toolbar comes back with it", restored.findViewById<View>(R.id.toolbar).isShown)
    }

    @Test
    fun homeOffersTeachableTagsAndRejectsAnOutOfRangeTagId() {
        val created = ScreenTestSupport.build(MeActivity::class.java)
        controller = created
        created.setup()
        idle()
        ScreenTestSupport.dismissChangelog()
        val activity = created.get()

        val list = activity.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.homeList)
        list.scrollToPosition(0)
        idle()
        val header = list.findViewHolderForAdapterPosition(0)!!.itemView
        assertDisplayed("teachableButton", header.findViewById(R.id.teachableButton))

        header.findViewById<View>(R.id.openByIdButton).performClick()
        idle()
        val dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog() as AlertDialog
        assertTrue("the Open Tag dialog opens", dialog.isShowing)
        val input = dialog.findViewById<android.widget.EditText>(R.id.openTagIdInput)!!
        assertEquals("Open Tag starts empty", "", input.text.toString())

        // 2147483648 is one past Int.MAX_VALUE, so it is not a tag id at all.
        input.setText("2147483648")
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        idle()

        assertTrue("an invalid id keeps the dialog open", dialog.isShowing)
        assertEquals(
            activity.getString(R.string.home_invalid_tag_id),
            dialog.findViewById<TextInputLayout>(R.id.openTagIdLayout)!!.error.toString(),
        )
        assertNull("and opens nothing", shadowOf(activity).nextStartedActivity)

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        idle()
        assertFalse("cancel dismisses it", dialog.isShowing)
        assertFalse("and leaves Home open", activity.isFinishing)
    }

    @Test
    fun theTeachableListShowsItsEmptyState() {
        val created = ScreenTestSupport.build(TeachableTagsActivity::class.java)
        controller = created
        created.setup()
        idle()
        assertTrue("nothing is saved as teachable", created.get().teachableTags.isEmpty())
        assertDisplayed("teachableEmptyState", created.get().findViewById(R.id.teachableEmptyState))
    }

    @Test
    fun ratingCancelDoesNotSubmitAndAnExplicitChoiceDoes() {
        val created = ScreenTestSupport.build(TagSearchActivity::class.java)
        controller = created
        created.setup()
        idle()
        val activity = created.get()

        val popup = RatingsPopup(activity)
        popup.show()
        idle()
        val dialog = org.robolectric.shadows.ShadowDialog.getLatestDialog() as AlertDialog
        assertFalse(
            "submit stays disabled until a rating is chosen",
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled,
        )
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        idle()
        assertFalse("cancel closes the dialog", dialog.isShowing)
        assertNull("cancel never submits a rating", popup.rating)

        val second = RatingsPopup(activity)
        second.show()
        idle()
        val chosen = org.robolectric.shadows.ShadowDialog.getLatestDialog() as AlertDialog
        chosen.findViewById<RatingBar>(R.id.ratingBar1)!!.rating = 4f
        idle()
        assertTrue(
            "choosing a rating enables submit",
            chosen.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled,
        )
        chosen.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
        idle()
        assertEquals("the chosen rating is what gets submitted", 4, second.rating)
    }
}
