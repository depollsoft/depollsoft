package depollsoft.pitchperfect

import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import depollsoft.lib.activity.RichApplication
import depollsoft.pitchperfect.lib.Accidental
import depollsoft.pitchperfect.lib.Note
import depollsoft.pitchperfect.lib.sound.NoteSound
import depollsoft.pitchperfect.ui.DialogListMoreBelow
import depollsoft.pitchperfect.ui.DialogListMoreAbove
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** The settings screen, signed out, and the prompts it opens. */
@RunWith(RobolectricTestRunner::class)
@Config(application = RichApplication::class, qualifiers = "w411dp-h891dp")
class SettingsScreenTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val screens = ComposeScreens(compose)

    @Before
    fun setUp() {
        ScreenTestSupport.startFromFirstLaunch()
        ScreenTestSupport.ensureFirebaseApp()
    }

    @After
    fun tearDown() {
        screens.finish()
    }

    private fun settings(): SettingsActivity = screens.launch(SettingsActivity::class.java).get()

    private fun toggleState(tag: String): ToggleableState? =
        compose.onNodeWithTag(tag).fetchSemanticsNode().config.getOrNull(SemanticsProperties.ToggleableState)

    private fun tapScrolled(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo().performClick()
        screens.settle()
    }

    // ==================== Pitch pipe ====================

    @Test
    fun notesPlayUntilPressedAgainTogglesTheSetting() {
        settings()
        assertEquals(ToggleableState.Off, toggleState(TestTags.TOGGLE_NOTES))
        screens.click(TestTags.TOGGLE_NOTES)
        assertTrue(SettingsModel.toggleNotes)
        assertEquals(ToggleableState.On, toggleState(TestTags.TOGGLE_NOTES))
        screens.click(TestTags.TOGGLE_NOTES)
        assertFalse(SettingsModel.toggleNotes)
    }

    @Test
    fun keepTheScreenOnTogglesTheSetting() {
        settings()
        screens.click(TestTags.WAKE_LOCK)
        assertTrue(SettingsModel.wakeLock)
        assertEquals(ToggleableState.On, toggleState(TestTags.WAKE_LOCK))
        screens.click(TestTags.WAKE_LOCK)
        assertFalse(SettingsModel.wakeLock)
    }

    @Test
    fun theSwitchesShowTheSavedSettingsAfterRecreation() {
        val controller = screens.launch(SettingsActivity::class.java)
        screens.click(TestTags.TOGGLE_NOTES)
        screens.click(TestTags.WAKE_LOCK)
        controller.recreate()
        screens.settle()
        assertEquals(ToggleableState.On, toggleState(TestTags.TOGGLE_NOTES))
        assertEquals(ToggleableState.On, toggleState(TestTags.WAKE_LOCK))
    }

    @Test
    fun tuningStartsAtA440AndAChoiceRetunesTheNotes() {
        settings()
        compose.onNodeWithTag(TestTags.TUNING).assert(hasText("A4 = 440 Hz", substring = true))
        screens.click(TestTags.TUNING)
        compose.onNodeWithTag(TestTags.TUNING_CHOICE + 440).assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        tapScrolled(TestTags.TUNING_CHOICE + 442)

        assertEquals(442, SettingsModel.referencePitch)
        assertEquals(442.0, Note.findNote("A", Accidental.Natural, 4)!!.tunedFrequency, 1e-9)
        assertEquals("the stored A440 frequency is kept", 440.0, Note.findNote("A", Accidental.Natural, 4)!!.frequency, 1e-9)
        assertFalse("choosing closes the dialog", screens.exists(TestTags.TUNING_CHOICE + 442))
        compose.onNodeWithTag(TestTags.TUNING).assert(hasText("A4 = 442 Hz", substring = true))
    }

    @Test
    fun cancellingTheTuningDialogKeepsTheTuning() {
        SettingsModel.referencePitch = 432
        settings()
        screens.click(TestTags.TUNING)
        compose.onNodeWithTag(TestTags.TUNING_CHOICE + 432).assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        compose.onNodeWithText("CANCEL").performClick()
        screens.settle()
        assertEquals(432, SettingsModel.referencePitch)
        assertFalse(screens.exists(TestTags.TUNING_CHOICE + 432))
    }

    @Test
    fun aTuningSyncedFromAnotherDeviceShowsOnTheOpenScreen() {
        settings()
        // Stands in for the account's snapshot listener: nothing on this screen asks to redraw.
        SettingsModel.applyRemoteReferencePitch(443)
        screens.settle()
        compose.onNodeWithTag(TestTags.TUNING).assert(hasText("A4 = 443 Hz", substring = true))
    }

    @Test
    fun aSyncedTuningThisVersionCannotUseFallsBackToA440() {
        SettingsModel.referencePitch = 442
        SettingsModel.applyRemoteReferencePitch(1000)
        assertEquals(440, SettingsModel.referencePitch)
        assertEquals(440.0, Note.getReferencePitch(), 1e-9)
    }

    @Test
    fun anUncommonSyncedTuningIsOfferedAndSelected() {
        SettingsModel.applyRemoteReferencePitch(431)
        settings()
        screens.click(TestTags.TUNING)
        compose.onNodeWithTag(TestTags.TUNING_CHOICE + 431).assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        assertEquals(listOf(415, 430, 431, 432), SettingsModel.referencePitchChoices(431).take(4))
    }

    @Test
    fun anUnexpectedStoredTuningReadsAsA440() {
        SettingsModel.referencePitch = 1000
        assertEquals("out-of-range values are refused", 440, SettingsModel.referencePitch)
    }

    // ==================== Sound ====================

    /** Records what the notes play, so the preview can be checked without audio. */
    private class RecordingPlayer : Note.NotePlayer {
        val events = mutableListOf<String>()

        override fun play(n: Note) {
            events += "play ${n.frequency} in ${Note.getSound().id}"
        }

        override fun stop(n: Note) {
            events += "stop ${n.frequency}"
        }
    }

    @Test
    fun soundStartsAsThePitchPipeAndAChoiceVoicesTheNotes() {
        settings()
        compose.onNodeWithTag(TestTags.SOUND).assert(hasText("Pitch Perfect (Loud)", substring = true))
        screens.click(TestTags.SOUND)
        compose.onNodeWithTag(TestTags.SOUND_CHOICE + "pitchPipe").assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        tapScrolled(TestTags.SOUND_CHOICE + "choir")

        assertEquals(NoteSound.CHOIR, SettingsModel.noteSound)
        assertEquals(NoteSound.CHOIR, Note.getSound())
        assertFalse("choosing closes the dialog", screens.exists(TestTags.SOUND_CHOICE + "choir"))
        compose.onNodeWithTag(TestTags.SOUND).assert(hasText("Choir", substring = true))
    }

    @Test
    fun theSoundDialogListsTheOriginalVoiceThenSustainedThenWavesThenPluckedAndStruck() {
        SettingsModel.noteSound = NoteSound.SAWTOOTH
        settings()
        screens.click(TestTags.SOUND)
        compose.onNodeWithTag(TestTags.SOUND_CHOICE + "sawtooth").assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        fun top(tag: String) = compose.onNode(hasTestTag(tag) or hasText(tag)).fetchSemanticsNode().positionInRoot.y
        val tops =
            listOf(
                TestTags.SOUND_CHOICE + "pitchPipe",
                "SUSTAINED",
                TestTags.SOUND_CHOICE + "organ",
                TestTags.SOUND_CHOICE + "reedOrgan",
                TestTags.SOUND_CHOICE + "accordion",
                TestTags.SOUND_CHOICE + "harmonica",
                TestTags.SOUND_CHOICE + "flute",
                "WAVES",
                TestTags.SOUND_CHOICE + "sine",
                TestTags.SOUND_CHOICE + "sawtooth",
                "PLUCKED & STRUCK",
                TestTags.SOUND_CHOICE + "piano",
                TestTags.SOUND_CHOICE + "harp",
            ).map(::top)
        assertEquals("in the contract's order", tops.sorted(), tops)
        compose.onNodeWithText("INSTRUMENTS").assertDoesNotExist()
        compose.onNodeWithText("Reed Organ").assertExists()
        compose.onNodeWithText("Harmonica").assertExists()
    }

    /** Chooses a sound without letting the main looper run on, so the preview is still playing. */
    private fun chooseSound(id: String) {
        compose.onNodeWithTag(TestTags.SOUND_CHOICE + id).performScrollTo().performClick()
        compose.waitForIdle()
    }

    private fun waitMillis(millis: Long) = shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(millis))

    @Test
    fun choosingASoundPlaysC4ForASecond() {
        val player = RecordingPlayer()
        Note.setPlayer(player)
        settings()
        screens.click(TestTags.SOUND)
        chooseSound("piano")

        val c4 = Note.getC4().frequency
        assertEquals(listOf("play $c4 in piano"), player.events)
        assertFalse("the preview doesn't light C4 elsewhere", Note.getC4().isPlaying)
        waitMillis(SoundPreview.DURATION_MS - 100)
        assertEquals(1, player.events.size)
        waitMillis(200)
        // Note.stop reaches the player twice (once more as its playing state clears).
        assertEquals(listOf("play $c4 in piano", "stop $c4"), player.events.distinct())
    }

    @Test
    fun leavingSettingsStopsThePreview() {
        val player = RecordingPlayer()
        Note.setPlayer(player)
        val controller = screens.launch(SettingsActivity::class.java)
        screens.click(TestTags.SOUND)
        chooseSound("organ")
        controller.pause()
        val c4 = Note.getC4().frequency
        assertEquals(listOf("play $c4 in organ", "stop $c4"), player.events.distinct())
        val count = player.events.size
        waitMillis(SoundPreview.DURATION_MS)
        assertEquals("nothing more once stopped", count, player.events.size)
    }

    @Test
    fun cancellingTheSoundDialogKeepsTheSoundAndPlaysNothing() {
        val player = RecordingPlayer()
        Note.setPlayer(player)
        SettingsModel.noteSound = NoteSound.TRUMPET
        settings()
        screens.click(TestTags.SOUND)
        compose.onNodeWithText("CANCEL").performClick()
        screens.settle()
        assertEquals(NoteSound.TRUMPET, SettingsModel.noteSound)
        assertTrue(player.events.isEmpty())
    }

    @Test
    fun aSoundSyncedFromAnotherDeviceShowsOnTheOpenScreen() {
        settings()
        SettingsModel.applyRemoteNoteSound("vibraphone")
        screens.settle()
        compose.onNodeWithTag(TestTags.SOUND).assert(hasText("Vibraphone", substring = true))
        assertEquals(NoteSound.VIBRAPHONE, Note.getSound())
    }

    @Test
    fun aSyncedSoundThisVersionDoesntKnowPlaysThePitchPipe() {
        SettingsModel.noteSound = NoteSound.GUITAR
        SettingsModel.applyRemoteNoteSound("theremin")
        assertEquals(NoteSound.PITCH_PIPE, SettingsModel.noteSound)
        assertEquals(NoteSound.PITCH_PIPE, Note.getSound())
    }

    @Test
    fun anUnknownStoredSoundReadsAsThePitchPipe() {
        depollsoft.lib.util.Preferences.set("depollsoft.pitchperfect.NoteSound", "bagpipes")
        assertEquals(NoteSound.PITCH_PIPE, SettingsModel.noteSound)
    }

    @Test
    fun theSoundSurvivesARestart() {
        SettingsModel.noteSound = NoteSound.CLARINET
        Note.setSound(NoteSound.PITCH_PIPE)
        SettingsModel.applyNoteSound()
        assertEquals(NoteSound.CLARINET, Note.getSound())
    }

    @Test
    fun clearAllSongsAsksFirstThenEmptiesEveryList() {
        SongsModel.get().defaultSongList.addSong(ComposeScreens.song("Blue Skies"))
        val other = SongsModel.get().createList("Saturday show")
        settings()
        screens.click(TestTags.CLEAR_SONGS)
        compose.onNodeWithText("NO").performClick()
        screens.settle()
        assertEquals(1, SongsModel.get().defaultSongList.songs.size)

        screens.click(TestTags.CLEAR_SONGS)
        compose.onNodeWithText("YES").performClick()
        screens.settle()
        assertTrue(SongsModel.get().defaultSongList.songs.isEmpty())
        assertFalse(SongsModel.get().songLists.containsKey(other))
    }

    // ==================== Account ====================

    @Test
    fun signedOutTheAccountSectionOffersLoginOnly() {
        settings()
        compose.onNodeWithTag(TestTags.LOG_IN).performScrollTo().assertIsDisplayed()
        assertFalse(screens.exists(TestTags.LOG_OUT))
        assertFalse(screens.exists(TestTags.DELETE_ACCOUNT))
    }

    @Test
    fun logInOpensTheSignInPromptAndNotNowClosesIt() {
        settings()
        tapScrolled(TestTags.LOG_IN)
        compose.onNodeWithText("Log in to Pitch Perfect").assertIsDisplayed()
        compose.onNodeWithTag(TestTags.LOGIN_BUTTON).assertIsEnabled()

        compose.onNodeWithText("NOT NOW").performClick()
        screens.settle()
        compose.onNodeWithText("Log in to Pitch Perfect").assertDoesNotExist()
    }

    @Test
    fun theSignInButtonOpensFirebaseUiAndShowsItIsBusy() {
        // FirebaseUI reads its configuration from the context the real app hands it at startup.
        com.firebase.ui.auth.AuthUI.setApplicationContext(RichApplication.getAppContext())
        val activity = settings()
        tapScrolled(TestTags.LOG_IN)
        compose.onNodeWithTag(TestTags.LOGIN_BUTTON).performClick()
        screens.settle()
        val started = shadowOf(activity).nextStartedActivityForResult
        assertTrue(
            "the provider choice is FirebaseUI's",
            started.intent.component!!.className.startsWith("com.firebase.ui.auth"),
        )
        compose.onNodeWithTag(TestTags.LOGIN_BUTTON).assertIsNotEnabled()
        // "Opening sign-in" is announced outright on the press; a live region would repeat it.
        val opening = compose.onAllNodesWithText(activity.getString(R.string.OpeningSignIn)).fetchSemanticsNodes()
        assertTrue(opening.isNotEmpty())
        assertTrue(opening.none { SemanticsProperties.LiveRegion in it.config })

        // A prompt opened again starts fresh, not stuck on the last attempt.
        compose.onNodeWithText("NOT NOW").performClick()
        screens.settle()
        tapScrolled(TestTags.LOG_IN)
        compose.onNodeWithTag(TestTags.LOGIN_BUTTON).assertIsEnabled()
    }

    @Test
    @Config(qualifiers = "w640dp-h320dp-land")
    fun onAShortLandscapeScreenTheDialogsKeepTheirButtonsOnScreen() {
        val activity = settings()
        tapScrolled(TestTags.CHANGELOG)
        compose.onNodeWithText(activity.getString(android.R.string.ok)).assertIsDisplayed()
        compose.onNodeWithText(activity.getString(android.R.string.ok)).performClick()
        screens.settle()

        tapScrolled(TestTags.LOG_IN)
        compose.onNodeWithText("NOT NOW").assertIsDisplayed()
        compose.onNodeWithTag(TestTags.LOGIN_BUTTON).performScrollTo().assertIsDisplayed()
    }

    /**
     * Opens each list dialog and scrolls to its last choice: the choice comes into view, the
     * Cancel button stays on screen the whole time, and choosing it works. Then the settings
     * screen itself scrolls to its last row.
     */
    private fun everyListScrollsToItsEnd() {
        val activity = settings()
        val cancel = activity.getString(android.R.string.cancel)

        screens.click(TestTags.SOUND)
        compose.onNodeWithText(cancel, ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithTag(TestTags.SOUND_CHOICE + "harp").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(cancel, ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithTag(TestTags.SOUND_CHOICE + "pitchPipe").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag(TestTags.SOUND_CHOICE + "harp").performScrollTo().performClick()
        screens.settle()
        assertEquals(NoteSound.HARP, SettingsModel.noteSound)
        Note.setSound(NoteSound.DEFAULT)

        tapScrolled(TestTags.TUNING)
        compose.onNodeWithText(cancel, ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithTag(TestTags.TUNING_CHOICE + 446).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(cancel, ignoreCase = true).assertIsDisplayed()
        compose.onNodeWithTag(TestTags.TUNING_CHOICE + 446).performClick()
        screens.settle()
        assertEquals(446, SettingsModel.referencePitch)
        SettingsModel.referencePitch = 440

        compose.onNodeWithTag(TestTags.PRIVACY_CHOICES).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun onATallPhoneEveryListScrollsToItsEnd() = everyListScrollsToItsEnd()

    /** Where [tag]'s node sits on screen now. */
    private fun top(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.y

    /** The dialog card's trailing edge in the dialog window: its button bar ends 12dp past Cancel. */
    private fun cardRight(): Float {
        val cancel = compose.onNodeWithText(RuntimeEnvironment.getApplication().getString(android.R.string.cancel), ignoreCase = true)
        val node = cancel.fetchSemanticsNode()
        return node.boundsInRoot.right + with(node.layoutInfo.density) { 12.dp.toPx() }
    }

    /**
     * Swipes up [list] with the finger 8dp in from the dialog card's trailing edge, clear of every
     * label (the user found that only a swipe starting on the text scrolled), and checks [first]
     * moved up.
     */
    private fun aSwipeBesideTheLabelsScrolls(
        list: String,
        first: String,
    ) {
        val before = top(first)
        val edge = cardRight()
        val listNode = compose.onNodeWithTag(list)
        val left = listNode.fetchSemanticsNode().boundsInRoot.left
        listNode.performTouchInput {
            val x = edge - left - 8.dp.toPx()
            swipe(Offset(x, bottom - 16.dp.toPx()), Offset(x, top + 16.dp.toPx()), durationMillis = 300)
        }
        screens.settle()
        assertTrue("$first moved from $before to ${top(first)}", top(first) < before - 50)
    }

    /** Taps [choice]'s row 28dp in from the card's trailing edge: past its label, inside its row. */
    private fun tapBesideTheLabel(choice: String) {
        val edge = cardRight()
        val row = compose.onNodeWithTag(choice)
        val left = row.fetchSemanticsNode().boundsInRoot.left
        row.performTouchInput { click(Offset(edge - left - 28.dp.toPx(), center.y)) }
        screens.settle()
    }

    @Test
    @Config(qualifiers = "w320dp-h480dp")
    fun theSoundListScrollsFromASwipeBesideItsLabels() {
        settings()
        screens.click(TestTags.SOUND)
        aSwipeBesideTheLabelsScrolls(TestTags.SOUND_LIST, TestTags.SOUND_CHOICE + "pitchPipe")
    }

    @Test
    @Config(qualifiers = "w320dp-h480dp")
    fun theTuningListScrollsFromASwipeBesideItsLabels() {
        settings()
        tapScrolled(TestTags.TUNING)
        aSwipeBesideTheLabelsScrolls(TestTags.TUNING_LIST, TestTags.TUNING_CHOICE + 415)
    }

    @Test
    fun aTapBesideAChoicesLabelChoosesIt() {
        settings()
        screens.click(TestTags.SOUND)
        tapBesideTheLabel(TestTags.SOUND_CHOICE + "organ")
        assertEquals(NoteSound.ORGAN, SettingsModel.noteSound)
        Note.setSound(NoteSound.DEFAULT)

        tapScrolled(TestTags.TUNING)
        tapBesideTheLabel(TestTags.TUNING_CHOICE + 432)
        assertEquals(432, SettingsModel.referencePitch)
    }

    /** Which edges of [list] show there is more past them: (above, below). */
    private fun edges(list: String): Pair<Boolean?, Boolean?> {
        val config = compose.onNodeWithTag(list).fetchSemanticsNode().config
        return config.getOrNull(DialogListMoreAbove) to config.getOrNull(DialogListMoreBelow)
    }

    @Test
    @Config(qualifiers = "w320dp-h480dp")
    fun aListTallerThanTheDialogMarksTheEdgesWithMorePastThem() {
        settings()
        screens.click(TestTags.SOUND)
        assertEquals(false to true, edges(TestTags.SOUND_LIST))
        compose.onNodeWithTag(TestTags.SOUND_CHOICE + "harp").performScrollTo()
        screens.settle()
        assertEquals(true to false, edges(TestTags.SOUND_LIST))
        compose.onNodeWithTag(TestTags.SOUND_CHOICE + "strings").performScrollTo()
        screens.settle()
        assertEquals(true to true, edges(TestTags.SOUND_LIST))
    }

    @Test
    fun aListThatFitsMarksNoEdgesAndDoesNotScroll() {
        settings()
        tapScrolled(TestTags.TUNING)
        assertEquals(false to false, edges(TestTags.TUNING_LIST))
        val range = compose.onNodeWithTag(TestTags.TUNING_LIST).fetchSemanticsNode().config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)
        assertEquals(0f, range?.maxValue?.invoke() ?: 0f)
    }

    /** The list's bottom edge cuts through a row, well clear of its top and bottom, so the cut shows. */
    private fun theListEndsPartwayThroughARow() {
        settings()
        screens.click(TestTags.SOUND)
        val list = compose.onNodeWithTag(TestTags.SOUND_LIST).fetchSemanticsNode()
        val fold = list.positionInRoot.y + list.size.height
        val margin = with(list.layoutInfo.density) { 8.dp.toPx() }
        val cut =
            NoteSound.entries.map { compose.onNodeWithTag(TestTags.SOUND_CHOICE + it.id).fetchSemanticsNode() }.firstOrNull {
                it.positionInRoot.y + margin < fold && it.positionInRoot.y + it.size.height - margin > fold
            }
        assertTrue("no row is cut at the fold ($fold)", cut != null)
    }

    @Test
    @Config(qualifiers = "w320dp-h480dp")
    fun onASmallPhoneTheSoundListEndsPartwayThroughARow() = theListEndsPartwayThroughARow()

    @Test
    fun onATallPhoneTheSoundListEndsPartwayThroughARow() = theListEndsPartwayThroughARow()

    @Test
    @Config(qualifiers = "w640dp-h320dp-land")
    fun inLandscapeTheSoundListEndsPartwayThroughARow() = theListEndsPartwayThroughARow()

    @Test
    @Config(qualifiers = "w320dp-h480dp")
    fun onASmallPhoneEveryListScrollsToItsEnd() = everyListScrollsToItsEnd()

    @Test
    @Config(qualifiers = "w640dp-h320dp-land")
    fun onAShortLandscapeScreenEveryListScrollsToItsEnd() = everyListScrollsToItsEnd()

    @Test
    @Config(qualifiers = "w320dp-h480dp", fontScale = 2f)
    fun atDoubleTextSizeEveryListScrollsToItsEnd() = everyListScrollsToItsEnd()

    @Test
    fun aSignInResultArrivingAfterRecreationReachesThePrompt() {
        com.firebase.ui.auth.AuthUI.setApplicationContext(RichApplication.getAppContext())
        val controller = screens.launch(SettingsActivity::class.java)
        tapScrolled(TestTags.LOG_IN)
        compose.onNodeWithTag(TestTags.LOGIN_BUTTON).performClick()
        screens.settle()
        val started = shadowOf(controller.get()).nextStartedActivityForResult

        // The phone rotates while FirebaseUI is up; its result comes back to the new activity.
        controller.recreate()
        screens.settle()
        controller.get().activityResultRegistry.dispatchResult(started.requestCode, android.app.Activity.RESULT_CANCELED, null)
        screens.settle()

        compose
            .onNodeWithText(controller.get().getString(R.string.SignInCanceled))
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        compose.onNodeWithTag(TestTags.LOGIN_BUTTON).assertIsEnabled()
    }

    @Test
    fun theSignInPromptUsesFirebaseUisMethodPicker() {
        assertFalse(LoginPrompt.CREDENTIAL_MANAGER_ENABLED)
        assertTrue(LoginPrompt.ALWAYS_SHOW_PROVIDER_CHOICE)
        assertEquals(setOf("password", "google.com", "facebook.com"), LoginPrompt.PROVIDER_IDS)
    }

    // ==================== Appearance ====================

    private fun selected(tag: String): Boolean = screens.isSelected(tag)

    @Test
    fun theThemeChoicesAreExclusiveAndApplyTheMode() {
        settings()
        assertTrue(selected(TestTags.THEME_SYSTEM))
        for ((tag, mode) in listOf(
            TestTags.THEME_LIGHT to AppCompatDelegate.MODE_NIGHT_NO,
            TestTags.THEME_DARK to AppCompatDelegate.MODE_NIGHT_YES,
            TestTags.THEME_SYSTEM to AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        )) {
            tapScrolled(tag)
            assertEquals(mode, PitchPerfectApplication.themeMode)
            assertEquals(mode, AppCompatDelegate.getDefaultNightMode())
            listOf(TestTags.THEME_SYSTEM, TestTags.THEME_LIGHT, TestTags.THEME_DARK).forEach {
                assertEquals("$it after choosing $tag", it == tag, selected(it))
            }
        }
    }

    @Test
    fun theThemeChoiceSurvivesRecreation() {
        val controller = screens.launch(SettingsActivity::class.java)
        tapScrolled(TestTags.THEME_DARK)
        controller.recreate()
        screens.settle()
        assertTrue(selected(TestTags.THEME_DARK))
    }

    // ==================== Subscription and about ====================

    @Test
    fun manageSubscriptionShowsOnlyOnceAdsAreRemoved() {
        settings()
        assertFalse(screens.exists(TestTags.MANAGE_SUBSCRIPTION))
        screens.finish()

        SettingsModel.areAdsRemoved = true
        val activity = settings()
        tapScrolled(TestTags.MANAGE_SUBSCRIPTION)
        val opened = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, opened.action)
        assertTrue(opened.data.toString().startsWith("https://play.google.com/store/account/subscriptions"))
    }

    @Test
    fun theChangelogOpensInADialog() {
        settings()
        tapScrolled(TestTags.CHANGELOG)
        compose.onNodeWithText("Pitch Perfect Changelog").assertIsDisplayed()
        compose.onNodeWithText("OK").performClick()
        screens.settle()
        compose.onNodeWithText("Pitch Perfect Changelog").assertDoesNotExist()
    }

    @Test
    fun theAboutLinksOpenTheirPages() {
        val activity = settings()
        for ((label, url) in listOf(
            "apps.depoll.com" to "http://apps.depoll.com",
            "Terms of Use" to "http://apps.depoll.com/terms-of-use",
            "DepollSoft" to "http://apps.depoll.com",
        )) {
            compose.onNodeWithText(label).performScrollTo().performClick()
            screens.settle()
            assertEquals(url, shadowOf(activity).nextStartedActivity.data.toString())
        }
        compose.onNodeWithText("Pitch Perfect · Version", substring = true).assertDoesNotExist()
    }

    @Test
    fun privacyChoicesOpensTheConsentPrompt() {
        settings()
        tapScrolled(TestTags.PRIVACY_CHOICES)
        // The consent prompt is the shared library's own dialog.
        assertTrue(org.robolectric.shadows.ShadowDialog.getLatestDialog().isShowing)
    }
}
