package depollsoft.tagmaster

import android.content.ActivityNotFoundException
import android.content.DialogInterface.OnDismissListener
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.bindroid.converters.BoolConverter
import com.bindroid.converters.ToStringConverter
import com.bindroid.trackable.Trackable
import com.bindroid.trackable.TrackableBoolean
import com.bindroid.trackable.Tracker
import com.bindroid.utils.*
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import depollsoft.lib.util.ContentCache
import depollsoft.tagmaster.lib.RatingConverter
import java.util.*

class TagSummaryFragment : Fragment() {
    private companion object {
        /** A chip shortens past this, instead of crowding out the ones beside it. */
        const val CHIP_MAX_WIDTH_DP = 220
    }

    // The pages sit inside TagDetailFragment (full-screen on phones, in the detail pane on
    // tablets). Fragment.getTag() is final, so the host they bind through is its TagDetailModel.
    val parent: TagDetailHost
        get() = (parentFragment as? TagDetailFragment)?.model ?: (activity as TagDetailHost)
    private val _canRate = TrackableBoolean(true)
    private val sheetMusicLoading = TrackableBoolean(false)
    private val ratingSubmitting = TrackableBoolean(false)
    private var ratingsPopup: RatingsPopup? = null
    val canRate: Boolean
        get() = _canRate.get() && !ratingSubmitting.get() && this.parent.tag != null && !RatingsModel.isRated(this.parent.tag!!.id)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.tagsummaryview, container, false)
        rootView.findViewById<View>(R.id.scrollView1).applyContentInsets(maxWidthRes = R.dimen.two_column_max_width)
        rootView.bindTo(R.id.sheetMusicProgress, "Loading", { sheetMusicLoading.get() })
        rootView.bindTo(R.id.sheetMusicLink, "Enabled", { !sheetMusicLoading.get() })
        rootView.bindTo(R.id.ratingSubmitProgress, "Loading", { ratingSubmitting.get() })

        rootView.bindTo(R.id.titleTextView, "Text", { "${parent.tag?.title}" })
        rootView.bindTo(
            R.id.titleTextView,
            "Visibility",
            { parent.tag?.title },
            BoolConverter.get(),
        )

        rootView.bindTo(R.id.akaTextView, "Text", { parent.tag?.alternativeTitle })
        rootView.bindTo(
            R.id.akaLayout,
            "Visibility",
            { parent.tag?.alternativeTitle },
            BoolConverter.get(),
        )

        rootView.bindTo(R.id.versionTextView, "Text", { parent.tag?.version })
        rootView.bindTo(
            R.id.versionLayout,
            "Visibility",
            { parent.tag?.version },
            BoolConverter.get(),
        )

        rootView.bindTo(R.id.tagIdTextView, "Text", { parent.tag?.id }, ToStringConverter())

        rootView.bindTo(
            R.id.ratingTextView,
            "Text",
            { parent.tag?.rating },
            ToStringConverter("%3.2f"),
        )
        rootView.bindTo(
            R.id.ratingTextView,
            "Visibility",
            { parent.tag?.rating },
            BoolConverter.get(),
        )

        rootView.bindTo(
            R.id.ratingProgressBar,
            "Rating",
            { parent.tag?.rating },
            RatingConverter(),
        )
        rootView.bindTo(
            R.id.rateButton,
            "Enabled",
            { canRate },
            BoolConverter.get(),
        )

        rootView.bindTo(
            R.id.partsTextView,
            "Text",
            { parent.tag?.parts },
            ToStringConverter(),
        )
        rootView.bindTo(
            R.id.partsRow,
            "Visibility",
            { parent.tag?.parts },
            BoolConverter.get(),
        )

        rootView.bindTo(
            R.id.tagTypeTextView,
            "Text",
            { parent.tag?.tagType },
            ToStringConverter(),
        )

        rootView.bindTo(R.id.playKeyNoteButton, "Note", { parent.tag?.keyNote })
        val keyButton = rootView.findViewById<depollsoft.pitchperfect.lib.ui.PitchPipeButton>(R.id.playKeyNoteButton)
        // Native View can reassert Pressed after note replacement; only the actual note owns this fill.
        rootView.bindTo(R.id.playKeyNoteButton, "Activated", { keyButton.note?.isPlaying == true })
        rootView.bindTo(R.id.playKeyNoteButton, "Text", { parent.tag?.writtenKey })
        rootView.bindTo(
            R.id.keyRow,
            "Visibility",
            { parent.tag?.writtenKey },
            BoolConverter.get(),
        )

        rootView.bindTo(
            R.id.classicTagTextView,
            "Text",
            { parent.tag?.classicTagNumber },
            ToStringConverter(),
        )
        rootView.bindTo(
            R.id.classicTagRow,
            "Visibility",
            { parent.tag?.classicTagNumber },
            BoolConverter.get(),
        )

        rootView.bindTo(R.id.notesTextView, "Text", { parent.tag?.notes })
        rootView.bindTo(R.id.notesRow, "Visibility", { parent.tag?.notes }, BoolConverter.get())

        rootView.bindTo(R.id.lyricsTextView, "Text", { parent.tag?.lyrics })
        rootView.bindTo(R.id.lyricsRow, "Visibility", { parent.tag?.lyrics }, BoolConverter.get())

        rootView.bindTo(
            R.id.linearLayout3,
            "Visibility",
            { parent.tag?.sheetMusicUri },
            BoolConverter.get(),
        )

        renderChips(rootView)
        rootView.findViewById<View>(R.id.sheetMusicLink).setOnClickListener {
            loadSheetMusic(rootView)
        }

        rootView.findViewById<View>(R.id.rateButton).setOnClickListener {
            if (!canRate || !isUsable(rootView)) return@setOnClickListener
            val popup = RatingsPopup(requireActivity())
            ratingsPopup = popup
            popup.setOnDismissListener(
                OnDismissListener {
                    ratingsPopup = null
                    val rating = popup.rating ?: return@OnDismissListener
                    if (isUsable(rootView)) submitRating(rootView, rating)
                },
            )
            popup.show()
        }

        return rootView
    }

    // Bindroid registrations are one-shot and each Trackable.track call adds another, so the page
    // holds exactly one and renews it only once it has fired.
    private var chipsTracking = false
    private val trackedLists = mutableListOf<ListModel>()
    private val chipsTracker =
        object : Tracker {
            override fun update() {
                chipsTracking = false
                view?.post { view?.let { renderChips(it) } }
            }
        }

    /**
     * The lists this tag is in, as chips, with Add to list at the end.
     *
     * Every chip is rebuilt on every change rather than diffed: a tag belongs to a handful of
     * lists at most, and a rename, a removal elsewhere or a sync from another device all arrive
     * as the same "the membership changed" signal.
     */
    private fun renderChips(rootView: View) {
        val host = activity ?: return
        val group = rootView.findViewById<ChipGroup>(R.id.savedStatusLayout) ?: return
        // Null means no tag has loaded yet; an empty list means a tag that is in no list.
        // The models whose ids this registers on are kept for as long as the chips are on screen,
        // so the tracked collections are exactly the ones the next render reads.
        val read =
            Function<List<String>?> {
                val current = parent.tag
                trackedLists.clear()
                current?.let { tag ->
                    TagLists.allKeys().filter { key ->
                        ListModel(key).also(trackedLists::add).contains(tag.id)
                    }
                }
            }
        val keys =
            if (chipsTracking) {
                read.evaluate()
            } else {
                Trackable.track(chipsTracker, read).also { chipsTracking = true }
            }
        group.removeAllViews()
        val tagId = parent.tag?.id
        if (keys == null || tagId == null) {
            group.visibility = View.GONE
            return
        }
        group.visibility = View.VISIBLE
        for (key in keys) group.addView(membershipChip(host, rootView, key, tagId))
        group.addView(addToListChip(host, tagId))
    }

    private fun styleChip(
        host: FragmentActivity,
        chip: Chip,
        iconRes: Int,
    ) {
        val foreground =
            ColorStateList.valueOf(
                MaterialColors.getColor(host, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY),
            )
        chip.chipIcon = AppCompatResources.getDrawable(host, iconRes)
        chip.chipIconTint = foreground
        chip.isChipIconVisible = true
        chip.isCheckable = false
        chip.isClickable = true
        chip.isFocusable = true
        // A long list name shortens rather than pushing the row off the screen.
        chip.isSingleLine = true
        chip.ellipsize = TextUtils.TruncateAt.END
        chip.maxWidth = (CHIP_MAX_WIDTH_DP * resources.displayMetrics.density).toInt()
        chip.setEnsureMinTouchTargetSize(true)
    }

    private fun membershipChip(
        host: FragmentActivity,
        rootView: View,
        key: String,
        tagId: Int,
    ): Chip {
        val name = TagLists.displayName(host, key)
        val chip = Chip(host)
        chip.text = if (key == TagLists.TEACHABLE) getString(R.string.list_chip_teachable) else name
        styleChip(host, chip, listIconRes(key))
        chip.closeIcon = AppCompatResources.getDrawable(host, R.drawable.ic_clear)
        chip.closeIconTint =
            ColorStateList.valueOf(
                MaterialColors.getColor(host, com.google.android.material.R.attr.colorOnSurfaceVariant, Color.GRAY),
            )
        chip.isCloseIconVisible = true
        val removeLabel = getString(R.string.list_chip_remove, name)
        chip.closeIconContentDescription = removeLabel
        chip.setOnClickListener { openList(host, key) }
        chip.setOnCloseIconClickListener { removeFromList(rootView, key, tagId, name) }
        // The close icon is not a separate node for a screen reader, so the same action is offered
        // on the chip itself.
        ViewCompat.addAccessibilityAction(chip, removeLabel) { _, _ ->
            removeFromList(rootView, key, tagId, name)
            true
        }
        return chip
    }

    private fun addToListChip(
        host: FragmentActivity,
        tagId: Int,
    ): Chip {
        val chip = Chip(host)
        chip.setText(R.string.list_add_to_list)
        styleChip(host, chip, R.drawable.ic_add)
        chip.setOnClickListener { ListPickerDialog.show(host.supportFragmentManager, tagId) }
        return chip
    }

    private fun openList(
        host: FragmentActivity,
        key: String,
    ) {
        val intent =
            when (key) {
                // Home is the root of the task; return to it rather than stacking another copy.
                TagLists.FAVORITE ->
                    Intent(host, MeActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                TagLists.TEACHABLE -> Intent(host, TeachableTagsActivity::class.java)
                else -> TagListActivity.intent(host, key)
            }
        startActivity(intent)
    }

    /** Removes the tag from [key], offering the place it came from back for a few seconds. */
    private fun removeFromList(
        rootView: View,
        key: String,
        tagId: Int,
        name: String,
    ) {
        val model = ListModel(key)
        val index = model.ids.indexOf(tagId)
        if (index < 0) return
        model.remove(tagId)
        if (!isUsable(rootView)) return
        Snackbar
            .make(rootView, getString(R.string.list_removed_from, name), Snackbar.LENGTH_LONG)
            .setAction(R.string.list_undo) {
                val current = ListModel(key)
                if (current.contains(tagId)) return@setAction
                // ListModel has no insert-at, so the tag goes back on the end and then home.
                current.add(tagId)
                if (index < current.ids.size) current.move(tagId, index)
            }.show()
    }

    private fun isUsable(rootView: View): Boolean {
        val host = activity ?: return false
        return view === rootView && rootView.isAttachedToWindow && !host.isFinishing && !host.isDestroyed
    }

    private fun loadSheetMusic(rootView: View) {
        if (!isUsable(rootView) || sheetMusicLoading.get()) return
        val host = requireActivity()
        val tag = parent.tag ?: return
        val location = tag.sheetMusicUri ?: return
        val sheetMusicType = location.type
        val sheetMusicUri = location.uri
        sheetMusicLoading.set(true)

        ContentCache(host).loadContentPublic(sheetMusicUri, sheetMusicType, false).continueWith { task ->
            host.runOnUiThread {
                if (!isUsable(rootView)) return@runOnUiThread
                sheetMusicLoading.set(false)
                if (task.isFaulted || task.isCancelled) {
                    showSheetMusicError(rootView)
                    return@runOnUiThread
                }
                try {
                    val contentPath = (
                        "content://${host.packageName}/" + sheetMusicType + "/" +
                            Base64.encodeToString(sheetMusicUri.toByteArray(), Base64.URL_SAFE) +
                            "/" + tag.id + "." + sheetMusicType
                    )
                    val path = Uri.parse(contentPath)
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.putExtra("tagId", tag.id)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    intent.setClass(host, SheetMusicActivity::class.java)
                    val mimeType =
                        if (sheetMusicType.lowercase(Locale.US) == "pdf") {
                            "application/pdf"
                        } else {
                            MimeTypeMap.getSingleton().getMimeTypeFromExtension(sheetMusicType.lowercase(Locale.US))
                        }
                    intent.setDataAndType(path, mimeType)
                    host.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Snackbar.make(rootView, getString(R.string.detail_sheet_music_no_app, sheetMusicType), Snackbar.LENGTH_LONG).show()
                } catch (e: Exception) {
                    showSheetMusicError(rootView)
                }
            }
            null
        }
    }

    private fun showSheetMusicError(rootView: View) {
        if (!isUsable(rootView)) return
        Snackbar
            .make(rootView, R.string.detail_sheet_music_failed, Snackbar.LENGTH_LONG)
            .setAction(R.string.detail_retry) { loadSheetMusic(rootView) }
            .show()
    }

    private fun submitRating(
        rootView: View,
        rating: Int,
    ) {
        if (!isUsable(rootView) || !canRate) return
        val host = requireActivity()
        val tag = parent.tag ?: return
        ratingSubmitting.set(true)
        tag.rate(rating).continueWith { task ->
            host.runOnUiThread {
                // Persist a successful submission even if the user has left this view.
                if (!task.isFaulted && !task.isCancelled) RatingsModel.addRating(tag.id)
                if (!isUsable(rootView)) return@runOnUiThread
                ratingSubmitting.set(false)
                if (task.isFaulted || task.isCancelled) {
                    Snackbar
                        .make(rootView, R.string.detail_rating_failed, Snackbar.LENGTH_LONG)
                        .setAction(R.string.detail_retry) { submitRating(rootView, rating) }
                        .show()
                } else {
                    _canRate.set(false)
                }
            }
            null
        }
    }

    override fun onDestroyView() {
        chipsTracking = false
        ratingsPopup?.setOnDismissListener(null)
        ratingsPopup?.dismiss()
        ratingsPopup = null
        sheetMusicLoading.set(false)
        ratingSubmitting.set(false)
        super.onDestroyView()
    }
}
