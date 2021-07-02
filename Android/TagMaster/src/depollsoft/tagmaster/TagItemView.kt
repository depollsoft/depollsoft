package depollsoft.tagmaster

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.bindroid.converters.BoolConverter
import com.bindroid.converters.ToStringConverter
import com.bindroid.trackable.trackable
import com.bindroid.ui.BoundUi
import com.bindroid.ui.UiBinder
import com.bindroid.utils.compiledProp
import com.bindroid.utils.uibind
import depollsoft.tagmaster.TagDetailActivity
import depollsoft.tagmaster.barbershop.Tag

class TagItemView : LinearLayout, BoundUi<Tag?> {
    var tag: Tag? by trackable()

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    override fun bind(dataSource: Tag?) {
        this.tag = dataSource
    }

    private fun init() {
        val inflater = this.context.getSystemService(
            Context.LAYOUT_INFLATER_SERVICE
        ) as LayoutInflater
        inflater.inflate(R.layout.tagitemview, this, true)
        this.isClickable = true
        this.isLongClickable = false
        setOnClickListener {
            if (this@TagItemView.tag != null) {
                val i = Intent(this@TagItemView.context, TagDetailActivity::class.java)
                val b = Bundle()
                b.putInt(TagDetailActivity.TAG_ID_EXTRA, this@TagItemView.tag!!.id)
                i.putExtras(b)
                this@TagItemView.context.startActivity(i)
            }
        }
        setOnLongClickListener { false }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        uibind(R.id.titleTextView, "Text", compiledProp { tag!!::title })
        uibind(
            R.id.akaTextView,
            "Text",
            { tag!!::alternativeTitle },
            converter = ToStringConverter(
                "a.k.a. %s"
            )
        )
        uibind(
            R.id.akaTextView,
            "Visibility",
            { tag!!::alternativeTitle },
            converter = BoolConverter.get()
        )
        uibind(R.id.idTextView, "Text", { tag!!::id }, converter = ToStringConverter())
        uibind(
            R.id.ratingTextView,
            "Text",
            { tag!!::rating },
            converter = ToStringConverter("%3.2f")
        )
        uibind(
            R.id.ratingContainer,
            "Visibility",
            { tag!!::rating },
            converter = BoolConverter.get()
        )
        UiBinder.bind(this, R.id.postedTextView, "Text", "Tag.Posted", ToStringConverter(" %tD"))
        UiBinder.bind(
            this, R.id.downloadsTextView, "Text", "Tag.DownloadCount", ToStringConverter(
                " %d"
            )
        )
        UiBinder.bind(
            this, R.id.downloadsContainer, "Visibility", "Tag.DownloadCount",
            BoolConverter.get()
        )
        UiBinder.bind(
            this, R.id.sheetMusicCheckBox, "Checked", "Tag.SheetMusicUri",
            BoolConverter.get()
        )
        UiBinder.bind(
            this, R.id.learningTracksCheckBox, "Checked", "Tag.Tracks",
            BoolConverter.get(false, true)
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }
}