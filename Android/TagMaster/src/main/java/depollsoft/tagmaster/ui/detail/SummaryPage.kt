package depollsoft.tagmaster.ui.detail

import depollsoft.compose.rememberDrawable
import depollsoft.compose.drawPlatform
import depollsoft.compose.PlatformIcon
import depollsoft.compose.ViewAlign
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.webkit.MimeTypeMap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bolts.Task
import depollsoft.lib.util.ContentCache
import depollsoft.tagmaster.ListModel
import depollsoft.tagmaster.MeActivity
import depollsoft.tagmaster.R
import depollsoft.tagmaster.RatingsModel
import depollsoft.tagmaster.SheetMusicActivity
import depollsoft.tagmaster.TagListActivity
import depollsoft.tagmaster.TagLists
import depollsoft.tagmaster.TeachableTagsActivity
import depollsoft.tagmaster.barbershop.Tag
import depollsoft.tagmaster.ui.ButtonStyle
import depollsoft.tagmaster.ui.CompactBarberPole
import depollsoft.tagmaster.ui.DialogButton
import depollsoft.tagmaster.ui.ListDialogs
import depollsoft.tagmaster.ui.LocalSnackbars
import depollsoft.tagmaster.ui.TagMasterButton
import depollsoft.tagmaster.ui.TagMasterDialog
import depollsoft.tagmaster.ui.TagMasterTheme
import depollsoft.tagmaster.ui.TagMasterType
import depollsoft.tagmaster.ui.TagMasterType.withoutLineHeight
import depollsoft.tagmaster.ui.WithTooltip
import depollsoft.tagmaster.ui.isPresent
import depollsoft.tagmaster.ui.listIconRes
import depollsoft.tagmaster.ui.noteDescription
import depollsoft.tagmaster.ui.notePress
import depollsoft.tagmaster.ui.rememberNotePlayer
import depollsoft.tagmaster.ui.rememberTextViewPaint
import depollsoft.tagmaster.ui.textViewWidth
import java.util.Locale

/**
 * The Summary page's request state: whether sheet music is being fetched, a rating being sent,
 * and whether this screen has already rated the tag.
 */
private class SummaryRequests {
    var sheetMusicLoading by mutableStateOf(false)
    var ratingSubmitting by mutableStateOf(false)
    var rated by mutableStateOf(false)
    var ratingDialog by mutableStateOf(false)
    var active = true
}

@Composable
fun SummaryPage(
    tag: Tag,
    dialogs: ListDialogs,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val snackbars = LocalSnackbars.current
    val colors = TagMasterTheme.colors
    val requests = remember(tag.id) { SummaryRequests() }
    DisposableEffect(requests) { onDispose { requests.active = false } }
    val canRate = !requests.rated && !requests.ratingSubmitting && !RatingsModel.isRated(tag.id)

    DetailScroll(modifier, maxWidth = if (LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 960 else 640) {
        Column(Modifier.fillMaxWidth()) {
            val title = tag.title
            if (title != null) Text(title, style = TagMasterType.headlineSmall, color = colors.text)
            if (tag.alternativeTitle.isPresent()) {
                LabelledLine(stringResource(R.string.aka), tag.alternativeTitle!!)
            }
            if (tag.version.isPresent()) {
                LabelledLine(stringResource(R.string.Version), tag.version!!)
            }
            ListChips(tag, dialogs)
        }
        SummaryColumns(
            Modifier.padding(top = 8.dp),
            facts = {
                DetailPairs(summaryFacts(tag, requests, canRate), summary = true)
                if (tag.writtenKey.isPresent()) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(R.string.Key),
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            style = TagMasterType.labelLarge,
                            color = colors.text,
                        )
                        KeyNoteButton(tag)
                    }
                }
                if (tag.sheetMusicUri.isPresent()) {
                    Box(Modifier.fillMaxWidth()) {
                        TagMasterButton(
                            stringResource(R.string.SheetMusic),
                            onClick = { loadSheetMusic(context, tag, requests, snackbars) },
                            modifier = Modifier.fillMaxWidth().testTag("sheetMusicLink"),
                            icon = R.drawable.ic_sheet_music,
                            enabled = !requests.sheetMusicLoading,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 44.dp, vertical = 8.dp),
                            inset = 0.dp,
                        )
                        CompactBarberPole(
                            requests.sheetMusicLoading,
                            stringResource(R.string.detail_loading),
                            Modifier
                                .align(ViewAlign.CenterEnd)
                                .padding(end = 12.dp),
                        )
                    }
                }
            },
            prose =
                listOfNotNull(
                    tag.lyrics.takeIf { it.isPresent() }?.let { stringResource(R.string.Lyrics) to it },
                    tag.notes.takeIf { it.isPresent() }?.let { stringResource(R.string.Notes) to it },
                ),
        )
    }
    if (requests.ratingDialog) {
        RatingDialog(
            onDismiss = { requests.ratingDialog = false },
            onRate = { rating ->
                requests.ratingDialog = false
                submitRating(tag, rating, requests, snackbars, context)
            },
        )
    }
}

@Composable
private fun LabelledLine(
    label: String,
    value: String,
) {
    val colors = TagMasterTheme.colors
    val style = TagMasterType.bodyMedium
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.textViewWidth(label, style), style = style, color = colors.text)
        Text(value, Modifier.padding(start = 4.dp).textViewWidth(value, style), style = style, color = colors.text)
    }
}

/**
 * Summary content is two columns — facts beside prose — when both have at least 280dp of scaled
 * reading width, and one column otherwise.
 */
@Composable
private fun SummaryColumns(
    modifier: Modifier,
    facts: @Composable () -> Unit,
    prose: List<Pair<String, String>>,
) {
    val fontScale = LocalConfiguration.current.fontScale
    val colors = TagMasterTheme.colors
    Layout(
        modifier = modifier.fillMaxWidth(),
        content = {
            DetailSections { facts() }
            DetailSections {
                for ((label, text) in prose) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(label, Modifier.padding(bottom = 4.dp), style = TagMasterType.labelLarge, color = colors.text)
                        Text(text, Modifier.fillMaxWidth(), style = TagMasterType.bodyLarge, color = colors.text)
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val gap = 16.dp.roundToPx()
        val hasProse = prose.isNotEmpty()
        val wide = hasProse && width >= (280.dp.toPx() * fontScale * 2 + gap)
        if (wide) {
            val column = (width - gap) / 2
            val left = measurables[0].measure(Constraints.fixedWidth(column))
            val right = measurables[1].measure(Constraints.fixedWidth(width - gap - column))
            layout(width, maxOf(left.height, right.height)) {
                left.placeRelative(0, 0)
                right.placeRelative(column + gap, 0)
            }
        } else {
            val top = measurables[0].measure(Constraints.fixedWidth(width))
            val bottom = if (hasProse) measurables[1].measure(Constraints.fixedWidth(width)) else null
            val height = top.height + (bottom?.let { it.height + gap } ?: 0)
            layout(width, height) {
                top.placeRelative(0, 0)
                bottom?.placeRelative(0, top.height + gap)
            }
        }
    }
}

/** Stacks visible sections with 16dp between them. */
@Composable
fun DetailSections(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
}

@Composable
private fun summaryFacts(
    tag: Tag,
    requests: SummaryRequests,
    canRate: Boolean,
): List<DetailPair> {
    val body = TagMasterType.bodyMedium
    val bodyPaint = rememberTextViewPaint(body)

    fun budget(text: String): (androidx.compose.ui.unit.Density) -> Int =
        { _ -> textBudget(kotlin.math.ceil(bodyPaint.measureText(text)).toInt(), bodyPaint.textSize) }
    val pairs = mutableListOf<DetailPair>()
    pairs += textPair(stringResource(R.string.TagId), "${tag.id}", budget("${tag.id}"))
    if (tag.parts.isPresent()) pairs += textPair(stringResource(R.string.Parts), "${tag.parts}", budget("${tag.parts}"))
    pairs += textPair(stringResource(R.string.Type), "${tag.tagType}", budget("${tag.tagType}"))
    if (tag.classicTagNumber.isPresent()) {
        pairs += textPair(stringResource(R.string.ClassicTag), "${tag.classicTagNumber}", budget("${tag.classicTagNumber}"))
    }
    val ratingText = tag.rating?.let { String.format("%3.2f", it) }
    val rateLabel = stringResource(R.string.Rate)
    val submitting = stringResource(R.string.detail_submitting_rating)
    pairs +=
        DetailPair(
            stringResource(R.string.Rating),
            valueBudget = { density ->
                with(density) {
                    (80.dp.roundToPx() + (ratingText?.let { 8.dp.roundToPx() + kotlin.math.ceil(bodyPaint.measureText(it)).toInt() } ?: 0) +
                        18.76.dp.roundToPx() + 56.dp.roundToPx())
                }
            },
            isRating = true,
        ) { lines ->
            Row(verticalAlignment = ViewAlign.CenterVertically) {
                Row(verticalAlignment = ViewAlign.CenterVertically) {
                    RatingStars((tag.rating ?: 0.0).toFloat())
                    if (ratingText != null) {
                        Text(
                            ratingText,
                            Modifier
                                .padding(start = 8.dp)
                                .textViewWidth(ratingText, body),
                            style = body,
                            color = TagMasterTheme.colors.text,
                            onTextLayout = { lines.lines = it.lineCount },
                        )
                    }
                }
                CompactBarberPole(requests.ratingSubmitting, submitting)
                // app:tooltipText named the icon on a long press, and its ripple was borderless.
                WithTooltip(rateLabel, Modifier.padding(start = 8.dp)) { tooltip ->
                    Box(
                        Modifier
                            .size(48.dp)
                            .combinedClickable(
                                enabled = canRate,
                                interactionSource = null,
                                indication = ripple(bounded = false, radius = 24.dp),
                                role = Role.Button,
                                onLongClick = tooltip::longPressed,
                            ) { requests.ratingDialog = true }
                            .semantics { contentDescription = rateLabel }
                            .testTag("rateButton"),
                        contentAlignment = ViewAlign.Center,
                    ) {
                        PlatformIcon(
                            R.drawable.ic_rate,
                            tint = if (canRate) TagMasterTheme.colors.primary else TagMasterTheme.colors.primary.copy(alpha = 0.38f),
                        )
                    }
                }
            }
        }
    return pairs
}

private fun textPair(
    caption: String,
    value: String,
    budget: (androidx.compose.ui.unit.Density) -> Int,
) = DetailPair(caption, budget) { lines ->
    Text(
        value,
        Modifier.fillMaxWidth(),
        style = TagMasterType.bodyMedium,
        color = TagMasterTheme.colors.text,
        onTextLayout = { lines.lines = it.lineCount },
    )
}

/**
 * The written key as a full-width button that sounds the key note while pressed: outlined, and
 * filled in the accent while the note sounds.
 */
@Composable
private fun KeyNoteButton(tag: Tag) {
    val colors = TagMasterTheme.colors
    val player = rememberNotePlayer()
    val note = tag.keyNote
    val playing = note?.isPlaying == true
    val shape = RoundedCornerShape(8.dp)
    // The View button's own state drawable: outlined, filled with the accent while activated.
    val background = rememberDrawable(R.drawable.key_button_background)
    // The accent fill is the press feedback (the ripple is transparent while pressed or
    // activated); keyboard focus and hover show the control highlight, as the View button did.
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val hovered by interactions.collectIsHoveredAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val highlight = !playing && ((focused && keyboard) || hovered)
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (playing) Modifier.shadow(2.dp, shape) else Modifier)
            .drawBehind {
                background.state =
                    if (playing) intArrayOf(android.R.attr.state_enabled, android.R.attr.state_activated) else intArrayOf(android.R.attr.state_enabled)
                drawPlatform(background, 0, 0, size.width.toInt(), size.height.toInt())
                if (highlight) drawOutline(shape.createOutline(size, layoutDirection, this), colors.controlHighlight)
            }.clip(shape)
            .notePress(player, { tag.keyNote }, description = noteDescription(note), view = LocalView.current, interactionSource = interactions)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("playKeyNoteButton"),
        contentAlignment = ViewAlign.Center,
    ) {
        val content = if (playing) colors.onPrimary else colors.primary
        Row(verticalAlignment = ViewAlign.CenterVertically) {
            PlatformIcon(R.drawable.ic_key, Modifier.padding(end = 8.dp), tint = content)
            val key = tag.writtenKey ?: ""
            val label = TagMasterType.labelLarge.withoutLineHeight()
            Text(key, Modifier.textViewWidth(key, label), style = label, color = content)
        }
    }
}

/**
 * The lists this tag is in, as outlined chips that open the list, each with a 48dp remove
 * control, then the accent "Add to list" chip.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListChips(
    tag: Tag,
    dialogs: ListDialogs,
) {
    val context = LocalContext.current
    val snackbars = LocalSnackbars.current
    // A rename or reorder moves only the registry version, so the chips read it too.
    TagLists.version
    val keys = TagLists.allKeys().filter { ListModel(it).contains(tag.id) }
    val label = stringResource(R.string.list_chips_label)
    FlowRow(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (key in keys) {
            val name = TagLists.displayName(context, key)
            val removedMessage = stringResource(R.string.list_removed_from, name)
            val undo = stringResource(R.string.list_undo)
            ListChip(
                text = name,
                icon = listIconRes(key),
                accent = false,
                onClick = { openList(context, key) },
                closeLabel = stringResource(R.string.list_chip_remove, name),
                onClose = { removeFromList(key, tag.id, removedMessage, undo, snackbars) },
                modifier = Modifier.testTag("chip:$key"),
            )
        }
        ListChip(
            text = stringResource(R.string.list_add_to_list),
            icon = R.drawable.ic_add,
            accent = true,
            onClick = { dialogs.pick(tag.id) },
            modifier = Modifier.testTag("chip:add"),
        )
    }
}

/** An outlined Material 3 chip, measured as the View chips were. */
@Composable
private fun ListChip(
    text: String,
    icon: Int,
    accent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    closeLabel: String? = null,
    onClose: (() -> Unit)? = null,
) {
    val colors = TagMasterTheme.colors
    val fontScale = LocalConfiguration.current.fontScale
    val shape = RoundedCornerShape(8.dp)
    val labelColor = if (accent) colors.primary else colors.onSurface
    Row(
        modifier
            .widthIn(max = 220.dp * fontScale)
            .heightIn(min = 48.dp)
            // An outlined Material chip is filled with the surface color, hiding the watermark.
            .background(colors.surface, shape)
            .border(BorderStroke(1.dp, colors.outlineVariant), shape)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                if (onClose != null && closeLabel != null) {
                    customActions = listOf(CustomAccessibilityAction(closeLabel) { onClose(); true })
                }
            }.padding(start = 8.dp),
        verticalAlignment = ViewAlign.CenterVertically,
    ) {
        PlatformIcon(icon, tint = if (accent) colors.primary else colors.onSurfaceVariant, size = 18.dp)
        // MDC's chip reads its text size as a float, not whole pixels as a TextView does.
        val label =
            TagMasterType.labelLarge.withoutLineHeight().copy(
                fontSize = 14.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            )
        Text(
            text,
            Modifier
                .weight(1f, fill = false)
                .padding(start = 8.dp, end = 4.dp)
                .textViewWidth(text, label),
            style = label,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onClose != null) {
            Box(
                Modifier
                    .size(width = 44.dp, height = 48.dp)
                    .clickable(role = Role.Button, onClick = onClose)
                    .semantics { contentDescription = closeLabel ?: "" }
                    .padding(start = 8.dp, end = 12.dp),
                contentAlignment = ViewAlign.Center,
            ) {
                PlatformIcon(R.drawable.ic_clear, tint = colors.onSurfaceVariant)
            }
        } else {
            Box(Modifier.size(4.dp, 1.dp))
        }
    }
}

private fun openList(
    context: Context,
    key: String,
) {
    val intent =
        when (key) {
            // Home is the root of the task; return to it rather than stacking another copy.
            TagLists.FAVORITE ->
                Intent(context, MeActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            TagLists.TEACHABLE -> Intent(context, TeachableTagsActivity::class.java)
            else -> TagListActivity.intent(context, key)
        }
    context.startActivity(intent)
}

/** Removes the tag from [key], offering the place it came from back for a few seconds. */
private fun removeFromList(
    key: String,
    tagId: Int,
    message: String,
    undo: String,
    snackbars: depollsoft.tagmaster.ui.Snackbars,
) {
    val model = ListModel(key)
    val index = model.ids.indexOf(tagId)
    if (index < 0) return
    model.remove(tagId)
    snackbars.show(message, undo) {
        val current = ListModel(key)
        if (current.contains(tagId)) return@show
        // ListModel has no insert-at, so the tag goes back on the end and then home.
        current.add(tagId)
        if (index < current.ids.size) current.move(tagId, index)
    }
}

private fun loadSheetMusic(
    context: Context,
    tag: Tag,
    requests: SummaryRequests,
    snackbars: depollsoft.tagmaster.ui.Snackbars,
) {
    if (requests.sheetMusicLoading) return
    val location = tag.sheetMusicUri ?: return
    val type = location.type ?: return
    val uri = location.uri ?: return
    requests.sheetMusicLoading = true
    ContentCache(context).loadContentPublic(uri, type, false).continueWith({ task ->
        if (!requests.active) return@continueWith null
        requests.sheetMusicLoading = false
        if (task.isFaulted || task.isCancelled) {
            showSheetMusicError(context, tag, requests, snackbars)
            return@continueWith null
        }
        try {
            val path =
                Uri.parse(
                    "content://${context.packageName}/" + type + "/" +
                        Base64.encodeToString(uri.toByteArray(), Base64.URL_SAFE) + "/" + tag.id + "." + type,
                )
            val mimeType =
                if (type.lowercase(Locale.US) == "pdf") {
                    "application/pdf"
                } else {
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(type.lowercase(Locale.US))
                }
            val intent =
                Intent(Intent.ACTION_VIEW)
                    .putExtra("tagId", tag.id)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .setClass(context, SheetMusicActivity::class.java)
                    .setDataAndType(path, mimeType)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            snackbars.show(context.getString(R.string.detail_sheet_music_no_app, type))
        } catch (e: Exception) {
            showSheetMusicError(context, tag, requests, snackbars)
        }
        null
    }, Task.UI_THREAD_EXECUTOR)
}

private fun showSheetMusicError(
    context: Context,
    tag: Tag,
    requests: SummaryRequests,
    snackbars: depollsoft.tagmaster.ui.Snackbars,
) {
    snackbars.show(context.getString(R.string.detail_sheet_music_failed), context.getString(R.string.detail_retry)) {
        loadSheetMusic(context, tag, requests, snackbars)
    }
}

private fun submitRating(
    tag: Tag,
    rating: Int,
    requests: SummaryRequests,
    snackbars: depollsoft.tagmaster.ui.Snackbars,
    context: Context,
) {
    if (requests.ratingSubmitting || requests.rated || RatingsModel.isRated(tag.id)) return
    requests.ratingSubmitting = true
    tag.rate(rating).continueWith({ task ->
        // Persist a successful submission even if the user has left this screen.
        if (!task.isFaulted && !task.isCancelled) RatingsModel.addRating(tag.id)
        if (!requests.active) return@continueWith null
        requests.ratingSubmitting = false
        if (task.isFaulted || task.isCancelled) {
            snackbars.show(context.getString(R.string.detail_rating_failed), context.getString(R.string.detail_retry)) {
                submitRating(tag, rating, requests, snackbars, context)
            }
        } else {
            requests.rated = true
        }
        null
    }, Task.UI_THREAD_EXECUTOR)
}

/** "Select a rating": one to five stars, then Submit. */
@Composable
private fun RatingDialog(
    onDismiss: () -> Unit,
    onRate: (Int) -> Unit,
) {
    var rating by remember { mutableStateOf(0) }
    val colors = TagMasterTheme.colors
    TagMasterDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.ChooseARating),
        wrapWidth = true,
        dismiss = DialogButton(stringResource(android.R.string.cancel), onClick = onDismiss),
        confirm = DialogButton(stringResource(R.string.detail_submit), id = "ratingSubmit", enabled = rating >= 1) { onRate(rating) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = ViewAlign.CenterHorizontally,
        ) {
            RatingPicker(rating, { rating = it }, stringResource(R.string.ChooseARating), Modifier.testTag("ratingPicker"))
            Text(
                stringResource(R.string.detail_rating_hint),
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                style = TagMasterType.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
