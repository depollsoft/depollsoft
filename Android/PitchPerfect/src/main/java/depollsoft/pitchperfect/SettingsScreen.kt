package depollsoft.pitchperfect

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import depollsoft.compose.ViewAlign
import depollsoft.compose.scrollViewScrollbar
import depollsoft.lib.kotlin.R as LibKotlinR
import depollsoft.lib.util.appVersionName
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

/** A switch row: its label, then the switch, at least 56dp tall. */
@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    tag: String,
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
        PlateText(label, style = plateText(16.sp, colors.ink), modifier = Modifier.weight(1f))
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

/**
 * One appearance choice, as a MaterialRadioButton drew it: a 32dp button area holding a 20dp ring
 * with a 2dp stroke (and a 5dp dot when chosen), then the label, the whole row 48dp tall.
 */
@Composable
private fun ThemeChoice(
    label: String,
    mode: Int,
    state: SettingsState,
    tag: String,
) {
    val colors = plateColors
    val selected = state.themeMode == mode
    // The radio button's animated drawable: the ring takes the accent and the dot grows in.
    val ring by animateColorAsState(
        if (selected) colors.accent else colors.ink.copy(alpha = UNSELECTED_RING_ALPHA),
        tween(RADIO_MS),
        label = "ring",
    )
    val dot by animateFloatAsState(if (selected) 1f else 0f, tween(RADIO_MS, easing = FastOutSlowInEasing), label = "dot")
    Row(
        Modifier
            .height(48.dp)
            .testTag(tag)
            .selectable(selected, role = Role.RadioButton) { state.themeMode = mode },
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
        PlateText(label, style = plateText(16.sp, colors.ink))
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
