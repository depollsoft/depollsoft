package depollsoft.pitchperfect

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import depollsoft.pitchperfect.ui.PlateText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import depollsoft.pitchperfect.ui.PlateBackground
import depollsoft.pitchperfect.ui.PlateBottomNavigation
import depollsoft.pitchperfect.ui.PlateDestination
import depollsoft.pitchperfect.ui.PlateNavigationRail
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
    val banner: android.view.View?,
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
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    page: @Composable (MainTab) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val destinations =
        MainTab.entries.map { PlateDestination(stringResource(it.label), it.icon, it.testTag) }
    val select: (Int) -> Unit = { index ->
        // Pages stay resident and static artwork is cached, so the standard transition does not
        // inflate or decode mid-swipe.
        if (pagerState.currentPage != index) scope.launch { pagerState.animateScrollToPage(index) }
    }
    val colors = plateColors
    Column(modifier.fillMaxSize().background(colors.ground)) {
        // The window action bar sat above the rail, so the rail's shadow never reached it.
        PlateTopBar(stringResource(R.string.app_name), Modifier.zIndex(2f), actions = actions)
        if (isTablet) {
            Row(Modifier.weight(1f)) {
                PlateNavigationRail(
                    destinations,
                    pagerState.currentPage,
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
            PlateBottomNavigation(destinations, pagerState.currentPage, select)
        }
    }
}

@Composable
private fun Pages(
    pagerState: PagerState,
    modifier: Modifier,
    page: @Composable (MainTab) -> Unit,
) {
    PlateBackground(modifier.fillMaxWidth()) {
        HorizontalPager(
            pagerState,
            Modifier.fillMaxSize().testTag(TestTags.PAGER),
            beyondViewportPageCount = MainTab.entries.size - 1,
        ) { index -> page(MainTab.entries[index]) }
    }
}

@Composable
internal fun AdArea(
    slot: AdSlot,
    endMargin: androidx.compose.ui.unit.Dp,
) {
    if (!slot.visible) return
    val context = LocalContext.current
    // `?android:textAppearanceSmall`, italic, as the View layout styled the link.
    val linkColor = androidx.compose.runtime.remember(context) { smallTextColor(context) }
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

private fun smallTextColor(context: android.content.Context): Color {
    val appearance =
        context.obtainStyledAttributes(intArrayOf(android.R.attr.textAppearanceSmall)).run {
            getResourceId(0, 0).also { recycle() }
        }
    val colors = context.obtainStyledAttributes(appearance, intArrayOf(android.R.attr.textColor))
    val color = colors.getColorStateList(0)?.defaultColor ?: android.graphics.Color.GRAY
    colors.recycle()
    return Color(color)
}
