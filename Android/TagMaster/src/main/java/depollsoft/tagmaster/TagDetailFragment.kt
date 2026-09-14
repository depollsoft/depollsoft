package depollsoft.tagmaster

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.MenuRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ShareCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import bolts.Task
import com.bindroid.trackable.ComparingTrackableField
import com.bindroid.trackable.trackable
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import depollsoft.lib.compat.ui.Activities
import depollsoft.lib.compat.ui.MenuItems
import depollsoft.lib.util.ContentCache
import depollsoft.tagmaster.barbershop.Tag

/**
 * The loaded tag, as the detail pages see it.
 *
 * [androidx.fragment.app.Fragment.getTag] is final and returns the fragment's name, so the object
 * the pages reach through their `parent` property cannot be the fragment itself. Each page resolves
 * `parent` to the host's [TagDetailModel], which keeps the Bindroid paths ("Parent.Tag.Title") and
 * lambdas (`parent.tag?.title`) working unchanged.
 */
interface TagDetailHost {
    val tag: Tag?
}

/** Holder for the tag currently on display, so Bindroid re-evaluates when it is replaced. */
class TagDetailModel : TagDetailHost {
    // Tag.equals compares IDs; a refreshed instance still needs to update every bound field.
    private val currentTag = ComparingTrackableField<Tag?>(null) { left, right -> left === right }

    override var tag: Tag?
        get() = currentTag.get()
        set(value) {
            currentTag.set(value)
        }
}

/**
 * One tag's detail: the loading composition, the error state, the four pages and their bottom tabs,
 * and the tag actions. Hosted full-screen by [TagDetailActivity] on phones and inside the detail
 * pane of a list screen on tablets ([TagPaneController]).
 */
class TagDetailFragment : Fragment() {
    val model = TagDetailModel()

    var tag: Tag?
        get() = model.tag
        set(value) {
            model.tag = value
            renderState()
        }

    var isLoading: Boolean by trackable(false)
    var loadFailed: Boolean by trackable(false)

    /** Per-screen request dependency. Tests can control completion without changing the cache. */
    internal var tagLoader: ((Int, Boolean) -> Task<Tag>)? = null

    /** Called whenever the loaded tag or the loading state changes, for host chrome. */
    internal var onStateChanged: (() -> Unit)? = null

    var tagId: Int = INVALID_TAG_ID
        private set

    private var requestGeneration = 0
    private var retryRefresh = false
    private var refreshError: Snackbar? = null
    private var revealPending = false

    private val loader: (Int, Boolean) -> Task<Tag>
        get() = tagLoader ?: (activity as? TagDetailActivity)?.tagLoader ?: { id, refresh -> Tag.loadTagById(id, refresh) }

    private val shareIntent: Intent
        get() {
            val current = model.tag!!
            return ShareCompat
                .IntentBuilder(requireActivity())
                .setChooserTitle(R.string.detail_share_title)
                .setType("text/plain")
                .setSubject(getString(R.string.detail_share_subject, current.title))
                .setText(getString(R.string.detail_share_text, current.title, current.tagUri))
                .createChooserIntent()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tagId =
            when {
                savedInstanceState?.containsKey(STATE_TAG_ID) == true -> savedInstanceState.getInt(STATE_TAG_ID)
                arguments?.containsKey(ARG_TAG_ID) == true -> requireArguments().getInt(ARG_TAG_ID)
                else -> (activity as? TagDetailActivity)?.tagId ?: INVALID_TAG_ID
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_TAG_ID, tagId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val rootView = inflater.inflate(R.layout.tag_detail_content, container, false)
        rootView.findViewById<TabLayout>(R.id.tabLayout).applyHorizontalInsetsAsPadding()
        rootView.findViewById<View>(R.id.detailLoadingState).applyHorizontalInsetsAsPadding()
        rootView.findViewById<View>(R.id.detailRetryButton).setOnClickListener { loadQueryItem(retryRefresh) }
        return rootView
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (model.tag != null) {
            renderState()
        } else {
            loadQueryItem(false)
        }
    }

    /** Switches the pane to another tag, keeping the pager (and so the open page) in place. */
    fun showTag(id: Int) {
        if (id == tagId && (model.tag != null || isLoading)) return
        ++requestGeneration
        tagId = id
        model.tag = null
        isLoading = false
        loadFailed = false
        revealPending = true
        if (view == null) return
        loadQueryItem(false)
    }

    fun refresh() = loadQueryItem(true)

    private fun usable(): Boolean {
        val host = activity ?: return false
        return view != null && !host.isFinishing && !host.isDestroyed
    }

    private fun loadQueryItem(refresh: Boolean) {
        if (isLoading || !usable()) return
        // No tag was named (an inflated preview, or an intent without the extra): nothing to load.
        if (tagId <= 0) return
        val host = requireActivity()
        val requestedId = tagId
        val generation = ++requestGeneration
        val original = model.tag
        retryRefresh = refresh
        refreshError?.dismiss()
        isLoading = true
        loadFailed = false
        renderState()
        Activities.invalidateOptionsMenu(host)

        val request =
            if (refresh && original != null) {
                Task
                    .callInBackground<Void> {
                        val cache = ContentCache(host)
                        for (loc in listOfNotNull(
                            original.allPartsTrackUri,
                            original.baritoneTrackUri,
                            original.bassTrackUri,
                            original.leadTrackUri,
                            original.notationUri,
                            original.other1TrackUri,
                            original.other2TrackUri,
                            original.other3TrackUri,
                            original.other4TrackUri,
                            original.tenorTrackUri,
                            original.sheetMusicUri,
                        )) {
                            cache.deletePrivateContent(loc.uri, loc.type)
                            cache.deletePublicContent(loc.uri, loc.type)
                        }
                        null
                    }.continueWithTask { loader(requestedId, refresh) }
            } else {
                try {
                    loader(requestedId, refresh)
                } catch (error: Exception) {
                    Task.forError<Tag>(error)
                }
            }
        request.continueWith { task ->
            host.runOnUiThread {
                if (!usable() || generation != requestGeneration || tagId != requestedId) return@runOnUiThread
                isLoading = false
                if (!task.isFaulted && !task.isCancelled && task.result?.id == requestedId) {
                    model.tag = task.result
                } else {
                    // A refresh failure must not discard an already-visible tag.
                    loadFailed = true
                    requireView().findViewById<TextView>(R.id.detailErrorText).text =
                        getString(R.string.detail_tag_load_failed, requestedId)
                    if (model.tag != null) {
                        refreshError =
                            Snackbar
                                .make(
                                    requireView().findViewById(R.id.frameLayout1),
                                    getString(R.string.detail_tag_refresh_failed, requestedId),
                                    Snackbar.LENGTH_INDEFINITE,
                                ).setAction(R.string.detail_retry) { loadQueryItem(true) }
                        refreshError?.show()
                    }
                }
                renderState()
                Activities.invalidateOptionsMenu(host)
            }
            null
        }
    }

    private fun renderState() {
        val rootView = view ?: return
        val loaded = model.tag != null
        val initialLoading = isLoading && !loaded

        fun show(
            id: Int,
            visible: Boolean,
        ) {
            rootView.findViewById<View>(id).visibility = if (visible) View.VISIBLE else View.GONE
        }
        show(R.id.detailLoadingState, initialLoading)
        rootView.findViewById<TagLoadingView>(R.id.quartetIllustration).loading = initialLoading
        val status = getString(R.string.detail_loading_tag, tagId)
        rootView.findViewById<TextView>(R.id.detailLoadingStatus).text = status
        rootView.findViewById<View>(R.id.detailLoadingComposition).contentDescription =
            getString(R.string.detail_gathering_quartet) + " " + status
        show(R.id.imageView1, loaded)
        val viewPager = rootView.findViewById<ViewPager2>(R.id.viewPager)
        val wasVisible = viewPager.visibility == View.VISIBLE
        show(R.id.viewPager, loaded)
        show(R.id.tabLayout, loaded)
        show(R.id.progress, isLoading && loaded)
        show(R.id.detailErrorState, loadFailed && !loaded && !isLoading)
        rootView.findViewById<View>(R.id.detailRetryButton).isEnabled = !isLoading
        if (loaded) setUpPagerIfNeeded(rootView)
        if (loaded && !wasVisible && revealPending) {
            revealPending = false
            // Motion only when the system allows it; otherwise the new tag simply appears.
            if (android.os.Build.VERSION.SDK_INT < 26 || ValueAnimator.areAnimatorsEnabled()) {
                viewPager.alpha = 0f
                viewPager.animate().alpha(1f).setDuration(REVEAL_DURATION_MS).start()
            } else {
                viewPager.alpha = 1f
            }
        }
        onStateChanged?.invoke()
    }

    private fun setUpPagerIfNeeded(rootView: View) {
        val viewPager = rootView.findViewById<ViewPager2>(R.id.viewPager)
        if (viewPager.adapter != null) return
        val tabLayout = rootView.findViewById<TabLayout>(R.id.tabLayout)
        viewPager.adapter =
            object : FragmentStateAdapter(this) {
                override fun getItemCount(): Int = 4

                override fun createFragment(position: Int): Fragment =
                    when (position) {
                        0 -> TagSummaryFragment()
                        1 -> TagMiscFragment()
                        2 -> TagTracksFragment()
                        3 -> TagVideosFragment()
                        else -> Fragment()
                    }
            }
        val tabs = PopupMenu(requireContext(), tabLayout).apply { inflate(R.menu.tagdetailnavigation) }.menu
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val item = tabs.getItem(position)
            tab.text = item.title
            tab.icon = item.icon
            tab.id = item.itemId
            tab.setCustomView(R.layout.bottom_tab_content)
        }.attach()
    }

    override fun onResume() {
        super.onResume()
        view?.findViewById<TagLoadingView>(R.id.quartetIllustration)?.hostResumed = true
    }

    override fun onPause() {
        view?.findViewById<TagLoadingView>(R.id.quartetIllustration)?.hostResumed = false
        super.onPause()
    }

    override fun onDestroyView() {
        ++requestGeneration
        refreshError?.dismiss()
        refreshError = null
        view?.findViewById<TagLoadingView>(R.id.quartetIllustration)?.apply {
            hostResumed = false
            loading = false
        }
        super.onDestroyView()
    }

    /** Builds the tag actions into [menu]; false while no tag is loaded, exactly as before. */
    fun buildMenu(
        menu: Menu,
        inflater: MenuInflater,
        @MenuRes menuRes: Int = R.menu.tagdetailmenu,
    ): Boolean {
        if (model.tag == null) return false
        inflater.inflate(menuRes, menu)
        for (id in intArrayOf(R.id.addFavoriteMenuItem, R.id.removeFavoriteMenuItem, R.id.shareMenuItem)) {
            menu.findItem(id)?.let { MenuItems.setShowAsAction(it, MenuItems.SHOW_AS_ACTION_IF_ROOM) }
        }
        return true
    }

    fun prepareMenu(menu: Menu) {
        val current = model.tag ?: return
        menu.findItem(R.id.refreshMenuItem)?.isEnabled = !isLoading
        menu.findItem(R.id.addFavoriteMenuItem)?.isVisible = !FavoritesModel.getIsFavorite(current.id)
        menu.findItem(R.id.removeFavoriteMenuItem)?.isVisible = FavoritesModel.getIsFavorite(current.id)
        menu.findItem(R.id.addTeachableTagMenuItem)?.isVisible = !TeachableTagsModel.getIsTeachableTag(current.id)
        menu.findItem(R.id.removeTeachableTagMenuItem)?.isVisible = TeachableTagsModel.getIsTeachableTag(current.id)
    }

    /** Runs a tag action. True when the item was the refresh action, matching the phone screen. */
    fun handleMenuItem(item: MenuItem): Boolean {
        val current = model.tag
        try {
            if (current != null) {
                when (item.itemId) {
                    R.id.addFavoriteMenuItem -> FavoritesModel.addFavorite(current.id)
                    R.id.removeFavoriteMenuItem -> FavoritesModel.removeFavorite(current.id)
                    R.id.addTeachableTagMenuItem -> TeachableTagsModel.addTeachableTag(current.id)
                    R.id.removeTeachableTagMenuItem -> TeachableTagsModel.removeTeachableTag(current.id)
                    R.id.shareMenuItem -> startActivity(shareIntent)
                    R.id.refreshMenuItem -> {
                        loadQueryItem(true)
                        return true
                    }
                }
            }
        } finally {
            activity?.let { Activities.invalidateOptionsMenu(it) }
            onStateChanged?.invoke()
        }
        return false
    }

    companion object {
        const val INVALID_TAG_ID = -1
        private const val ARG_TAG_ID = "depollsoft.tagmaster.TagDetailFragment.tagId"
        private const val STATE_TAG_ID = "depollsoft.tagmaster.TagDetailFragment.state.tagId"
        private const val REVEAL_DURATION_MS = 200L

        fun newInstance(tagId: Int): TagDetailFragment =
            TagDetailFragment().apply {
                arguments = Bundle().apply { putInt(ARG_TAG_ID, tagId) }
            }
    }
}
