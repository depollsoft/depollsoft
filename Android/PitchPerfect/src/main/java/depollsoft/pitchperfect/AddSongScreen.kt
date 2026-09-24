package depollsoft.pitchperfect

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import depollsoft.pitchperfect.ui.LegacyText
import depollsoft.pitchperfect.ui.PlateText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import depollsoft.pitchperfect.lib.Key
import depollsoft.pitchperfect.lib.KeyType
import depollsoft.pitchperfect.lib.PitchedSong
import depollsoft.pitchperfect.ui.PlateBackground
import depollsoft.pitchperfect.ui.PlateFilledField
import depollsoft.pitchperfect.ui.PlateFonts
import depollsoft.pitchperfect.ui.PlateModeToggle
import depollsoft.pitchperfect.ui.PlatePrimaryButton
import depollsoft.pitchperfect.ui.PlateSectionHeader
import depollsoft.pitchperfect.ui.plateColors
import depollsoft.pitchperfect.ui.plateText
import kotlinx.coroutines.flow.first

/**
 * The song editor's state: a song being added to [list], or a copy of one being edited, so
 * nothing changes in the list until it is saved.
 */
@Stable
class SongEditorState(
    val list: SongList,
    songId: String?,
) {
    private val original: PitchedSong? = songId?.let { id -> list.songs.firstOrNull { it.id == id } }

    /** Whether an existing song is being edited, rather than a new one added. */
    val editing: Boolean get() = original != null

    var title by mutableStateOf(TextFieldValue(original?.name.orEmpty()))

    var key: Key by mutableStateOf(original?.key ?: Key.getMajorKeys()[Key.getMajorKeys().size / 2])
        private set

    /** Shown under the title after a save without one. */
    var titleMissing by mutableStateOf(false)
        private set

    val minor: Boolean get() = key.keyType == KeyType.Minor

    /** Choosing a key is silent; the pitch pipe and Keys tab are where notes sound. */
    fun choose(key: Key) {
        this.key = key
    }

    /** Keeps the same signature when the mode flips: a relative key shares it. */
    fun setMinor(minor: Boolean) {
        key = SongKeys.relative(key, minor)
    }

    fun titleChanged(value: TextFieldValue) {
        title = value
        if (titleMissing && value.text.isNotBlank()) titleMissing = false
    }

    /** What the editor holds so far, for [restore] after the activity is recreated. */
    fun save(into: android.os.Bundle) {
        into.putString(SAVED_TITLE, title.text)
        into.putInt(SAVED_SELECTION_START, title.selection.start)
        into.putInt(SAVED_SELECTION_END, title.selection.end)
        into.putBoolean(SAVED_MINOR, minor)
        into.putInt(SAVED_KEY, (if (minor) Key.getMinorKeys() else Key.getMajorKeys()).indexOf(key))
        into.putBoolean(SAVED_TITLE_MISSING, titleMissing)
    }

    fun restore(saved: android.os.Bundle) {
        val text = saved.getString(SAVED_TITLE) ?: return
        title =
            TextFieldValue(
                text,
                androidx.compose.ui.text.TextRange(saved.getInt(SAVED_SELECTION_START), saved.getInt(SAVED_SELECTION_END)),
            )
        (if (saved.getBoolean(SAVED_MINOR)) Key.getMinorKeys() else Key.getMajorKeys())
            .getOrNull(saved.getInt(SAVED_KEY))
            ?.let { key = it }
        titleMissing = saved.getBoolean(SAVED_TITLE_MISSING)
    }

    /** Saves the song. Returns false, and flags the title, when there is no title. */
    fun save(): Boolean {
        val name = title.text.trim()
        if (name.isEmpty()) {
            titleMissing = true
            return false
        }
        val existing = original
        if (existing != null) {
            existing.name = name
            existing.key = key
            list.notifyOfChange()
        } else {
            list.addSong(
                PitchedSong().also {
                    it.name = name
                    it.key = key
                },
            )
        }
        return true
    }

    /** Removes the song being edited; adding one has nothing to remove. */
    fun remove() {
        original?.let { list.removeSong(it) }
    }
}

/** The song editor: its title, its key's mode and signature, and Save Song. */
@Composable
fun AddSongScreen(
    state: SongEditorState,
    onSaved: () -> Unit,
    titleFocus: FocusRequester = remember { FocusRequester() },
) {
    val view = LocalView.current
    val colors = plateColors
    val save: () -> Unit = {
        if (state.save()) {
            view.performHapticFeedback(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CONTEXT_CLICK,
            )
            onSaved()
        } else {
            titleFocus.requestFocus()
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        }
    }
    PlateBackground {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp)) {
                PlateSectionHeader(stringResource(R.string.SongTitle))
                PlateFilledField(
                    state.title,
                    state::titleChanged,
                    hint = stringResource(R.string.SongTitleHint),
                    description = stringResource(R.string.SongTitle),
                    textStyle = plateText(24.sp, colors.ink, PlateFonts.condensed, letterSpacing = 0.009375f),
                    error = if (state.titleMissing) stringResource(R.string.SongTitleRequired) else null,
                    onDone = save,
                    focusRequester = titleFocus,
                    modifier = Modifier.padding(top = 2.dp).fillMaxWidth(),
                    fieldModifier = Modifier.testTag(TestTags.SONG_TITLE),
                )
                Row(
                    Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                    verticalAlignment = ViewCenterVertically,
                ) {
                    PlateSectionHeader(stringResource(R.string.SongKey), Modifier.weight(1f))
                    PlateModeToggle(
                        options =
                            listOf(
                                stringResource(R.string.major) to stringResource(R.string.KeyModeMajor),
                                stringResource(R.string.minor) to stringResource(R.string.KeyModeMinor),
                            ),
                        selected = if (state.minor) 1 else 0,
                        onSelect = { state.setMinor(it == 1) },
                        modifier = Modifier.testTag(TestTags.KEY_MODE),
                    )
                }
            }
            KeyPicker(state, Modifier.weight(1f))
            PlatePrimaryButton(
                stringResource(R.string.SaveSong),
                Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 16.dp).fillMaxWidth().testTag(TestTags.SAVE_SONG),
                onClick = save,
            )
        }
    }
}

/** The Keys tab's signature list for the chosen mode, the chosen key lit. */
@Composable
private fun KeyPicker(
    state: SongEditorState,
    modifier: Modifier,
) {
    val keys = SongKeys.keysOf(state.minor)
    val listState = rememberLazyListState()
    CenterChosenKey(listState, keys, state)
    LazyColumn(
        modifier.fillMaxWidth().testTag(TestTags.SONG_KEY_LIST),
        state = listState,
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        itemsIndexed(keys) { _, key ->
            KeyChoice(key, chosen = key == state.key) { state.choose(key) }
            Hairline()
        }
    }
}

/**
 * Brings the chosen key to the middle of the list when it first shows and after a mode switch:
 * the View scrolled it to half the list's height less half a 64dp row.
 */
@Composable
private fun CenterChosenKey(
    listState: LazyListState,
    keys: List<Key>,
    state: SongEditorState,
) {
    val rowHeight = with(LocalDensity.current) { 64.dp.roundToPx() }
    LaunchedEffect(state.minor) {
        val height = snapshotFlow { listState.layoutInfo.viewportSize.height }.first { it > 0 }
        val index = keys.indexOfFirst { it == state.key }
        if (index < 0) return@LaunchedEffect
        withFrameNanos { }
        listState.scrollToItem(index, -maxOf(0, height / 2 - rowHeight / 2))
    }
}

@Composable
private fun KeyChoice(
    key: Key,
    chosen: Boolean,
    onChoose: () -> Unit,
) {
    val colors = plateColors
    val view = LocalView.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // The row stays lit while it is the chosen key, and lights on press.
    val lit = chosen || pressed
    val ink = if (lit) colors.onAccent else colors.ink
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(if (lit) colors.accent else Color.Transparent)
            .clickable(interaction, indication = null, role = Role.RadioButton) {
                // A second tap on the chosen row leaves it chosen.
                if (!chosen) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onChoose()
                }
            }.semantics(mergeDescendants = true) {
                contentDescription = SongKeys.spokenName(key)
                selected = chosen
            },
        verticalAlignment = ViewCenterVertically,
    ) {
        LegacyText(NoteText.keySignature(key), 24.sp, ink, android.graphics.Typeface.DEFAULT, Modifier.weight(1f).padding(start = 20.dp), wrapWidth = true)
        LegacyText(NoteText.keyName(key), 22.sp, ink, PlateFonts.condensedTypeface, Modifier.padding(end = 20.dp), wrapWidth = true)
    }
}

private const val SAVED_TITLE = "title"
private const val SAVED_SELECTION_START = "selectionStart"
private const val SAVED_SELECTION_END = "selectionEnd"
private const val SAVED_MINOR = "minor"
private const val SAVED_KEY = "key"
private const val SAVED_TITLE_MISSING = "titleMissing"
