package depollsoft.tagmaster

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bindroid.converters.BoolConverter
import com.bindroid.converters.ToStringConverter
import com.bindroid.ui.UiBinder

class TagMiscFragment : Fragment() {
    val parent: TagDetailActivity
        get() = this.activity as TagDetailActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.tagmiscview, container, false)

        UiBinder.bind(rootView, R.id.titleTextView, "Text", this, "Parent.Tag.Title")

        UiBinder.bind(rootView, R.id.tagIdTextView, "Text", this, "Parent.Tag.Id", ToStringConverter())

        UiBinder.bind(rootView, R.id.lastRefreshedTextView, "Text", this, "Parent.Tag.LastRefreshed",
                ToStringConverter("%1\$tD %1\$tr"))

        UiBinder.bind(rootView, R.id.downloadsTextView, "Text", this, "Parent.Tag.DownloadCount",
                ToStringConverter())

        UiBinder.bind(rootView, R.id.linkHyperlink, "HyperlinkUri", this, "Parent.Tag.TagUri")

        UiBinder.bind(rootView, R.id.postedByTextView, "Text", this, "Parent.Tag.Provider")
        UiBinder.bind(rootView, R.id.postedByTextView, "HyperlinkUri", this, "Parent.Tag.ProviderWebsite")
        UiBinder.bind(rootView, R.id.postedByRow, "Visibility", this, "Parent.Tag.Provider", BoolConverter.get())

        UiBinder.bind(rootView, R.id.postedTextView, "Text", this, "Parent.Tag.Posted", ToStringConverter(
                "%1\$tA, %1\$tB %1\$te, %1\$tY"))

        UiBinder.bind(rootView, R.id.arrangedByTextView, "Text", this, "Parent.Tag.Arranger")
        UiBinder.bind(rootView, R.id.arrangedByTextView, "HyperlinkUri", this, "ArrangerWebsite")
        UiBinder.bind(rootView, R.id.arrangedByRow, "Visibility", this, "Parent.Tag.Arranger",
                BoolConverter.get())

        UiBinder.bind(rootView, R.id.yearArrangedTextView, "Text", this, "Parent.Tag.YearArranged",
                ToStringConverter())
        UiBinder.bind(rootView, R.id.yearArrangedRow, "Visibility", this, "Parent.Tag.YearArranged",
                BoolConverter.get())

        UiBinder.bind(rootView, R.id.sungByTextView, "Text", this, "Parent.Tag.SungBy")
        UiBinder.bind(rootView, R.id.sungByTextView, "HyperlinkUri", this, "Parent.Tag.SungByWebsite")
        UiBinder.bind(rootView, R.id.sungByRow, "Visibility", this, "Parent.Tag.SungBy", BoolConverter.get())

        UiBinder.bind(rootView, R.id.yearSungTextView, "Text", this, "Parent.Tag.SungYear",
                ToStringConverter())
        UiBinder.bind(rootView, R.id.yearSungRow, "Visibility", this, "Parent.Tag.SungYear", BoolConverter.get())

        return rootView
    }
}
