package depollsoft.pitchperfect

import depollsoft.compose.listViewScrollbar
import depollsoft.compose.ViewAlign
import depollsoft.pitchperfect.ui.hairlineWidth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import depollsoft.pitchperfect.ui.LegacyText
import depollsoft.pitchperfect.ui.PlateText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.LifecycleResumeEffect
import depollsoft.pitchperfect.lib.Key
import depollsoft.pitchperfect.ui.PlateExtendedFab
import depollsoft.pitchperfect.ui.PlateFonts
import depollsoft.pitchperfect.ui.plateColors
import depollsoft.pitchperfect.ui.plateText
import kotlinx.coroutines.launch

/**
 * The Keys tab: every major or minor key signature, each sounding its tonic while held, and a
 * switch between the two lists that keeps the same place.
 */
@Composable
fun KeySignatureScreen(
    isCurrentPage: Boolean,
    model: KeySignatureModel = remember { KeySignatureModel() },
) {
    val stopPlaying = {
        BriefNotes.cancelAll()
        model.majorKeys.forEach { it.note.stop() }
        model.minorKeys.forEach { it.note.stop() }
    }
    // The tab silences its notes as it comes up and as it goes, and its switch only shows while
    // it is the tab on screen.
    var resumed by remember { mutableStateOf(false) }
    // Every tab stays composed, and the notes are shared with the other tabs: only the tab on
    // screen silences them as the app comes back, as only the current fragment was resumed.
    val onScreen by rememberUpdatedState(isCurrentPage)
    LifecycleResumeEffect(model) {
        if (onScreen) stopPlaying()
        resumed = true
        onPauseOrDispose {
            resumed = false
            stopPlaying()
        }
    }
    OnPageVisibilityChange(isCurrentPage) { stopPlaying() }

    val major = rememberLazyListState()
    val minor = rememberLazyListState()
    CenterOnFirstLayout(major, model.majorKeys.size)
    CenterOnFirstLayout(minor, model.minorKeys.size)
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        KeyList(model.majorKeys, major, visible = model.isMajor, TestTags.MAJOR_KEYS)
        KeyList(model.minorKeys, minor, visible = !model.isMajor, TestTags.MINOR_KEYS)
        PlateExtendedFab(
            icon = if (model.isMajor) R.drawable.ic_major else R.drawable.ic_minor,
            text = stringResource(if (model.isMajor) R.string.major else R.string.minor),
            description = stringResource(if (model.isMajor) R.string.SwitchToMinorKeys else R.string.SwitchToMajorKeys),
            onClick = {
                model.isMajor = !model.isMajor
                // The other list opens at the row this one was showing.
                val (from, to) = if (model.isMajor) minor to major else major to minor
                scope.launch { to.scrollToItem(from.firstVisibleItemIndex) }
                stopPlaying()
            },
            modifier = Modifier.align(Alignment.BottomEnd).zIndex(2f).padding(16.dp).testTag(TestTags.MAJOR_MINOR_FAB),
            visible = isCurrentPage && resumed,
        )
    }
}

@Composable
private fun KeyList(
    keys: List<Key>,
    state: LazyListState,
    visible: Boolean,
    tag: String,
) {
    // Both lists stay laid out, as the two ListViews did, so each keeps its own place.
    LazyColumn(
        Modifier
            .fillMaxSize()
            // The shown list is on top, so it alone takes touches; the hidden one is also hidden
            // from screen readers.
            .zIndex(if (visible) 1f else 0f)
            .alpha(if (visible) 1f else 0f)
            .then(if (visible) Modifier else Modifier.clearAndSetSemantics {})
            .listViewScrollbar(state, bottom = 90.dp, divider = hairlineWidth)
            .testTag(tag),
        state = state,
        contentPadding = PaddingValues(bottom = 90.dp),
        userScrollEnabled = visible,
    ) {
        hairlineDivided(keys.size) { index -> KeyRow(keys[index], Modifier.testTag(TestTags.keyRow(index))) }
    }
}

@Composable
private fun KeyRow(
    key: Key,
    modifier: Modifier = Modifier,
) {
    val colors = plateColors
    val note = key.note
    val lit = note.isPlaying
    val ink = if (lit) colors.onAccent else colors.ink
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(if (lit) colors.accent else Color.Transparent)
            .soundsWhileHeld(
                toggleMode = { SettingsModel.toggleNotes },
                isPlaying = { note.isPlaying },
                play = { note.play() },
                stop = { note.stop() },
            ).semantics(mergeDescendants = true) {
                contentDescription = SongKeys.spokenName(key)
                role = Role.Button
                selected = lit
                onClick { BriefNotes.activate(note) }
            },
        verticalAlignment = ViewAlign.CenterVertically,
    ) {
        // The staff is left-to-right text, which a Layout keeps at its left; in a right-to-left row it
        // belongs at the row's start, clear of the key name.
        LegacyText(
            NoteText.keySignature(key),
            24.sp,
            ink,
            android.graphics.Typeface.DEFAULT,
            Modifier.weight(1f).padding(start = 20.dp),
            align = if (LocalLayoutDirection.current == LayoutDirection.Rtl) TextAlign.End else TextAlign.Start,
            wrapWidth = true,
        )
        LegacyText(NoteText.keyName(key), 22.sp, ink, PlateFonts.condensedTypeface, Modifier.padding(end = 20.dp), wrapWidth = true)
    }
}
