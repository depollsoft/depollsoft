package depollsoft.tagmaster

import android.app.Application
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasText
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

/**
 * Form controls give the feedback their Material Views did: dialogs animate in and out, an open
 * dropdown shows as focused and marks the current choice across the field's width, the search
 * clear icon shows only while the field is in use, the list-name counter warns past its limit,
 * and the Settings switch is a switch to a screen reader.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w411dp-h891dp-xxhdpi")
class FormFeedbackScreenTest : ComposeScreenTest() {
    @Test
    fun dialogsUseMaterialsWindowAnimation() {
        launch(MeActivity::class.java)
        click("newListButton")
        val window = ShadowDialog.getLatestDialog().window!!
        assertEquals(com.google.android.material.R.style.MaterialAlertDialog_Material3_Animation, window.attributes.windowAnimations)
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp-xxhdpi")
    fun aDialogInLandscapeStopsAtAppCompatsMinimumWidth() {
        launch(MeActivity::class.java)
        click("newListButton")
        val density = app.resources.displayMetrics.density
        val width = compose.onNode(androidx.compose.ui.test.hasTestTag("dialog")).fetchSemanticsNode().size.width / density
        // AppCompat's landscape minimum on a phone is 65% of the screen; the dialog no longer fills it.
        assertTrue("dialog ${width}dp wide", width <= 891 * 0.65f - 48 + 1)
    }

    @Test
    fun anOpenDropdownMarksTheCurrentChoiceAcrossTheFieldsWidth() {
        launch(TagSearchActivity::class.java)
        val fieldWidth = node("sortBySpinner").fetchSemanticsNode().size.width
        click("sortBySpinner")
        val current = node("sortBySpinner:0").fetchSemanticsNode()
        assertEquals(true, current.config.getOrNull(SemanticsProperties.Selected))
        assertEquals(false, node("sortBySpinner:1").fetchSemanticsNode().config.getOrNull(SemanticsProperties.Selected))
        assertEquals("the list is as wide as the field", fieldWidth.toFloat(), current.size.width.toFloat(), fieldWidth * 0.02f)
    }

    @Test
    fun theClearIconShowsOnlyWhileTheFieldIsInUse() {
        launch(TagSearchActivity::class.java)
        val clear = app.getString(com.google.android.material.R.string.clear_text_end_icon_content_description)

        fun shown() = compose.onAllNodes(hasContentDescription(clear), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        assertFalse(shown())
        node("searchTextBox").performTextInput("heart")
        idle()
        assertTrue("focused with text", shown())
        // Tab reaches the clear icon itself, which stays so a keyboard user can press it.
        node("searchTextBox").performKeyInput { pressKey(Key.Tab) }
        idle()
        assertTrue("the icon has focus", shown())
        compose.onNode(hasContentDescription(clear), useUnmergedTree = true).assertIsFocused()
        // Focus moves on to the next control.
        compose.onNode(hasContentDescription(clear), useUnmergedTree = true).performKeyInput { pressKey(Key.Tab) }
        idle()
        assertFalse("text but no focus", shown())
    }

    @Test
    fun clearingTheSearchFromTheKeyboardPutsFocusBackInTheField() {
        launch(TagSearchActivity::class.java)
        val clear = app.getString(com.google.android.material.R.string.clear_text_end_icon_content_description)
        node("searchTextBox").performTextInput("heart")
        node("searchTextBox").performKeyInput { pressKey(Key.Tab) }
        idle()
        compose.onNode(hasContentDescription(clear), useUnmergedTree = true).performKeyInput { pressKey(Key.Enter) }
        idle()
        node("searchTextBox").assertIsFocused()
        assertEquals("", node("searchTextBox").fetchSemanticsNode().config.getOrNull(SemanticsProperties.EditableText)?.text)
    }

    @Test
    fun eachSearchDropdownIsNamedByItsLabelWithItsChoiceAsState() {
        launch(TagSearchActivity::class.java)
        val sheet = node("sheetMusicSpinner").fetchSemanticsNode().config
        assertEquals(listOf(app.getString(R.string.SheetMusicSentence)), sheet.getOrNull(SemanticsProperties.ContentDescription))
        assertEquals(app.resources.getStringArray(R.array.SheetMusicChoices)[0], sheet.getOrNull(SemanticsProperties.StateDescription))
        val sort = node("sortBySpinner").fetchSemanticsNode().config
        assertEquals(listOf(app.getString(R.string.SortBy)), sort.getOrNull(SemanticsProperties.ContentDescription))
    }

    @Test
    fun aFieldsErrorIsExposedAndAnnounced() {
        launch(MeActivity::class.java)
        click("openByIdButton")
        node("openTagIdInput").performTextInput("abc")
        click("openTagConfirm")
        val message = app.getString(R.string.home_invalid_tag_id)
        assertEquals(message, node("openTagIdInput").fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error))
        val shown = compose.onNode(hasText(message), useUnmergedTree = true).fetchSemanticsNode().config
        assertEquals(LiveRegionMode.Polite, shown.getOrNull(SemanticsProperties.LiveRegion))
    }

    @Test
    fun theListNameCounterWarnsPastItsLimit() {
        launch(MeActivity::class.java)
        click("newListButton")
        node("listNameInput").performTextInput("x".repeat(TagLists.MAX_NAME_LENGTH + 1))
        idle()
        val over = app.getString(com.google.android.material.R.string.character_counter_overflowed_content_description, TagLists.MAX_NAME_LENGTH + 1, TagLists.MAX_NAME_LENGTH)
        assertTrue(compose.onAllNodes(hasContentDescription(over), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun keepScreenOnIsASwitchToAScreenReader() {
        launch(SettingsActivity::class.java)
        val config = node("sheetMusicWakeLockCheckBox").fetchSemanticsNode().config
        assertEquals(androidx.compose.ui.semantics.Role.Switch, config.getOrNull(SemanticsProperties.Role))
        val before = config.getOrNull(SemanticsProperties.ToggleableState)
        assertTrue("toggleable, not selectable: $before", before == ToggleableState.On || before == ToggleableState.Off)
        click("sheetMusicWakeLockCheckBox")
        assertTrue(node("sheetMusicWakeLockCheckBox").fetchSemanticsNode().config.getOrNull(SemanticsProperties.ToggleableState) != before)
    }
}
