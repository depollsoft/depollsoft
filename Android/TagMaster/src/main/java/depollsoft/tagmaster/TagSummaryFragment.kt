package depollsoft.tagmaster

import android.content.ActivityNotFoundException
import android.content.DialogInterface.OnDismissListener
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.fragment.app.Fragment
import com.bindroid.converters.BoolConverter
import com.bindroid.converters.ToStringConverter
import com.bindroid.trackable.TrackableBoolean
import com.bindroid.utils.*
import com.google.android.material.snackbar.Snackbar
import depollsoft.lib.util.ContentCache
import depollsoft.tagmaster.lib.RatingConverter
import java.util.*

class TagSummaryFragment : Fragment() {
    val parent: TagDetailActivity
        get() = this.activity as TagDetailActivity
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

        rootView.bindTo(
            R.id.favoriteMarkerTextView,
            "Visibility",
            { parent.tag?.let { FavoritesModel.getIsFavorite(it.id) } ?: false },
            BoolConverter.get(),
        )
        rootView.bindTo(
            R.id.teachableMarkerTextView,
            "Visibility",
            { parent.tag?.let { TeachableTagsModel.getIsTeachableTag(it.id) } ?: false },
            BoolConverter.get(),
        )

        rootView.bindTo(
            R.id.savedStatusLayout,
            "Visibility",
            { parent.tag?.let { FavoritesModel.getIsFavorite(it.id) || TeachableTagsModel.getIsTeachableTag(it.id) } ?: false },
            BoolConverter.get(),
        )
        rootView.findViewById<View>(R.id.sheetMusicLink).setOnClickListener {
            loadSheetMusic(rootView)
        }

        rootView.findViewById<View>(R.id.rateButton).setOnClickListener {
            if (!canRate || !isUsable(rootView)) return@setOnClickListener
            val popup = RatingsPopup(parent)
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

    private fun isUsable(rootView: View): Boolean {
        val host = activity ?: return false
        return view === rootView && rootView.isAttachedToWindow && !host.isFinishing && !host.isDestroyed
    }

    private fun loadSheetMusic(rootView: View) {
        if (!isUsable(rootView) || sheetMusicLoading.get()) return
        val host = parent
        val tag = host.tag ?: return
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
        val host = parent
        val tag = host.tag ?: return
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
        ratingsPopup?.setOnDismissListener(null)
        ratingsPopup?.dismiss()
        ratingsPopup = null
        sheetMusicLoading.set(false)
        ratingSubmitting.set(false)
        super.onDestroyView()
    }
}
