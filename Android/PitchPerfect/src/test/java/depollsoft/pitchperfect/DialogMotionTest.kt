package depollsoft.pitchperfect

import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import depollsoft.lib.activity.RichApplication
import depollsoft.pitchperfect.ui.DialogButton
import depollsoft.pitchperfect.ui.PlateAlertDialog
import depollsoft.pitchperfect.ui.PlateOutlinedField
import depollsoft.pitchperfect.ui.PlateTheme
import depollsoft.pitchperfect.ui.plateText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

/** How the plate dialogs and their text fields move: in, out, around the keyboard. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], application = RichApplication::class, qualifiers = "w411dp-h891dp-xxhdpi")
class DialogMotionTest {
    @get:Rule
    val compose = createComposeRule()

    private var dismissed = 0

    private fun showDialog(field: Boolean = false) {
        compose.setContent {
            PlateTheme {
                PlateAlertDialog(
                    onDismissRequest = { dismissed++ },
                    title = "Delete “Afterglow”?",
                    message = if (field) null else "The list's songs go with it.",
                    buttons = listOf(DialogButton("Cancel", {}), DialogButton("Delete", {})),
                ) {
                    if (field) {
                        var value by androidx.compose.runtime.remember { mutableStateOf(TextFieldValue("")) }
                        PlateOutlinedField(
                            value,
                            { value = it },
                            label = "Name",
                            textStyle = plateText(20.sp),
                            fieldModifier = Modifier.testTag("field"),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    private fun dialogWindow() = ShadowDialog.getLatestDialog().window!!

    @Test
    fun theCardFadesInAsTheDialogOpens() {
        compose.mainClock.autoAdvance = false
        showDialog()
        compose.mainClock.advanceTimeByFrame()
        val first = opaqueShare()
        compose.mainClock.advanceTimeBy(300)
        val settled = opaqueShare()
        assertTrue("barely there at first ($first), solid once open ($settled)", first < 0.5f && settled > 0.9f)
        compose.mainClock.autoAdvance = true
    }

    /** How much of the dialog's card area is drawn fully opaque. */
    private fun opaqueShare(): Float {
        val pixels = compose.onNode(isDialog()).captureToImage().toPixelMap()
        var opaque = 0
        var total = 0
        for (y in pixels.height / 3 until pixels.height * 2 / 3 step 4) {
            for (x in pixels.width / 4 until pixels.width * 3 / 4 step 4) {
                total++
                if (pixels[x, y].alpha > 0.99f) opaque++
            }
        }
        return opaque / total.toFloat()
    }

    @Test
    fun theWindowFadesOutHoweverItCloses() {
        showDialog()
        compose.waitForIdle()
        assertEquals(R.style.Animation_Plate_Dialog, dialogWindow().attributes.windowAnimations)
    }

    @Test
    fun aTapInTheBandAroundTheCardDismissesItButATapOnTheCardDoesNot() {
        showDialog()
        compose.waitForIdle()
        compose.onNodeWithText("The list's songs go with it.").performClick()
        assertEquals(0, dismissed)
        compose.onNode(isDialog()).performTouchInput { click(androidx.compose.ui.geometry.Offset(centerX, 4f)) }
        compose.waitForIdle()
        assertEquals("the inset band is outside the dialog", 1, dismissed)
    }

    @Test
    fun theCardMovesAboveTheKeyboard() {
        showDialog(field = true)
        compose.waitForIdle()
        val before = compose.onNode(isDialog()).fetchSemanticsNode().size.height
        val ime = 900
        compose.runOnIdle {
            val decor = dialogWindow().decorView
            val insets =
                WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, ime))
                    .setVisible(WindowInsetsCompat.Type.ime(), true)
                    .build()
            ViewCompat.dispatchApplyWindowInsets(decor, insets)
        }
        compose.waitForIdle()
        val after = compose.onNode(isDialog()).fetchSemanticsNode().size.height
        assertEquals("the keyboard's height is kept free below the card", before + ime, after)
    }

    @Test
    fun theNameFieldAsksForNoAutocorrect() {
        showDialog(field = true)
        compose.onNodeWithTag("field").performClick()
        compose.waitForIdle()
        val info = EditorInfo()
        compose.runOnIdle { composeViewOf(dialogWindow().decorView)!!.onCreateInputConnection(info) }
        assertEquals(InputType.TYPE_TEXT_FLAG_CAP_WORDS, info.inputType and InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        assertEquals("no autocorrect", 0, info.inputType and InputType.TYPE_TEXT_FLAG_AUTO_CORRECT)
    }

    private fun composeViewOf(view: View): View? {
        if (view.javaClass.name.endsWith("AndroidComposeView")) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) composeViewOf(view.getChildAt(i))?.let { return it }
        return null
    }

    @Test
    fun theLabelRestsInsideTheEmptyBoxAndFloatsUpOnFocus() {
        showDialog(field = true)
        compose.waitForIdle()
        val field = compose.onNodeWithTag("field").fetchSemanticsNode().boundsInRoot
        val resting = labelBounds()
        // The tagged node is the text line; resting, the label sits on it where the text will be.
        assertEquals("resting on the text line: $resting in $field", field.center.y, resting.center.y, 2f)
        compose.onNodeWithTag("field").performClick()
        compose.waitForIdle()
        val floated = labelBounds()
        assertTrue("floated onto the outline", floated.bottom < field.top)
        assertTrue("and smaller", floated.height < resting.height)
    }

    private fun labelBounds() =
        compose.onAllNodesWithText("Name", useUnmergedTree = true).fetchSemanticsNodes()
            .first { !it.config.contains(androidx.compose.ui.semantics.SemanticsProperties.EditableText) }.boundsInRoot

    @Test
    fun anErrorFadesInOverTheHelper() {
        var error by mutableStateOf<String?>(null)
        compose.setContent {
            PlateTheme {
                PlateOutlinedField(
                    TextFieldValue(""),
                    {},
                    label = "Name",
                    textStyle = plateText(20.sp),
                    helper = "Name your list",
                    error = error,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        error = "That name is taken"
        androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        assertTrue("both lines are on screen mid-change: " + texts(), exists("Name your list") && exists("That name is taken"))
        compose.mainClock.advanceTimeBy(400)
        assertFalse(exists("Name your list"))
        assertTrue(exists("That name is taken"))
        compose.mainClock.autoAdvance = true
    }

    private fun texts() =
        compose.onAllNodes(androidx.compose.ui.test.isRoot().not(), useUnmergedTree = true).fetchSemanticsNodes()
            .mapNotNull { node -> node.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.Text) { null } }

    private fun exists(text: String) = compose.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
}
