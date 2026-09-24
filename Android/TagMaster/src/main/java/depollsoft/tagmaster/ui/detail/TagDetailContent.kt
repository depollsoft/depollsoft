package depollsoft.tagmaster.ui.detail

import android.animation.ValueAnimator
import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import depollsoft.tagmaster.FavoritesModel
import depollsoft.tagmaster.R
import depollsoft.tagmaster.TagDetailState
import depollsoft.tagmaster.TeachableTagsModel
import depollsoft.tagmaster.barbershop.Tag
import depollsoft.tagmaster.ui.BarAction
import depollsoft.tagmaster.ui.BarberPoleWatermark
import depollsoft.tagmaster.ui.BottomTabs
import depollsoft.tagmaster.ui.ButtonStyle
import depollsoft.tagmaster.ui.ListDialogs
import depollsoft.tagmaster.ui.LocalSnackbars
import depollsoft.tagmaster.ui.QuartetIllustration
import depollsoft.tagmaster.ui.ShowAs
import depollsoft.tagmaster.ui.TabItem
import depollsoft.tagmaster.ui.TagMasterButton
import depollsoft.tagmaster.ui.TagMasterTheme
import depollsoft.tagmaster.ui.TagMasterType
import depollsoft.tagmaster.ui.ViewAlign
import kotlinx.coroutines.launch

/**
 * One tag's detail below its toolbar: the quartet while the first load runs, the error and Retry
 * when it fails, and once loaded the four pages with their bottom tabs. Beside a list, the
 * two-pane screen paints the watermark once behind both panes, so [inPane] leaves it out.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TagDetailContent(
    state: TagDetailState,
    dialogs: ListDialogs,
    inPane: Boolean,
    modifier: Modifier = Modifier,
) {
    val tag = state.tag
    val loaded = tag != null
    val snackbars = LocalSnackbars.current
    val refreshFailed = stringResource(R.string.detail_tag_refresh_failed, state.tagId)
    val retry = stringResource(R.string.detail_retry)
    // Up while this tag's refresh has failed; the next load, another tag or leaving the screen
    // takes it down, as the fragment dismissed it at the start of every load and on destroy.
    LaunchedEffect(state.refreshFailed, state.tagId) {
        if (state.refreshFailed) {
            snackbars.showNow(refreshFailed, retry, indefinite = true) { state.retry() }
        }
    }
    val pager = rememberPagerState(initialPage = state.page) { pageTabs.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager) { snapshotFlow { pager.settledPage }.collect { state.page = it } }
    LaunchedEffect(state.page) { if (pager.settledPage != state.page) pager.scrollToPage(state.page) }
    Column(modifier.fillMaxSize()) {
        if (state.isLoading && loaded) {
            LinearProgressIndicator(
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "" }
                    .testTag("refreshProgress"),
                color = TagMasterTheme.colors.primary,
            )
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (tag != null) {
                if (!inPane) BarberPoleWatermark()
                DetailPages(state, tag, dialogs, pager)
            }
            if (state.isLoading && !loaded) LoadingState(state.tagId)
            if (state.loadFailed && !loaded && !state.isLoading) ErrorState(state)
        }
        if (tag != null) {
            BottomTabs(
                tabs = pageTabs.mapIndexed { index, (label, icon) -> TabItem(stringResource(label), icon, "detailTab:$index") },
                selected = pager.currentPage,
                position = pager.currentPage + pager.currentPageOffsetFraction,
                onSelect = { scope.launch { pager.animateScrollToPage(it) } },
                modifier = Modifier.testTag("detailTabs"),
            )
        }
    }
}

private val pageTabs =
    listOf(
        R.string.summary to R.drawable.ic_barberpole,
        R.string.details to R.drawable.ic_details,
        R.string.tracks to R.drawable.ic_tracks,
        R.string.videos to R.drawable.ic_video,
    )

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DetailPages(
    state: TagDetailState,
    tag: Tag,
    dialogs: ListDialogs,
    pager: androidx.compose.foundation.pager.PagerState,
) {
    // A tag chosen in the pane fades in, when the system allows motion.
    val reveal = remember { Animatable(1f) }
    LaunchedEffect(tag) {
        if (state.revealPending) {
            state.revealPending = false
            if (ValueAnimator.areAnimatorsEnabled()) {
                reveal.snapTo(0f)
                reveal.animateTo(1f, tween(200))
            }
        }
    }
    HorizontalPager(
        state = pager,
        modifier =
            Modifier
                .fillMaxSize()
                .alpha(reveal.value)
                .testTag("detailPager"),
        beyondViewportPageCount = 1,
        key = { it },
    ) { page ->
        when (page) {
            0 -> SummaryPage(tag, dialogs)
            1 -> DetailsPage(tag)
            2 -> TracksPage(tag, current = pager.currentPage == 2)
            else -> VideosPage(tag)
        }
    }
}

@Composable
private fun LoadingState(tagId: Int) {
    val colors = TagMasterTheme.colors
    val status = stringResource(R.string.detail_loading_tag, tagId)
    val gathering = stringResource(R.string.detail_gathering_quartet)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = ViewAlign.CenterHorizontally,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = false) {
                    contentDescription = "$gathering $status"
                    liveRegion = LiveRegionMode.Polite
                }.testTag("detailLoading"),
            horizontalAlignment = ViewAlign.CenterHorizontally,
        ) {
            QuartetIllustration(loading = true)
            Text(
                gathering,
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clearAndSetSemantics { },
                style = TagMasterType.titleLarge,
                color = colors.text,
                textAlign = TextAlign.Center,
            )
            Text(
                status,
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clearAndSetSemantics { },
                style = TagMasterType.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ErrorState(state: TagDetailState) {
    val colors = TagMasterTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp)
            .testTag("detailError"),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = ViewAlign.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.detail_tag_load_failed, state.tagId),
            Modifier.fillMaxWidth(),
            style = TagMasterType.bodyLarge,
            color = colors.text,
            textAlign = TextAlign.Center,
        )
        TagMasterButton(
            stringResource(R.string.detail_retry),
            onClick = { state.retry() },
            modifier = Modifier.padding(top = 16.dp).testTag("detailRetry"),
            style = ButtonStyle.Tonal,
            icon = R.drawable.ic_refresh,
            enabled = !state.isLoading,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 10.dp),
        )
    }
}

/**
 * The tag actions a detail toolbar offers once a tag has loaded: favorite and teachable toggles,
 * Add to list, Refresh and Share, in the order the menu listed them.
 */
@Composable
fun tagActions(
    state: TagDetailState,
    dialogs: ListDialogs,
): List<BarAction> {
    val tag = state.tag ?: return emptyList()
    val context = LocalContext.current
    val favorite = FavoritesModel.getIsFavorite(tag.id)
    val teachable = TeachableTagsModel.getIsTeachableTag(tag.id)
    return listOf(
        if (favorite) {
            BarAction("removeFavorite", stringResource(R.string.RemoveFavorite), R.drawable.ic_favorite) { FavoritesModel.removeFavorite(tag.id) }
        } else {
            BarAction("addFavorite", stringResource(R.string.AddFavorite), R.drawable.ic_favorite_border) { FavoritesModel.addFavorite(tag.id) }
        },
        if (teachable) {
            BarAction("removeTeachable", stringResource(R.string.RemoveTeachableTag), R.drawable.ic_people) {
                TeachableTagsModel.removeTeachableTag(tag.id)
            }
        } else {
            BarAction("addTeachable", stringResource(R.string.AddTeachableTag), R.drawable.ic_people_border) {
                TeachableTagsModel.addTeachableTag(tag.id)
            }
        },
        BarAction("addToList", stringResource(R.string.list_add_to_list), R.drawable.ic_playlist_add) { dialogs.pick(tag.id) },
        BarAction("refresh", stringResource(R.string.RefreshTag), R.drawable.ic_refresh, enabled = !state.isLoading) { state.refresh() },
        BarAction("share", stringResource(R.string.share), R.drawable.ic_share) { context.startActivity(shareIntent(context, tag)) },
    ).map { it.copy(showAs = ShowAs.IfRoom) }
}

private fun shareIntent(
    context: Context,
    tag: Tag,
) = ShareCompat
    .IntentBuilder(context)
    .setChooserTitle(R.string.detail_share_title)
    .setType("text/plain")
    .setSubject(context.getString(R.string.detail_share_subject, tag.title))
    .setText(context.getString(R.string.detail_share_text, tag.title, tag.tagUri))
    .createChooserIntent()
