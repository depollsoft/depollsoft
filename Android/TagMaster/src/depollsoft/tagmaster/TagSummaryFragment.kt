package depollsoft.tagmaster

import android.app.ProgressDialog
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
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bindroid.converters.BoolConverter
import com.bindroid.converters.ToStringConverter
import com.bindroid.trackable.TrackableBoolean
import com.bindroid.ui.UiBinder
import com.bindroid.utils.*
import depollsoft.lib.kotlin.ui.safeDismiss
import depollsoft.lib.ui.Hyperlink
import depollsoft.lib.util.ContentCache
import depollsoft.tagmaster.lib.RatingConverter
import java.util.*

class TagSummaryFragment : Fragment() {
    val parent: TagDetailActivity
        get() = this.activity as TagDetailActivity
    private val _canRate = TrackableBoolean(true)
    val canRate: Boolean
        get() = _canRate.get() && this.parent.tag != null && !RatingsModel.isRated(this.parent.tag!!.id)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.tagsummaryview, container, false)

        rootView.bindTo(R.id.titleTextView, "Text", { "${parent.tag?.title}" })
        rootView.bindTo(
            R.id.titleTextView,
            "Visibility",
            { parent.tag?.title },
            BoolConverter.get()
        )

        rootView.bindTo(R.id.akaTextView, "Text", { parent.tag?.alternativeTitle })
        rootView.bindTo(
            R.id.akaLayout,
            "Visibility",
            { parent.tag?.alternativeTitle },
            BoolConverter.get()
        )

        rootView.bindTo(R.id.versionTextView, "Text", { parent.tag?.version })
        rootView.bindTo(
            R.id.versionLayout,
            "Visibility",
            { parent.tag?.version },
            BoolConverter.get()
        )

        rootView.bindTo(R.id.tagIdTextView, "Text", { parent.tag?.id }, ToStringConverter())

        rootView.bindTo(
            R.id.ratingTextView,
            "Text",
            { parent.tag?.rating },
            ToStringConverter("%3.2f")
        )
        rootView.bindTo(
            R.id.ratingTextView,
            "Visibility",
            { parent.tag?.rating },
            BoolConverter.get()
        )

        rootView.bindTo(
            R.id.ratingProgressBar,
            "Progress",
            { parent.tag?.rating },
            RatingConverter()
        )
        rootView.bindTo(
            R.id.rateButton,
            "Enabled",
            { canRate },
            BoolConverter.get()
        )

        rootView.bindTo(
            R.id.partsTextView,
            "Text",
            { parent.tag?.parts },
            ToStringConverter()
        )
        rootView.bindTo(
            R.id.partsRow,
            "Visibility",
            { parent.tag?.parts },
            BoolConverter.get()
        )

        rootView.bindTo(
            R.id.tagTypeTextView,
            "Text",
            { parent.tag?.tagType },
            ToStringConverter()
        )

        rootView.bindTo(R.id.playKeyNoteButton, "Note", { parent.tag?.keyNote })
        rootView.bindTo(R.id.playKeyNoteButton, "Text", { parent.tag?.writtenKey })
        rootView.bindTo(
            R.id.keyRow,
            "Visibility",
            { parent.tag?.writtenKey },
            BoolConverter.get()
        )

        rootView.bindTo(
            R.id.classicTagTextView,
            "Text",
            { parent.tag?.classicTagNumber },
            ToStringConverter()
        )
        rootView.bindTo(
            R.id.classicTagRow,
            "Visibility",
            { parent.tag?.classicTagNumber },
            BoolConverter.get()
        )

        rootView.bindTo(R.id.notesTextView, "Text", { parent.tag?.notes })
        rootView.bindTo(R.id.notesRow, "Visibility", { parent.tag?.notes }, BoolConverter.get())

        rootView.bindTo(R.id.lyricsTextView, "Text", { parent.tag?.lyrics })
        rootView.bindTo(R.id.lyricsRow, "Visibility", { parent.tag?.lyrics }, BoolConverter.get())

        rootView.bindTo(R.id.sheetMusicLink, "HyperlinkUri", { parent.tag?.sheetMusicUri?.uri })
        rootView.bindTo(
            R.id.sheetMusicLink,
            "Visibility",
            { parent.tag?.sheetMusicUri },
            BoolConverter.get()
        )

        rootView.bindTo(
            R.id.favoriteMarkerTextView,
            "Visibility",
            { FavoritesModel.getIsFavorite(parent.tag!!.id) },
            BoolConverter.get()
        )
        rootView.bindTo(
            R.id.teachableMarkerTextView,
            "Visibility",
            { TeachableTagsModel.getIsTeachableTag(parent.tag!!.id) },
            BoolConverter.get()
        )

        val link = rootView.findViewById(R.id.sheetMusicLink) as Hyperlink
        link.setOnClickListener {
            val tag = parent.tag
            val sheetMusicType = tag!!.sheetMusicUri!!.type
            val sheetMusicUri = tag.sheetMusicUri!!.uri
            val progress = ProgressDialog(parent)
            progress.isIndeterminate = true
            progress.setMessage("Loading...")
            progress.show()

            val cache = ContentCache(parent)
            cache.loadContentPublic(sheetMusicUri, sheetMusicType, false).continueWith { task ->
                if (task.isFaulted) {
                    parent.runOnUiThread {
                        Toast.makeText(
                            parent,
                            "Unable to load sheet music.  Please try again later.",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        progress.safeDismiss()
                    }
                } else {
                    try {
                        val contentPath = ("content://depollsoft.tagmaster/" + sheetMusicType + "/"
                                + Base64.encodeToString(
                            sheetMusicUri.toByteArray(),
                            Base64.URL_SAFE
                        ) +
                                "/" + tag.id + "." + sheetMusicType)
                        val path = Uri.parse(contentPath)
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.putExtra("tagId", tag.id)
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        intent.setClass(requireContext(), SheetMusicActivity::class.java)

                        if (sheetMusicType.toLowerCase(Locale.US) == "pdf") {
                            intent.setDataAndType(path, "application/pdf")
                        } else {
                            val map = MimeTypeMap.getSingleton()
                            val mimeType = map.getMimeTypeFromExtension(
                                sheetMusicType
                                    .toLowerCase(Locale.US)
                            )
                            intent.setDataAndType(path, mimeType)
                        }
                        try {
                            parent.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            parent.runOnUiThread {
                                Toast.makeText(
                                    parent,
                                    "No application available to view this sheet music (" + sheetMusicType
                                            + ").", Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        progress.safeDismiss()
                    }
                }
                null
            }
        }

        val rateButton = rootView.findViewById<View>(R.id.rateButton)
        rateButton.setOnClickListener {
            if (!canRate) return@setOnClickListener
            val popup = RatingsPopup(parent)
            popup.setOnDismissListener(OnDismissListener {
                if (popup.rating == null)
                    return@OnDismissListener
                val tag = parent.tag
                val pd = ProgressDialog(parent)
                pd.isIndeterminate = true
                pd.setMessage("Submitting rating...")
                pd.show()
                tag!!.rate(popup.rating!!).continueWith { task ->
                    if (task.isFaulted) {
                        parent.runOnUiThread {
                            Toast.makeText(
                                parent,
                                "Failed to submit rating.  Please try again later.",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            pd.safeDismiss()
                        }
                    } else {
                        RatingsModel.addRating(tag.id)
                        _canRate.set(false)
                        pd.safeDismiss()
                    }
                    null
                }
            })
            popup.show()
        }

        return rootView
    }
}
