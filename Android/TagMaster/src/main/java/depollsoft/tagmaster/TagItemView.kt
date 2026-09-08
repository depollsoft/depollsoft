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
import com.bindroid.utils.bindTo
import depollsoft.tagmaster.barbershop.Tag

class TagItemView :
    LinearLayout,
    BoundUi<Tag?> {
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
        val inflater =
            this.context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE,
            ) as LayoutInflater
        inflater.inflate(R.layout.tagitemview, this, true)
        val background = android.util.TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, background, true)
        setBackgroundResource(background.resourceId)
        this.isFocusable = true
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
        bindTo(R.id.titleTextView, "Text", { tag?.title })
        bindTo(
            R.id.akaTextView,
            "Text",
            { tag?.alternativeTitle },
            ToStringConverter(context.getString(R.string.alternative_title_format)),
        )
        bindTo(
            R.id.akaTextView,
            "Visibility",
            { tag?.alternativeTitle },
            BoolConverter.get(),
        )
        bindTo(R.id.idTextView, "Text", { tag?.id }, ToStringConverter())
        bindTo(
            R.id.ratingTextView,
            "Text",
            { tag?.rating },
            ToStringConverter("%3.2f"),
        )
        bindTo(
            R.id.ratingContainer,
            "Visibility",
            { tag?.rating },
            BoolConverter.get(),
        )
        bindTo(R.id.postedTextView, "Text", { tag?.posted }, NullableDateConverter(" %tD"))
        bindTo(R.id.postedContainer, "Visibility", { tag?.posted }, BoolConverter.get())
        bindTo(R.id.downloadsTextView, "Text", { tag?.downloadCount }, ToStringConverter("%d"))
        bindTo(R.id.downloadsContainer, "Visibility", { tag?.downloadCount }, BoolConverter.get())
        bindTo(R.id.sheetMusicCheckBox, "IsAvailable", { tag?.sheetMusicUri }, BoolConverter.get())
        bindTo(
            R.id.learningTracksCheckBox,
            "IsAvailable",
            { tag?.tracks },
            BoolConverter.get(false, true),
        )
    }
}
