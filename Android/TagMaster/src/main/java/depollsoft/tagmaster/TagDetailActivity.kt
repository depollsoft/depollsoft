package depollsoft.tagmaster

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import bolts.Task
import depollsoft.tagmaster.barbershop.Tag
import depollsoft.tagmaster.ui.ListDialogsHost
import depollsoft.tagmaster.ui.TagMasterTopBar
import depollsoft.tagmaster.ui.detail.TagDetailContent
import depollsoft.tagmaster.ui.detail.tagActions
import depollsoft.tagmaster.ui.navigateUpOrHome
import depollsoft.tagmaster.ui.rememberListDialogs
import depollsoft.tagmaster.ui.setTagMasterContent

/**
 * Full-screen host for one tag (phones, deep links, Random Tag): the toolbar with the tag's title
 * and actions over its detail.
 */
class TagDetailActivity : AppCompatActivity() {
    /**
     * Per-screen request dependency, read when the first load starts. Tests set it before
     * onCreate without changing the cache contract.
     */
    internal var tagLoader: (Int, Boolean) -> Task<Tag> = { id, refresh -> Tag.loadTagById(id, refresh) }

    lateinit var detail: TagDetailState
        private set

    val tag: Tag?
        get() = detail.tag

    val isLoading: Boolean
        get() = detail.isLoading

    val loadFailed: Boolean
        get() = detail.loadFailed

    val tagId: Int
        get() = intent.getIntExtra(TAG_ID_EXTRA, -1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val restoredId = savedInstanceState?.getInt(STATE_TAG_ID, -1)?.takeIf { it > 0 }
        detail = TagDetailState(restoredId ?: tagId, this, tagLoader)
        detail.page = savedInstanceState?.getInt(STATE_PAGE, 0) ?: 0
        setTagMasterContent { TagDetailScreen(detail) { navigateUpOrHome() } }
        detail.start()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getIntExtra(TAG_ID_EXTRA, -1) == tagId) return
        setIntent(intent)
        detail.showTag(tagId)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_TAG_ID, detail.tagId)
        outState.putInt(STATE_PAGE, detail.page)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        detail.stop()
        super.onDestroy()
    }

    companion object {
        @JvmField
        val TAG_ID_EXTRA = "depollsoft.tagmaster.tagid"
        private const val STATE_TAG_ID = "depollsoft.tagmaster.TagDetailActivity.tagId"
        private const val STATE_PAGE = "depollsoft.tagmaster.TagDetailActivity.page"
    }
}

/** The detail screen: the tag's title (the app name until it loads) and actions, then its pages. */
@Composable
fun TagDetailScreen(
    detail: TagDetailState,
    onNavigateUp: () -> Unit,
) {
    val dialogs = rememberListDialogs()
    val tag = detail.tag
    Column(Modifier.fillMaxSize()) {
        TagMasterTopBar(
            title = tag?.title ?: stringResource(R.string.detail_brand_title),
            brandTitle = tag?.title == null,
            onNavigateUp = onNavigateUp,
            actions = tagActions(detail, dialogs),
            modifier = Modifier.fillMaxWidth(),
        )
        TagDetailContent(detail, dialogs, inPane = false, modifier = Modifier.weight(1f))
    }
    ListDialogsHost(dialogs)
}
