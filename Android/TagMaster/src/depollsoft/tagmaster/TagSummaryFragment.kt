package depollsoft.tagmaster

import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.DialogInterface.OnDismissListener
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.bindroid.BindingMode
import com.bindroid.converters.BoolConverter
import com.bindroid.converters.ToStringConverter
import com.bindroid.trackable.TrackableBoolean
import com.bindroid.ui.UiBinder
import com.bindroid.utils.Function
import com.bindroid.utils.Property
import com.bindroid.utils.ReflectedProperty
import depollsoft.lib.ui.Hyperlink
import depollsoft.lib.util.ContentCache
import depollsoft.tagmaster.lib.RatingConverter
import java.util.*

class TagSummaryFragment : Fragment() {
    val parent: TagDetailActivity
        get() = this.activity as TagDetailActivity
    private val _canRate = TrackableBoolean(true)
    val canRate: Boolean
        get() = _canRate.get() && !RatingsModel.isRated(this.parent.tag!!.id)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.tagsummaryview, container, false)

        UiBinder.bind(rootView, R.id.titleTextView, "Text", this, "Parent.Tag.Title")
        UiBinder.bind(rootView, R.id.titleTextView, "Visibility", this, "Parent.Tag.Title", BoolConverter.get())

        UiBinder.bind(rootView, R.id.akaTextView, "Text", this, "Parent.Tag.AlternativeTitle")
        UiBinder.bind(rootView, R.id.akaLayout, "Visibility", this, "Parent.Tag.AlternativeTitle",
                BoolConverter.get())

        UiBinder.bind(rootView, R.id.versionTextView, "Text", this, "Parent.Tag.Version")
        UiBinder
                .bind(rootView, R.id.versionLayout, "Visibility", this, "Parent.Tag.Version", BoolConverter.get())

        UiBinder.bind(rootView, R.id.ratingTextView, "Text", this, "Parent.Tag.Rating", ToStringConverter(
                "%3.2f"))
        UiBinder
                .bind(rootView, R.id.ratingTextView, "Visibility", this, "Parent.Tag.Rating", BoolConverter.get())
        UiBinder.bind(rootView, R.id.ratingProgressBar, "Progress", this, "Parent.Tag.Rating",
                RatingConverter())
        UiBinder.bind(rootView, R.id.rateButton, "Enabled", this, "CanRate", BoolConverter.get())

        UiBinder.bind(rootView, R.id.partsTextView, "Text", this, "Parent.Tag.Parts", ToStringConverter())
        UiBinder.bind(rootView, R.id.partsRow, "Visibility", this, "Parent.Tag.Parts", BoolConverter.get())

        UiBinder
                .bind(rootView, R.id.tagTypeTextView, "Text", this, "Parent.Tag.TagType", ToStringConverter())

        UiBinder.bind(rootView, R.id.playKeyNoteButton, "Note", this, "Parent.Tag.KeyNote")
        UiBinder.bind(rootView, R.id.playKeyNoteButton, "Text", this, "Parent.Tag.WrittenKey")
        UiBinder.bind(rootView, R.id.keyRow, "Visibility", this, "Parent.Tag.WrittenKey", BoolConverter.get())

        UiBinder.bind(rootView, R.id.classicTagTextView, "Text", this, "Parent.Tag.ClassicTagNumber",
                ToStringConverter())
        UiBinder.bind(rootView, R.id.classicTagRow, "Visibility", this, "Parent.Tag.ClassicTagNumber",
                BoolConverter.get())

        UiBinder.bind(rootView, R.id.notesTextView, "Text", this, "Parent.Tag.Notes")
        UiBinder.bind(rootView, R.id.notesRow, "Visibility", this, "Parent.Tag.Notes", BoolConverter.get())

        UiBinder.bind(rootView, R.id.lyricsTextView, "Text", this, "Parent.Tag.Lyrics")
        UiBinder.bind(rootView, R.id.lyricsRow, "Visibility", this, "Parent.Tag.Lyrics", BoolConverter.get())

        UiBinder.bind(rootView, R.id.sheetMusicLink, "HyperlinkUri", this, "Parent.Tag.SheetMusicUri.Uri")
        UiBinder.bind(rootView, R.id.sheetMusicLink, "Visibility", this, "Parent.Tag.SheetMusicUri",
                BoolConverter.get())

        UiBinder.bind(ReflectedProperty(rootView.findViewById(R.id.favoriteMarkerTextView),
                "Visibility"), Property(Function {
            FavoritesModel.getIsFavorite(parent.tag!!.id)
        }, null, Boolean::class.java), BindingMode.ONE_WAY, BoolConverter.get())
        UiBinder.bind(ReflectedProperty(rootView.findViewById(R.id.teachableMarkerTextView),
                "Visibility"), Property(Function {
            TeachableTagsModel.getIsTeachableTag(parent.tag!!.id)
        }, null, Boolean::class.java), BindingMode.ONE_WAY, BoolConverter.get())

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
                        Toast.makeText(parent,
                                "Unable to load sheet music.  Please try again later.", Toast.LENGTH_SHORT)
                                .show()
                        progress.dismiss()
                    }
                } else {
                    try {
                        val contentPath = ("content://depollsoft.tagmaster/" + sheetMusicType + "/"
                                + Base64.encodeToString(sheetMusicUri.toByteArray(), Base64.URL_SAFE) +
                                "/" + tag.id + "." + sheetMusicType)
                        val path = Uri.parse(contentPath)
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.putExtra("tagId", tag!!.id)
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        intent.setClass(requireContext(), SheetMusicActivity::class.java)

                        if (sheetMusicType.toLowerCase(Locale.US) == "pdf") {
                            intent.setDataAndType(path, "application/pdf")
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        } else {
                            val map = MimeTypeMap.getSingleton()
                            val mimeType = map.getMimeTypeFromExtension(sheetMusicType
                                    .toLowerCase(Locale.US))
                            intent.setDataAndType(path, mimeType)
                        }
                        try {
                            parent.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            parent.runOnUiThread {
                                Toast.makeText(
                                        parent,
                                        "No application available to view this sheet music (" + sheetMusicType
                                                + ").", Toast.LENGTH_SHORT).show()
                            }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        progress.dismiss()
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
                            Toast.makeText(parent,
                                    "Failed to submit rating.  Please try again later.", Toast.LENGTH_SHORT)
                                    .show()
                            pd.dismiss()
                        }
                    } else {
                        RatingsModel.addRating(tag.id)
                        _canRate.set(false)
                        pd.dismiss()
                    }
                    null
                }
            })
            popup.show()
        }

        return rootView
    }
}
