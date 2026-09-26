package depollsoft.pitchperfect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import depollsoft.compose.ViewAlign
import depollsoft.compose.listViewScrollbar
import depollsoft.pitchperfect.lib.Note
import depollsoft.pitchperfect.ui.LegacyText
import depollsoft.pitchperfect.ui.PlateFonts
import depollsoft.pitchperfect.ui.PlateText
import depollsoft.pitchperfect.ui.hairlineWidth
import depollsoft.pitchperfect.ui.plateColors
import depollsoft.pitchperfect.ui.plateText

/** The Notes tab: every pitch from C0 to B7, each sounding while held, starting on middle C. */
@Composable
fun NoteListScreen(
    isCurrentPage: Boolean,
    notes: List<Note> = remember { Note.getPrunedNotes() },
) {
    val stopPlaying = {
        BriefNotes.cancelAll()
        notes.forEach { it.stop() }
    }
    LifecycleResumeEffect(notes) { onPauseOrDispose { stopPlaying() } }
    OnPageVisibilityChange(isCurrentPage) { current -> if (!current) stopPlaying() }

    val listState = rememberLazyListState()
    CenterOnFirstLayout(listState, notes.size)
    LazyColumn(
        Modifier.fillMaxSize().listViewScrollbar(listState, divider = hairlineWidth).testTag(TestTags.NOTE_LIST),
        state = listState,
    ) {
        hairlineDivided(notes.size) { index -> NoteRow(notes[index], Modifier.testTag(TestTags.noteRow(index))) }
    }
}

@Composable
private fun NoteRow(
    note: Note,
    modifier: Modifier = Modifier,
) {
    val colors = plateColors
    val lit = note.isPlaying
    val name = NoteText.noteName(note)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(if (lit) colors.accent else Color.Transparent)
            .soundsWhileHeld(
                toggleMode = { SettingsModel.toggleNotes },
                isPlaying = { note.isPlaying },
                play = { note.play() },
                stop = { note.stop() },
            ).semantics(mergeDescendants = true) {
                role = Role.Button
                selected = lit
                onClick { BriefNotes.activate(note) }
            },
        verticalAlignment = ViewAlign.CenterVertically,
    ) {
        LegacyText(name, 24.sp, if (lit) colors.onAccent else colors.ink, PlateFonts.condensedTypeface, Modifier.weight(1f).padding(start = 20.dp, top = 10.dp, bottom = 10.dp), wrapWidth = true)
        PlateText(
            "%1.2f Hz".format(note.frequency),
            style = plateText(14.sp, if (lit) colors.onAccent else colors.inkSecondary, PlateFonts.mono, letterSpacing = 0.04f),
            modifier = Modifier.padding(end = 20.dp, top = 10.dp, bottom = 10.dp),
        )
    }
}
