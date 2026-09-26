package depollsoft.tagmaster.ui

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Which tab a pager's tab strip shows as selected, as TabLayoutMediator decided: a tapped tab is
 * selected at once and stays selected while the pager animates across the pages in between; a
 * swipe selects the page it is heading for once the finger lifts, never the pages it passes.
 */
@Stable
class PagerTabs internal constructor(
    private val pager: PagerState,
    private val scope: CoroutineScope,
    private val dragged: State<Boolean>,
) {
    private var tapped: Int? by mutableStateOf(null)

    val selected: Int
        get() = tapped ?: if (dragged.value) pager.settledPage else pager.targetPage

    /** A tap on tab [page]: select it and animate there. */
    fun select(page: Int) {
        tapped = page
        scope.launch {
            try {
                pager.animateScrollToPage(page)
            } finally {
                if (tapped == page) tapped = null
            }
        }
    }
}

@Composable
fun rememberPagerTabs(pager: PagerState): PagerTabs {
    val scope = rememberCoroutineScope()
    val dragged = pager.interactionSource.collectIsDraggedAsState()
    return remember(pager) { PagerTabs(pager, scope, dragged) }
}

private val LocalUsualViewConfiguration = compositionLocalOf<ViewConfiguration?> { null }

/**
 * A pager inside [content] starts a swipe only past the platform's paging touch slop, twice the
 * usual one, so a finger that drifts sideways on a row scrolls the list or
 * plays its note instead of turning the page. Wrap each page in [UsualTouchSlop] so the page's
 * own scrolling keeps the usual slop.
 */
@Composable
fun PagingTouchSlop(content: @Composable () -> Unit) {
    val usual = LocalViewConfiguration.current
    val context = LocalContext.current
    val paging =
        remember(usual, context) {
            val slop = android.view.ViewConfiguration.get(context).scaledPagingTouchSlop.toFloat()
            object : ViewConfiguration by usual {
                override val touchSlop: Float = slop
            }
        }
    CompositionLocalProvider(LocalViewConfiguration provides paging, LocalUsualViewConfiguration provides usual, content = content)
}

/** A page of a [PagingTouchSlop] pager, back on the usual touch slop. */
@Composable
fun UsualTouchSlop(content: @Composable () -> Unit) {
    val usual = LocalUsualViewConfiguration.current
    if (usual == null) content() else CompositionLocalProvider(LocalViewConfiguration provides usual, content = content)
}
