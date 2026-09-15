package depollsoft.tagmaster

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bindroid.converters.AdapterConverter
import com.bindroid.converters.BoolConverter
import com.bindroid.ui.UiBinder

class TagVideosFragment : Fragment() {
    // The pages sit inside TagDetailFragment (full-screen on phones, in the detail pane on
    // tablets). Fragment.getTag() is final, so the host they bind through is its TagDetailModel.
    val parent: TagDetailHost
        get() = (parentFragment as? TagDetailFragment)?.model ?: (activity as TagDetailHost)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val rootView = inflater.inflate(R.layout.tagvideosview, container, false)
        rootView.applyContentInsets(bottom = false)
        rootView.findViewById<View>(R.id.videoList).applyBottomInsetsAsPadding()

        UiBinder.bind(
            rootView,
            R.id.videoList,
            "Adapter",
            this,
            "Parent.Tag.Videos",
            AdapterConverter(VideoDisplay::class.java),
        )

        UiBinder.bind(
            rootView,
            R.id.teachingVideoRow,
            "Visibility",
            this,
            "Parent.Tag.TeachingVideo",
            BoolConverter.get(),
        )

        UiBinder.bind(
            rootView,
            R.id.sorryTextView,
            "Visibility",
            this,
            "Parent.Tag.Videos",
            BoolConverter.get(true, true),
        )

        UiBinder.bind(rootView, R.id.teachingVideoDisplay, "Tag", this, "Parent.Tag")

        return rootView
    }
}
