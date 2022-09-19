package depollsoft.tagmaster

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.bindroid.converters.AdapterConverter
import com.bindroid.converters.BoolConverter
import com.bindroid.ui.UiBinder

class TagVideosFragment : Fragment() {
    val parent: TagDetailActivity
        get() = this.activity as TagDetailActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.tagvideosview, container, false)

        UiBinder.bind(rootView, R.id.videoList, "Adapter", this, "Parent.Tag.Videos", AdapterConverter(
                VideoDisplay::class.java))

        UiBinder.bind(rootView, R.id.teachingVideoRow, "Visibility", this, "Parent.Tag.TeachingVideo",
                BoolConverter.get())

        UiBinder.bind(rootView, R.id.sorryTextView, "Visibility", this, "Parent.Tag.Videos",
                BoolConverter.get(true, true))

        UiBinder.bind(rootView, R.id.teachingVideoDisplay, "Tag", this, "Parent.Tag")

        return rootView
    }
}
