package depollsoft.pitchperfect

import depollsoft.compose.MenuKey
import depollsoft.compose.LocalMenuKey
import depollsoft.compose.ListMotion
import depollsoft.compose.ViewAlign
import androidx.compose.animation.core.animateFloatAsState

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import depollsoft.pitchperfect.ui.PlateText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import depollsoft.pitchperfect.lib.PitchedSong
import depollsoft.pitchperfect.ui.DrawableIcon
import depollsoft.pitchperfect.ui.LegacyText
import depollsoft.pitchperfect.ui.PlateActionIcon
import depollsoft.pitchperfect.ui.PlateBackground
import depollsoft.pitchperfect.ui.PlateFonts
import depollsoft.pitchperfect.ui.PlateMenuItem
import depollsoft.pitchperfect.ui.PlateOverflowMenu
import depollsoft.pitchperfect.ui.PlatePrimaryButton
import depollsoft.pitchperfect.ui.PlateSectionHeader
import depollsoft.pitchperfect.ui.PlateTheme
import depollsoft.pitchperfect.ui.PlateTopBar
import depollsoft.pitchperfect.ui.plateColors
import depollsoft.pitchperfect.ui.plateText

/**
 * What "Add songs" offers and what has been ticked. One section per other list, in list order;
 * songs whose title (ignoring case) and key already exist in the target are left out entirely.
 */
@Stable
class AddableSongs(
    private val model: SongsModel,
    val targetId: String,
) {
    val sections: List<Pair<String, List<PitchedSong>>> =
        model.addableSongs(targetId).map { (list, songs) -> model.displayName(list) to songs }

    private val offered: List<PitchedSong> = sections.flatMap { it.second }

    /**
     * The songs ticked so far, by identity: song ids are only promised unique within one list,
     * and two source lists could carry the same id.
     */
    private val ticked = mutableStateListOf<PitchedSong>()

    val count: Int get() = ticked.size
    val total: Int get() = offered.size

    fun isTicked(song: PitchedSong): Boolean = ticked.any { it === song }

    fun toggle(song: PitchedSong) {
        val index = ticked.indexOfFirst { it === song }
        if (index >= 0) ticked.removeAt(index) else ticked.add(song)
    }

    /** Ticks every offered song, or clears the ticks once they are all in. */
    fun toggleAll() {
        if (offered.isEmpty()) return
        val all = ticked.size == offered.size
        ticked.clear()
        if (!all) ticked.addAll(offered)
    }

    /** Appends deep copies of the ticked songs, in the order the sections present them. */
    fun confirm(): Boolean {
        if (ticked.isEmpty()) return false
        model.copySongs(offered.filter { isTicked(it) }, targetId)
        return true
    }
}

/** "Add songs": a sectioned checklist of every song the current set list does not already have. */
class AddSongsFromListActivity : AppCompatActivity() {
    internal lateinit var addable: AddableSongs
        private set

    /** The Menu key opens the Select all menu, as the window action bar's overflow did. */
    private val menuKey = depollsoft.compose.MenuKey()

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.AddSongsTitle)
        val model = SongsModel.get()
        addable = AddableSongs(model, intent.getStringExtra(LIST_EXTRA) ?: model.currentListId)
        setContent {
            PlateTheme {
                androidx.compose.runtime.CompositionLocalProvider(depollsoft.compose.LocalMenuKey provides menuKey) {
                    Column(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
                        val count = addable.count
                        val label =
                            if (count == 0) stringResource(R.string.AddSongsConfirm) else pluralStringResource(R.plurals.AddSongsCount, count, count)
                        PlateTopBar(stringResource(R.string.AddSongsTitle), navigationUp = ::finish) {
                            // Vector menu icons do not dim on their own; a dead checkmark must not read as live.
                            PlateActionIcon(R.drawable.ic_check, label, ::confirm, Modifier.testTag(TestTags.CONFIRM_ADD), enabled = count > 0)
                            if (addable.total > 0) {
                                PlateOverflowMenu(
                                    listOf(
                                        PlateMenuItem(
                                            stringResource(
                                                if (count == addable.total) R.string.AddSongsClearSelection else R.string.AddSongsSelectAll,
                                            ),
                                            TestTags.SELECT_ALL,
                                            onClick = addable::toggleAll,
                                        ),
                                    ),
                                )
                            }
                        }
                        AddSongsFromListScreen(addable, label, ::confirm)
                    }
                }
            }
        }
        menuKey.install(window)
    }

    /** Copies the ticked songs into the target list and closes. */
    internal fun confirm() {
        if (!addable.confirm()) return
        setResult(RESULT_OK)
        finish()
    }

    companion object {
        const val LIST_EXTRA = "depollsoft.pitchperfect.AddSongsFromList.listId"
    }
}

@Composable
fun AddSongsFromListScreen(
    addable: AddableSongs,
    confirmLabel: String,
    onConfirm: () -> Unit,
) {
    val colors = plateColors
    val context = LocalContext.current
    PlateBackground {
        Column(Modifier.fillMaxSize()) {
            if (addable.sections.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
                    LegacyText(
                        stringResource(R.string.NoSongsToAdd),
                        size = 15.sp,
                        color = colors.inkSecondary,
                        typeface = remember(context) { PlateFonts.oswaldTypeface(context) },
                        letterSpacing = 0.12f,
                        align = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().testTag(TestTags.NOTHING_TO_ADD),
                    )
                }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag(TestTags.ADDABLE_LIST)) {
                    // Keyed by section, since a duplicated list can hold songs with the same ids.
                    addable.sections.forEachIndexed { section, (title, songs) ->
                        item(key = "section:$section:$title") {
                            Column(Modifier.animateItem(ListMotion.fade, ListMotion.placement, ListMotion.fade)) {
                                PlateSectionHeader(title, Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 6.dp))
                                Hairline()
                            }
                        }
                        items(songs, key = { "$section:${it.id}" }) { song ->
                            Column(Modifier.animateItem(ListMotion.fade, ListMotion.placement, ListMotion.fade)) {
                                AddableRow(song, addable.isTicked(song)) { addable.toggle(song) }
                                Hairline()
                            }
                        }
                    }
                }
            }
            PlatePrimaryButton(
                confirmLabel,
                Modifier.padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 16.dp).fillMaxWidth().testTag(TestTags.CONFIRM_ADD_BUTTON),
                enabled = addable.count > 0,
                onClick = onConfirm,
            )
        }
    }
}

@Composable
private fun AddableRow(
    song: PitchedSong,
    ticked: Boolean,
    onToggle: () -> Unit,
) {
    val colors = plateColors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .testTag(TestTags.addableRow(song.id))
            .background(if (pressed) colors.accent else Color.Transparent)
            // A row is a checkbox to a screen reader: it has a checked state, and toggling it
            // announces the change.
            .clickable(interaction, indication = null, role = Role.Checkbox, onClick = onToggle)
            .semantics(mergeDescendants = true) { toggleableState = ToggleableState(ticked) },
        verticalAlignment = ViewAlign.CenterVertically,
    ) {
        PlateText(
            song.name.orEmpty(),
            style = plateText(20.sp, if (pressed) colors.onAccent else colors.ink, PlateFonts.condensed),
            modifier = Modifier.weight(1f).padding(start = 20.dp, top = 14.dp, bottom = 14.dp),
        )
        LegacyText(
            song.key?.let { NoteText.keyName(it) } ?: "",
            18.sp,
            if (pressed) colors.onAccent else colors.inkSecondary,
            android.graphics.Typeface.MONOSPACE,
            Modifier.padding(end = 12.dp),
            letterSpacing = 0.06f,
            wrapWidth = true,
        )
        val tick by animateFloatAsState(if (ticked) 1f else 0f, ListMotion.change(), label = "tick")
        Box(Modifier.width(44.dp).padding(horizontal = 10.dp).alpha(tick), contentAlignment = Alignment.Center) {
            DrawableIcon(R.drawable.ic_check, colors.ink)
        }
    }
}
