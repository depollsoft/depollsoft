package depollsoft.tagmaster.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import depollsoft.tagmaster.R
import depollsoft.tagmaster.barbershop.RemoteLocation
import depollsoft.tagmaster.barbershop.Tag
import depollsoft.tagmaster.ui.CompactBarberPole
import depollsoft.tagmaster.ui.LocalSnackbars
import depollsoft.tagmaster.ui.PlatformIcon
import depollsoft.tagmaster.ui.TagMasterTheme
import depollsoft.tagmaster.ui.TagMasterType
import depollsoft.tagmaster.ui.ViewAlign

/** The parts a tag can have tracks for, in the order the picker lists them. */
private fun parts(tag: Tag): List<Pair<Int, RemoteLocation?>> =
    listOf(
        R.string.AllParts to tag.allPartsTrackUri,
        R.string.Tenor to tag.tenorTrackUri,
        R.string.Lead to tag.leadTrackUri,
        R.string.Baritone to tag.baritoneTrackUri,
        R.string.Bass to tag.bassTrackUri,
        R.string.Other1 to tag.other1TrackUri,
        R.string.Other2 to tag.other2TrackUri,
        R.string.Other3 to tag.other3TrackUri,
        R.string.Other4 to tag.other4TrackUri,
    )

/**
 * The Tracks page: recording notes, the transport, and a part picker. Choosing a part only arms
 * the player; nothing downloads until Play. In landscape the picker sits beside the player.
 */
@Composable
fun TracksPage(
    tag: Tag,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val snackbars = LocalSnackbars.current
    val colors = TagMasterTheme.colors
    val failed = stringResource(R.string.detail_track_failed)
    val retry = stringResource(R.string.detail_retry)
    lateinit var player: TrackPlayer
    player = remember { TrackPlayer(context) { snackbars.show(failed, retry) { player.play() } } }
    DisposableEffect(player) { onDispose { player.release() } }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, player) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE) player.stop() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    var selected by rememberSaveable(tag.id) { mutableIntStateOf(-1) }
    val available = parts(tag)
    val empty = tag.tracks?.isEmpty() == true
    // A missing selection or an in-flight download is not an empty catalog; a tag with no tracks
    // clears the selection and stops the player before its controls go away.
    LaunchedEffect(empty) {
        if (empty) {
            selected = -1
            player.select(null)
        }
    }
    LaunchedEffect(selected, tag) {
        val location = available.getOrNull(selected)?.second
        if (location != player.location) player.select(location)
    }
    val landscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    @Composable
    fun notes(modifier: Modifier) {
        if (empty) {
            Text(
                stringResource(R.string.SorryNoTracks),
                modifier.fillMaxWidth(),
                style = TagMasterType.bodyLarge,
                color = colors.text,
            )
        }
        if (tag.recordingMethod != null) {
            Column(modifier.fillMaxWidth()) {
                Text(stringResource(R.string.RecordingNotes), style = TagMasterType.labelLarge, color = colors.text)
                Text(
                    tag.recordingMethod ?: "",
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    style = TagMasterType.bodyMedium,
                    color = colors.text,
                )
            }
        }
    }

    @Composable
    fun picker(modifier: Modifier) {
        if (empty) return
        Column(modifier.selectableGroup()) {
            available.forEachIndexed { index, (label, location) ->
                if (location != null) {
                    PartChoice(stringResource(label), selected == index, Modifier.testTag("part:$index")) { selected = index }
                }
            }
        }
    }

    DetailScroll(modifier, maxWidth = if (landscape) 960 else 640, top = false) {
        if (landscape) {
            Row(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .weight(1f)
                        .padding(end = 8.dp, top = 16.dp),
                ) {
                    notes(Modifier.padding(bottom = 16.dp))
                    if (!empty) Transport(player, enabled = player.location != null)
                }
                picker(
                    Modifier
                        .weight(1f)
                        .padding(start = 8.dp, top = 8.dp),
                )
            }
        } else {
            notes(Modifier.padding(top = 16.dp))
            if (!empty) Transport(player, enabled = player.location != null, Modifier.padding(top = 16.dp))
            picker(Modifier.padding(vertical = 16.dp))
        }
    }
}

@Composable
private fun PartChoice(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onSelect: () -> Unit,
) {
    val colors = TagMasterTheme.colors
    Row(
        modifier
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = ViewAlign.CenterVertically,
    ) {
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Box(Modifier.size(32.dp), contentAlignment = ViewAlign.Center) {
                RadioButton(
                    selected = selected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = colors.primary, unselectedColor = colors.onSurfaceVariant),
                )
            }
        }
        Text(label, style = TagMasterType.bodyLarge.let { with(TagMasterType) { it.withoutLineHeight() } }, color = colors.text)
    }
}

/** Play/pause, stop, the loading pole, position text, the seek slider and the balance slider. */
@Composable
private fun Transport(
    player: TrackPlayer,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = TagMasterTheme.colors
    val playDescription = stringResource(if (player.isPlaying) R.string.Pause else R.string.Play)
    val stopDescription = stringResource(R.string.Stop)
    val secondary = colors.textSecondary
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = ViewAlign.CenterVertically) {
            val playEnabled = enabled && !player.isLoading
            Box(
                Modifier
                    .size(48.dp)
                    .background(if (playEnabled) colors.primary else colors.onSurface.copy(alpha = 0.10f), CircleShape)
                    .clip(CircleShape)
                    .clickable(enabled = playEnabled, role = Role.Button) { player.togglePlay() }
                    .semantics { contentDescription = playDescription }
                    .testTag("playPause"),
                contentAlignment = ViewAlign.Center,
            ) {
                PlatformIcon(
                    if (player.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                    tint = if (playEnabled) colors.onPrimary else colors.onSurface.copy(alpha = 0.38f),
                )
            }
            val stopEnabled = enabled && (player.isPrepared || player.isLoading)
            Box(
                Modifier
                    .padding(start = 8.dp)
                    .size(48.dp)
                    .border(BorderStroke(1.dp, if (stopEnabled) colors.outlineVariant else colors.onSurface.copy(alpha = 0.12f)), CircleShape)
                    .clip(CircleShape)
                    .clickable(enabled = stopEnabled, role = Role.Button) { player.stop() }
                    .semantics { contentDescription = stopDescription }
                    .testTag("stop"),
                contentAlignment = ViewAlign.Center,
            ) {
                PlatformIcon(R.drawable.ic_stop, tint = if (stopEnabled) colors.onSurfaceVariant else colors.onSurface.copy(alpha = 0.38f))
            }
            CompactBarberPole(player.isLoading, stringResource(R.string.detail_track_loading), Modifier.padding(start = 8.dp))
            Text(
                player.positionText,
                Modifier
                    .weight(1f)
                    .widthIn(min = 96.dp)
                    .padding(start = 8.dp),
                style = TagMasterType.labelMedium,
                color = secondary,
                textAlign = TextAlign.End,
            )
        }
        val positionLabel = stringResource(R.string.detail_playback_position)
        Slider(
            value = player.position.toFloat().coerceAtMost(maxOf(1, player.length).toFloat()),
            onValueChange = { player.seekTo(it.toInt()) },
            valueRange = 0f..maxOf(1, player.length).toFloat(),
            enabled = enabled && player.isPrepared,
            modifier =
                Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .semantics { contentDescription = positionLabel }
                    .testTag("position"),
            colors = sliderColors(),
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = ViewAlign.CenterVertically) {
            val balance = stringResource(R.string.Balance)
            Text(balance, Modifier.padding(end = 16.dp), style = TagMasterType.labelMedium, color = secondary)
            Slider(
                value = player.balance.toFloat(),
                onValueChange = { player.changeBalance(it.toInt()) },
                valueRange = 0f..1000f,
                enabled = enabled,
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = balance }
                        .testTag("balance"),
                colors = sliderColors(),
            )
        }
    }
}

@Composable
private fun sliderColors() =
    TagMasterTheme.colors.let { colors ->
        SliderDefaults.colors(
            thumbColor = colors.primary,
            activeTrackColor = colors.primary,
            inactiveTrackColor = colors.secondaryContainer,
        )
    }
