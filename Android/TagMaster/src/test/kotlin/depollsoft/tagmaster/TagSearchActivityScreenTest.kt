package depollsoft.tagmaster

import android.app.Application
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import depollsoft.lib.json.JsonSerializer
import depollsoft.tagmaster.barbershop.TagCollection
import depollsoft.tagmaster.barbershop.TagSortOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The search form: every field reaches the query the results screen runs, and survives recreation. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class TagSearchActivityScreenTest : ComposeScreenTest() {
    private fun search(): TagSearchActivity = launch(TagSearchActivity::class.java)

    private fun choose(
        field: String,
        index: Int,
    ) {
        click(field)
        node("$field:$index").performSemanticsAction(SemanticsActions.OnClick)
        idle()
    }

    private fun submitted(activity: TagSearchActivity): QueryModel {
        val started = nextStarted(activity)!!
        assertEquals(TagSearchResultsActivity::class.java.name, started.component?.className)
        return JsonSerializer.deserialize(org.json.JSONObject(started.getStringExtra(TagSearchResultsActivity.QUERY_MODEL)!!)) as QueryModel
    }

    @Test
    fun theFormOpensSortedByTitle() {
        val activity = search()
        assertEquals(TagSortOptions.Title, activity.model.sortBy)
        assertTrue(text("sortBySpinner").startsWith(app.resources.getStringArray(R.array.SortByChoices)[0]))
    }

    @Test
    fun theSearchButtonSubmitsTheQuery() {
        val activity = search()
        node("searchTextBox").performTextInput("heart")
        click("searchButton")
        assertEquals("heart", submitted(activity).query)
    }

    @Test
    fun theKeyboardSearchActionSubmitsTheQuery() {
        val activity = search()
        node("searchTextBox").performTextInput("sunshine")
        node("searchTextBox").performImeAction()
        idle()
        assertEquals("sunshine", submitted(activity).query)
    }

    @Test
    fun everyFilterReachesTheResultsScreen() {
        val activity = search()
        choose("sortBySpinner", 3)
        choose("sheetMusicSpinner", 1)
        choose("learningTracksSpinner", 2)
        choose("partsSpinner", 2)
        choose("tagCollectionSpinner", 1)
        assertTrue(text("tagCollectionSpinner").startsWith(app.resources.getStringArray(R.array.TagCollectionChoices)[1]))
        click("searchButton")
        val query = submitted(activity)
        assertEquals(TagSortOptions.Rating, query.sortBy)
        assertEquals(true, query.hasSheetMusic)
        assertEquals(false, query.hasLearningTracks)
        assertEquals(4, query.parts)
        assertEquals(TagCollection.ClassicTags, query.collection)
    }

    @Test
    fun repeatedSelectionsUseTheLatestValue() {
        val activity = search()
        choose("tagCollectionSpinner", 1)
        choose("tagCollectionSpinner", 2)
        assertEquals(TagCollection.EasyTags, activity.model.collection)
    }

    @Test
    fun theQueryAndFiltersSurviveRecreation() {
        search()
        node("searchTextBox").performTextInput("heart")
        choose("tagCollectionSpinner", 2)
        val recreated = recreate<TagSearchActivity>()
        assertEquals("heart", recreated.model.query)
        assertEquals(TagCollection.EasyTags, recreated.model.collection)
        assertTrue(text("searchTextBox").startsWith("heart"))
    }
}
