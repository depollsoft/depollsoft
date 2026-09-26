package depollsoft.pitchperfect

import android.content.Context
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import depollsoft.pitchperfect.ui.PlateBackground
import depollsoft.pitchperfect.ui.PlateBottomNavigation
import depollsoft.pitchperfect.ui.PlateDestination
import depollsoft.pitchperfect.ui.PlateNavigationRail
import depollsoft.pitchperfect.ui.PlateText
import depollsoft.pitchperfect.ui.PlateTopBar
import depollsoft.pitchperfect.ui.isTablet
import depollsoft.pitchperfect.ui.plateColors
import depollsoft.pitchperfect.ui.plateText
import kotlinx.coroutines.launch

/** The four instruments, in tab order. */
enum class MainTab(
    val label: Int,
    val icon: Int,
    val testTag: String,
) {
    PITCH_PIPE(R.string.quick_pitch, R.drawable.ic_pitchpipe, TestTags.TAB_PITCH_PIPE),
    NOTES(R.string.notes, R.drawable.ic_note, TestTags.TAB_NOTES),
    KEYS(R.string.keys, R.drawable.ic_keys, TestTags.TAB_KEYS),
    SONGS(R.string.songs, R.drawable.ic_songs, TestTags.TAB_SONGS),
}

/**
 * The banner slot: whether it shows, how tall the adaptive banner will be, and the banner once
 * it exists. The activity owns loading; the screen only reserves the space and hosts the view.
 */
class AdSlot(
    val visible: Boolean,
    val heightPx: Int,
    val banner: View?,
    val onRemoveAds: () -> Unit,
)

/**
 * The main screen: the instrument pages under the action bar, the banner slot, and the bottom
 * navigation (a rail on tablets).
 */
@Composable
fun MainScreen(
    pagerState: PagerState,
    adSlot: AdSlot,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
    page: @Composable (MainTab) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val destinations =
        MainTab.entries.map { PlateDestination(stringResource(it.label), it.icon, it.testTag) }
    val selection = rememberTabSelection(pagerState)
    val select: (Int) -> Unit = { index ->
        // Pages stay resident and static artwork is cached, so the standard transition does not
        // inflate or decode mid-swipe.
        if (selection.selected != index) scope.launch { selection.scrollTo(index) }
    }
    val colors = plateColors
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(colors.ground)) {
            // The window action bar sat above the rail, so the rail's shadow never reached it.
            PlateTopBar(stringResource(R.string.app_name), Modifier.zIndex(2f), actions = actions)
            if (isTablet) {
                Row(Modifier.weight(1f)) {
                    PlateNavigationRail(
                        destinations,
                        selection.selected,
                        select,
                        Modifier.fillMaxHeight().zIndex(1f).shadow(8.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Pages(pagerState, Modifier.weight(1f), page)
                        AdArea(adSlot, endMargin = 8.dp)
                    }
                }
            } else {
                Pages(pagerState, Modifier.weight(1f), page)
                AdArea(adSlot, endMargin = 4.dp)
                PlateBottomNavigation(destinations, selection.selected, select)
            }
        }
        overlay()
    }
}

/**
 * Which navigation item is lit, as ViewPager2 reported it: a tapped item lights at once and stays
 * lit while the pager scrolls past the pages between; a drag changes it as the finger lets go,
 * toward the page the pager then settles on.
 */
@Stable
class TabSelection(
    private val pager: PagerState,
    private val dragged: State<Boolean>,
) {
    private var tapped by mutableStateOf<Int?>(null)

    val selected: Int
        get() = tapped ?: if (dragged.value) pager.settledPage else pager.targetPage

    suspend fun scrollTo(index: Int) {
        tapped = index
        try {
            pager.animateScrollToPage(index)
        } finally {
            if (tapped == index) tapped = null
        }
    }
}

@Composable
fun rememberTabSelection(pager: PagerState): TabSelection {
    val dragged = pager.interactionSource.collectIsDraggedAsState()
    return remember(pager) { TabSelection(pager, dragged) }
}

@Composable
private fun Pages(
    pagerState: PagerState,
    modifier: Modifier,
    page: @Composable (MainTab) -> Unit,
) {
    // ViewPager2 waited for twice the usual slop (the paging slop) before a sideways finger became
    // a page swipe, so a slightly sideways press on a note did not page and cut it off. The pages
    // themselves keep the usual slop for their own gestures.
    val content = LocalViewConfiguration.current
    val paging = rememberPagingViewConfiguration(content)
    PlateBackground(modifier.fillMaxWidth()) {
        CompositionLocalProvider(LocalViewConfiguration provides paging) {
            HorizontalPager(
                pagerState,
                Modifier.fillMaxSize().testTag(TestTags.PAGER),
                beyondViewportPageCount = MainTab.entries.size - 1,
            ) { index ->
                CompositionLocalProvider(LocalViewConfiguration provides content) { page(MainTab.entries[index]) }
            }
        }
    }
}

/** [base] with the platform's paging touch slop, for a pager's own swipe. */
@Composable
fun rememberPagingViewConfiguration(base: ViewConfiguration): ViewConfiguration {
    val context = LocalContext.current
    return remember(base, context) {
        val slop = android.view.ViewConfiguration.get(context).scaledPagingTouchSlop.toFloat()
        object : ViewConfiguration by base {
            override val touchSlop: Float get() = slop
        }
    }
}

@Composable
internal fun AdArea(
    slot: AdSlot,
    endMargin: Dp,
) {
    if (!slot.visible) return
    val context = LocalContext.current
    // The link is `?android:textAppearanceSmall`, italic.
    val linkColor = remember(context) { smallTextColor(context) }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        PlateText(
            stringResource(R.string.RemoveAds),
            style = plateText(14.sp, linkColor, style = FontStyle.Italic),
            modifier =
                Modifier
                    .padding(end = endMargin)
                    .testTag(TestTags.REMOVE_ADS)
                    .clickable(onClick = slot.onRemoveAds),
        )
    }
    val height = with(LocalDensity.current) { slot.heightPx.toDp() }
    Box(Modifier.fillMaxWidth().height(height).testTag(TestTags.AD_CONTAINER)) {
        val banner = slot.banner
        if (banner != null) {
            // A reload replaces the AdView; keying on it hosts the new view, not the destroyed one.
            key(banner) { AndroidView(factory = { banner }, modifier = Modifier.fillMaxSize()) }
        }
    }
}

private fun smallTextColor(context: Context): Color {
    val appearance =
        context.obtainStyledAttributes(intArrayOf(android.R.attr.textAppearanceSmall)).run {
            getResourceId(0, 0).also { recycle() }
        }
    val colors = context.obtainStyledAttributes(appearance, intArrayOf(android.R.attr.textColor))
    val color = colors.getColorStateList(0)?.defaultColor ?: android.graphics.Color.GRAY
    colors.recycle()
    return Color(color)
}
