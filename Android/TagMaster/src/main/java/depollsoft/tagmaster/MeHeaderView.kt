package depollsoft.tagmaster

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.bindroid.converters.BoolConverter
import com.bindroid.trackable.Trackable
import com.bindroid.trackable.TrackableBoolean
import com.bindroid.trackable.Tracker
import com.bindroid.ui.UiBinder
import com.bindroid.utils.Function
import com.bindroid.utils.bindTo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import depollsoft.lib.kotlin.ui.safeDismiss
import depollsoft.tagmaster.barbershop.Tag
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Random

class MeHeaderView : LinearLayout {
    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private val loading = TrackableBoolean(false)
    val isLoading: Boolean
        @JvmName("getIsLoading")
        get() = loading.get()

    private var requestScope: CoroutineScope? = null
    private var openTagDialog: AlertDialog? = null
    private var released = false

    // Bindroid registrations are one-shot and each Trackable.track call adds another, so the
    // header keeps exactly one and renews it only once it has fired.
    private var listsTracking = false
    private val trackedLists = mutableListOf<ListModel>()
    private val listsTracker =
        object : Tracker {
            override fun update() {
                listsTracking = false
                if (released) return
                post { rebuildLists() }
            }
        }

    // This view is a RecyclerView item on the home screen, so it can be detached while the user
    // scrolls. In-flight work is therefore tied to the host activity's lifecycle, not to attachment.
    private var observedLifecycle: Lifecycle? = null
    private val lifecycleObserver =
        LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) release()
        }

    private val feedbackHost: View?
        get() =
            (context as? Activity)
                ?.takeIf { !it.isFinishing && !it.isDestroyed }
                ?.findViewById(android.R.id.content)

    private val canShowFeedback: Boolean
        get() = feedbackHost != null

    private fun init() {
        inflate(context, R.layout.meviewheader, this)
        if (isInEditMode) return

        for (id in intArrayOf(
            R.id.browseButton,
            R.id.teachableButton,
            R.id.randomTagButton,
            R.id.openByIdButton,
            R.id.newListButton,
        )) {
            ViewCompat.setAccessibilityDelegate(
                findViewById(id),
                object : AccessibilityDelegateCompat() {
                    override fun onInitializeAccessibilityNodeInfo(
                        host: View,
                        info: AccessibilityNodeInfoCompat,
                    ) {
                        super.onInitializeAccessibilityNodeInfo(host, info)
                        info.className = Button::class.java.name
                    }
                },
            )
        }
        bindTo(R.id.favoritesEmptyText, "Visibility", { FavoritesModel.favoriteIds.size == 0 }, BoolConverter.get())
        UiBinder.bind(this, R.id.randomTagProgress, "Loading", "IsLoading")
        UiBinder.bind(this, R.id.randomTagButton, "Enabled", "IsLoading", BoolConverter.get(true))
        findViewById<View>(R.id.randomTagButton).setOnClickListener { loadRandomTag() }
        findViewById<View>(R.id.browseButton).setOnClickListener {
            context.startActivity(Intent(context, TagBrowserActivity::class.java))
        }
        bindTo(R.id.teachableCount, "Text", { listCountText(context, TagLists.TEACHABLE) })
        findViewById<View>(R.id.teachableButton).setOnClickListener {
            context.startActivity(Intent(context, TeachableTagsActivity::class.java))
        }
        findViewById<View>(R.id.openByIdButton).setOnClickListener { showOpenTagDialog() }
        findViewById<View>(R.id.newListButton).setOnClickListener {
            host()?.let { ListNameDialog.create(it.supportFragmentManager) }
        }
        rebuildLists()
    }

    private fun host(): FragmentActivity? =
        (context as? FragmentActivity)?.takeIf { !it.isFinishing && !it.isDestroyed }

    /**
     * Rebuilds the Lists group from [TagLists] and re-registers for the next change.
     *
     * The rows are plain views rather than another adapter: there are only ever a handful, and
     * this way their name and tag count are ordinary Bindroid bindings that follow a rename or an
     * added tag without anyone rebuilding anything.
     */
    private fun rebuildLists() {
        if (released || isInEditMode) return
        val container = findViewById<LinearLayout>(R.id.listsContainer) ?: return
        val read = Function<List<String>> { TagLists.customKeys.toList() }
        val keys =
            if (listsTracking) {
                read.evaluate()
            } else {
                Trackable.track(listsTracker, read).also { listsTracking = true }
            }
        container.removeAllViews()
        // Keep the models whose counts these rows bind to, so the tracked collections are exactly
        // the ones the next rebuild reads.
        trackedLists.clear()
        keys.mapTo(trackedLists) { ListModel(it) }
        val inflater = LayoutInflater.from(context)
        for (key in keys) {
            val row = inflater.inflate(R.layout.list_row, container, false)
            row.findViewById<AppCompatImageView>(R.id.listRowIcon).setImageResource(listIconRes(key))
            row.bindTo(R.id.listRowName, "Text", { TagLists.name(key) })
            row.bindTo(R.id.listRowDetail, "Text", { listCountText(context, key) })
            row.setOnClickListener { context.startActivity(TagListActivity.intent(context, key)) }
            host()?.let { ListRowMenu.install(it, row, key) }
            container.addView(row)
        }
    }

    private fun loadRandomTag() {
        if (isLoading || !canShowFeedback) return
        loading.set(true)
        requestScope?.cancel()
        requestScope =
            CoroutineScope(Dispatchers.Main + Job()).also { scope ->
                scope.launch {
                    try {
                        val count =
                            Tag
                                .query(
                                    null,
                                    0,
                                    0,
                                    null,
                                    SettingsModel.randomLearningTracksFilter,
                                    SettingsModel.randomSheetMusicFilter,
                                    null,
                                    null,
                                    SettingsModel.minimumRandomTagRating,
                                    SettingsModel.minimumRandomDownloads,
                                    false,
                                    "id",
                                ).await()
                        if (count.available == 0) {
                            feedbackHost?.let { host ->
                                Snackbar
                                    .make(host, R.string.home_random_no_matches, Snackbar.LENGTH_LONG)
                                    .setTextMaxLines(6)
                                    .setAction(R.string.Settings) {
                                        if (canShowFeedback) context.startActivity(Intent(context, SettingsActivity::class.java))
                                    }.show()
                            }
                            return@launch
                        }
                        val chosenNumber = Random().nextInt(count.available)
                        val result =
                            Tag
                                .query(
                                    null,
                                    1,
                                    chosenNumber,
                                    null,
                                    SettingsModel.randomLearningTracksFilter,
                                    SettingsModel.randomSheetMusicFilter,
                                    null,
                                    null,
                                    SettingsModel.minimumRandomTagRating,
                                    SettingsModel.minimumRandomDownloads,
                                    false,
                                    "id",
                                ).await()
                        if (canShowFeedback) openTag(result.tags[0].id)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        feedbackHost?.let { host ->
                            Snackbar
                                .make(host, R.string.home_random_error, Snackbar.LENGTH_LONG)
                                .setAction(R.string.home_retry) { loadRandomTag() }
                                .show()
                        }
                    } finally {
                        loading.set(false)
                    }
                }
            }
    }

    private fun showOpenTagDialog() {
        val content = inflate(context, R.layout.dialog_open_tag, null)
        val input = content.findViewById<TextInputEditText>(R.id.openTagIdInput)
        // Material's 80dp vertical insets leave too little room for a wrapped error
        // above the keyboard on small screens. Keep a 24dp margin instead.
        val verticalInset = (24 * resources.displayMetrics.density).toInt()
        val dialog =
            MaterialAlertDialogBuilder(context)
                .setBackgroundInsetTop(verticalInset)
                .setBackgroundInsetBottom(verticalInset)
                .setTitle(R.string.home_enter_tag_id)
                .setView(content)
                .setPositiveButton(R.string.home_open, null)
                .setNegativeButton(R.string.home_cancel, null)
                .create()

        fun submit(): Boolean {
            val id = input.text?.toString()?.toIntOrNull()
            val field = content.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.openTagIdLayout)
            if (id == null || id <= 0) {
                field.error = context.getString(R.string.home_invalid_tag_id)
                return false
            }
            field.error = null
            if (!canShowFeedback) return false
            openTag(id)
            dialog.safeDismiss()
            return true
        }
        input.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP)
            ) {
                submit()
            } else {
                false
            }
        }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { submit() }
            input.requestFocus()
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
        dialog.setOnDismissListener { openTagDialog = null }
        openTagDialog = dialog
        dialog.show()
    }

    private fun openTag(id: Int) {
        val host = context as? TagPaneHost
        if (host != null && host.hasDetailPane) {
            host.showTag(id)
            return
        }
        val intent = Intent(context, TagDetailActivity::class.java)
        intent.putExtra(TagDetailActivity.TAG_ID_EXTRA, id)
        context.startActivity(intent)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // A recycled header that was released while its host was alive starts observing again.
        if (released && host() != null) {
            released = false
            listsTracking = false
            rebuildLists()
        }
        val lifecycle = findViewTreeLifecycleOwner()?.lifecycle
        if (lifecycle !== observedLifecycle) {
            observedLifecycle?.removeObserver(lifecycleObserver)
            observedLifecycle = lifecycle?.also { it.addObserver(lifecycleObserver) }
        }
    }

    override fun onDetachedFromWindow() {
        // Scrolling this row off screen must not cancel a random-tag request or close the Open Tag
        // dialog; only a finishing host (or one without a lifecycle to observe) releases them here.
        val host = context as? Activity
        if (observedLifecycle == null || host == null || host.isFinishing || host.isDestroyed) release()
        super.onDetachedFromWindow()
    }

    private fun release() {
        released = true
        requestScope?.cancel()
        requestScope = null
        loading.set(false)
        openTagDialog?.safeDismiss()
        openTagDialog = null
        observedLifecycle?.removeObserver(lifecycleObserver)
        observedLifecycle = null
    }
}
