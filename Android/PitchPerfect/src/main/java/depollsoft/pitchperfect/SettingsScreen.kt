package depollsoft.pitchperfect

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import depollsoft.pitchperfect.ui.PlateFonts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import depollsoft.compose.ViewAlign
import depollsoft.compose.scrollViewScrollbar
import depollsoft.lib.kotlin.R as LibKotlinR
import depollsoft.lib.util.appVersionName
import depollsoft.pitchperfect.lib.Note
import depollsoft.pitchperfect.lib.sound.NoteSound
import depollsoft.pitchperfect.ui.AppCompatAlertDialog
import depollsoft.pitchperfect.ui.DialogButton
import depollsoft.pitchperfect.ui.DialogChoiceList
import depollsoft.pitchperfect.ui.PlateBackground
import depollsoft.pitchperfect.ui.PlateContainedButton
import depollsoft.pitchperfect.ui.PlateSectionHeader
import depollsoft.pitchperfect.ui.PlateSettingsButton
import depollsoft.pitchperfect.ui.PlateText
import depollsoft.pitchperfect.ui.plateColors
import depollsoft.pitchperfect.ui.plateText

/**
 * What the settings screen shows. The settings themselves live in [SettingsModel] and the app's
 * preferences; [changed] makes this screen re-read them after it writes, and after sign-in
 * state changes, whether or not the preference store announces the change itself.
 */
@Stable
class SettingsState {
    private var version by mutableIntStateOf(0)

    fun changed() {
        version++
    }

    var toggleNotes: Boolean
        get() = version.let { SettingsModel.toggleNotes }
        set(value) {
            SettingsModel.toggleNotes = value
            changed()
        }

    var wakeLock: Boolean
        get() = version.let { SettingsModel.wakeLock }
        set(value) {
            SettingsModel.wakeLock = value
            changed()
        }

    /** The classic grid of big buttons in place of the radial pitch pipe. */
    var classicPitchPipe: Boolean
        get() = version.let { SettingsModel.classicPitchPipe }
        set(value) {
            SettingsModel.classicPitchPipe = value
            changed()
            PitchPerfectAnalytics.reportSettings()
        }

    /** The A4 the notes are tuned to, in Hz. */
    var referencePitch: Int
        // Reads the notes' tuning too, which is snapshot state: a change synced from another
        // device redraws the open screen, not only a change made here.
        get() = version.let { Note.getReferencePitch().let { SettingsModel.referencePitch } }
        set(value) {
            SettingsModel.referencePitch = value
            changed()
            PitchPerfectAnalytics.reportSettings()
        }

    /** The voice notes sound in. */
    var noteSound: NoteSound
        // Reads the notes' sound too, which is snapshot state, so a synced change redraws.
        get() = version.let { Note.getSound().let { SettingsModel.noteSound } }
        set(value) {
            SettingsModel.noteSound = value
            changed()
            PitchPerfectAnalytics.reportSettings()
        }

    var themeMode: Int
        get() = version.let { PitchPerfectApplication.themeMode }
        set(value) {
            PitchPerfectApplication.themeMode = value
            changed()
        }

    val loggedIn: Boolean get() = version.let { Firebase.auth.currentUser != null }
    val licensed: Boolean get() = version.let { SettingsModel.licensed }
    val areAdsRemoved: Boolean get() = version.let { SettingsModel.areAdsRemoved }

    /** The connected watches, once found; an empty list hides the watch section. */
    var watches by mutableStateOf<List<WatchNode>>(emptyList())

    /** "Build 123 · PR #45" on a private build, or null. */
    var privateBuild by mutableStateOf<String?>(null)
}

/** What the settings screen's controls do; the activity supplies them. */
class SettingsActions(
    val chooseTuning: () -> Unit,
    val chooseSound: () -> Unit,
    val clearSongs: () -> Unit,
    val installOnWatch: (WatchNode) -> Unit,
    val logIn: () -> Unit,
    val logOut: () -> Unit,
    val deleteAccount: () -> Unit,
    val manageSubscription: () -> Unit,
    val showChangelog: () -> Unit,
    val openLink: (String) -> Unit,
    val privacyChoices: () -> Unit,
    val copyLogs: () -> Unit,
)

@Composable
fun SettingsScreen(
    state: SettingsState,
    actions: SettingsActions,
) {
    val colors = plateColors
    val scroll = rememberScrollState()
    PlateBackground {
        Column(
            Modifier
                .fillMaxSize()
                .scrollViewScrollbar(scroll)
                .verticalScroll(scroll)
                .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 28.dp),
        ) {
            PlateSectionHeader(stringResource(R.string.SectionPitchPipe), Modifier.padding(top = 12.dp))
            SettingSwitch(stringResource(R.string.NotesToggle), state.toggleNotes, TestTags.TOGGLE_NOTES) { state.toggleNotes = it }
            SettingSwitch(stringResource(R.string.WakeLock), state.wakeLock, TestTags.WAKE_LOCK) { state.wakeLock = it }
            SettingSwitch(
                stringResource(R.string.ClassicPitchPipe),
                state.classicPitchPipe,
                TestTags.CLASSIC_SETTING,
                support = stringResource(R.string.ClassicPitchPipeSupport),
            ) { state.classicPitchPipe = it }
            DropDownRows(
                listOf(
                    DropDown(
                        stringResource(R.string.Tuning),
                        stringResource(R.string.TuningSupport),
                        stringResource(R.string.TuningValue, state.referencePitch),
                        measured = true,
                        TestTags.TUNING,
                        actions.chooseTuning,
                    ),
                    DropDown(
                        stringResource(R.string.Sound),
                        stringResource(R.string.SoundSupport),
                        soundLabel(state.noteSound),
                        measured = false,
                        TestTags.SOUND,
                        actions.chooseSound,
                    ),
                ),
            )
            PlateSettingsButton(
                stringResource(R.string.ClearAllSongs),
                Modifier.padding(top = 8.dp).fillMaxWidth().testTag(TestTags.CLEAR_SONGS),
                onClick = actions.clearSongs,
            )

            if (state.watches.isNotEmpty()) {
                Column(Modifier.padding(top = 28.dp).testTag(TestTags.WATCH_SECTION)) {
                    PlateSectionHeader(stringResource(R.string.SectionWatch))
                    val context = LocalContext.current
                    PlateText(
                        state.watches.joinToString("\n") { node ->
                            context.getString(if (node.installed) R.string.WatchInstalledOn else R.string.WatchNotInstalledOn, node.name)
                        },
                        style = plateText(16.sp, colors.inkSecondary),
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth().testTag(TestTags.WATCH_STATUS),
                    )
                    Column(Modifier.padding(top = 12.dp)) {
                        state.watches.filterNot { it.installed }.forEach { node ->
                            PlateSettingsButton(
                                stringResource(R.string.WatchInstallOn, node.name),
                                Modifier.fillMaxWidth().testTag(TestTags.WATCH_INSTALL),
                            ) {
                                actions.installOnWatch(node)
                            }
                        }
                    }
                }
            }

            PlateSectionHeader(stringResource(R.string.SectionAccount), Modifier.padding(top = 28.dp))
            PlateText(
                stringResource(R.string.LogInInfo),
                style = plateText(16.sp, colors.inkSecondary),
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
            )
            Column(Modifier.padding(top = 12.dp)) {
                if (state.loggedIn) {
                    PlateSettingsButton(stringResource(R.string.LogOut), Modifier.fillMaxWidth().testTag(TestTags.LOG_OUT), onClick = actions.logOut)
                    PlateSettingsButton(
                        stringResource(R.string.DeleteAccount),
                        Modifier.fillMaxWidth().testTag(TestTags.DELETE_ACCOUNT),
                        onClick = actions.deleteAccount,
                    )
                } else {
                    PlateSettingsButton(stringResource(R.string.LogIn), Modifier.fillMaxWidth().testTag(TestTags.LOG_IN), onClick = actions.logIn)
                }
            }

            PlateSectionHeader(stringResource(R.string.SectionAppearance), Modifier.padding(top = 28.dp))
            Row(Modifier.padding(top = 4.dp)) {
                ThemeChoice(stringResource(R.string.theme_system), AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, state, TestTags.THEME_SYSTEM)
                ThemeChoice(stringResource(R.string.theme_light), AppCompatDelegate.MODE_NIGHT_NO, state, TestTags.THEME_LIGHT)
                ThemeChoice(stringResource(R.string.theme_dark), AppCompatDelegate.MODE_NIGHT_YES, state, TestTags.THEME_DARK)
            }
            if (state.areAdsRemoved) {
                PlateSettingsButton(
                    stringResource(R.string.manage_subscription),
                    Modifier.padding(top = 8.dp).fillMaxWidth().testTag(TestTags.MANAGE_SUBSCRIPTION),
                    onClick = actions.manageSubscription,
                )
            }

            PlateSectionHeader(stringResource(R.string.SectionAbout), Modifier.padding(top = 28.dp))
            PlateSettingsButton(
                stringResource(R.string.ViewChangelog),
                Modifier.padding(top = 8.dp).fillMaxWidth().testTag(TestTags.CHANGELOG),
                onClick = actions.showChangelog,
            )
            AboutFooter(state.licensed, actions.openLink)
            PlateContainedButton(
                stringResource(LibKotlinR.string.privacy_title),
                Modifier.padding(top = 16.dp).fillMaxWidth().testTag(TestTags.PRIVACY_CHOICES),
                onClick = actions.privacyChoices,
            )
            state.privateBuild?.let { metadata ->
                Column(Modifier.padding(top = 20.dp)) {
                    PlateSectionHeader(PRIVATE_BUILD)
                    PlateText(metadata, style = plateText(14.sp, colors.inkSecondary), modifier = Modifier.padding(top = 4.dp))
                    PlateSettingsButton(COPY_LOGS, Modifier.padding(top = 8.dp).fillMaxWidth(), onClick = actions.copyLogs)
                }
            }
        }
    }
}

/** A switch row: its label (and a line saying what it does, if any), then the switch, at least 56dp tall. */
@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    tag: String,
    support: String? = null,
    onChange: (Boolean) -> Unit,
) {
    val colors = plateColors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(tag)
            .toggleable(checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = ViewAlign.CenterVertically,
    ) {
        if (support == null) {
            PlateText(label, style = plateText(16.sp, colors.ink), modifier = Modifier.weight(1f))
        } else {
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                PlateText(label, style = plateText(16.sp, colors.ink))
                PlateText(support, style = plateText(14.sp, colors.inkSecondary))
            }
        }
        Switch(
            checked,
            onCheckedChange = null,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = colors.accent,
                    checkedTrackColor = colors.accent,
                    uncheckedThumbColor = colors.surface,
                    uncheckedTrackColor = colors.ink,
                ),
        )
    }
}

/** A setting with choices: its label and what it sets, and the current [value] (in the mono face when [measured]). */
private class DropDown(
    val label: String,
    val support: String,
    val value: String,
    val measured: Boolean,
    val tag: String,
    val onClick: () -> Unit,
)

/**
 * Settings with choices, such as the tuning: each label with a line saying what it sets, then the
 * current choice in an outlined field with a drop-down arrow, so it reads as a control; a tap
 * anywhere on a row offers the choices. The fields share one width, as wide as the widest value,
 * so they line up as one column. When that column would squeeze the labels (large text, a narrow
 * screen), each field goes under its label instead.
 */
@Composable
private fun DropDownRows(rows: List<DropDown>) {
    val colors = plateColors
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val valueStyles = rows.map { dropDownValueStyle(it.measured, colors.ink) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val maxWidth = maxWidth
        val widest = rows.zip(valueStyles).maxOf { (row, style) -> measurer.measure(row.value, style, maxLines = 1).size.width }
        val fieldWidth = with(density) { widest.toDp() } + FIELD_CHROME
        val stacked = fieldWidth > maxWidth * STACK_BEYOND
        Column(Modifier.fillMaxWidth()) {
            rows.zip(valueStyles).forEach { (row, style) ->
                DropDownRow(row, style, if (stacked) minOf(fieldWidth, maxWidth) else fieldWidth, stacked)
            }
        }
    }
}

@Composable
@ReadOnlyComposable
private fun dropDownValueStyle(
    measured: Boolean,
    ink: Color,
): TextStyle = if (measured) plateText(16.sp, ink, PlateFonts.mono, letterSpacing = 0.04f) else plateText(16.sp, ink)

@Composable
private fun DropDownRow(
    row: DropDown,
    valueStyle: TextStyle,
    fieldWidth: Dp,
    stacked: Boolean,
) {
    val colors = plateColors
    val labels =
        @Composable { modifier: Modifier ->
            Column(modifier) {
                PlateText(row.label, style = plateText(16.sp, colors.ink))
                // Balanced, so a line that wraps beside the field never leaves one word alone.
                PlateText(row.support, style = plateText(14.sp, colors.inkSecondary).copy(lineBreak = LineBreak.Heading))
            }
        }
    val rowModifier =
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .testTag(row.tag)
            .clickable(role = Role.Button, onClick = row.onClick)
    if (stacked) {
        Column(rowModifier.padding(vertical = 8.dp)) {
            labels(Modifier.fillMaxWidth())
            DropDownField(row.value, valueStyle, Modifier.padding(top = 8.dp).width(fieldWidth).testTag(row.tag + FIELD_TAG))
        }
    } else {
        Row(rowModifier, verticalAlignment = ViewAlign.CenterVertically) {
            labels(Modifier.weight(1f).padding(end = 12.dp))
            DropDownField(row.value, valueStyle, Modifier.width(fieldWidth).testTag(row.tag + FIELD_TAG))
        }
    }
}

/** The current choice in an outlined field with Material's drop-down arrow. */
@Composable
private fun DropDownField(
    value: String,
    style: TextStyle,
    modifier: Modifier,
) {
    val colors = plateColors
    val shape = RoundedCornerShape(2.dp)
    Row(
        modifier
            .heightIn(min = 40.dp)
            .background(colors.surface, shape)
            .border(1.dp, colors.hairline, shape)
            .padding(start = FIELD_START, end = FIELD_END),
        verticalAlignment = ViewAlign.CenterVertically,
    ) {
        PlateText(value, style = style, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        // Material's drop-down arrow: a 10x5dp triangle in a 24dp box.
        Box(
            Modifier
                .padding(start = ARROW_GAP)
                .size(ARROW_BOX)
                .drawBehind {
                    val w = 10.dp.toPx()
                    val h = 5.dp.toPx()
                    val left = (size.width - w) / 2f
                    val top = (size.height - h) / 2f
                    val arrow =
                        Path().apply {
                            moveTo(left, top)
                            lineTo(left + w, top)
                            lineTo(left + w / 2f, top + h)
                            close()
                        }
                    drawPath(arrow, colors.inkSecondary)
                },
        )
    }
}

/** Tags a row's field, after the row's own tag. */
const val FIELD_TAG = ".field"

private val FIELD_START = 12.dp
private val FIELD_END = 8.dp
private val ARROW_GAP = 4.dp
private val ARROW_BOX = 24.dp

/** Everything in a field but its value, plus a pixel of slack for rounding. */
private val FIELD_CHROME = FIELD_START + FIELD_END + ARROW_GAP + ARROW_BOX + 1.dp

/** The share of the row the fields may take before they move under their labels. */
private const val STACK_BEYOND = 0.55f

/** The choices for A4, one radio row each, opened on the current one; choosing one closes the dialog. */
@Composable
fun TuningDialog(
    selected: Int,
    onDismiss: () -> Unit,
    onChoose: (Int) -> Unit,
) {
    AppCompatAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.TuningTitle),
        buttons = listOf(DialogButton(stringResource(android.R.string.cancel), onDismiss)),
    ) {
        // Each row is tappable across the whole width: a drag that starts beside a short label must
        // still land on the list, not the dialog behind it.
        val choices = SettingsModel.referencePitchChoices(selected)
        DialogChoiceList(TestTags.TUNING_LIST, reveal = choices.indexOf(selected)) {
            val measured = plateText(16.sp, plateColors.ink, PlateFonts.mono, letterSpacing = 0.04f)
            choices.forEach { hz ->
                RadioChoice(
                    stringResource(R.string.TuningValue, hz),
                    hz == selected,
                    "${TestTags.TUNING_CHOICE}$hz",
                    Modifier.fillMaxWidth(),
                    labelStyle = measured,
                    detail = tuningName(hz),
                ) { onChoose(hz) }
            }
        }
    }
}

/**
 * The sounds, one radio row each: the original voice first, then the sections under their
 * headings, opened on the current one. Choosing a sound plays it and leaves the dialog open, so
 * the user can try another; Done closes it. Nothing is pending, so there is nothing to cancel.
 */
@Composable
fun SoundDialog(
    selected: NoteSound,
    onDismiss: () -> Unit,
    onChoose: (NoteSound) -> Unit,
) {
    AppCompatAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.Sound),
        buttons = listOf(DialogButton(stringResource(R.string.SoundDone), onDismiss)),
    ) {
        // Each heading is a child of the list too, so count it when finding the chosen row.
        var children = 0
        var chosen = 0
        NoteSound.entries.forEachIndexed { index, sound ->
            if (sound.section != NoteSound.entries.getOrNull(index - 1)?.section && sound.section != NoteSound.Section.DEFAULT) children++
            if (sound == selected) chosen = children
            children++
        }
        // As the tuning list: a row is tappable across the whole width.
        DialogChoiceList(TestTags.SOUND_LIST, reveal = chosen) {
            NoteSound.entries.forEachIndexed { index, sound ->
                val previous = NoteSound.entries.getOrNull(index - 1)
                if (sound.section != previous?.section) {
                    when (sound.section) {
                        NoteSound.Section.SUSTAINED -> SoundHeading(stringResource(R.string.SoundSustained))
                        NoteSound.Section.WAVES -> SoundHeading(stringResource(R.string.SoundWaves))
                        NoteSound.Section.PLUCKED_AND_STRUCK -> SoundHeading(stringResource(R.string.SoundPluckedAndStruck))
                        NoteSound.Section.DEFAULT -> {}
                    }
                }
                RadioChoice(soundLabel(sound), sound == selected, "${TestTags.SOUND_CHOICE}${sound.id}", Modifier.fillMaxWidth()) { onChoose(sound) }
            }
        }
    }
}

/** A group heading in the sound list, in the settings sections' caps. */
@Composable
private fun SoundHeading(text: String) {
    PlateSectionHeader(text, Modifier.fillMaxWidth().padding(start = 6.dp, top = 12.dp, bottom = 4.dp).semantics { heading() })
}

@Composable
fun soundLabel(sound: NoteSound): String =
    stringResource(
        when (sound) {
            NoteSound.PITCH_PIPE -> R.string.SoundPitchPipe
            NoteSound.SINE -> R.string.SoundSine
            NoteSound.TRIANGLE -> R.string.SoundTriangle
            NoteSound.SQUARE -> R.string.SoundSquare
            NoteSound.SAWTOOTH -> R.string.SoundSawtooth
            NoteSound.PIANO -> R.string.SoundPiano
            NoteSound.ELECTRIC_PIANO -> R.string.SoundElectricPiano
            NoteSound.HARPSICHORD -> R.string.SoundHarpsichord
            NoteSound.VIBRAPHONE -> R.string.SoundVibraphone
            NoteSound.ORGAN -> R.string.SoundOrgan
            NoteSound.REED_ORGAN -> R.string.SoundReedOrgan
            NoteSound.ACCORDION -> R.string.SoundAccordion
            NoteSound.HARMONICA -> R.string.SoundHarmonica
            NoteSound.GUITAR -> R.string.SoundGuitar
            NoteSound.HARP -> R.string.SoundHarp
            NoteSound.STRINGS -> R.string.SoundStrings
            NoteSound.CHOIR -> R.string.SoundChoir
            NoteSound.TRUMPET -> R.string.SoundTrumpet
            NoteSound.CLARINET -> R.string.SoundClarinet
            NoteSound.FLUTE -> R.string.SoundFlute
        },
    )

/** The name of a historical or standard A4, if it has one. */
@Composable
private fun tuningName(hz: Int): String? =
    when (hz) {
        415 -> R.string.TuningBaroque
        430 -> R.string.TuningClassical
        440 -> R.string.TuningStandard
        else -> null
    }?.let { stringResource(it) }

/** One appearance choice. */
@Composable
private fun ThemeChoice(
    label: String,
    mode: Int,
    state: SettingsState,
    tag: String,
) {
    RadioChoice(label, state.themeMode == mode, tag) { state.themeMode = mode }
}

/**
 * A choice as a MaterialRadioButton drew it: a 32dp button area holding a 20dp ring with a 2dp
 * stroke (and a 5dp dot when chosen), then the label, the whole row 48dp tall.
 */
@Composable
private fun RadioChoice(
    label: String,
    selected: Boolean,
    tag: String,
    modifier: Modifier = Modifier,
    labelStyle: TextStyle? = null,
    detail: String? = null,
    onSelect: () -> Unit,
) {
    val colors = plateColors
    // The radio button's animated drawable: the ring takes the accent and the dot grows in.
    val ring by animateColorAsState(
        if (selected) colors.accent else colors.ink.copy(alpha = UNSELECTED_RING_ALPHA),
        tween(RADIO_MS),
        label = "ring",
    )
    val dot by animateFloatAsState(if (selected) 1f else 0f, tween(RADIO_MS, easing = FastOutSlowInEasing), label = "dot")
    val spoken = detail?.let { stringResource(R.string.TuningChoiceSpoken, label, it) }.orEmpty()
    Row(
        modifier
            .height(48.dp)
            .testTag(tag)
            .selectable(selected, role = Role.RadioButton, onClick = onSelect)
            .then(if (detail == null) Modifier else Modifier.semantics { contentDescription = spoken }),
        verticalAlignment = ViewAlign.CenterVertically,
    ) {
        Box(
            Modifier
                .width(32.dp)
                .height(48.dp)
                .drawBehind {
                    val stroke = 2.dp.toPx()
                    drawCircle(ring, radius = 10.dp.toPx() - stroke / 2f, style = Stroke(stroke))
                    if (dot > 0f) drawCircle(ring, radius = 5.dp.toPx() * dot)
                },
        )
        val shown = if (detail == null) Modifier else Modifier.clearAndSetSemantics {}
        PlateText(label, style = labelStyle ?: plateText(16.sp, colors.ink), modifier = shown)
        if (detail != null) {
            PlateText(detail, style = plateText(16.sp, colors.inkSecondary, PlateFonts.condensed), modifier = shown.padding(start = 12.dp))
        }
    }
}

private const val UNSELECTED_RING_ALPHA = 0.51f
private const val RADIO_MS = 200

/** The about lines: name and version, the publisher, the home page and the terms. */
@Composable
private fun AboutFooter(
    licensed: Boolean,
    openLink: (String) -> Unit,
) {
    val colors = plateColors
    val style = plateText(14.sp, colors.inkSecondary)
    val link = style.copy(textDecoration = TextDecoration.Underline)
    Column(
        Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
        horizontalAlignment = ViewAlign.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlateText(stringResource(R.string.app_name), style = style)
            PlateText(" · ", style = style)
            val context = LocalContext.current
            PlateText(stringResource(R.string.version_label, remember(context) { appVersionName(context) }), style = style)
            if (licensed) PlateText(stringResource(R.string.Purchased), style = style, modifier = Modifier.padding(start = 4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PlateText(stringResource(R.string.DepollSoft), style = link, modifier = Modifier.clickable(role = Role.Button) { openLink(HOME_PAGE) })
            PlateText(stringResource(R.string.Copyright), style = style)
        }
        PlateText(stringResource(R.string.HomePage), style = link, modifier = Modifier.clickable(role = Role.Button) { openLink(HOME_PAGE) })
        PlateText(stringResource(R.string.TermsOfUse), style = link, modifier = Modifier.clickable(role = Role.Button) { openLink(TERMS_OF_USE) })
    }
}

private const val HOME_PAGE = "http://apps.depoll.com"
private const val TERMS_OF_USE = "http://apps.depoll.com/terms-of-use"
private const val PRIVATE_BUILD = "Private Build"
private const val COPY_LOGS = "Copy Logs"
