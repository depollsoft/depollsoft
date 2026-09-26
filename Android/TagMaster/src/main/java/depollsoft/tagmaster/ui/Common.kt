package depollsoft.tagmaster.ui

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import depollsoft.compose.PlatformIcon
import depollsoft.compose.ViewAlign
import depollsoft.compose.drawCenterInside
import depollsoft.compose.rememberDrawable
import depollsoft.tagmaster.R

/** The barber pole behind a screen's content, at its own size and centered (CENTER_INSIDE). */
@Composable
fun BarberPoleWatermark(modifier: Modifier = Modifier) {
    val drawable = rememberDrawable(R.drawable.ic_barberpole)
    Box(modifier.fillMaxSize().drawBehind { drawCenterInside(drawable) })
}

/**
 * A text link: underlined when it goes somewhere, opened with ACTION_VIEW, announced as a button,
 * with a 48dp target around the text.
 */
@Composable
fun Hyperlink(
    text: String,
    uri: String?,
    modifier: Modifier = Modifier,
    style: TextStyle = TagMasterType.bodySmall,
    textAlign: TextAlign? = TextAlign.Center,
    contentAlignment: Alignment = ViewAlign.Center,
) {
    val context = LocalContext.current
    val shown =
        with(TagMasterType) { style.withoutLineHeight() }
            .copy(textDecoration = if (uri != null) TextDecoration.Underline else null)
    val color = if (uri != null) TagMasterTheme.colors.primary else TagMasterTheme.colors.text
    // One centered line sits where a centered TextView would put it; anything else wraps normally.
    val singleCenteredLine = textAlign == TextAlign.Center && contentAlignment == ViewAlign.Center && '\n' !in text
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button) {
                if (uri != null) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
            },
        contentAlignment = contentAlignment,
        propagateMinConstraints = singleCenteredLine,
    ) {
        if (singleCenteredLine) {
            ViewCenteredText(text, shown, color)
        } else {
            Text(text, style = shown, color = color, textAlign = textAlign)
        }
    }
}

/** A heading that opens a section of a list: Lists, Favorites. */
@Composable
fun SectionHeading(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
                .semantics { heading() },
        style = TagMasterType.titleLarge,
        color = TagMasterTheme.colors.text,
    )
}

/**
 * A 56dp action row: a primary-tinted icon, a medium-weight title and optional trailing content,
 * announced as a button.
 */
@Composable
fun ActionRow(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    startGutter: Dp = 16.dp,
    endGutter: Dp = 16.dp,
    leading: @Composable RowScope.() -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(start = startGutter, end = endGutter, top = 12.dp, bottom = 12.dp),
        verticalAlignment = ViewAlign.CenterVertically,
    ) {
        leading()
        PlatformIcon(icon, Modifier.padding(end = 16.dp), tint = TagMasterTheme.colors.primary)
        Box(Modifier.weight(1f)) { content() }
        trailing()
    }
}

/** The medium-weight title of an [ActionRow]. */
@Composable
fun ActionTitle(
    text: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text,
        modifier = modifier.fillMaxWidth(),
        style = TagMasterType.titleLarge.copy(fontWeight = FontWeight.Medium),
        color = TagMasterTheme.colors.text,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** A secondary line under an [ActionTitle]: "12 tags". */
@Composable
fun ActionDetail(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        modifier = modifier.fillMaxWidth(),
        style = TagMasterType.bodyMedium,
        color = TagMasterTheme.colors.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** The floating Search button in the bottom-end corner of a list screen. */
@Composable
fun BoxScope.SearchFab(onClick: () -> Unit) {
    val colors = TagMasterTheme.colors
    val label = stringResource(R.string.Search)
    FloatingActionButton(
        onClick = onClick,
        modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
                .semantics { contentDescription = label }
                .testTag("searchButton"),
        shape = RoundedCornerShape(16.dp),
        containerColor = colors.primary,
        contentColor = colors.onPrimary,
        elevation = FloatingActionButtonDefaults.elevation(),
    ) {
        PlatformIcon(R.drawable.ic_search, tint = colors.onPrimary)
    }
}

/** A centered empty state: a message, a hint under it and optional actions. */
@Composable
fun EmptyState(
    message: String,
    hint: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    val colors = TagMasterTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = ViewAlign.CenterHorizontally,
    ) {
        Text(message, Modifier.fillMaxWidth(), style = TagMasterType.bodyLarge, color = colors.text, textAlign = TextAlign.Center)
        Text(
            hint,
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            style = TagMasterType.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        actions()
    }
}

/**
 * Lists and forms keep a readable measure on wide windows, as the View screens' content insets
 * did: [content] is handed the horizontal inset that centres it at most [maxWidth] wide, to add as
 * padding inside its scrolling container. The container keeps the full width, so its scrollbar
 * stays at the edge and its margins still scroll. In the narrow list pane of the two-pane layout
 * the inset is zero.
 */
@Composable
fun ReadingWidth(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 640.dp,
    content: @Composable (inset: Dp) -> Unit,
) {
    BoxWithConstraints(modifier) {
        val inset = with(LocalDensity.current) { ((constraints.maxWidth - maxWidth.roundToPx()).coerceAtLeast(0) / 2).toDp() }
        content(inset)
    }
}
